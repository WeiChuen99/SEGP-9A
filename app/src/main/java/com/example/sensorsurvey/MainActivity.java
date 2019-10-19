package com.example.sensorsurvey;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;

    // Individual light and proximity sensors.
    private Sensor mSensorProximity;
    private Sensor mSensorLight;

    // TextViews to display current sensor values
    private TextView mTextSensorLight;
    private TextView mTextSensorProximity;

    //system service that allows you to access the device sensors
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get each sensor name and list it out
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        List<Sensor> sensorList  =
//                mSensorManager.getSensorList(Sensor.TYPE_ALL);
//
//        //iterate through the string and get the name of sensor
//        StringBuilder sensorText = new StringBuilder();
//        for (Sensor currentSensor : sensorList ) {
//            sensorText.append(currentSensor.getName()).append(
//                    System.getProperty("line.separator"));
//        }
//        //display the list
//        TextView sensorTextView = (TextView) findViewById(R.id.sensor_list);
//        sensorTextView.setText(sensorText);
        mTextSensorLight = (TextView) findViewById(R.id.label_light);
        mTextSensorProximity = (TextView) findViewById(R.id.label_proximity);

        mSensorProximity =
                mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        //get error string from strings.xml
        String sensor_error = getResources().getString(R.string.error_no_sensor);

        //test for existence of sensors
        if (mSensorLight == null) {
            mTextSensorLight.setText(sensor_error);
        }
        if (mSensorProximity == null) {
            mTextSensorProximity.setText(sensor_error);
        }
    }
    //register sensor listeners when app starts only
    @Override
    protected void onStart() {
        super.onStart();

        if (mSensorProximity != null) {
            mSensorManager.registerListener(this, mSensorProximity,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mSensorLight != null) {
            mSensorManager.registerListener(this, mSensorLight,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    //unregister sensors from app pauses
    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        float currentValue = event.values[0];
        switch (sensorType) {
            // Event came from the light sensor.
            case Sensor.TYPE_LIGHT:
                // Handle light sensor
                mTextSensorLight.setText(getResources().getString(
                        R.string.label_light, currentValue));
                break;
            case Sensor.TYPE_PROXIMITY:
                mTextSensorProximity.setText(getResources().getString(
                        R.string.label_proximity, currentValue));
                break;
            default:
                // do nothing
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
