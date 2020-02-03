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

    // Sensors
    private Sensors sensors;

    // Values in app
    private TextView mTextSensorAccelerometer;
    private TextView mTextSensorGyroscope;
    private TextView mTextSensorOrientation;

    // Graph
    private Charts charts;

    // Variables to store data retrieved from sensor
    private float xValue, yValue, zValue;

    private Thread thread;
    private boolean plotData = true;

    // Writing sensor data to file
    private File fileDir, file;
    private TextView tv;
    private Context context;

    // Buttons to start and stop
    private Button startButton;
    private Button stopButton;
    private boolean record = false;

    // Server related
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabase;

    public TextView getmTextSensorAccelerometer() {
        return mTextSensorAccelerometer;
    }

    public TextView getmTextSensorGyroscope() {
        return mTextSensorGyroscope;
    }

    public TextView getmTextSensorOrientation() {
        return mTextSensorOrientation;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get database info
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabase = mFirebaseDatabase.getReference("data");

        this.sensors = new Sensors(this);
        this.charts = new Charts(this);

        // Access text in the app.
        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_accelerometer);
        mTextSensorOrientation = (TextView) findViewById(R.id.label_compass);

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
                 * Data logging part, create/check for file.
                 * Very rough implementation, can possibly be improved.
                 * */
                String currentTime = new SimpleDateFormat("HH.mm.ss", Locale.getDefault()).format(new Date());
                String pathName = context.getExternalFilesDir(null) + "/" + "AccelLog " + currentTime + ".csv";
                file = new File(pathName); // Create subfolder + text file

                if(file.exists()){
                    Log.v("fileDir ", "Exists");
                    Log.v("fileDir ", context.getExternalFilesDir(null)+ "***");
                }
                else {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.v("fileDir ", "Not created");
                    }
                }

                // Show in app for debugging purposes. Can be removed if direct access to csv is possible
                tv = (TextView)findViewById(R.id.text_view);
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
        sensors.sensorStart();
    }

    @Override
    protected void onStop(){
        super.onStop();
        sensors.sensorStop();
    }

    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        switch (sensorType){
            case Sensor.TYPE_LINEAR_ACCELERATION :
                setValues(event);
                addEntry(event, charts.mChartAccel);
                // Set the text in the app
                mTextSensorAccelerometer.setText(getResources().getString(R.string.label_accelerometer, xValue, yValue, zValue));

                if (record == true) {
                    mDatabase.child("X").setValue(xValue);
                    mDatabase.child("Y").setValue(yValue);
                    mDatabase.child("Z").setValue(zValue);
                }
                break;

            case Sensor.TYPE_GYROSCOPE :
                setValues(event);
                //mTextSensorGyroscope.setText(getResources().getString(R.string.label_gyroscope, xValue, yValue, zValue));
                addEntry(event, charts.mChartGyro);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                setValues(event);
                addEntry(event, charts.mChartMagneto);
                break;

            case Sensor.TYPE_ORIENTATION:
                xValue = Math.round(event.values[0]);
                mTextSensorOrientation.setText("Compass : " + Float.toString(xValue) + (char) 0x00B0);
                if (record == true) {
                    mDatabase.child("compass").setValue(xValue);
                }
                break;

            default :
                break;
        }
    }

    public void setValues (SensorEvent event) {
        xValue = event.values[0];
        yValue = event.values[1];
        zValue = event.values[2];
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
        sensors.sensorPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensors.sensorResume();
    }

    @Override
    protected void onDestroy() {
        sensors.sensorDestroy();
        thread.interrupt();
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
