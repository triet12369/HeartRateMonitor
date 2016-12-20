package com.triet12369.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import java.util.UUID;
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
    private BluetoothDevice device;
    final int handlerState = 0;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static String address; //"20:16:05:24:64:80"

    int Cpause=0;

    private final Handler mHandler = new Handler();
    private LineGraphSeries mSeries, mSeries2;
    private double graph2LastXValue = 5d;

    private LinkedList Data = new LinkedList();
    private LinkedList smoothBuffer = new LinkedList();
    int DATA_SIZE = 1000;
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
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

                heartVal = (int)(60/RRCal(BufferData));

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
                Log.d(TAG, "Read: " + readFromFile(filename, getContext()));
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



