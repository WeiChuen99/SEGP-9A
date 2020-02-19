package com.example.androidsensor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensorAccelerometer;
    private Sensor mSensorGyroscope;
    private Sensor mSensorMagnetometer;
    private Sensor mSensorOrientation;

    private TextView mTextSensorAccelerometer;
    private TextView mTextSensorGyroscope;
    private TextView mTextSensorOrientation;
    private TextView mTextVelocity;
    //private TextView mTextVelocityGPS;
    private TextView mTextVelocityKMH;

    private LineChart mChartGyro, mChartAccel, mChartMagneto;

    private float firstValue, secondValue, thirdValue;// Variables to store data retrieved form sensor
    private float rMat[] = new float[9];
    private float[] orientation = new float[3];
    private double velocityX = 0.0f; // Assume device not moving when starting
    private double velocityY = 0.0f;
    private double velocityZ = 0.0f;
    private double timestamp = 0.0f;

    private double displacementY = 0.0f;
    private double previousTimestamp = 0.0f;
    // Convert nanoseconds to seconds
    private static final float nanosecond2second = 1.0f / 1000000000.0f;
    double deltaTime = 0;
    double deltaY = 0;
    double dAdT = 0;
    double constant = 0;

    // Variables to store data retrieved from sensor
    private float xValue, yValue = 0, zValue;
    private float previousYValue = 0;

    private Thread thread;
    private boolean plotData = true;

    private File fileDir, file;
    private TextView tv;
    private Context context;

    private Button startButton;
    private Button stopButton;
    private boolean record = false;

    // Server related
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get database info
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabase = mFirebaseDatabase.getReference("data");




        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // SensorManager to access device sensors

        // Access text in the app.
        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_accelerometer);
        mTextSensorOrientation = (TextView) findViewById(R.id.label_compass);
        //mTextSensorOrientation = (TextView) findViewById(R.id.label_compass);
        mTextVelocity = (TextView) findViewById(R.id.label_velocity);
        //mTextVelocityGPS = (TextView) findViewById((R.id.label_velocity_GPS));
        mTextVelocityKMH = (TextView) findViewById(R.id.label_velocity_KMH);

        // Variables to get sensors
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
        mSensorOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);


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

        // get IDs of buttons
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        context = this.getApplicationContext();

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                record = true;
                startButton.setEnabled(false);

                /*
                 * Datalogging part, create/check for file.
                 * Very rough implementation, can possibly be improved.
                 * */
                String currentTime = new SimpleDateFormat("HH.mm.ss", Locale.getDefault()).format(new Date());
                String pathName = context.getExternalFilesDir(null) + "/" + "AccelLog " + currentTime + ".csv";
                file = new File(pathName); // Create subfolder + text file

                if(!file.exists()){
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.v("fileDir ", "Not created");
                    }
                }

                if(file.exists()){
                    Log.v("fileDir ", "Exists");
                    Log.v("fileDir ", context.getExternalFilesDir(null)+ "***");
                }


                tv = (TextView)findViewById(R.id.text_view); // Show in app for debugging purposes. Can be removed if direct access to csv is possible

                // End of datalogging part
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                record = false;
                startButton.setEnabled(true);
            }
        });


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
                double deltaTime = 0;

                firstValue = event.values[0];
                secondValue = event.values[1];
                thirdValue = event.values[2];

                /*
                 *  Filter small movements to reduce noise.
                 *  If accelerometer values ALL read between -0.1 and 0.1,
                 *  assume the user is simply standing/not moving
                 *  Value should be adjusted later on after further testing
                 */

                if(Math.abs(firstValue) <= 0.1 && Math.abs(secondValue) <= 0.1 && Math.abs(thirdValue) <= 0.1)
                {
                    firstValue = 0; // Movements below 0.1 threshold wont be considered
                    secondValue = 0;
                    thirdValue = 0;
                }


                // Set the text in the app


                mTextSensorAccelerometer.setText(getResources().getString(R.string.label_accelerometer, firstValue, secondValue, thirdValue)); // Set the text in the app

                addEntry(event, mChartAccel);

                // Timestamp returns time in nanoseconds, which should be much more accurate
                if(timestamp == 0) // Initial timestamp, when data is read the very first time

                //deltaY = yValue - previousYValue;
                //previousYValue = yValue;

                // Timestamp returns time in nanoseconds, which should be much more accurate


                if(timestamp == 0)
                {
                    deltaTime = 0; // This is the time passed since the last reading. It needs to be 0 for the very first reading
                }
                else
                {
                    deltaTime = (event.timestamp - timestamp) * nanosecond2second;
                }

                timestamp = event.timestamp;

                timestamp = event.timestamp;


                // Track previous timestamp t0
                /*
                previousTimestamp = timestamp;
                // Track current timestamp t1
                timestamp = event.timestamp*nanosecond2second;
                // (t1 - t0)
                deltaTime = (event.timestamp*nanosecond2second - previousTimestamp);

                dAdT = deltaY/deltaTime;

                constant = yValue - (dAdT*(timestamp));*/

                // Find K
                //double K = velocityY - (0.5* dAdT * previousTimestamp * previousTimestamp) - (constant * previousTimestamp);

                // Find current V
                /*
                velocityY = (0.5*dAdT*timestamp*timestamp)-(constant*timestamp);
                System.out.printf("V : %f\n",velocityY*3.6);
                System.out.printf("dadt : %f \n c : %f\n DY : %f\n ",dAdT,constant,deltaY);*/

                /*  V0 = V + AT
                 *  A = Acceleration, T = Time in seconds
                 *  T = time since previous reading
                 *  V0 = Previously calculated Velocity. Assume initial velocity is 0m/s.
                 */


                velocityX = (velocityX + (firstValue*deltaTime));
                velocityY = (velocityY + (secondValue*deltaTime));
                velocityZ = (velocityZ + (thirdValue*deltaTime));


                //velocityX = (velocityX + (xValue*deltaTime));
                velocityY = (velocityY + (yValue*deltaTime));
                displacementY = (displacementY + (velocityY*deltaTime));
                System.out.println(yValue);
                System.out.printf("V : %f\n",velocityY*3.6);
                //velocityZ = (velocityZ + (zValue*deltaTime));



                /* Note :
                 * Consider using GPS data along with accelerometer data (sensor fusion).
                 * Accelerometer on it's own may not be accurate enough
                 */

                mTextVelocity.setText(getResources().getString(R.string.label_velocity, velocityX, velocityY, velocityZ));
                //mTextVelocityGPS.setText(getResources().getString(R.string.label_velocity, velocityX, velocityY, velocityZ));
                mTextVelocityKMH.setText(getResources().getString(R.string.label_velocity_KMH, velocityX*3.6, velocityY*3.6, velocityZ*3.6));


                mTextVelocity.setText(getResources().getString(R.string.label_velocity, velocityX, displacementY, velocityZ));
                //mTextVelocityGPS.setText(getResources().getString(R.string.label_velocity, velocityX, velocityY, velocityZ));
                mTextVelocityKMH.setText(getResources().getString(R.string.label_velocity_KMH, velocityX*3.6, velocityY*3.6, velocityZ*3.6));


                /*
                 *  BELOW
                 *  Writing to AccelLog.csv
                 *  Very rough implementation, can possibly be improved.
                 * */
                if (record == true) {

                    mDatabase.child("X").setValue(firstValue);
                    mDatabase.child("Y").setValue(secondValue);
                    mDatabase.child("Z").setValue(thirdValue);

                    /*
                    if (file.exists()) {
                        try {
                            FileWriter fileWriter = new FileWriter(file, true);

                            //String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                            fileWriter.append("X,Y,Z,Compass,Time\n");
                            fileWriter.append(String.format("%.2f", firstValue));
                            fileWriter.append(',');
                            fileWriter.append(String.format("%.2f", secondValue));
                            fileWriter.append(',');
                            fileWriter.append(String.format("%.2f", thirdValue));
                            fileWriter.append(',');
                            //fileWriter.append(currentTime);
                            //fileWriter.append("\n");

                            fileWriter.flush();
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    */
                }

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

            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(rMat,event.values);
                firstValue = Math.round( (int) (Math.toDegrees(SensorManager.getOrientation(rMat,orientation)[0]) + 360) % 360);
                mTextSensorOrientation.setText("Compass : " + Float.toString(firstValue) + (char) 0x00B0);




                if (record == true) {

                    mDatabase.child("compass").setValue(firstValue);


                    /*
                    if (file.exists()) {
                        try {
                            FileWriter fileWriter = new FileWriter(file, true);

                            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                            //fileWriter.append("X    Y   Z   Time\n");
                            fileWriter.append(String.format("%.2f", firstValue));
                            fileWriter.append(',');
                            fileWriter.append(currentTime);
                            fileWriter.append("\n");

                            fileWriter.flush();
                            fileWriter.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    */
                }

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
