package com.example.mdp_group18;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {

    private static final String TAG = "Bluetooth Connection Service";
    private static final String appName = "MDP_Group18";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mBluetoothAdapter;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    Context mContext;
    public static boolean mConnectionStatus = false;

    public BluetoothConnectionService(Context context){
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(TAG, "TESTTTTTT");
        start();
    }


    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mServerSocket;
        @SuppressLint("MissingPermission")
        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up server using: " + MY_UUID_INSECURE);
            } catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            mServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG,"run: AcceptThread running.");

            BluetoothSocket socket = null;

            try{
                Log.d(TAG, "run: RFCOMM server socket start.");

                socket = mServerSocket.accept();

                Log.d(TAG, "run: RFCOMM server socket accepted connection.");
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            if (socket != null){
                connected(socket, mDevice);
            }

            Log.d(TAG, "END mAcceptThread.");
        }

        public void cancel(){
            Log.d(TAG, "cancel: Cancelling AcceptThread.");
            try {
                mServerSocket.close();
            } catch (IOException e){
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "ConnectThread: started.");
            mDevice = device;
            deviceUUID = uuid;
        }

        @SuppressLint("MissingPermission")
        public void run(){
            BluetoothSocket tmp = null;
            try{
                Log.d(TAG, "ConnectThread: Creating InsecureRfCommSocket using UUID: " + MY_UUID_INSECURE);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e){
                Log.e(TAG, "ConnectThread: Could not create InsecureRfCommSocket. " + e.getMessage());
            }
            mSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try{
                mSocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");

            } catch (IOException e){
                try{
                    mSocket.close();
                    Log.d(TAG,"run: Closed Socket.");
                } catch (IOException e1){
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in Socket " + e1.getMessage());
                }
                Log.d(TAG, "mConnectThread: run: Could not connect to UUID " + MY_UUID_INSECURE);
            }

            connected(mSocket,mDevice);
        }

        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket.");
            try {
                mSocket.close();

            } catch (IOException e){
                Log.e(TAG, "cancel: Close of mSocket in ConnectThread failed. " + e.getMessage());
            }
        }
    }

    public synchronized void start(){ //Initiates AcceptThread
        Log.d(TAG, "start.");
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid){ //Initiates ConnectThread
        Log.d(TAG, "startClient: start.");

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please wait...", true);
        mConnectThread = new ConnectThread(device,uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;
        private boolean stopThread = false;

        @SuppressLint("MissingPermission")
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mConnectionStatus = true;
            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                mProgressDialog.dismiss();
            } catch (NullPointerException e){
                Log.d(TAG, "ConnectedThread: " + e.getMessage());
            }

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                mConnectionStatus = false;
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder messageBuffer = new StringBuilder();

            while (true){
                try {
                    bytes = inStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: "+ incomingMessage);

                    messageBuffer.append(incomingMessage);

                    // Check if the buffer contains the delimiter
                    int delimiterIndex = messageBuffer.indexOf("\n");
                    if (delimiterIndex != -1) {
                        // Split the buffer contents and process each message
                        String[] messages = messageBuffer.toString().split("\n");
                        for (String message : messages) {
                            // Send broadcast for each incoming message
                            Intent incomingMessageIntent = new Intent("incomingMessage");
                            incomingMessageIntent.putExtra("receivedMessage", message);

                            LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                        }

                        // Reset the message buffer
                        messageBuffer = new StringBuilder();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. "+e.getMessage());

                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: "+text);
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }

        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try{
                this.stopThread = true;
                mSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {

        Log.d(TAG, "connected: Starting.");
        mDevice =  device;
        // stops the AcceptThread when received request
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public static void write(byte[] out){
        ConnectedThread tmp;

        Log.d(TAG, "write: Write is called." );
        mConnectedThread.write(out);
    }
}
