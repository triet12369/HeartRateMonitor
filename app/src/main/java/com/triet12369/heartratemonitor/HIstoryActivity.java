package com.triet12369.heartratemonitor;

import android.app.Activity;
import android.content.Context;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.R.attr.button;
import static android.R.attr.dashGap;
import static android.R.attr.data;
import static android.R.attr.format;

public class HistoryActivity extends AppCompatActivity{
    private static final String TAG = "HistoryActivity";
    private String filename = "history_log";
    private List<Integer> heartVal = new ArrayList<Integer>();
    private List<String> date = new ArrayList<String>();
    private StringBuilder temp = new StringBuilder();
    private PointsGraphSeries mSeries;
    Calendar calendar = Calendar.getInstance();
    Date d = calendar.getTime();
    private ArrayList<Date> dateList = new ArrayList<Date>();
    private Button buttonClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        buttonClear = (Button) findViewById(R.id.buttonClear);
        buttonClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearFile(filename, getApplicationContext());
                finish();
            }
        });

        int check1 = 0;
        int check2 = 1;
        setTitle("History");
        String ret = readFromFile(filename, this);
        Log.d(TAG, "ret: " + ret);
        for (int i = 0; i < ret.length(); i++) {

            if (ret.charAt(i) == ',') {
                check1 = 1;
                check2 = 0;
                heartVal.add(Integer.parseInt(temp.toString()));
                temp.delete(0, temp.length());

            }

            if (check2 == 1 && ret.charAt(i) != ';') {
                temp.append(ret.charAt(i));
            }
            if (ret.charAt(i) == ';') {
                check2 = 1;
                check1 = 0;
                date.add(temp.toString());
                temp.delete(0, temp.length());
            }
            if (check1 == 1 && ret.charAt(i) != ',') {
                temp.append(ret.charAt(i));
            }
        }
        Log.d(TAG, "heartVal: " + heartVal);
        Log.d(TAG, "date: " + date);
        if (date.size() > 0) {
            GraphView graph = (GraphView) findViewById(R.id.graph_history);
            mSeries = new PointsGraphSeries(generateData(heartVal, date));
            graph.addSeries(mSeries);
            mSeries.setShape(PointsGraphSeries.Shape.POINT);


            // set date label formatter
            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
            graph.getGridLabelRenderer().setNumHorizontalLabels(3);
            graph.getGridLabelRenderer().setHumanRounding(false);
            graph.getViewport().setMinX(dateList.get(0).getTime());
            graph.getViewport().setMaxX(dateList.get(dateList.size()-1).getTime());
            graph.getViewport().setMinY(0);
            graph.getViewport().setMaxY(150);
            graph.getViewport().setScalable(true);
            mSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    Date date = new Date((long) dataPoint.getX());
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+7"));
                    String formattedDate = sdf.format(date);
                    Toast.makeText(HistoryActivity.this, "HR: "+ (int) dataPoint.getY() + ", "+ formattedDate, Toast.LENGTH_SHORT).show();
                }
            });
        }



    }

    private String readFromFile(String filename, Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(filename);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
    private DataPoint[] generateData(List<Integer> heartVal, List<String> date) {
        DataPoint[] values = new DataPoint[heartVal.size()];
        for (int i = 0; i < heartVal.size(); i++) {
            DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
            try {
                d = format.parse(date.get(i));
                dateList.add(d);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            DataPoint v = new DataPoint(d, heartVal.get(i));
            values[i] = v;
        }
        return values;
    }
    private void clearFile(String filename,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE));
            outputStreamWriter.write("");
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}
