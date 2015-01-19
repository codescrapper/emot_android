package com.emot.androidclient.service;

import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jivesoftware.smack.util.StringUtils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.provider.Settings;
import com.emot.androidclient.util.Log;
import android.widget.Toast;

import com.emot.androidclient.IXMPPRosterCallback;
import com.emot.androidclient.chat.IXMPPChatCallback;
import com.emot.androidclient.exceptions.EmotXMPPException;
import com.emot.androidclient.util.ConnectionState;
import com.emot.androidclient.util.PreferenceConstants;
import com.emot.androidclient.util.StatusMode;
import com.emot.constants.ApplicationConstants;
import com.emot.emotobjects.Contact;
import com.emot.model.EmotApplication;
import com.emot.screens.ContactScreen;
import com.emot.screens.R;

public class XMPPService extends GenericService {

	private AtomicBoolean mConnectionDemanded = new AtomicBoolean(false); // should we try to reconnect?
	private static final int RECONNECT_AFTER = 5;
	private static final int RECONNECT_MAXIMUM = 10*60;
	private static final String RECONNECT_ALARM = "org.emot.androidclient.RECONNECT_ALARM";
	private static final String TAG = XMPPService.class.getSimpleName();
	private static final long STATUS_SEND_INTERVAL = 2000;
	private int mReconnectTimeout = RECONNECT_AFTER;
	private String mReconnectInfo = "";
	private Intent mAlarmIntent = new Intent(RECONNECT_ALARM);
	private PendingIntent mPAlarmIntent;
	private BroadcastReceiver mAlarmReceiver = new ReconnectAlarmReceiver();
	private BroadcastReceiver mMissedCall;;
	private ServiceNotification mServiceNotification = null;

	private Smackable mSmackable;
	private boolean create_account = false;
	private IXMPPRosterService.Stub mService2RosterConnection;
	private IXMPPChatService.Stub mServiceChatConnection;
	private IXMPPGroupChatService.Stub mGroupServiceChatConnection;

	private RemoteCallbackList<IXMPPRosterCallback> mRosterCallbacks = new RemoteCallbackList<IXMPPRosterCallback>();
	private HashSet<String> mIsBoundTo = new HashSet<String>();
	private Handler mMainHandler = new Handler();
	private RemoteCallbackList<IXMPPChatCallback> chatCallbacks = new RemoteCallbackList<IXMPPChatCallback>();
	private String grpSubject;
	private boolean lastRunningStatus = false;
	
	private BroadcastReceiver mSubjectChangedReciever = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("GROUP_SUBJECT_CHANGED")){
				Log.i(TAG, "Broadcast received " + "GROUP_SUBJECT_CHANGED");
				if(mSmackable != null && mSmackable.isAuthenticated()){
					mSmackable.changeGroupSubject(intent.getStringExtra("grpID"),intent.getStringExtra("newGrpSubject"));
				}else{
					Toast.makeText(XMPPService.this, "Not connected to network", Toast.LENGTH_LONG).show();
				}
			}
			
		}
	};
	
