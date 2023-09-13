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
import java.util.logging.Handler;

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
    public static int mState;
    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public BluetoothConnectionService(Context context){
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }
    public synchronized int getState() {
        return mState;
    }

    public synchronized void start(){ //Initiates AcceptThread
        Log.d(TAG, "start.");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
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

    public synchronized void connect(BluetoothDevice device){ //Initiates ConnectThread
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    public static void write(byte[] out){

        Log.d(TAG, "write: Write is called." );
        mConnectedThread.write(out);

    }

    private void connectionFailed() {
        // Start the service over to restart listening mode
        BluetoothConnectionService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Start the service over to restart listening mode
        BluetoothConnectionService.this.start();
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

            while(mState != STATE_CONNECTED){
                try{
                    Log.d(TAG, "run: RFCOMM server socket start.");

                    socket = mServerSocket.accept();

                    Log.d(TAG, "run: RFCOMM server socket accepted connection.");
                }catch (IOException e){
                    Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
                    break;
                }

                if (socket != null){
                    synchronized (BluetoothConnectionService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
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

        @SuppressLint("MissingPermission")
        public ConnectThread(BluetoothDevice device){
            Log.d(TAG, "ConnectThread: started.");
            mDevice = device;

            BluetoothSocket tmp = null;
            try{
                Log.d(TAG, "ConnectThread: Creating InsecureRfCommSocket using UUID: " + MY_UUID_INSECURE);
                tmp = mDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e){
                Log.e(TAG, "ConnectThread: Could not create InsecureRfCommSocket. " + e.getMessage());
            }
            mSocket = tmp;
        }

        @SuppressLint("MissingPermission")
        public void run(){

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
                connectionFailed();
                return;
            }

            synchronized (BluetoothConnectionService.this) {
                mConnectThread = null;
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



    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;
        private boolean stopThread = false;

        @SuppressLint("MissingPermission")
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

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
                Log.e(TAG, "Connected Thread: Temp sockets not created", e);
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run(){
            Log.d(TAG, "BEGIN mConnectedThread.");
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
                    connectionLost();
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

}
