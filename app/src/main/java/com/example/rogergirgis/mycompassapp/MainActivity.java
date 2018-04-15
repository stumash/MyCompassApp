package com.example.rogergirgis.mycompassapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

// sensor tutorial:
// https://developer.android.com/reference/android/hardware/SensorManager.html#getOrientation(float[],%20float[])
// https://www.youtube.com/watch?v=nOQxq2YpEjQ

// camera tutorial:
// https://www.youtube.com/watch?v=CuvVpsFc77w&t=2s

public class MainActivity
        extends AppCompatActivity
        implements SensorEventListener,
                   ActivityCompat.OnRequestPermissionsResultCallback
{
    // all variables related to sensor reading and writing
    //=====================================================
    private TextView xAccelText, yAccelText, zAccelText;
    private TextView xMagnText, yMagnText, zMagnText;
    private TextView xOrientationText, yOrientationText, zOrientationText;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    private String xAccel;
    private String yAccel;
    private String zAccel;

    private Context context;
    private String path;
    private String directory;
    private String csv;
    private File file;
    private FileWriter file_writer;

    private volatile long startTime = System.currentTimeMillis();
    private volatile long currTime = System.currentTimeMillis();
    private volatile long timeOfLastUpdate = 0L;
    private volatile long timeSinceLastUpdate = 0L;
    private volatile long timeSinceStart = 0L;

    // all variables related to camera viewing and saving
    //====================================================
    private File mPictureFolder;

    private CameraDevice mCameraDevice;
    private String mCameraId;
    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private static final int MY_REQUEST_CAMERA_PERMISSION_CODE = 0;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };
    SurfaceTexture mSurfaceTexture;
    Surface mPreviewSurface;
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private static SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }
    private static class CompareSizeByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() / (long) rhs.getWidth() * rhs.getHeight());
        }
    }


    // METHODS
    //====================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // initialization for sensors
        xAccelText = findViewById(R.id.xAccelTV);
        yAccelText = findViewById(R.id.yAccelTV);
        zAccelText = findViewById(R.id.zAccelTV);

        xMagnText = findViewById(R.id.xMagnTV);
        yMagnText = findViewById(R.id.yMagnTV);
        zMagnText = findViewById(R.id.zMagnTV);

        xOrientationText = findViewById(R.id.xOrientationTV);
        yOrientationText = findViewById(R.id.yOrientationTV);
        zOrientationText = findViewById(R.id.zOrientationTV);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // get permissions to write to external storage if not already granted by user
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1010);
        }

        // open file writer
        context = getApplicationContext();
        csv = "AnalysisData.csv";
        directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        path = directory + File.separator + csv;
        file = new File(path);
        try {
            file_writer = new FileWriter(file, false);
        }
        catch (IOException e){
            e.printStackTrace();
        }

        createPictureFolder();

        mTextureView = (TextureView) findViewById(R.id.textureView);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_REQUEST_CAMERA_PERMISSION_CODE: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(),
                            "Application won't run without camera permissions",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
            connectCamera();
        }
        else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        try {
            file_writer.close();
        }
        catch(Exception e) {e.printStackTrace();}

        closeCamera();

        stopBackgroundThread();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        currTime = System.currentTimeMillis();
        timeSinceStart = currTime - startTime;
        timeSinceLastUpdate = currTime - timeOfLastUpdate;

        if (sensorEvent.sensor == mAccelerometer) {
            xAccelText.setText("X:" + sensorEvent.values[0]);
            yAccelText.setText("Y:" + sensorEvent.values[1]);
            zAccelText.setText("Z:" + sensorEvent.values[2]);
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        }
        else if (sensorEvent.sensor == mMagnetometer) {
            xMagnText.setText(("X:" + sensorEvent.values[0]));
            yMagnText.setText(("Y:" + sensorEvent.values[1]));
            zMagnText.setText(("Z:" + sensorEvent.values[2]));
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }

        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float xOrientationDeg = convertToDegrees(mOrientation[0]);
            float yOrientationDeg = convertToDegrees(mOrientation[1]);
            float zOrientationDeg = convertToDegrees(mOrientation[2]);
            xOrientationText.setText("X: " + xOrientationDeg);
            yOrientationText.setText("Y: " + yOrientationDeg);
            zOrientationText.setText("Z: " + zOrientationDeg);
        }

        //Record orientation co-ords 5 times a second, in order to properly test against captured video
        if ((timeSinceLastUpdate > 50) && (sensorEvent.sensor == mAccelerometer)) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            timeOfLastUpdate = currTime;

            xAccel = String.valueOf(sensorEvent.values[0]);
            yAccel = String.valueOf(sensorEvent.values[1]);
            zAccel = String.valueOf(sensorEvent.values[2]);

            //save records of orientation, along with timestamp, to a csv file
            try {
                String time = String.valueOf(timeSinceStart);
                writeToCsv(time, xAccel, yAccel, zAccel);
            }
            catch(IOException e){ e.printStackTrace(); }
        }
    }

    public void writeToCsv(String t, String x, String y, String z) throws IOException {
        String s = t + "," + x + "," + y + "," + z + "\n";
        try {
            file_writer.append(s);
            file_writer.flush();
        }
        catch(Exception e) { e.printStackTrace(); }
     }

    public float convertToDegrees(float rad) {
        float deg = (float) (Math.toDegrees(rad) + 360) % 360;
        return deg;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Not used
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void setupCamera(int width, int height) {
        // get back-facing camera
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    int deviceOrientation = getWindowManager().getDefaultDisplay().getRotation();
                    int totalRotation = sensorToDeviceRotation(cameraCharacteristics, deviceOrientation);
                    boolean swapRotation = totalRotation == 90 || totalRotation == 270;
                    int rotatedWidth = width;
                    int rotatedHeight = height;
                    if (swapRotation) {
                        rotatedWidth = height;
                        rotatedHeight = width;
                    }
                    mPreviewSize = chooseOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                    mCameraId = cameraId;
                    return;
                }
            }
        }
        catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED)
            {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
            }
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this, "Requires Camera Access", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, MY_REQUEST_CAMERA_PERMISSION_CODE);
                }
            }
            cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void startPreview() {
        // get preview surface to capture output of camera data request
        mSurfaceTexture = mTextureView.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mPreviewSurface = new Surface(mSurfaceTexture);

        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(mPreviewSurface);

            mCameraDevice.createCaptureSession(
                Arrays.asList(mPreviewSurface),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        try {
                            cameraCaptureSession.setRepeatingRequest(
                                mCaptureRequestBuilder.build(),
                                null,
                                    mHandler
                            );
                        } catch (CameraAccessException e) { e.printStackTrace(); }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Toast.makeText(getApplicationContext(), "Unable to setup Camera Preview", Toast.LENGTH_SHORT);
                    }
                },
                null
            );
        } catch (CameraAccessException e) { e.printStackTrace(); }
    }

    private void startBackgroundThread() {
        mHandlerThread = new HandlerThread("Camera2ImageThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }
    private void stopBackgroundThread() {
        mHandlerThread.quitSafely();
        try {
            mHandlerThread.join();
            mHandlerThread = null;
            mHandler = null;
        }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

    private static int sensorToDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation) {
        int sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation = ORIENTATIONS.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360) % 360;
    }

    private static Size chooseOptimalPreviewSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width
                    && option.getWidth() >= width
                    && option.getHeight() >= height)
            {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        else {
            return choices[0]; // default
        }
    }

    private void createPictureFolder() {
        mPictureFolder = new File(directory, "pictureFolder");
        if (!mPictureFolder.exists()) {
            mPictureFolder.mkdirs();
        }
    }
}
