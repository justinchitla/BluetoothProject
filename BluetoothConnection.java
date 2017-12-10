package com.chitlapps.justinchitla.bluetoothapplication;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothConnection {
    private static final String TAG = "Bluetooth_Connection";

    private static final String appName = "Bluetooth App";

    private static final UUID UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter adapter;
    Context context;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private TextView messageTextView;

    public BluetoothConnection(Context context, TextView messageTextView1) {
        this.context = context;
        adapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = new Handler(this.context.getMainLooper());
        messageTextView = messageTextView1;
        start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            try{
                tmp = adapter.listenUsingInsecureRfcommWithServiceRecord(appName, UUID_INSECURE);

                Log.d(TAG, "Setting up Server using: " + UUID_INSECURE);
            }catch (IOException e){e.printStackTrace();}
            mmServerSocket = tmp;
        }

        public void run(){
            BluetoothSocket socket = null;

            try{
                socket = mmServerSocket.accept();
            }catch (IOException e){e.printStackTrace();}

            if(socket != null){
                connected(socket,mmDevice);
            }
        }

        public void end() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }

    }


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;

            try {
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {e.printStackTrace();}

            mmSocket = tmp;

            adapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e1) {e1.printStackTrace();}
            }

            connected(mmSocket,mmDevice);
        }
        public void end() {
            try {
                mmSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }


    public synchronized void start() {
        if (mConnectThread != null) {
            mConnectThread.end();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }


    public void startClient(BluetoothDevice device,UUID uuid){
        mProgressDialog = ProgressDialog.show(context,"Connecting Bluetooth"
                ,"One Moment...",true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException e){e.printStackTrace();}


            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {e.printStackTrace();}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            mmBuffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(mmBuffer);
                    final String message = new String(mmBuffer, 0, bytes);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            messageTextView.setText(message);
                        }
                    });
                } catch (IOException e) {break;}
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {e.printStackTrace();}
        }

        public void end() {
            try {
                mmSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {
        mConnectedThread.write(out);
    }

}

