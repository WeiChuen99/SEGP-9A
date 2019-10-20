package com.example.androidsensor;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

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
    private Sensor mSensorGyroscope;

    private TextView mTextSensorAccelerometer;
    private TextView mTextSensorGyroscope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // SensorManager to access device sensors

        // Access text in the app.
        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_accelerometer);
        mTextSensorGyroscope = (TextView) findViewById(R.id.label_gyroscope);

        // Variables to get sensors
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        /*   NOTE : It's using TYPE_LINEAR_ACCELERATION instead of TYPE_ACCELEROMETER
         *   TYPE_ACCELEROMETER will add gravity into it's calculations which then allows to detect orientation
         *   whereas TYPE_LINEAR_ACCELERATION will output raw acceleration data without calculating gravity.
         *
         *   Will need further discussion on which is more appropriate as the X Y Z values given might differ.
         * */

        // Print the string variable if no sensor is detected, e.g device doesn't have the sensor.
        String sensor_error = getResources().getString(R.string.error_no_sensor);
        if(mSensorAccelerometer == null) {
            mTextSensorAccelerometer.setText(sensor_error);
        }
        if(mSensorGyroscope == null) {
            mTextSensorGyroscope.setText(sensor_error);
        }

    }

    @Override
    protected void onStart(){
        super.onStart();

        // Listener to retrieve data
        if(mSensorAccelerometer != null){
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorGyroscope != null){
            mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        mSensorManager.unregisterListener(this); // Remove Listener
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float firstValue; //variables to store data retrieved form sensor
        float secondValue;
        float thirdValue;

        switch (sensorType){
            case Sensor.TYPE_LINEAR_ACCELERATION :
                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];
                mTextSensorAccelerometer.setText(getResources().getString(R.string.label_accelerometer, firstValue, secondValue, thirdValue)); // Set the text in the app
                break;

            case Sensor.TYPE_GYROSCOPE :
                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];
                mTextSensorGyroscope.setText(getResources().getString(R.string.label_gyroscope, firstValue, secondValue, thirdValue));
                break;

            default :
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
