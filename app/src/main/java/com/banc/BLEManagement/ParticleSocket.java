package com.banc.BLEManagement;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class ParticleSocket {

    private final static String TAG = "ParticleSocket";

    //live server
    private String mCloudServer = "54.208.229.4";
    //staging server
//	private String mCloudServer = "staging-device.spark.io";
    //local server
//	private String mCloudServer = "10.1.10.175";
    private int mCloudPort = 5683;

    private Socket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public ParticleSocket() {

    }

    public void connect() throws IOException {

        class Retrievedata extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... params) {
                try {
                    Log.d(TAG, "Connecting to Spark Cloud");
                    InetAddress serveraddress = InetAddress.getByName(mCloudServer);
                    Log.d(TAG, "We should have the IP Address " + serveraddress);
                    mSocket = new Socket(mCloudServer, mCloudPort);
                    Log.d(TAG, "Did we connect? " + Boolean.toString(mSocket.isConnected()));

                    mInputStream = mSocket.getInputStream();
                    mOutputStream = mSocket.getOutputStream();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
        String params = "";
        new Retrievedata().execute(params);
    }

    public void disconnect() throws IOException {
        if (mInputStream != null) {
            mInputStream.close();
            mInputStream = null;
        }
        if (mOutputStream != null) {
            mOutputStream.close();
            mOutputStream = null;
        }
        if (mSocket != null) {
            mSocket.close();
            mSocket = null;
        }
    }

    public void write(byte[] data) throws IOException {
//		Log.d(TAG, "When writing, are we connected: " + Boolean.toString(socket.isConnected()));
        mOutputStream.write(data);
        mOutputStream.flush();
//		StringBuilder sb = new StringBuilder();

//	    for (byte b : data) {
//	        sb.append(String.format("%02X ", b));
//	    }
//	    Log.d(TAG, "Sending data: " + sb.toString());
        Log.d(TAG, "Sending data to Cloud of size: " + data.length);
    }

    public int Available() throws IOException {
        if (mInputStream != null) {
            return mInputStream.available();
        }
        return 0;
    }

    public boolean isConnected() {
        return mInputStream != null && mSocket.isConnected();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public byte[] read() throws IOException {
        //need to append our service ID and socket number to the beginning of the data
        byte[] data = new byte[0];
        if (mInputStream != null) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            try {
                int bytesAvailable = mInputStream.available();
                data = new byte[bytesAvailable];

                //copy the cloud data in with the service ID and socket number
                mInputStream.read(data, 0, bytesAvailable);

                StringBuilder sb = new StringBuilder();
                for (byte b : data) {
                    sb.append(String.format("%02X ", b));
                }
                Log.d(TAG, "Got data: " + sb.toString());
                Log.d(TAG, "Got data from Cloud of size: " + data.length);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return data;
    }
}
