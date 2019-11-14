package com.example.androidsensor;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorGyroscope;
    private Sensor mSensorMagnetometer;
    private Sensor mSensorOrientation;

    private TextView mTextSensorAccelerometer;
    private TextView mTextSensorGyroscope;
    private TextView mTextSensorOrientation;

    private LineChart mChartGyro, mChartAccel, mChartMagneto;

    private float firstValue, secondValue, thirdValue; // Variables to store data retrieved form sensor

    private Thread thread;
    private boolean plotData = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // SensorManager to access device sensors

        // Access text in the app.
        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_accelerometer);
        mTextSensorOrientation = (TextView) findViewById(R.id.label_compass);
        //mTextSensorOrientation = (TextView) findViewById(R.id.label_compass);

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
        String sensor_error = getResources().getString(R.string.error_no_sensor);
        if(mSensorAccelerometer == null) {
            mTextSensorAccelerometer.setText(sensor_error);
        }
        if(mSensorGyroscope == null) {
            mTextSensorGyroscope.setText(sensor_error);
        }

        if(mSensorOrientation == null) {
            mTextSensorOrientation.setText(sensor_error);
        }

        mChartGyro = createChart(R.id.chart_gyroscope, mChartGyro, -10, 10);
        mChartAccel = createChart(R.id.chart_accelerometer, mChartAccel, -10, 10);
        mChartMagneto = createChart(R.id.chart_magnetometer, mChartMagneto, -100, 100);

        feedMultiple();

    }

    @Override
    protected void onStart(){
        super.onStart();

        // Listener to retrieve data
        if(mSensorAccelerometer != null){
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorGyroscope != null) {
            mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorMagnetometer != null){
            mSensorManager.registerListener(this, mSensorMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(mSensorOrientation != null){
            mSensorManager.registerListener(this, mSensorOrientation, SensorManager.SENSOR_DELAY_NORMAL);
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

        switch (sensorType){
            case Sensor.TYPE_LINEAR_ACCELERATION :
                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];
                mTextSensorAccelerometer.setText(getResources().getString(R.string.label_accelerometer, firstValue, secondValue, thirdValue)); // Set the text in the app
                addEntry(event, mChartAccel);

                break;

            case Sensor.TYPE_GYROSCOPE :
                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];
                //mTextSensorGyroscope.setText(getResources().getString(R.string.label_gyroscope, firstValue, secondValue, thirdValue));
                addEntry(event, mChartGyro);

                break;

            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];
                addEntry(event, mChartMagneto);

                break;

            case Sensor.TYPE_ORIENTATION:
                firstValue = Math.round(event.values[0]);
                mTextSensorOrientation.setText("Compass : " + Float.toString(firstValue) + (char) 0x00B0);

                break;


            default :
                break;
        }

    }

    private void addEntry(SensorEvent event, LineChart chart) {

        LineData data = chart.getData();

        if (data != null) {

            ILineDataSet setX = data.getDataSetByIndex(0);
            ILineDataSet setY = data.getDataSetByIndex(1);
            ILineDataSet setZ = data.getDataSetByIndex(2);
            // set.addEntry(...); // can be called as well

            if (setX == null) {
                setX = createSet(Color.RED, "X");
                data.addDataSet(setX);
            }
            if (setY == null) {
                setY = createSet(Color.GREEN, "Y");
                data.addDataSet(setY);
            }
            if (setZ == null) {
                setZ = createSet(Color.BLUE, "Z");
                data.addDataSet(setZ);
            }

            //data.addEntry(new Entry(set.getEntryCount(), (float) (Math.random() * 80) + 10f), 0);
            data.addEntry(new Entry(setX.getEntryCount(), event.values[0]), 0);
            data.addEntry(new Entry(setY.getEntryCount(), event.values[1]), 1);
            data.addEntry(new Entry(setZ.getEntryCount(), event.values[2]), 2);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(150);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());

        }
    }

    private LineDataSet createSet(int colorID, String label) {

        LineDataSet set = new LineDataSet(null, label);
        //set.setLabel(label);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(3f);
        set.setColor(colorID);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);
        return set;
    }

    private void feedMultiple() {

        if (thread != null){
            thread.interrupt();
        }

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true){
                    plotData = true;
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }

    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.interrupt();
        }
        mSensorManager.unregisterListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(MainActivity.this);
        thread.interrupt();
        super.onDestroy();
    }

    private LineChart createChart(int viewID, LineChart mChart, float YMin, float YMax){

        mChart = (LineChart) findViewById(viewID);

        mChart.getDescription().setEnabled(true); // Enable description text
        mChart.getDescription().setText("Gyroscope measurements in radians");
        mChart.getDescription().setTextSize(12f);

        mChart.setTouchEnabled(true); // Enable touch gestures

        mChart.setDragEnabled(true); // Enable scaling and dragging
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(true);

        mChart.setPinchZoom(true); // If disabled, scaling can be done on x- and y-axis separately
        mChart.setBackgroundColor(Color.WHITE); // Set an alternative background color

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setTextColor(Color.BLACK);
        rightAxis.setDrawGridLines(true);
        rightAxis.setAxisMaximum(YMax);
        rightAxis.setAxisMinimum(YMin);
        rightAxis.setDrawGridLines(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMaximum(YMax);
        leftAxis.setAxisMinimum(YMin);
        leftAxis.setDrawGridLines(true);

        mChart.getAxisRight().setDrawGridLines(false);
        mChart.getXAxis().setDrawGridLines(false);
        mChart.setDrawBorders(true);

        return mChart;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
