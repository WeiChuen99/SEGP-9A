package com.example.androidsensor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
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
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Service.START_STICKY;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // Sensors
    private Sensors sensors;

    // Values in app
    private TextView mTextSensorAccelerometer;
    private TextView mTextSensorGyroscope;
    private TextView mTextSensorOrientation;
    private TextView mTextVelocity;
    private TextView mTextVelocityGPS;
    private TextView mTextVelocityKMH;
    private TextView mTextActivity;
    private TextView mTextConfidence;

    // Graph
    private Charts charts;

    // Database
    private float rMat[] = new float[9];
    private float[] orientation = new float[3];
    // Assume device not moving when starting
    private float velocityX = 0.0f;
    private float velocityY = 0.0f;
    private float velocityZ = 0.0f;
    private long timestamp = 0;
    private long previousTimestamp = 0;
    // Convert nanoseconds to seconds
    private static final double nanosecond2second = 1.0f / 1000000000.0f;
    float deltaTime = 0;
    float deltaY = 0;
    float dAdT = 0;
    float constant = 0;

    // Variables to store data retrieved from sensor
    private float xValue, yValue = 0.0f, zValue;
    private float previousYValue = 0;

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

    private List<ActivityTransition> transitions;
    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent transitionPendingIntent;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        activityRecognitionClient = ActivityRecognition.getClient(mContext);


        Intent intent = new Intent(this, TransitionIntentService.class);
        transitionPendingIntent = PendingIntent.getService(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get database info
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mDatabase = mFirebaseDatabase.getReference("data");

        this.sensors = new Sensors(this);
        this.charts = new Charts(this);

        // Access text in the app.
        mTextSensorAccelerometer = (TextView) findViewById(R.id.label_accelerometer);
        mTextSensorOrientation = (TextView) findViewById(R.id.label_compass);
        mTextVelocity = (TextView) findViewById(R.id.label_velocity);
        //mTextVelocityGPS = (TextView) findViewById((R.id.label_velocity_GPS));
        mTextVelocityKMH = (TextView) findViewById(R.id.label_velocity_KMH);
        mTextActivity = (TextView) findViewById(R.id.label_activity);

        feedMultiple();

        // get IDs of buttons
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        context = this.getApplicationContext();

        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                record = true;
                startButton.setEnabled(false);

                registerHandler();

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

                xValue = event.values[0];
                yValue = event.values[1];
                zValue = event.values[2];

                /*
                 *  Filter small movements to reduce noise.
                 *  If accelerometer values ALL read between -0.1 and 0.1,
                 *  assume the user is simply standing/not moving
                 *  Value should be adjusted later on after further testing
                 */

                if(Math.abs(xValue) <= 0.1 && Math.abs(yValue) <= 0.1 && Math.abs(zValue) <= 0.1)
                {
                    // Movements below 0.1 threshold wont be considered
                    xValue = 0; 
                    yValue = 0;
                    zValue = 0;
                }

                // Set the text in the app
                mTextSensorAccelerometer.setText(getResources().getString(R.string.label_accelerometer, xValue, yValue, zValue)); 

                addEntry(event, charts.mChartAccel);

                deltaY = yValue - previousYValue;
                previousYValue = yValue;

                // Timestamp returns time in nanoseconds, which should be much more accurate

                /*
                if(timestamp == 0)
                {
                    // This is the time passed since the last reading. It needs to be 0 for the very first reading
                    deltaTime = 0;
                    System.out.println("Here");
                }
                else
                {
                    previousTimestamp = timestamp;
                    deltaTime = (event.timestamp*nanosecond2second - timestamp);
                }
                */

                // Track previous timestamp t0
                previousTimestamp = timestamp;
                // Track current timestamp t1
                timestamp = event.timestamp;

                // (t1 - t0)
                deltaTime = (float) ((timestamp - previousTimestamp)*nanosecond2second);

                dAdT = deltaY/deltaTime;

                // y = mx + c
                // accel = (dAdT * t) + constant
                // constant = accel - (dAdT * t)
                constant = (float) (yValue - (dAdT*(timestamp*nanosecond2second)));

                // V = (0.5 * dAdT * t * t) + (constant * t) + K
                // K = V - (0.5 * dAdT * t * t) - (constant * t)

                // Find K
                float K = (float) (velocityY - (0.5 * dAdT * previousTimestamp*nanosecond2second * previousTimestamp*nanosecond2second) - (constant * previousTimestamp*nanosecond2second));

                // Find current V
                velocityY = (float) ((0.5*dAdT*timestamp*nanosecond2second*timestamp*nanosecond2second)+(constant*timestamp*nanosecond2second) + K);

                //System.out.println(velocityY*3.6);
                //System.out.printf("k : %f \ndadt : %f \n c : %f\n",K,dAdT,constant);

                /*  V0 = V + AT
                 *  A = Acceleration, T = Time in seconds
                 *  T = time since previous reading
                 *  V0 = Previously calculated Velocity. Assume initial velocity is 0m/s.
                 */

                /*
                velocityX = (velocityX + (xValue*deltaTime));
                velocityY = (velocityY + (yValue*deltaTime));
                velocityZ = (velocityZ + (zValue*deltaTime));
                */


                /* Note :
                 * Consider using GPS data along with accelerometer data (sensor fusion).
                 * Accelerometer on it's own may not be accurate enough
                 */

                /*
                mTextVelocity.setText(getResources().getString(R.string.label_velocity, velocityX, velocityY, velocityZ));
                //mTextVelocityGPS.setText(getResources().getString(R.string.label_velocity, velocityX, velocityY, velocityZ));
                mTextVelocityKMH.setText(getResources().getString(R.string.label_velocity_KMH, velocityX*3.6, velocityY*3.6, velocityZ*3.6));
                */

                /*
                 *  BELOW
                 *  Writing to AccelLog.csv
                 *  Very rough implementation, can possibly be improved.
                 * */
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

            case Sensor.TYPE_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(rMat,event.values);
                xValue = Math.round( (int) (Math.toDegrees(SensorManager.getOrientation(rMat,orientation)[0]) + 360) % 360);
                mTextSensorOrientation.setText("Compass : " + Float.toString(xValue) + (char) 0x00B0);

                if (record == true) {
                    mDatabase.child("compass").setValue(xValue);
                }
                break;

            default :
                break;
        }
    }

    public void registerHandler() {
        transitions = new ArrayList<>();

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());


        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.ON_FOOT)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());


        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        ActivityTransitionRequest activityTransitionRequest
                = new ActivityTransitionRequest(transitions);

        Task<Void> task = activityRecognitionClient.requestActivityTransitionUpdates(activityTransitionRequest, transitionPendingIntent);

        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(mContext, "Transition update set up", Toast.LENGTH_LONG).show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(mContext, "Transition update Failed to set up", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
    }

    public void deregisterHandler() {
        Task<Void> task = activityRecognitionClient.removeActivityTransitionUpdates(transitionPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                transitionPendingIntent.cancel();
                Toast.makeText(mContext, "Remove Activity Transition Successfully", Toast.LENGTH_LONG).show();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(mContext, "Remove Activity Transition Failed", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
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
        activityRecognitionClient.removeActivityTransitionUpdates(transitionPendingIntent);
        deregisterHandler();

    }
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
