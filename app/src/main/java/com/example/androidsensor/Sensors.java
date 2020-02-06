package com.example.androidsensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class Sensors {

    // Sensors on device
    public SensorManager mSensorManager;
    public Sensor mSensorAccelerometer;
    public Sensor mSensorGyroscope;
    public Sensor mSensorMagnetometer;
    public Sensor mSensorOrientation;
    private MainActivity main;

    // Constructor
    public Sensors(MainActivity main) {
        this.main = main;
        // SensorManager to access device sensors
        mSensorManager = (SensorManager) main.getSystemService(Context.SENSOR_SERVICE);

        // Variables to get sensors
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
        mSensorOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        /*   NOTE : There's TYPE_ACCELEROMETER and TYPE_LINEAR_ACCELERATION that can be used
         *   TYPE_ACCELEROMETER will provide data with gravity calculations
         *   TYPE_LINEAR_ACCELERATION will give raw data
         *   Will need further discussion on which is more appropriate as the X Y Z values given might differ.
         * */

        // Print the string variable if no sensor is detected, e.g device doesn't have the sensor.
        String sensor_error = main.getResources().getString(R.string.error_no_sensor);
        if(mSensorAccelerometer == null) {
            main.getmTextSensorAccelerometer().setText(sensor_error);
        }

        if(mSensorGyroscope == null) {
            main.getmTextSensorGyroscope().setText(sensor_error);
        }

        if(mSensorOrientation == null) {
            main.getmTextSensorOrientation().setText(sensor_error);
        }
    }

    public void sensorStart() {
        // Listener to retrieve data
        if(mSensorAccelerometer != null){
            mSensorManager.registerListener(main, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorGyroscope != null) {
            mSensorManager.registerListener(main, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorMagnetometer != null){
            mSensorManager.registerListener(main, mSensorMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorOrientation != null){
            mSensorManager.registerListener(main, mSensorOrientation, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void sensorStop() {
        mSensorManager.unregisterListener(main);
    }

    public void sensorPause() {
        mSensorManager.unregisterListener(main);
    }

    public void sensorResume() {
        mSensorManager.registerListener(main, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(main, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(main, mSensorMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void sensorDestroy() {
        mSensorManager.unregisterListener(main);
    }
}