private BroadcastReceiver mWentOnStop = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(ApplicationConstants.USER_STATUS_CHANGED)){
				Log.i(TAG, ApplicationConstants.USER_STATUS_CHANGED);
				boolean rnig = intent.getBooleanExtra(ApplicationConstants.GOING_AWAY, false);
		    	Log.i(TAG, "Running status = "+rnig);
		    	
		    		if(!rnig){
		    			EmotApplication.setValue(PreferenceConstants.STATUS_MODE, StatusMode.available.name());
		    			mSmackable.setStatusFromConfig();
		    		}else{
		    			EmotApplication.setValue(PreferenceConstants.STATUS_MODE, StatusMode.away.name());
		    			//mSmackable.setLastActivity();
		    			mSmackable.setStatusFromConfig();
		    		}
		    		lastRunningStatus = rnig;
		    		
		    	
			}
			
		}
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		userStartedWatching();
		Long date = intent.getLongExtra("sinceDate", -1);
		grpSubject = intent.getStringExtra("groupSubject");
		Log.i(TAG, "date is " +date);
		String chatPartner = intent.getDataString();
		boolean isforgrpchat = intent.getBooleanExtra("isforgrpchat", false);
		if ((chatPartner != null) && !isforgrpchat) {
			resetNotificationCounter(chatPartner);
			mIsBoundTo.add(chatPartner);
			return mServiceChatConnection;
			
		}else if(chatPartner != null && isforgrpchat){
			return mGroupServiceChatConnection;
		}
		return mService2RosterConnection;
	}

	@Override
	public void onRebind(Intent intent) {
		Log.i(TAG, "on Rebind called");
		userStartedWatching();
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.add(chatPartner);
			resetNotificationCounter(chatPartner);
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		String chatPartner = intent.getDataString();
		if ((chatPartner != null)) {
			mIsBoundTo.remove(chatPartner);
		}
		userStoppedWatching();

		return true;
	}
	
	private boolean haveNetworkConnection() {
	    boolean haveConnectedWifi = false;
	    boolean haveConnectedMobile = false;

	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
	    for (NetworkInfo ni : netInfo) {
	        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
	            if (ni.isConnected())
	                haveConnectedWifi = true;
	        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
	            if (ni.isConnected())
	                haveConnectedMobile = true;
	    }
	    boolean a = haveConnectedWifi || haveConnectedMobile;
	    Log.i(TAG,"have network connection " +a);
	    return haveConnectedWifi || haveConnectedMobile;
	}
	private BroadcastReceiver mConnectivityChangedReceiver = new BroadcastReceiver() {      
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Hey, net?");
           
            	if(haveNetworkConnection()){
            		new Thread(new Runnable() {
						
						@Override
						public void run() {
						try {
							if(mSmackable != null){
								Log.i(TAG, "while network switch");
							mSmackable.requestConnectionState(ConnectionState.ONLINE);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
							
						}
					}).start();
				
            	}else{
            		Log.i(TAG, "Not having WiFi or Mobile connection");
            	}
			
        }};
        
        private BroadcastReceiver mMissedCallReceiver = new BroadcastReceiver() {
			
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.i(TAG,"Missed Call received");
				
			}
		};
		private WakeLock wakeLock = null;
        private WifiLock wifiLock = null;
		
		private void setWifiLock(){
			 
		        try {
		            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		            // acquire a WakeLock to keep the CPU running
		            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
		                    "MyWakeLock");
		            if(!wakeLock.isHeld()){
		                wakeLock.acquire();
		                Log.i(TAG, "WakeLock acquired!");
		            }

		           


		            WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL , "MyWifiLock");
		            if(!wifiLock.isHeld()){
		                wifiLock.acquire();
		                Log.i(TAG, "WiFiLock acquired!");
		            }

		            
		        } finally {
		            // release the WakeLock to allow CPU to sleep
//		            if (wakeLock != null) {
//		                if (wakeLock.isHeld()) {
//		                    wakeLock.release();
//		                    Log.i("ServiceAlarmBroadcastReceiver", "WakeLock released!");
//		                }
//		            }
//
//		            // release the WifiLock
//		            if (wifiLock != null) {
//		                if (wifiLock.isHeld()) {
//		                    wifiLock.release();
//		                    Log.i("ServiceAlarmBroadcastReceiver", "WiFi Lock released!");
//		                }
//		            }
		        }
		 
			
		}
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "JABBER ID " + mConfig.jabberID);
		createServiceRosterStub();
		createServiceChatStub();
		createGroupServiceChatStub();
		registerReceiver(mConnectivityChangedReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
		registerReceiver(mMissedCallReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
		registerReceiver(mSubjectChangedReciever, new IntentFilter("GROUP_SUBJECT_CHANGED"));
		registerReceiver(mWentOnStop, new IntentFilter(ApplicationConstants.USER_STATUS_CHANGED));
		mPAlarmIntent = PendingIntent.getBroadcast(this, 0, mAlarmIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
		registerReceiver(mAlarmReceiver, new IntentFilter(RECONNECT_ALARM));

		EmotBroadcastReceiver.initNetworkStatus(getApplicationContext());

		if (mConfig.autoConnect && mConfig.jid_configured) {
			/*
			 * start our own service so it remains in background even when
			 * unbound
			 */
			Intent xmppServiceIntent = new Intent(this, XMPPService.class);
			startService(xmppServiceIntent);
		}

		mServiceNotification = ServiceNotification.getInstance();
//		Settings.System.putInt(getContentResolver(),
//				  Settings.System.WIFI_SLEEP_POLICY, 
//				  Settings.System.WIFI_SLEEP_POLICY_NEVER);
		//setWifiLock();
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		((AlarmManager)getSystemService(Context.ALARM_SERVICE)).cancel(mPAlarmIntent);
		mRosterCallbacks.kill();
		if (mSmackable != null) {
		    manualDisconnect();
		    mSmackable.unRegisterCallback();
		}
		unregisterReceiver(mConnectivityChangedReceiver);
		unregisterReceiver(mAlarmReceiver);
		unregisterReceiver(mMissedCallReceiver);
		unregisterReceiver(mSubjectChangedReciever);
		unregisterReceiver(mWentOnStop);
		  if (wakeLock != null) {
              if (wakeLock.isHeld()) {
                  wakeLock.release();
                  Log.i("ServiceAlarmBroadcastReceiver", "WakeLock released!");
              }
          }

          // release the WifiLock
          if (wifiLock != null) {
              if (wifiLock.isHeld()) {
                  wifiLock.release();
                  Log.i("ServiceAlarmBroadcastReceiver", "WiFi Lock released!");
              }
          }

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		logInfo("onStartCommand(), mConnectionDemanded=" + mConnectionDemanded.get());
		if (intent != null) {
			create_account = intent.getBooleanExtra("create_account", false);
			
			if ("disconnect".equals(intent.getAction())) {
				failConnection(getString(R.string.conn_no_network));
				return START_STICKY;
			} else
			if ("reconnect".equals(intent.getAction())) {
				// TODO: integrate the following steps into one "RECONNECT"
				failConnection(getString(R.string.conn_no_network));
				// reset reconnection timeout
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
				return START_STICKY;
			} else
			if ("ping".equals(intent.getAction())) {
				if (mSmackable != null) {
					mSmackable.sendServerPing();
					return START_STICKY;
				}
				// if not yet connected, fall through to doConnect()
			}
		}
		
		mConnectionDemanded.set(mConfig.autoConnect);
		doConnect();
		return START_STICKY;
	}
	
	private void createGroupServiceChatStub(){
		
		mGroupServiceChatConnection = new IXMPPGroupChatService.Stub() {
			
			@Override
			public void sendGroupMessage(String user, String message, String tag) throws RemoteException {
				
				if (mSmackable != null){
				//	return mSmackable.sendGroupMessage(message);
				Log.i(TAG, "mSmackable.sendGroupMessage")	;
				mSmackable.sendGroupMessage(message, user);
				}else{
					SmackableImp.sendOfflineMessage(getContentResolver(),
							user, message, "groupchat");
				}
				
				
				
			}
			
			@Override
			public boolean isAuthenticated() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.isAuthenticated();
				}
				return false;
			}
			
			@Override
			public void clearNotifications(String Jid) throws RemoteException {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean createGroup(String grpName,
					List<Contact> members){
				if(mSmackable != null && mSmackable.isAuthenticated()){
				Log.i(TAG, "members   ----" +members.get(0).getName());
				mSmackable.initMUC(grpName);
				mSmackable.joinUsers(members);
				return true;
				}else{
					Log.i(TAG, "not connected to network");
					Intent intent = new Intent();
					intent.setAction("GROUP_CREATED_ERROR");
					mSmackable.getService().sendBroadcast(intent);
					return false;
				}
				
			}

			@Override
			public void joinGroup(String grpName, boolean isCreateGroup,
					long date) throws RemoteException {
				Log.i(TAG, "isCreateGroup is " +isCreateGroup);
				if(!isCreateGroup){
					Log.i(TAG, "joining group " +grpName);
					mSmackable.joinGroup(grpName, date);
					
				}
				
			}

			@Override
			public void leaveGroup(String grpName) throws RemoteException {
				Log.i(TAG, "leaving group " +grpName);
				mSmackable.leaveGroup(grpName);
				
			}

			@Override
			public String getGroupSubject() throws RemoteException {
				// TODO Auto-generated method stub
				return mSmackable.getGroupSubject();
			}

			@Override
			public List<String> getGroupMembers() throws RemoteException {
				// TODO Auto-generated method stub
				return mSmackable.getGroupMembers();
			}

			

			

			
		};
	}

	private void createServiceChatStub() {
		mServiceChatConnection = new IXMPPChatService.Stub() {
			
			public void registerChatCallback(IXMPPChatCallback chatCallback){
				Log.i(TAG, "Registering chat callback");
				chatCallbacks.register(chatCallback);
			}
			
			public void unregisterChatCallback(IXMPPChatCallback chatCallback){
				Log.i(TAG, "Unregistering chat callback");
				chatCallbacks.unregister(chatCallback);
			}

			public void sendMessage(String user, String message)
					throws RemoteException {
				if (mSmackable != null)
					mSmackable.sendMessage(user, message);
				else
					SmackableImp.sendOfflineMessage(getContentResolver(),
							user, message, "chat");
			}

			public boolean isAuthenticated() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.isAuthenticated();
				}

				return false;
			}
			
			public void clearNotifications(String Jid) throws RemoteException {
				clearNotification(Jid);
			}

			@Override
			public void sendChatState(String user, String state)
					throws RemoteException {
				if (mSmackable != null)
					mSmackable.sendChatState(user, state);
			}
		};
	}

	private void createServiceRosterStub() {
		mService2RosterConnection = new IXMPPRosterService.Stub() {

			public void registerRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				Log.i(TAG, "Registering callback "+callback);
				if (callback != null)
					mRosterCallbacks.register(callback);
			}

			public void unregisterRosterCallback(IXMPPRosterCallback callback)
					throws RemoteException {
				if (callback != null)
					mRosterCallbacks.unregister(callback);
			}

			public int getConnectionState() throws RemoteException {
				if (mSmackable != null) {
					return mSmackable.getConnectionState().ordinal();
				} else {
					return ConnectionState.OFFLINE.ordinal();
				}
			}

			public String getConnectionStateString() throws RemoteException {
				return XMPPService.this.getConnectionStateString();
			}


			public void setStatusFromConfig()
					throws RemoteException {
				Log.i(TAG, "mconfig val "+mConfig.statusMessage);
				if (mSmackable != null) { // this should always be true, but stil...
					mSmackable.setStatusFromConfig();
					updateServiceNotification();
				}
			}

			public void addRosterItem(String user, String alias, String group)
					throws RemoteException {
				try {
					mSmackable.addRosterItem(user, alias, group);
				} catch (EmotXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void addRosterGroup(String group) throws RemoteException {
				mSmackable.addRosterGroup(group);
			}

			public void removeRosterItem(String user) throws RemoteException {
				try {
					mSmackable.removeRosterItem(user);
				} catch (EmotXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void moveRosterItemToGroup(String user, String group)
					throws RemoteException {
				try {
					mSmackable.moveRosterItemToGroup(user, group);
				} catch (EmotXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void renameRosterItem(String user, String newName)
					throws RemoteException {
				try {
					mSmackable.renameRosterItem(user, newName);
				} catch (EmotXMPPException e) {
					shortToastNotify(e);
				}
			}

			public void renameRosterGroup(String group, String newGroup)
					throws RemoteException {
				mSmackable.renameRosterGroup(group, newGroup);
			}

			@Override
			public String changePassword(String newPassword)
					throws RemoteException {
				return mSmackable.changePassword(newPassword);
			}

			public void disconnect() throws RemoteException {
				manualDisconnect();
			}

			public void connect() throws RemoteException {
				mConnectionDemanded.set(true);
				mReconnectTimeout = RECONNECT_AFTER;
				doConnect();
			}

			public void sendPresenceRequest(String jid, String type)
					throws RemoteException {
				mSmackable.sendPresenceRequest(jid, type);
			}

			@Override
			public void setAvatar() throws RemoteException {
				mSmackable.setAvatar();
			}

		};
	}

	private String getConnectionStateString() {
		StringBuilder sb = new StringBuilder();
		sb.append(mReconnectInfo);
		if (mSmackable != null && mSmackable.getLastError() != null) {
			sb.append("\n");
			sb.append(mSmackable.getLastError());
		}
		return sb.toString();
	}
	public String getGroupSubject(){
		return grpSubject;
	}
	public String getStatusTitle(ConnectionState cs) {
		if (cs != ConnectionState.ONLINE)
			return mReconnectInfo;
		String status = getString(StatusMode.fromString(mConfig.statusMode).getTextId());

		if (mConfig.statusMessage.length() > 0) {
			status = status + " (" + mConfig.statusMessage + ")";
		}

		return status;
	}

	private void updateServiceNotification() {
		ConnectionState cs = ConnectionState.OFFLINE;
		if (mSmackable != null) {
			cs = mSmackable.getConnectionState();
		}

		Log.i(TAG, "Broadcasting "+cs);
		// HACK to trigger show-offline when XEP-0198 reconnect is going on
//		getContentResolver().notifyChange(RosterProvider.CONTENT_URI, null);
//		getContentResolver().notifyChange(RosterProvider.GROUPS_URI, null);
		// end-of-HACK

		broadcastConnectionState(cs);

		// do not show notification if not a foreground service
		if (!mConfig.foregroundService)
			return;

		if (cs == ConnectionState.OFFLINE) {
			mServiceNotification.hideNotification(this, SERVICE_NOTIFICATION);
			return;
		}
		Notification n = new Notification(R.drawable.sb_message, null,
				System.currentTimeMillis());
		n.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;

		Intent notificationIntent = new Intent(this, ContactScreen.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		n.contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		if (cs == ConnectionState.ONLINE)
			n.icon = R.drawable.sb_message;

		String title = getString(R.string.conn_title, mConfig.jabberID);
		String message = getStatusTitle(cs);
		n.setLatestEventInfo(this, title, message, n.contentIntent);

		mServiceNotification.showNotification(this, SERVICE_NOTIFICATION,
				n);
	}

	private void doConnect() {
		mReconnectInfo = getString(R.string.conn_connecting);
		updateServiceNotification();
		if (mSmackable == null) {
			createAdapter();
		}

		mSmackable.requestConnectionState(ConnectionState.ONLINE, create_account);
	}

	private void broadcastConnectionState(ConnectionState cs) {
		final int broadCastItems = mRosterCallbacks.beginBroadcast();

		for (int i = 0; i < broadCastItems; i++) {
			try {
				mRosterCallbacks.getBroadcastItem(i).connectionStateChanged(cs.ordinal());
			} catch (RemoteException e) {
				logError("caught RemoteException: " + e.getMessage());
			}
		}
		mRosterCallbacks.finishBroadcast();
	}
	
	private void broadcastChatState(int state, String from) {
		final int broadCastItems = chatCallbacks.beginBroadcast();
		Log.i(TAG, "Chat callbacks = "+broadCastItems);
		for (int i = 0; i < broadCastItems; i++) {
			try {
				chatCallbacks.getBroadcastItem(i).chatStateChanged(state, from);
			} catch (RemoteException e) {
				Log.i(TAG, "caught RemoteException: " + e.getMessage());
			}
		}
		chatCallbacks.finishBroadcast();
	}

	private NetworkInfo getNetworkInfo() {
		Context ctx = getApplicationContext();
		ConnectivityManager connMgr =
				(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
		return connMgr.getActiveNetworkInfo();
	}
	private boolean networkConnected() {
		NetworkInfo info = getNetworkInfo();

		return info != null && info.isConnected();
	}
	private boolean networkConnectedOrConnecting() {
		NetworkInfo info = getNetworkInfo();

		return info != null && info.isConnectedOrConnecting();
	}

	// call this when Android tells us to shut down
	private void failConnection(String reason) {
		logInfo("failConnection: " + reason);
		mReconnectInfo = reason;
		updateServiceNotification();
		if (mSmackable != null)
			mSmackable.requestConnectionState(ConnectionState.DISCONNECTED);
	}

	// called from Smackable when connection broke down
	private void connectionFailed(String reason) {
		logInfo("connectionFailed: " + reason);
		Log.i(TAG, "network connected? " + networkConnected() + " authenticated? " + mSmackable.isAuthenticated());
		//mLastConnectionError = reason;
		if (!networkConnected()) {
			mReconnectInfo = getString(R.string.conn_no_network);
			mSmackable.requestConnectionState(ConnectionState.RECONNECT_NETWORK);

		} else if (mConnectionDemanded.get()) {
			mReconnectInfo = getString(R.string.conn_reconnect, mReconnectTimeout);
			mSmackable.requestConnectionState(ConnectionState.RECONNECT_DELAYED);
			logInfo("connectionFailed(): registering reconnect in " + mReconnectTimeout + "s");
			((AlarmManager)getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + mReconnectTimeout * 1000, mPAlarmIntent);
			mReconnectTimeout = mReconnectTimeout * 2;
			if (mReconnectTimeout > RECONNECT_MAXIMUM)
				mReconnectTimeout = RECONNECT_MAXIMUM;
		} else if(networkConnected()){
			
			mReconnectInfo = "Request online";
			
			mSmackable.requestConnectionState(ConnectionState.ONLINE);
			
		}else {
			connectionClosed();
		}

	}

	private void connectionClosed() {
		logInfo("connectionClosed.");
		mReconnectInfo = "";
		mServiceNotification.hideNotification(this, SERVICE_NOTIFICATION);
	}

	public void manualDisconnect() {
		mConnectionDemanded.set(false);
		mReconnectInfo = getString(R.string.conn_disconnecting);
		performDisconnect();
	}

	public void performDisconnect() {
		if (mSmackable != null) {
			// this is non-blocking
			mSmackable.requestConnectionState(ConnectionState.OFFLINE);
		}
	}

	private void createAdapter() {
		System.setProperty("smack.debugEnabled", "" + mConfig.smackdebug);
		try {
			mSmackable = new SmackableImp(mConfig, getContentResolver(), this);
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}

		mSmackable.registerCallback(new XMPPServiceCallback() {
			public void newMessage(String from, String message, boolean silent_notification, boolean grpchat, String msgSenderInGrp) {
				logInfo("notification: " + from);
				notifyClient(from, mSmackable.getNameForJID(from), message, !mIsBoundTo.contains(from), silent_notification, false, grpchat, msgSenderInGrp );
				//notifyClient(from, "testi@conference.emot-net", message, !mIsBoundTo.contains(from), silent_notification, false, grpchat);
			}

			public void messageError(final String from, final String error, final boolean silent_notification) {
				logInfo("error notification: " + from);
				mMainHandler.post(new Runnable() {
					public void run() {
						// work around Toast fallback for errors
						notifyClient(from, mSmackable.getNameForJID(from), error,
							!mIsBoundTo.contains(from), silent_notification, true, false, "");
					}});
				}

			public void rosterChanged() {
			}

			public void connectionStateChanged() {
				// TODO: OFFLINE is sometimes caused by XMPPConnection calling
				// connectionClosed() callback on an error, need to catch that?
				switch (mSmackable.getConnectionState()) {
				//case OFFLINE:
				case DISCONNECTED:
					connectionFailed(getString(R.string.conn_disconnected));
					break;
				case ONLINE:
					mReconnectTimeout = RECONNECT_AFTER;
				default:
					updateServiceNotification();
				}
			}

			@Override
			public void chatStateChanged(int state, String from) {
				Log.i(TAG, "state = "+state + " from = "+from );
				broadcastChatState(state, from);
			}
		});
		
		//Timer which keeps updating user status
//		Timer timer = new Timer();
//		timer.scheduleAtFixedRate(new TimerTask() {
//
//		    @Override
//		    public void run() {
//		    	boolean rnig = mSmackable.isRunning();
//		    	Log.i(TAG, "Running status = "+rnig);
//		    	if(lastRunningStatus!=rnig){
//		    		if(rnig){
//		    			EmotApplication.setValue(PreferenceConstants.STATUS_MODE, StatusMode.available.name());
//		    		}else{
//		    			EmotApplication.setValue(PreferenceConstants.STATUS_MODE, StatusMode.away.name());
//		    		}
//		    		lastRunningStatus = rnig;
//		    		mSmackable.setStatusFromConfig();
//		    	}
//		    	
//		    }
//
//		}, 0, STATUS_SEND_INTERVAL);
	}

	private class ReconnectAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			logInfo("Alarm received.");
			if (!mConnectionDemanded.get()) {
				return;
			}
			if (mSmackable != null && mSmackable.getConnectionState() == ConnectionState.ONLINE) {
				logError("Reconnect attempt aborted: we are connected again!");
				return;
			}
			doConnect();
		}
	}

	private int number_of_eyes = 0;
	private void userStartedWatching() {
		number_of_eyes += 1;
		logInfo("userStartedWatching: " + number_of_eyes);
		if (mSmackable != null)
			mSmackable.setUserWatching(true);
	}

	private void userStoppedWatching() {
		number_of_eyes -= 1;
		logInfo("userStoppedWatching: " + number_of_eyes);
		// delay deactivation by 3s, in case we happen to be immediately re-bound
		mMainHandler.postDelayed(new Runnable() {
			public void run() {
				if (mSmackable != null && number_of_eyes == 0)
					mSmackable.setUserWatching(false);
			}}, 3000);
	}
}
