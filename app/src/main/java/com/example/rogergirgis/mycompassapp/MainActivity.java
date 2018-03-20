package com.example.rogergirgis.mycompassapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.Context;
import android.widget.VideoView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity
        extends AppCompatActivity
        implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private boolean videoStartedRecording = false;
    private final int REQUEST_VIDEO_CAPTURE = 1;
    private VideoView mVideoView;
    private Uri videoUri;
    private File videoFile;
    private final String videoFilename = "AnalysisData.vid";

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

    private long startTime = System.currentTimeMillis();
    private long currTime = System.currentTimeMillis();
    private long timeOfLastUpdate = 0L;
    private long timeSinceLastUpdate = 0L;
    private long timeSinceStart = 0L;

    private String xAccel;
    private String yAccel;
    private String zAccel;

    private Context context;
    private String path;
    private String directory;
    private String csv;
    private File file;
    private FileWriter file_writer;

    // https://developer.android.com/reference/android/hardware/SensorManager.html#getOrientation(float[],%20float[])
    // https://www.youtube.com/watch?v=nOQxq2YpEjQ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mVideoView = findViewById(R.id.videoView);

        xAccelText = findViewById(R.id.xAccelTV);
        yAccelText = findViewById(R.id.yAccelTV);
        zAccelText = findViewById(R.id.zAccelTV);

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

        // dispatch Intent to start recording video
        dispatchTakeVideoIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case 1010: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                }
            }
        }
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        try {
            file_writer.close();
        } catch(Exception e) {e.printStackTrace();}

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!videoStartedRecording) return;

        currTime = System.currentTimeMillis();
        timeSinceStart = currTime - startTime;
        timeSinceLastUpdate = currTime - timeOfLastUpdate;

        if (sensorEvent.sensor == mAccelerometer) {
            xAccelText.setText("X:" + sensorEvent.values[0]);
            yAccelText.setText("Y:" + sensorEvent.values[1]);
            zAccelText.setText("Z:" + sensorEvent.values[2]);
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor == mMagnetometer) {
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
            catch(IOException e){
                e.printStackTrace();
            }



        }

    }

    public void writeToCsv(String t, String x, String y, String z) throws IOException {
        boolean success = true;
        if (success) {
            String s = t + "," + x + "," + y + "," + z + "\n";
            try {
                file_writer.append(s);
                file_writer.flush();
            } catch(Exception e) {
                e.printStackTrace();
                e.printStackTrace();
            }

        }
     }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoFile = new File(directory + File.separator + videoFilename);
        videoUri = Uri.fromFile(videoFile);
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = intent.getData();
            mVideoView.setVideoURI(videoUri);
            videoStartedRecording = true;
        }
    }

    public float convertToDegrees(float rad) {
        float deg = (float) (Math.toDegrees(rad) + 360) % 360;
        return deg;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Not used
    }
}
