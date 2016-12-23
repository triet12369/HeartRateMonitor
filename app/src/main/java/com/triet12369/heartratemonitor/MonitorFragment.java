package com.triet12369.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.sql.BatchUpdateException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;





public class MonitorFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "MonitorFragment";

    TextView testValue, textHeartValue, ecgStatus;
    Button buttonPause, buttonConnect, buttonAppend;
    Handler bluetoothIn;
    CheckBox checkBox;

    private SharedPreferences mSharedPreference;
    private String mHistoryInterval;
    private String mDetectMethod;

    private BluetoothDevice device;
    final int handlerState = 0;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String address; //"20:16:05:24:64:80"

    int Cpause=0;

    private final Handler mHandler = new Handler();
    Timer t = new Timer();
    private LineGraphSeries mSeries, mSeries2;
    private double graph2LastXValue = 5d;

    private LinkedList Data = new LinkedList();
    private LinkedList smoothBuffer = new LinkedList();
    int DATA_SIZE = 2000;
    int VIEW_WINDOW = 250;
    int handlerControl = 0;

    private int heartVal;
    private double fs = 105;
    double Rthreshold=500;
    private int maxY = 500;
    private int minY = -200;
    private boolean showThreshold = false;
    private int output;
    long timeTemp = 0, timeDiff;
    int count = 0;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        View rootView = inflater.inflate(R.layout.fragment_menu_monitor, container, false);


        GraphView graph = (GraphView) rootView.findViewById(R.id.graph);
        mSeries = new LineGraphSeries<>();
        mSeries2 = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.addSeries(mSeries2);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(VIEW_WINDOW);
        graph.getGridLabelRenderer().setHighlightZeroLines(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.getViewport().setMinY(minY);
        graph.getViewport().setMaxY(maxY);

        return rootView;
    }


    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Heart Rate Monitor");
        testValue = (TextView) getView().findViewById(R.id.textView2);
        textHeartValue = (TextView) getView().findViewById(R.id.HeartVal);
        ecgStatus = (TextView) getView().findViewById(R.id.ECGStatus);
        ecgStatus.setText(R.string.status_standby);
        buttonPause = (Button) getView().findViewById(R.id.buttonPause);
        buttonPause.setOnClickListener(this);
        buttonConnect = (Button) getView().findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(this);
        buttonAppend = (Button) getView().findViewById(R.id.buttonAppend);
        buttonAppend.setOnClickListener(this);
        checkBox = (CheckBox) getActivity().findViewById(R.id.checkThreshold);
        checkBox.setOnClickListener(this);
        Data.clear();
        mSharedPreference = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mDetectMethod = mSharedPreference.getString("list_preference", "randomguessing");
        mHistoryInterval = mSharedPreference.getString("edit_text_preference_1", "69");
        int nHistoryInterval;
        try {
            nHistoryInterval = Integer.parseInt(mHistoryInterval);
        } catch (NumberFormatException e) {
            nHistoryInterval = 10;
            Toast.makeText(getActivity(), "Invalid input for history update interval", Toast.LENGTH_LONG).show();
        }

        testValue.setText(getString(R.string.current_method, mDetectMethod));


        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                StringBuilder temp = new StringBuilder();
                int check = 0;
                if (msg.what == handlerControl) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    if (recDataString.charAt(recDataString.length()-1) != ']') {
                        for (int i=0; i < recDataString.length(); i++) {
                            if (recDataString.charAt(i) == '[') {
                                check = 1;
                            }
                            if (recDataString.charAt(i) == ']' && check == 1) {
                                if (temp.charAt(0) == 'a') {
                                    check = 0;
                                    ecgStatus.setText(R.string.leads_off);
                                    graph2LastXValue += 1d;
                                    mSeries.appendData(new DataPoint(graph2LastXValue, 500), true, VIEW_WINDOW);
                                } else {
                                    check = 0;
                                    ecgStatus.setText(R.string.status_connected);
                                    //testValue.setText(""+readMessage);
                                    graph2LastXValue += 1d;
                                    mSeries.appendData(new DataPoint(graph2LastXValue, Integer.parseInt(temp.toString())), true, VIEW_WINDOW);
                                    if (showThreshold) {
                                        mSeries2.appendData(new DataPoint(graph2LastXValue - 1,  (int) Rthreshold), true, VIEW_WINDOW);
                                    }

                                    count++;
                                    if (count >= Data.size()/2) {
                                        timeDiff = (System.nanoTime() - timeTemp);
                                        timeTemp = System.nanoTime();
                                        double f = (double)timeDiff/1000000000.0;
                                        count = 0;
                                        fs = (Data.size()/2.0)/f;
                                    }

                                    if (Data.size() < DATA_SIZE) {
                                        Data.add(Integer.parseInt(temp.toString()));
                                    } else {
                                        Data.removeFirst();
                                        Data.add(Integer.parseInt(temp.toString()));
                                    }
                                    recDataString.delete(0, i);
                                }
                                temp.delete(0, temp.length());
                            }
                            if (check == 1 && recDataString.charAt(i) != '[') {
                                temp.append(recDataString.charAt(i));
                            }
                        }

                    }
                    }
                }
        };

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int[] BufferData = new int[Data.size()/4];
                int[] heartValArray = new int[smoothBuffer.size()];
                for (int i = 0; i < Data.size()/4; i++) {
                    BufferData[i] = (Integer) Data.get(i+(Data.size()*3/4));
                }
                switch (mDetectMethod) {
                    case "Thresholding":
                        heartVal = (int)(60/RRCal(BufferData));
                        break;
                    case "Pan-Tompkins":
                        checkBox.setVisibility(View.GONE);
                        heartVal = (int)(60*fs/pan_tompkin(BufferData));
                        break;
                }

                for (int i = 0; i < smoothBuffer.size(); i++) {
                    heartValArray[i] = (Integer) smoothBuffer.get(i);
                }
                //Log.d(TAG, "debug: " + Arrays.toString(heartValArray));

                if (heartVal > 20 && heartVal < 200) {
                    if (smoothBuffer.size() < 5) {
                        smoothBuffer.add(heartVal);
                    } else {
                        smoothBuffer.removeFirst();
                        smoothBuffer.add(heartVal);
                    }
                }
                output = meanInt(heartValArray);

                if (BufferData.length > 0) {
                    textHeartValue.setText(getString(R.string.placeholder_int, output));
                    if (output > 60 && output < 90) {
                        textHeartValue.setTextColor(getResources().getColorStateList(R.color.darkGreen));
                    } else {
                        textHeartValue.setTextColor(getResources().getColorStateList(R.color.darkYellow));
                    }

                    maxY = maxInt(BufferData);
                    minY = minInt(BufferData);
                    GraphView graph = (GraphView) getView().findViewById(R.id.graph);
                    graph.getViewport().setMinY(minY-100);
                    graph.getViewport().setMaxY(maxY+100);
                    Rthreshold = maxY - (maxY-minY)/3;

                }
                mHandler.postDelayed(this, 1000);
            }
        }, 0);

        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if (heartVal > 20 && heartVal < 200 && handlerControl == 0) {
                    Calendar calendar = Calendar.getInstance();
                    Date d = calendar.getTime();
                    String data = output + "," + d + ";" + "\n";
                    String filename = "history_log";
                    FileOutputStream outputStream;
                    try {
                        outputStream = getContext().openFileOutput(filename, Context.MODE_APPEND);
                        outputStream.write(data.getBytes());
                        Toast.makeText(getActivity(), "Added to history", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Append: " + data);
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //Log.d(TAG, "Read: " + readFromFile(filename, getContext()));
                }
            }

        },
            //Set how long before to start calling the TimerTask (in milliseconds)
                0,
                //Set the amount of time between each execution (in milliseconds)
                nHistoryInterval*1000);





    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null){
                btSocket.close();
            }
        } catch (IOException e) {

        }
        mHandler.removeCallbacksAndMessages(null);
        t.cancel();
    }
    public void onClick (View view) {
        switch (view.getId()){
            case R.id.buttonPause:
                if (handlerControl == 0) {
                    handlerControl = 1;
                    DataPoint[] bufferData = new DataPoint[Data.size()];
                    for (int i = 0; i < Data.size(); i++) {
                        bufferData[i] = new DataPoint(i, (Integer) Data.get(i));
                    }
                    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(bufferData);
                    GraphView graph = (GraphView) getView().findViewById(R.id.graph);
                    graph.getViewport().setMinY(minY-100);
                    graph.getViewport().setMaxY(maxY+100);
                    graph.getViewport().setMinX(Data.size() - VIEW_WINDOW);
                    graph.getViewport().setMaxX(Data.size());
                    graph.getViewport().setYAxisBoundsManual(true);
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setScalable(true);
                    //graph.getViewport().setScalableY(true);
                    graph.removeAllSeries();
                    graph.addSeries(series);
                    buttonPause.setText("Unpause");

                } else {
                    handlerControl = 0;
                    GraphView graph = (GraphView) getView().findViewById(R.id.graph);
                    mSeries = new LineGraphSeries<>();
                    graph.removeAllSeries();
                    graph.addSeries(mSeries);
                    graph.getViewport().setMinX(0);
                    graph.getViewport().setMaxX(VIEW_WINDOW);
                    graph.getGridLabelRenderer().setHighlightZeroLines(false);
                    graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
                    graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
                    graph.getViewport().setMinY(minY-100);
                    graph.getViewport().setMaxY(maxY+100);
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setYAxisBoundsManual(true);
                    buttonPause.setText("Pause");
                }
                break;
            case R.id.buttonConnect:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ecgStatus.setText(R.string.status_connecting);
                            }
                        });
                        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                        if (address == null) {
                            address = "20:16:05:24:64:80";
                            device = btAdapter.getRemoteDevice(address);
                        } else {
                            device = btAdapter.getRemoteDevice(address);
                        }
                        if (btAdapter.isEnabled()){
                            try {
                                btSocket = createBluetoothSocket(device);
                            } catch (IOException e) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                                    }
                                });                        }
                            // Establish the Bluetooth socket connection.
                            try
                            {
                                btSocket.connect();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "Connected", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } catch (IOException e) {
                                try
                                {
                                    btSocket.close();
                                } catch (IOException e2)
                                {
                                }
                            }
                            ConnectedThread mConnectedThread = new ConnectedThread(btSocket);
                            mConnectedThread.start();
                            }
                        }
                    }).start();
                break;
            case R.id.checkThreshold:
                if (checkBox.isChecked()) {
                    showThreshold = true;
                    Log.d(TAG, "debug: " + showThreshold);
                    checkBox.setChecked(true);
                } else {
                    showThreshold = false;
                    Log.d(TAG, "debug: " + showThreshold);
                    checkBox.setChecked(false);
                }
                break;
            case R.id.buttonAppend:
                Calendar calendar = Calendar.getInstance();
                Date d = calendar.getTime();
                String data = output + "," + d + ";" + "\n";
                String filename = "history_log";
                FileOutputStream outputStream;
                try {
                    outputStream = getContext().openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write(data.getBytes());
                    Toast.makeText(getActivity(), "Added to history", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Append: " + data);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Log.d(TAG, "Read: " + readFromFile(filename, getContext()));
                break;
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final StringBuilder temp = new StringBuilder();

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[20];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    if (mmInStream.available() > 0){
                        bytes = mmInStream.read(buffer);            //read bytes from input buffer
                        String readMessage = new String(buffer, 0, bytes);
                        // Send the obtained bytes to the UI Activity via handler
                        bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getActivity(), "Connection Failure", Toast.LENGTH_LONG).show();

            }
        }

    }
    private int maxInt(int[] data) {
        int maxV=0;
        for (int i=0; i<data.length; i++){
            if (data[i]>=maxV){
                maxV=data[i];
            }
        }
        return maxV;
    }
    private int minInt(int[] data) {
        int minV=data[0];
        for (int i=0; i<data.length; i++){
            if (data[i]<=minV){
                minV=data[i];
            }
        }
        return minV;
    }
    private int[] findpeaksloc(int[] data){
        int[] peaklocsTemp= new int[data.length];
        int countP=0;
        for (int i=1;i<data.length-1;i++){
            if ((data[i]>=Rthreshold)&&(data[i-1]<data[i])&&(data[i+1]<data[i])){
                countP=countP+1;
                peaklocsTemp[countP-1]=i;
            }
        }
        int[] peaklocs=new int[countP];
        for (int j=0;j<countP;j++){
            peaklocs[j]=peaklocsTemp[j];
        }

        return peaklocs;
    }

    //calculate the mean of heart rate
    private double RRCal (int[] data) {
        int[] peak = findpeaksloc(data);
//        int count=0;
        int sumdiff = 0;
        double RRVal = 0;
        if (peak.length > 1) {
            double[] RR = new double[peak.length - 1];
            for (int i = 1; i < peak.length; i++) {
                int diff = peak[i] - peak[i - 1];
                RR[i - 1] = (double) diff / fs;
//            sumdiff=sumdiff+RR;
//            count=count+1;
            }
            //Log.d(TAG, "debug: " + Arrays.toString(RR));
            RRVal = meanDouble(RR);
        }
        //RR=sumdiff/count;
        return RRVal;

    }
    private double meanDouble(double[] data){
        double sum=0;
        double meanV;
        for (int i=0;i<data.length;i++){
            sum=sum+data[i];
        }
        meanV=sum/(data.length - 0.0);
        //Log.d(TAG, "debug: " + meanV);

        return meanV;
    }
    private int meanInt (int[] data) {
        int sum = 0;
        for (int d : data) sum += d;
        return (int)(1.0d * sum/data.length);
    }

    private double pan_tompkin (int[] ecg){
        double[] ecg_h=new double[ecg.length];
        int delay=0;
        for (int i=0;i<ecg_h.length;i++){
            ecg_h[i]=(double)ecg[i]/(double) maxInt(ecg);
        }
        //derivative filter
        double[] h_d={-1, -2, 0, 2, 1};

        for(int i=0;i<h_d.length;i++){
            h_d[i]=h_d[i]/8.0;
        }

        double[] ecg_d_temp=conv3(ecg_h,h_d);
        double max_ecg_d_temp=maxDouble(ecg_d_temp);
        double[] ecg_d=new double[ecg_d_temp.length];
        for (int i=0;i<ecg_h.length;i++){
            ecg_d[i]=ecg_d_temp[i]/max_ecg_d_temp;//derivative data
        }
        delay=delay+2;

        //squaring
        double[] ecg_s=new double[ecg_d.length];
        for (int i=0;i<ecg_s.length;i++){
            ecg_s[i]=ecg_d[i]*ecg_d[i];
        }

        //moving average
        int[] h_m_temp=ones(round(0.15*fs));
        double[] h_m=new double[h_m_temp.length];
        for (int i=0;i<h_m.length;i++){
            h_m[i]=(double)h_m_temp[i]/(double)round(0.15*fs);
        }
        double[] ecg_m=conv3(ecg_s,h_m);//moving avg data
        delay=delay+15;

        //Fiducial Mark
        int[] locs=findpeaksLoc(ecg_m,round(0.2*fs));
        double[] pks=findpeakVal(ecg_m,round(0.2*fs));

        //initialize the training phase (2 seconds of the signal) to determine the THR_SIG and THR_NOISE
        double THR_SIG = maxDouble(Arrays.copyOfRange(ecg_m,0,round(4*fs-1)))/3.0;
        double THR_NOISE = meanDouble(Arrays.copyOfRange(ecg_m,0,round(4*fs-1)))/2.0;
        double SIG_LEV = THR_SIG;
        double NOISE_LEV = THR_NOISE;

        //Initialize bandpath filter threshold(2 seconds of the bandpass signal)
        double THR_SIG1=maxDouble(Arrays.copyOfRange(ecg_h,0,round(4*fs-1)))/3.0;
        double THR_NOISE1=meanDouble(Arrays.copyOfRange(ecg_h,0,round(4*fs-1)))/2.0;
        double SIG_LEV1 = THR_SIG1;
        double NOISE_LEV1 = THR_NOISE1;
        double y_i=0;
        double x_i=0;
        //Log.d(TAG, "pan-Tomkins: level 1");
        //Initialize
        double[] qrs_c=new double[0];
        int[] qrs_i=new int[0];
        double mean_RR=0;
        double m_selected_RR=0;
        double test_m=0;
        double[] qrs_i_raw=new double[0];
        double[] qrs_amp_raw=new double[0];
        double[] nois_c=new double[0];
        double[] nois_i=new double[0];
        int skip=0;
        int not_nois=0;
        int ser_back=0;
        double[] SIGL_buf=new double[0];
        double[] NOISL_buf=new double[0];
        double[] THRL_buf=new double[0];
        double[] SIGL_buf1=new double[0];
        double[] NOISL_buf1=new double[0];
        double[] THRL_buf1=new double[0];
        double a=0;
        //Thresholding and online desicion rule
        for (int i=0;i<pks.length;i++){

            //locate the corresponding peak in the filtered signal
            if ((locs[i]-round(0.150*fs)>=1)&&(locs[i]<=ecg_h.length)){
                y_i=maxDouble(Arrays.copyOfRange(ecg_h,locs[i]-round(0.150*fs),locs[i]));
                x_i=imaxDouble(Arrays.copyOfRange(ecg_h,locs[i]-round(0.150*fs),locs[i]));
            }
            else if (i==0){
                y_i=maxDouble(Arrays.copyOfRange(ecg_h,0,locs[i]));
                x_i=imaxDouble(Arrays.copyOfRange(ecg_h,0,locs[i]));
            }
            else if (locs[i]>=ecg_h.length-1){
                y_i=maxDouble(Arrays.copyOfRange(ecg_h,locs[i]-round(0.150*fs),ecg_h.length-1));
                x_i=imaxDouble(Arrays.copyOfRange(ecg_h,locs[i]-round(0.150*fs),ecg_h.length-1));
            }

            //update the heart_rate (Two heart rate means one the most recent and the other selected)
            if (qrs_c.length>=9){
                int[] diffRR=diffInt(Arrays.copyOfRange(qrs_i,qrs_i.length-8-1,qrs_i.length-1));
                mean_RR=(double) meanInt(diffRR);
                int comp=qrs_i[qrs_i.length-1]-qrs_i[qrs_i.length-1-1];
                if ((comp<=0.92*mean_RR)||(comp>=1.16*mean_RR)){
                    //lower down thresholds to detect better in MVI
                    THR_SIG=0.5*THR_SIG;
                    //lower down thresholds to detect better in Bandpass filtered
                    THR_SIG1 = 0.5*THR_SIG1;
                }
                else {
                    m_selected_RR=mean_RR;
                }
            }

            //calculate the mean of the last 8 R waves to make sure that QRS is not missing
            //(If no R detected , trigger a search back) 1.66*mean
            if (m_selected_RR!=0){
                test_m=m_selected_RR;
            }
            else if ((mean_RR!=0)&&(m_selected_RR==0)){
                test_m=mean_RR;
            }
            else {
                test_m=0;
            }
            if (test_m!=0){
                if ((locs[i]-qrs_i[qrs_i.length-1])>=(round(1.66*test_m))){
                    int temp = (qrs_i[qrs_i.length-1]+round(0.200*fs)) - (locs[i]-round(0.200*fs));
                    //Log.d(TAG, "pan-Tomkins: " + temp);
                    if (temp < 0) {
                        double pks_temp=maxDouble(Arrays.copyOfRange(ecg_m,qrs_i[qrs_i.length-1]+round(0.200*fs),locs[i]-round(0.200*fs)));
                        double locs_temp=imaxDouble(Arrays.copyOfRange(ecg_m,qrs_i[qrs_i.length-1]+round(0.200*fs),locs[i]-round(0.200*fs)));
                        locs_temp=qrs_i[qrs_i.length-1]+round(0.200*fs)+locs_temp-1-1;
                        if (pks_temp>THR_NOISE){
                            qrs_c=Arrays.copyOf(qrs_c,qrs_c.length+1);
                            qrs_c[qrs_c.length-1]=pks_temp;
                            double y_i_t;
                            double x_i_t;
                            if (locs_temp<=ecg_h.length-1){
                                y_i_t=maxDouble(Arrays.copyOfRange(ecg_h,(int)(locs_temp-round(0.150*fs)),(int)locs_temp));
                                x_i_t=imaxDouble(Arrays.copyOfRange(ecg_h,(int)(locs_temp-round(0.150*fs)),(int)locs_temp));
                            }
                            else{
                                y_i_t=maxDouble(Arrays.copyOfRange(ecg_h,(int)(locs_temp-round(0.150*fs)),ecg_h.length-1));
                                x_i_t=imaxDouble(Arrays.copyOfRange(ecg_h,(int)(locs_temp-round(0.150*fs)),ecg_h.length-1));
                            }
                            if (y_i_t>THR_NOISE1){
                                SIG_LEV1=0.25*y_i_t+0.75*SIG_LEV1;
                                qrs_i_raw=Arrays.copyOf(qrs_i_raw,qrs_i_raw.length+1);
                                qrs_i_raw[qrs_i_raw.length-1]=locs_temp-round(0.15*fs)+(x_i_t-1);
                                qrs_amp_raw=Arrays.copyOf(qrs_amp_raw,qrs_amp_raw.length+1);
                                qrs_amp_raw[qrs_amp_raw.length-1]=y_i_t;

                            }
                            not_nois=1;
                            SIG_LEV=0.25*pks_temp+0.75*SIG_LEV;
                        }
                        else {not_nois=0;}
                    }
                }
            }

            //find noise and QRS peaks
            if (pks[i]>=THR_SIG){
                //if a QRS candidate occurs within 360ms of the previous QRS
                //the algorithm determines if its T wave or QRS
                if (qrs_c.length>=3){
                    if ((locs[i]-qrs_i[qrs_i.length-1])<=(round(0.3600*fs))){
                        double Slope1 =meanDouble(diffDouble(Arrays.copyOfRange(ecg_m,locs[i]-round(0.075*fs),locs[i])));
                        double Slope2 =meanDouble(diffDouble(Arrays.copyOfRange(ecg_m,qrs_i[qrs_i.length-1]-round(0.075*fs),qrs_i[qrs_i.length-1])));
                        if (Math.abs(Slope1)<=Math.abs(Slope2)){
                            nois_c=Arrays.copyOf(nois_c,nois_c.length+1);
                            nois_c[nois_c.length-1]=pks[i];
                            nois_i=Arrays.copyOf(nois_i,nois_i.length+1);
                            nois_i[nois_i.length-1]=locs[i];
                            skip=1;
                            NOISE_LEV1=0.125*y_i + 0.875*NOISE_LEV1;
                            NOISE_LEV = 0.125*pks[i] + 0.875*NOISE_LEV;
                        }
                        else {skip=0;}
                    }
                }
                if (skip==0){
                    qrs_c=Arrays.copyOf(qrs_c,qrs_c.length+1);
                    qrs_c[qrs_c.length-1]=pks[i];
                    qrs_i=Arrays.copyOf(qrs_i,qrs_i.length+1);
                    qrs_i[qrs_i.length-1]=locs[i];
                    if (y_i>=THR_SIG1){
                        if (ser_back!=0){
                            qrs_i_raw=Arrays.copyOf(qrs_i_raw,qrs_i_raw.length+1);
                            qrs_i_raw[qrs_i_raw.length-1]=x_i;

                        }
                        else {
                            qrs_i_raw=Arrays.copyOf(qrs_i_raw,qrs_i_raw.length+1);
                            qrs_i_raw[qrs_i_raw.length-1]=locs[i]-round(0.150*fs)+(x_i-1);

                        }
                        qrs_amp_raw=Arrays.copyOf(qrs_amp_raw,qrs_amp_raw.length+1);
                        qrs_amp_raw[qrs_amp_raw.length-1]=y_i;
                        SIG_LEV1 = 0.125*y_i + 0.875*SIG_LEV1;
                    }
                    SIG_LEV = 0.125*pks[i] + 0.875*SIG_LEV ;
                    if (qrs_i_raw.length>=3){
                        a=meanDouble(diffDouble(Arrays.copyOfRange(qrs_i_raw,qrs_i_raw.length-3,qrs_i_raw.length-1)));
                    }
                    else {a=80;}

                }
            }
            else if ((THR_NOISE <= pks[i]) && (pks[i]<THR_SIG)){
                NOISE_LEV1 = 0.125*y_i + 0.875*NOISE_LEV1;
                NOISE_LEV = 0.125*pks[i] + 0.875*NOISE_LEV;
            }
            else if (pks[i] < THR_NOISE){
                nois_c=Arrays.copyOf(nois_c,nois_c.length+1);
                nois_c[nois_c.length-1]=pks[i];
                nois_i=Arrays.copyOf(nois_i,nois_i.length+1);
                nois_i[nois_i.length-1]=locs[i];
                NOISE_LEV1 = 0.125*y_i + 0.875*NOISE_LEV1;
                NOISE_LEV = 0.125*pks[i] + 0.875*NOISE_LEV;
            }

            // adjust the threshold with SNR
            if ((NOISE_LEV!=0)||(SIG_LEV!=0)){
                THR_SIG=NOISE_LEV+0.25*(Math.abs(SIG_LEV-NOISE_LEV));
                THR_NOISE = 0.5*(THR_SIG);
            }
            if ((NOISE_LEV1!=0)||(SIG_LEV1!=0)){
                THR_SIG1=NOISE_LEV1 + 0.25*(Math.abs(SIG_LEV1 - NOISE_LEV1));
                THR_NOISE1 = 0.5*(THR_SIG1);
            }
            SIGL_buf=Arrays.copyOf(SIGL_buf,SIGL_buf.length+1);
            SIGL_buf[SIGL_buf.length-1]=SIG_LEV;
            NOISL_buf=Arrays.copyOf(NOISL_buf,NOISL_buf.length+1);
            NOISL_buf[NOISL_buf.length-1]=NOISE_LEV;
            THRL_buf=Arrays.copyOf(THRL_buf,THRL_buf.length+1);
            THRL_buf[THRL_buf.length-1]=THR_SIG;

            SIGL_buf1=Arrays.copyOf(SIGL_buf1,SIGL_buf1.length+1);
            SIGL_buf1[SIGL_buf1.length-1]=SIG_LEV1;
            NOISL_buf1=Arrays.copyOf(NOISL_buf1,NOISL_buf1.length+1);
            NOISL_buf1[NOISL_buf1.length-1]=NOISE_LEV1;
            THRL_buf1=Arrays.copyOf(THRL_buf1,THRL_buf1.length+1);
            THRL_buf1[THRL_buf1.length-1]=THR_SIG1;

            skip = 0; //reset parameters
            not_nois = 0; //reset parameters
            ser_back = 0;  //reset bandpass param
        }
        //Log.d(TAG, "pan-Tomkins: level 2");


        return a;
    }

    private int[] sign(double[] data){
        int[] s = new int[data.length];
        for (int i=0;i<s.length;i++){
            if (data[i]>0){s[i]=1;}
            else if (data[i]<0){s[i]=-1;}
            else {s[i]=0;}
        }
        return s;
    }
    private int[] find (int[] data,int value){
        int[] f=new int[0];
        for (int i=0;i<data.length;i++){
            if (data[i]==value){
                f=Arrays.copyOf(f,f.length+1);
                f[f.length-1]=i;
            }
        }
        return f;
    }
    //find location of peaks of an array with defined threshold
    private int[] findpeaksLoc(double[] data,int MPD){
        int[] trend=sign(diffDouble(data));
        int[] idx=find(trend,0);
        int[] locs=new int[1];
        for (int i=idx.length-1;i>=0;i--){
            if (trend[Math.min(idx[i]+1,trend.length-1)]>=0){
                trend[idx[i]]=1;
            }
            else {
                trend[idx[i]]=-1;
            }
        }
        int[] idxp=find(diffInt(trend),-2);
        for (int i=0;i<idxp.length-1;i++){
            idxp[i]+=1;
            if (i==0){
                locs[i]=idxp[i];
            }
        }
        int j=0;
        for (int i=1;i<idxp.length;i++){
            if ((idxp[i]-idxp[j])>MPD){
                locs=Arrays.copyOf(locs,locs.length+1);
                locs[locs.length-1]=idxp[i];
                j=i;
            }
        }
        return idxp;
    }
    private double[] conv3(double[]u,double[] v){
        int m=u.length;
        int n=v.length;
        int wl=m+n-1;
        double[] w= new double[wl];
        for (int k=1;k<wl;k++){
            double sum=0;
            int minj=Math.max(1,k+1-n);
            int maxj=Math.min(k,m);
            for (int j=minj;j<=maxj;j++){
                double temp=(double) u[j-1]*v[k-j+1-1];
                sum=sum+temp;
            }
            w[k-1]=sum;
        }
        return w;
    }

    private double[] findpeakVal(double[] data,int MPD){
        int[] locs=findpeaksLoc(data,MPD);
        double[] pks=new double[locs.length];
        for (int i=0;i<pks.length;i++){
            pks[i]=data[locs[i]];
        }
        return pks;
    }
    private int[] conv(int[]u,int[] v){
        int m=u.length;
        int n=v.length;
        int wl=m+n-1;
        int[] w= new int[wl];
        for (int k=1;k<wl;k++){
            int sum=0;
            int minj=Math.max(1,k+1-n);
            int maxj=Math.min(k,m);
            for (int j=minj;j<=maxj;j++){
                int temp=u[j-1]*v[k-j+1-1];
                sum=sum+temp;
            }
            w[k-1]=sum;
        }
        return w;
    }
    private double maxDouble(double[] data) {
        double maxV=0;
        for (int i=0; i<data.length; i++){
            if (data[i]>=maxV){
                maxV=data[i];
            }
        }
        return maxV;
    }
    private double imaxDouble(double[] data) {
        double maxi=0;
        double maxV=0;
        for (int i=0; i<data.length; i++){
            if (data[i]>maxV){
                maxV=data[i];
                maxi=(double) i;
            }
        }
        return maxi;
    }
    private int[] diffInt (int[] data){
        int[] d= new int[data.length-1];
        for (int i=1;i<data.length;i++){
            d[i-1]=data[i]-data[i-1];
        }
        return d;
    }
    private double[] diffDouble (double[] data){
        double[] d= new double[data.length-1];
        for (int i=1;i<data.length;i++){
            d[i-1]=data[i]-data[i-1];
        }
        return d;
    }
    private int[] ones(int column){
        int[] array=new int[column];
        for (int i=0;i<column;i++){
            array[i]=1;
        }
        return array;
    }

    private int round(double x){
        int r=0;
        if ((x-(int)x)>=0.5){
            r=(int)x+1;
        }
        else {
            r=(int)x;
        }
        return r;
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
                    stringBuilder.append(receiveString).append("\n");
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
}



