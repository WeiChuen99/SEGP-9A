package com.example.sensorsurvey;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private TextView mTextSensorAccelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_accelerometer);

        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        String sensor_error = getResources().getString(R.string.error_no_sensor);

        if(mSensorAccelerometer == null) {
            mTextSensorAccelerometer.setText(sensor_error);
        }

    }

    @Override
    protected void onStart(){
        super.onStart();

        if(mSensorAccelerometer != null){
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float firstValue;
        float secondValue;
        float thirdValue;

        if (sensorType == Sensor.TYPE_LINEAR_ACCELERATION){
                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];
                mTextSensorAccelerometer.setText(getResources().getString(R.string.label_accelerometer, firstValue, secondValue, thirdValue));

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
