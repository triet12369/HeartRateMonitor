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
//        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(16);
        graph.getViewport().setMinY(-1);
        graph.getViewport().setMaxY(1);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);

        TextView commentMessage = (TextView) rootView.findViewById(R.id.comment);
        commentMessage.setText(Comment(heartVal));

        TextView heartValue = (TextView) rootView.findViewById(R.id.HeartVal);
        heartValue.setText(""+heartVal);
//        float[] l=lowPass(DemoData,2);
//        heartValue.setText(""+QRS(l,2));
        return rootView;
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Heart Rate Monitor");
    }


    int heartVal = 0;
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
    int j=0;
    @Override
    public void onResume() {
        super.onResume();
        mTimer1 = new Runnable() {
            @Override
            public void run() {
                if (j<28){
                j=j+1;} else {j=0;}
                mSeries1.resetData(generateData());
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(mTimer1, 200);
    }

//    @Override
//    public void onPause() {
//        mHandler.removeCallbacks(mTimer1);
//
//        super.onPause();
//    }
double[] DemoData = {0.02, 0.02, 0.02, 0.02, 0.05, 0.07, 0.06, 0.09, 0.06,
                -0.2, 0.9, 0.5, -0.1, 0.02, 0.02, 0.04, 0.1, 0.08, 0.03, 0.02, 0.02,0.02, 0.02, 0.02, 0.05, 0.07, 0.06, 0.09, 0.06,
                -0.2, 0.9, 0.5, -0.1, 0.02, 0.02, 0.04, 0.1, 0.08, 0.03, 0.02, 0.02, 0.02, 0.02, 0.02};

    private DataPoint[] generateData() {
        int count = 16;

        DataPoint[] values = new DataPoint[count];
//        int j=0;

            int i=0;
        while (i<count){
            double x = i;
            double f = mRand.nextDouble()*0.15+0.3;
            double y;
//            y = Math.sin(i*f+2) + mRand.nextDouble()*0.3;

              y=DemoData[i+j];
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
            i=i+1;
//            if (i==10){
//                j=j+1;
//            }

        }

        return values;
    }


    Random mRand = new Random();



//  
}

