package com.example.rogergirgis.mycompassapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;
import android.content.Context;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

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

    private Context context;
    private String path;
    private String directory;
    private String csv;
    private File file;
    private FileWriter file_writer;

    /**
     *  ;asldkjfj;asdflk;asd f
     *
     *
     */
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

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        context = getApplicationContext();
        csv = "AnalysisData.csv";
        directory = Environment.getExternalStorageDirectory().getPath();
        path = directory + File.separator + csv;
        file = new File(path);
        try {
            file_writer = new FileWriter(file, false);
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
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
        if ((timeSinceLastUpdate > 200) && mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            timeOfLastUpdate = currTime;


            String xOrientationDeg = String.valueOf(convertToDegrees(mOrientation[0]));
            String yOrientationDeg = String.valueOf(convertToDegrees(mOrientation[1]));
            String zOrientationDeg = String.valueOf(convertToDegrees(mOrientation[2]));
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
            } catch(Exception e) {e.printStackTrace();}

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
