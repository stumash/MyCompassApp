package com.example.rogergirgis.mycompassapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.TextView;

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
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
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
