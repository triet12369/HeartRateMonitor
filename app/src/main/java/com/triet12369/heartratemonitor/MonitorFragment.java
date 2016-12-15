package com.triet12369.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MonitorFragment extends Fragment implements View.OnClickListener{

    TextView testValue;
    Button buttonPause, buttonConnect;
    Handler bluetoothIn;

    final int handlerState = 0;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String address = "20:16:05:24:64:80";

    int Cpause=0;

    private Handler mHandler = new Handler();
    private Runnable mTimer;
    private LineGraphSeries mSeries;
    private double graph2LastXValue = 5d;

    private LinkedList Data = new LinkedList();
    int DATA_SIZE = 500;
    int handlerControl = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        View rootView = inflater.inflate(R.layout.fragment_menu_monitor, container, false);


        GraphView graph = (GraphView) rootView.findViewById(R.id.graph);
        mSeries = new LineGraphSeries<>();
        graph.addSeries(mSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(40);
        graph.getGridLabelRenderer().setHighlightZeroLines(false);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(1200);

        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Heart Rate Monitor");
        testValue = (TextView) getView().findViewById(R.id.textView2);
        buttonPause = (Button) getView().findViewById(R.id.buttonPause);
        buttonPause.setOnClickListener(this);
        buttonConnect = (Button) getView().findViewById(R.id.buttonConnect);
        buttonConnect.setOnClickListener(this);
        Data.clear();
        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerControl) {
                    String readMessage = (String) msg.obj;
                    /*
                    recDataString.append(readMessage);
                    if (recDataString.length() > 5) {
                        recDataString.delete(0, recDataString.length());
                    } */
                    Pattern p = Pattern.compile("\\[(.*?)\\]");
                    Matcher m = p.matcher(readMessage);
                    while (m.find()){
                        testValue.setText(""+Data.size());
                        graph2LastXValue += 1d;
                        mSeries.appendData(new DataPoint(graph2LastXValue, Integer.parseInt(m.group(1))), true, 42);
                        if (Data.size() < DATA_SIZE) {
                            Data.add(Integer.parseInt(m.group(1)));
                        } else {
                            Data.removeFirst();
                            Data.add(Integer.parseInt(m.group(1)));

                        }

                    }
                }
            }
        };







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
                    graph.getViewport().setMinY(0);
                    graph.getViewport().setMaxY(1200);
                    graph.getViewport().setMinX(Data.size() - 40);
                    graph.getViewport().setMaxX(Data.size());
                    graph.getViewport().setYAxisBoundsManual(true);
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setScalable(true);
                    graph.getViewport().setScalableY(true);
                    graph.removeAllSeries();
                    graph.addSeries(series);

                } else {
                    handlerControl = 0;
                    GraphView graph = (GraphView) getView().findViewById(R.id.graph);
                    mSeries = new LineGraphSeries<>();
                    graph.removeAllSeries();
                    graph.addSeries(mSeries);
                    graph.getViewport().setMinX(0);
                    graph.getViewport().setMaxX(40);
                    graph.getGridLabelRenderer().setHighlightZeroLines(false);
                    graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
                    graph.getGridLabelRenderer().setVerticalLabelsVisible(false);
                    graph.getViewport().setMinY(0);
                    graph.getViewport().setMaxY(1200);
                    graph.getViewport().setXAxisBoundsManual(true);
                    graph.getViewport().setYAxisBoundsManual(true);

                }
                break;
            case R.id.buttonConnect:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity(), "Connecting", Toast.LENGTH_SHORT).show();
                            }
                        });
                        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                        BluetoothDevice device = btAdapter.getRemoteDevice(address);
                        if (btAdapter.isEnabled()){
                            try {
                                btSocket = createBluetoothSocket(device);
                            } catch (IOException e) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getActivity(), "Socket creation failed", Toast.LENGTH_LONG).show();
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
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

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
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
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

}



