package com.example.mdp_group18;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothPopup extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG = "BluetoothPopUp";
    private String connStatus;
    Switch btSwitch;
    Button searchBtn;
    Button connectBtn;
    Button backBtn;
    BluetoothConnectionService mBluetoothConnection;
    BluetoothDevice mBTDevice;
    BluetoothAdapter btAdapter;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public ArrayList<BluetoothDevice> mNewBTDevices;
    public ArrayList<BluetoothDevice> mPairedBTDevices;
    public DeviceListAdapter mDeviceListAdapter;
    public DeviceListAdapter mPairedDeviceListAdapter;
    public ListView lvNewDevices;
    public ListView lvPairedDevices;

    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_popup);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btSwitch = findViewById(R.id.bluetoothSwitchOnOff);
        searchBtn = findViewById(R.id.searchBtn);

        connectBtn = findViewById(R.id.connectBtn);

        backBtn = findViewById(R.id.backBtn);

        lvNewDevices = (ListView) findViewById(R.id.otherDevicesList);
        lvNewDevices.setOnItemClickListener(this);
        mNewBTDevices = new ArrayList<>();

        lvPairedDevices = (ListView) findViewById(R.id.pairedDevicesList);
        lvPairedDevices.setOnItemClickListener(this);
        mPairedBTDevices = new ArrayList<>();
        refreshPairedList();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        if (btAdapter.isEnabled()) {
            btSwitch.setChecked(true);
        } else {
            btSwitch.setChecked(false);
        }

        btSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.d(TAG, "onChecked: Switch button toggled. Enabling/Disabling Bluetooth");

                if (btAdapter == null) {
                    Log.d(TAG, "enableDisableBT: Device does not support Bluetooth capabilities!");
                    Toast.makeText(getApplicationContext(), "Device Does Not Support Bluetooth capabilities!", Toast.LENGTH_SHORT).show();
                    compoundButton.setChecked(false);
                } else {
                    if (!btAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: enabling Bluetooth");
                        Log.d(TAG, "enableDisableBT: Making device discoverable for 600 seconds.");

                        Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivity(enableBTIntent);

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(mBroadcastReceiver1, BTIntent);

                        compoundButton.setChecked(true);

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                        startActivity(discoverableIntent);

                        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                        registerReceiver(mBroadcastReceiver2, intentFilter);
                    }
                    if (btAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: disabling Bluetooth");
                        btAdapter.disable();

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(mBroadcastReceiver1, BTIntent);

                        compoundButton.setChecked(false);
                    }
                }
            }
        });

         searchBtn.setOnClickListener(view -> {
             Log.d(TAG, "searchBtn: Making device discoverable for 300 seconds.");
             Log.d(TAG, "searchBtn: Looking for unpaired devices.");

             if (!btAdapter.isEnabled()){
                 Toast.makeText(getApplicationContext(), "Please Turn On Bluetooth on this device.", Toast.LENGTH_SHORT).show();
             } else if (btAdapter.isDiscovering()) {
                 btAdapter.cancelDiscovery();
                 Log.d(TAG, "searchBtn: Cancelling Discovery.");

                 checkBTPermissions();

                 btAdapter.startDiscovery();
                 IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                 registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
             } else if (!btAdapter.isDiscovering()) {
                 checkBTPermissions();

                 btAdapter.startDiscovery();
                 IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                 registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
             }
         });

        connectBtn.setOnClickListener(view -> {
            if(mBTDevice ==null)
            {
                Toast.makeText(BluetoothPopup.this, "Please Select a Device before connecting.", Toast.LENGTH_SHORT).show();
            }
            else {
                startConnection();
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothPopup.super.finish();
            }
        });
    }

    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
        int parentId = parent.getId();
        if (parentId == R.id.otherDevicesList){
            btAdapter.cancelDiscovery();

            String deviceName = mNewBTDevices.get(i).getName();
            String deviceAddress = mNewBTDevices.get(i).getAddress();

            Log.d(TAG, "onItemClick: deviceName = " + deviceName + ", deviceAddress = " + deviceAddress);

            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
                Log.d(TAG,"Trying to pair with " + deviceName);
                mNewBTDevices.get(i).createBond();

                //mBTDevice = mNewBTDevices.get(i);
                //mBluetoothConnection = new BluetoothConnectionService(this);

            }
        } else if (parentId == R.id.pairedDevicesList){
            btAdapter.cancelDiscovery();

            mBTDevice = mPairedBTDevices.get(i);
            Log.d(TAG, "Attempting to Connect - Device: " + mBTDevice.getName());
            mBluetoothConnection = new BluetoothConnectionService(BluetoothPopup.this);
        }

    }
    @SuppressLint("MissingPermission")
    private void refreshPairedList(){
        mPairedBTDevices.clear();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0){
            for (BluetoothDevice d : pairedDevices){
                mPairedBTDevices.add(d);
                mPairedDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, mPairedBTDevices);
                lvPairedDevices.setAdapter(mPairedDeviceListAdapter);
            }
        }
    }

    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initialising RFCOMM Bluetooth Connection.");
        mBluetoothConnection.startClient(device,uuid);
    }

    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
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

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: DISCOVERABILITY ENABLED.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver1: DISCOVERABILITY DISABLED. ABLE TO RECEIVE CONNECTIONS.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver1: DISCOVERABILITY DISABLED. UNABLE TO RECEIVE CONNECTIONS.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver1: CONNECTING...");
                        break;

                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver1: CONNECTED.");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "OnReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName() != null) {
                    mNewBTDevices.add(device);
                }
                Log.d(TAG, "OnReceive: " + device.getName() + ": " + device.getAddress());

                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mNewBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "broadcastReceiver: BOND_BONDED.");
                    Toast.makeText(getApplicationContext(), "Device has been successfully paired", Toast.LENGTH_SHORT).show();
                    mBTDevice = device;
                    refreshPairedList();
                } else if (device.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "broadcastReceiver: BOND_BONDING.");
                } else if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "broadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }

}

