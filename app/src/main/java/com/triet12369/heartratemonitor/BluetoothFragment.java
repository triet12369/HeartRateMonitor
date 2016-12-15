package com.triet12369.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.ToggleButton;

import java.util.ArrayList;


public class BluetoothFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private static final String TAG = "BluetoothFragment";
    BluetoothAdapter mBluetoothAdapter;
    ToggleButton btnONOFF;
    Button btnDA;
    Button btnD;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;

    // Create a BroadcastReceiver for ACTION_FOUND
    private BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }

        }
    };
    // Create receiver for discoverability
    private BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        if (mBroadcastReceiver1 != null) {
            // Sometimes the Fragment onDestroy() unregisters the observer before calling below code
            // See <a>http://stackoverflow.com/questions/6165070/receiver-not-registered-exception-error</a>
            try  {
                getActivity().unregisterReceiver(mBroadcastReceiver1);
                mBroadcastReceiver1 = null;
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
                }
            }

        if (mBroadcastReceiver2 != null) {
            // Sometimes the Fragment onDestroy() unregisters the observer before calling below code
            // See <a>http://stackoverflow.com/questions/6165070/receiver-not-registered-exception-error</a>
            try  {
                getActivity().unregisterReceiver(mBroadcastReceiver2);
                mBroadcastReceiver2 = null;
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
                }
            }
        if (mBroadcastReceiver3 != null) {
            // Sometimes the Fragment onDestroy() unregisters the observer before calling below code
            // See <a>http://stackoverflow.com/questions/6165070/receiver-not-registered-exception-error</a>
            try  {
                getActivity().unregisterReceiver(mBroadcastReceiver3);
                mBroadcastReceiver3 = null;
            }
            catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        View view = inflater.inflate(R.layout.fragment_menu_bluetooth,container,false);

//        editText=(EditText)view.findViewById(R.id.name);
//        button=(Button)view.findViewById(R.id.submit_id);
//        button.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View view){
//                String Name = editText.getText().toString();
//                onNameSetListener.setName(Name);
//            }
//        });
        return view;

    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Bluetooth Devices");
        btnONOFF = (ToggleButton) getView().findViewById(R.id.toggleButton);
        btnONOFF.setOnCheckedChangeListener(this);
        btnDA = (Button) getView().findViewById(R.id.buttonDiscoverAbility);
        btnDA.setOnClickListener(this);
        btnD = (Button) getView().findViewById(R.id.buttonDiscover);
        btnD.setOnClickListener(this);
        lvNewDevices = (ListView) getView().findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
            }

    public void enableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableBT: Does not have bluetooth capabilities");
        } else {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(mBroadcastReceiver1, BTIntent);
            Log.d(TAG, "enableBT: called");
        }
    }

    public void disableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "disableBT: Does not have bluetooth capabilities");
        } else {
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            getActivity().registerReceiver(mBroadcastReceiver1, BTIntent);
            Log.d(TAG, "disableBT: called");
        }
    }

    public void onCheckedChanged(CompoundButton btnONOFF, boolean isChecked) {
        if (isChecked) {
            enableBT();
        } else {
            disableBT();
        }
    }
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonDiscoverAbility:
                Log.d(TAG, "btnDA: Making device discoverable for 300 seconds.");

                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);

                IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                getActivity().registerReceiver(mBroadcastReceiver2, intentFilter);
                break;
            case R.id.buttonDiscover:
                Log.d(TAG, "btnD: Looking for unpaired devices.");
                if (mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                    Log.d(TAG, "btnD: Canceling discovery.");
                    checkBTPermissions();
                    mBluetoothAdapter.startDiscovery();
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    getActivity().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                }
                if (!mBluetoothAdapter.isDiscovering()){
                    checkBTPermissions();
                    mBluetoothAdapter.startDiscovery();
                    IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    getActivity().registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                }
                break;
        }
    }
    // This method is required for all devices running API 23+
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions.");
        }
    }

//    EditText editText;
//    Button button;
//    OnNameSetListener onNameSetListener;
//
//
//    public interface OnNameSetListener {
//        public void setName (String name);
//    }

//    @Override
//    public void onAttach(Activity activity){
//        super.onAttach(activity);
//        try {
//            onNameSetListener = (OnNameSetListener) activity;
//        } catch (Exception e){}
//    }
}
