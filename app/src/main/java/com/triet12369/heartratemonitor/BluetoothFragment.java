package com.triet12369.heartratemonitor;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import static android.content.ContentValues.TAG;


public class BluetoothFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    BluetoothAdapter mBluetoothAdapter;
    ToggleButton btnONOFF;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
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

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        getActivity().unregisterReceiver(mBroadcastReceiver1);
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
                return inflater.inflate(R.layout.fragment_menu_bluetooth, container, false);

    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Bluetooth Devices");
        btnONOFF = (ToggleButton) getView().findViewById(R.id.toggleButton);
        btnONOFF.setOnCheckedChangeListener(this);
            }

    public void enableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableBT: Does not have bluetooth capabilities");
        }
        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBTIntent);

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(mBroadcastReceiver1, BTIntent);
        Log.d(TAG, "enableBT: called");
    }

    public void disableBT() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "disableBT: Does not have bluetooth capabilities");
        }
        mBluetoothAdapter.disable();

        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(mBroadcastReceiver1, BTIntent);
        Log.d(TAG, "disableBT: called");
    }

    public void onCheckedChanged(CompoundButton btnONOFF, boolean isChecked) {
        if (isChecked) {
            enableBT();
        } else {
            disableBT();
        }
    }
}
