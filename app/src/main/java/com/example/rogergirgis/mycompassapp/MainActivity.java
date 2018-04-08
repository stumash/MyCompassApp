package com.example.rogergirgis.mycompassapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.Context;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.Math;


public class MainActivity
        extends AppCompatActivity
        implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private TextView xAccelText, yAccelText, zAccelText;
    private TextView xMagnText, yMagnText, zMagnText;
    private TextView xOrientationText, yOrientationText, zOrientationText;
    private TextView rOkText;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private Sensor mRotationVector;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private float[] mLastRotationVector = new float[4];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private boolean mLastRotationVectorSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    private long startTime = System.currentTimeMillis();
    private long currTime = System.currentTimeMillis();
    private long timeOfLastUpdate = 0L;
    private long timeSinceLastUpdate = 0L;
    private long timeSinceStart = 0L;

    private Context context;
    private String path;
    private String directory;
    private String csv;
    private File file;
    private FileWriter file_writer;

    private float withinLastReading = 5;

    // https://developer.android.com/reference/android/hardware/SensorManager.html#getOrientation(float[],%20float[])
    // https://www.youtube.com/watch?v=nOQxq2YpEjQ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        xAccelText = findViewById(R.id.xAccelTV);
        yAccelText = findViewById(R.id.yAccelTV);
        zAccelText = findViewById(R.id.zAccelTV);

        xMagnText = findViewById(R.id.xMagnTV);
        yMagnText = findViewById(R.id.yMagnTV);
        zMagnText = findViewById(R.id.zMagnTV);

        xOrientationText = findViewById(R.id.xOrientationTV);
        yOrientationText = findViewById(R.id.yOrientationTV);
        zOrientationText = findViewById(R.id.zOrientationTV);

        rOkText = findViewById(R.id.rOkTV);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


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
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        mSensorManager.unregisterListener(this, mRotationVector);
        try {
            file_writer.close();
        } catch(Exception e) {e.printStackTrace();}

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
        } else if (sensorEvent.sensor == mMagnetometer) {
            xMagnText.setText(("X:" + sensorEvent.values[0]));
            yMagnText.setText(("Y:" + sensorEvent.values[1]));
            zMagnText.setText(("Z:" + sensorEvent.values[2]));
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        } else if (sensorEvent.sensor == mRotationVector) {

            float[] rotationVector = new float[4];
            float[] rotationMatrix = new float[9];
            float[] remapRotationMatrix = new float[9];
            float[] verticalOrientation = new float[3];
            int worldAxisX = SensorManager.AXIS_X;
            int worldAxisZ = SensorManager.AXIS_Z;

            //Copy first 4 values of rotation vector, they are the only ones needed for pitch, yaw, roll
            System.arraycopy(sensorEvent.values, 0, rotationVector, 0, 4);
            //Compute the rotation matrix
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
            //Remap the coordinates of the phone, since we are using it vertically instead of horizontally
            SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, remapRotationMatrix);
            //Get the orientation of the phone
            SensorManager.getOrientation(remapRotationMatrix, verticalOrientation);

            //Check last reading
            if(angleBetween(mLastRotationVector[0], convertToDegrees(verticalOrientation[1])) < withinLastReading &&
                    angleBetween(mLastRotationVector[1], convertToDegrees(verticalOrientation[2])) < withinLastReading &&
                    angleBetween(mLastRotationVector[2], convertToDegrees(verticalOrientation[0])) < withinLastReading){
                rOkText.setText("Okay");
            }else{
                rOkText.setText("Not Okay");
            }

            //[pitch, roll, yaw]
            mLastRotationVector[0] = convertToDegrees(verticalOrientation[1]);
            mLastRotationVector[1] = convertToDegrees(verticalOrientation[2]);
            mLastRotationVector[2] = convertToDegrees(verticalOrientation[0]);

            xOrientationText.setText("Pitch: " + mLastRotationVector[0]);
            yOrientationText.setText("Roll: " + mLastRotationVector[1]);
            zOrientationText.setText("Yaw: " + mLastRotationVector[2]);

            mLastRotationVectorSet = true;
        }
        if ((timeSinceLastUpdate > 200) && mLastRotationVectorSet) {
            timeOfLastUpdate = currTime;

            String xOrientationDeg = String.valueOf(mLastRotationVector[0]);
            String yOrientationDeg = String.valueOf(mLastRotationVector[1]);
            String zOrientationDeg = String.valueOf(mLastRotationVector[2]);
            //save records of orientation, along with timestamp, to a csv file
            try {

                String time = String.valueOf(timeSinceStart);
                writeToCsv(time, xOrientationDeg, yOrientationDeg, zOrientationDeg);
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

    public float convertToDegrees(float rad) {
        float deg = (float) (Math.toDegrees(rad) + 360) % 360;
        return deg;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Not used
    }

    public float angleBetween(float a, float b){
        if(Math.abs(a-b) < 180){
            return Math.abs(a-b);
        }else {
            if (a < b) {
                return 360 + a - b;
            } else {
                return 360 + b - a;
            }
        }
    }
}
