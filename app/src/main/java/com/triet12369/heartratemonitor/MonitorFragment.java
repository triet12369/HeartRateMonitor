package com.triet12369.heartratemonitor;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Heart Rate Monitor");


    }


        private final Handler mHandler = new Handler();
        private Runnable mTimer1;

        private LineGraphSeries<DataPoint> mSeries1;


        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//
//        }

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

        @Override
        public void onPause() {
            mHandler.removeCallbacks(mTimer1);

            super.onPause();
        }

        private DataPoint[] generateData() {
            int count = 30;
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

        double mLastRandom = 2;
        Random mRand = new Random();
        private double getRandom() {
            return mLastRandom += mRand.nextDouble()*0.5 - 0.25;
        }


}