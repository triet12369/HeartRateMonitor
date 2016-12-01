package com.triet12369.heartratemonitor;


import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class MonitorFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        View rootView = inflater.inflate(R.layout.fragment_menu_monitor, container, false);

        GraphView graph = (GraphView) rootView.findViewById(R.id.graph);
        mSeries1 = new LineGraphSeries<>(generateData());
        graph.addSeries(mSeries1);
        graph.getGridLabelRenderer().setHighlightZeroLines(false); //remove highlight of zero lines

        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);// remove horizontal x labels and line
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(10);
        graph.getViewport().setMinY(-2.0);
        graph.getViewport().setMaxY(2.0);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);

        TextView commentMessage = (TextView) rootView.findViewById(R.id.comment);
        commentMessage.setText(Comment(heartVal));

        TextView heartValue = (TextView) rootView.findViewById(R.id.HeartVal);
        heartValue.setText(""+heartVal);
        return rootView;
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Heart Rate Monitor");
    }


    int heartVal = 120;
    String heartComment=" ";

    private String Comment(int heartVal) {

        if ((heartVal >= 60) && (heartVal <= 100)) {
            heartComment = "normal";
        } else if (heartVal > 100) {
            heartComment = "too high";
        } else if (heartVal < 60) {
            heartComment = "too low";
        }
        String Message="Your heart rate is " + heartComment;
        return Message;
    }

    private final Handler mHandler = new Handler();
    private Runnable mTimer1;
    private LineGraphSeries<DataPoint> mSeries1;

    @Override
    public void onResume() {
        super.onResume();
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                mSeries1.resetData(generateData());
                mHandler.postDelayed(this, 300);
            }
        };
        mHandler.postDelayed(mTimer1, 300);
    }

//    @Override
//    public void onPause() {
//        mHandler.removeCallbacks(mTimer1);
//
//        super.onPause();
//    }

    private DataPoint[] generateData() {
        int count = 10;
        DataPoint[] values = new DataPoint[count];
        for (int i=0; i<count; i++) {
            double x = i;
            double f = mRand.nextDouble()*0.15+0.3;
            double y;
            y = Math.sin(i*f+2) + mRand.nextDouble()*0.3;

            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }
    Random mRand = new Random();

}


