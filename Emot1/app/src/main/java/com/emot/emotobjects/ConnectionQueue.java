package com.emot.emotobjects;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.jivesoftware.smack.XMPPConnection;

import com.emot.androidclient.util.Log;

public class ConnectionQueue {
	private static String TAG = ConnectionQueue.class.getSimpleName();
	private ConnectionQueue(){
		
	}
	
	private final static BlockingQueue<XMPPConnection> connectionQueue = new ArrayBlockingQueue<XMPPConnection>(5);
	
//	public static ConnectionQueue getInstance(){
//		if(connectionQueue==null){
//			Log.i(TAG, "Initializing ...");
//			connectionQueue = new ArrayBlockingQueue<XMPPConnection>(5);
//		}
//		return connectionQueue;
//		
//	}
	
	public static void add(XMPPConnection conn){
		if(connectionQueue==null){
			Log.i(TAG, "Initializing ...");
		}
		Log.i(TAG, "Queue size in add start = "+connectionQueue.size());
		try {
			connectionQueue.put(conn);
		} catch (InterruptedException e) {
			Log.i(TAG, "Error putting to queeenkjsdnkv");
			//e.printStackTrace();
		}
		Log.i(TAG, "Queue size in add end = "+connectionQueue.size());
	}
	
	
	public static XMPPConnection get(){
		Log.i(TAG, "Queue size in get start = "+connectionQueue.size());
		XMPPConnection conn = null;
		try {
			conn = connectionQueue.take();
		} catch (InterruptedException e) {
			Log.i(TAG, "Error taking from queeenkjsdnkv");
			//e.printStackTrace();
		}
		Log.i(TAG, "Queue size in get end = "+connectionQueue.size());
		return conn;
	}

}
