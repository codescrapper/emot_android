package com.emot.androidclient.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.DNSJavaResolver;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.LastActivityManager;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.carbons.Carbon;
import org.jivesoftware.smackx.carbons.CarbonManager;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.entitycaps.cache.SimpleDirectoryPersistentCache;
import org.jivesoftware.smackx.entitycaps.provider.CapsExtensionProvider;
import org.jivesoftware.smackx.forward.Forwarded;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.RoomInfo;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.DelayInfo;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.packet.Version;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.provider.PingProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import com.emot.androidclient.data.ChatProvider;
import com.emot.androidclient.data.ChatProvider.ChatConstants;
import com.emot.androidclient.data.EmotConfiguration;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.data.RosterProvider.RosterConstants;
import com.emot.androidclient.exceptions.EmotXMPPException;
import com.emot.androidclient.util.ConnectionState;
import com.emot.androidclient.util.EmotUtils;
import com.emot.androidclient.util.Log;
import com.emot.androidclient.util.PreferenceConstants;
import com.emot.androidclient.util.StatusMode;
import com.emot.common.ImageHelper;
import com.emot.constants.ApplicationConstants;
import com.emot.emotobjects.Contact;
import com.emot.model.EmotApplication;
import com.emot.screens.UpdateProfileScreen;

public class SmackableImp implements Smackable {
	final static private String TAG = SmackableImp.class.getSimpleName();

	final static private int PACKET_TIMEOUT = 45000;
	private static final String CHATTYPE = "chat";
	private static final String GROUPCHATTYPE = "groupchat";
	final static private String[] SEND_OFFLINE_PROJECTION = new String[] {
		ChatConstants._ID, ChatConstants.JID,
		ChatConstants.MESSAGE, ChatConstants.DATE, ChatConstants.PACKET_ID ,ChatConstants.CHAT_TYPE};
	final static private String SEND_OFFLINE_SELECTION =
			ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING + " AND " +
					ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_NEW;

	final static private String[] SEND_FAILED_PROJECTION = new String[] {
		ChatConstants._ID, ChatConstants.JID,
		ChatConstants.MESSAGE, ChatConstants.DATE, ChatConstants.PACKET_ID };
	final static private String SEND_FAILED_SELECTION =
			ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING + " AND " +
					ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_FAILED;

	final static private String[] SEND_UNACKNOWLEDGED_PROJECTION = new String[] {
		ChatConstants._ID, ChatConstants.JID,
		ChatConstants.MESSAGE, ChatConstants.DATE, ChatConstants.PACKET_ID ,ChatConstants.CHAT_TYPE };

	final static private String SEND_UNACKNOWLEDGED_SELECTION =
			ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING + " AND " +

		"("+
		ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_FAILED +
		" OR "+ ChatConstants.DELIVERY_STATUS + " = " + ChatConstants.DS_SENT_OR_READ +
		")";


	//	final static private String SEND_UNACKNOWLEDGED_SELECTION =
	//			ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING;

	static final DiscoverInfo.Identity EMOT_IDENTITY = new DiscoverInfo.Identity("client",
			EmotApplication.XMPP_IDENTITY_NAME,
			EmotApplication.XMPP_IDENTITY_TYPE);

	private final static int INTERVAL = 1000 * 30;

	//	private static Handler mHandler = new Handler();
	//	Runnable mHandlerTask = new Runnable()
	//	{
	//	     @Override 
	//	     public void run() {
	//	    	  Log.i(TAG, "trying for unacknowledged message");
	//	          sendNotacknowledgedMessages();
	//	          mHandler.postDelayed(mHandlerTask, INTERVAL);
	//	     }
	//	};

	static File capsCacheDir = null; ///< this is used to cache if we already initialized EntityCapsCache

	static {
		SmackAndroid.init(EmotApplication.getAppContext());
		registerSmackProviders();
		DNSUtil.setDNSResolver(DNSJavaResolver.getInstance());

		// initialize smack defaults before any connections are created

		SmackConfiguration.setPacketReplyTimeout(PACKET_TIMEOUT);
		SmackConfiguration.setDefaultPingInterval(0);
	}

	static void registerSmackProviders() {
		ProviderManager pm = ProviderManager.getInstance();
		// add IQ handling
		pm.addIQProvider("query","http://jabber.org/protocol/disco#info", new DiscoverInfoProvider());
		pm.addIQProvider("query","http://jabber.org/protocol/disco#items", new DiscoverItemsProvider());
		// add delayed delivery notifications
		pm.addExtensionProvider("delay","urn:xmpp:delay", new DelayInfoProvider());
		pm.addExtensionProvider("x","jabber:x:delay", new DelayInfoProvider());
		// add XEP-0092 Software Version
		pm.addIQProvider("query", Version.NAMESPACE, new Version.Provider());

		// add carbons and forwarding
		pm.addExtensionProvider("forwarded", Forwarded.NAMESPACE, new Forwarded.Provider());
		pm.addExtensionProvider("sent", Carbon.NAMESPACE, new Carbon.Provider());
		pm.addExtensionProvider("received", Carbon.NAMESPACE, new Carbon.Provider());
		// add delivery receipts
		pm.addExtensionProvider(DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceipt.Provider());
		pm.addExtensionProvider(DeliveryReceiptRequest.ELEMENT, DeliveryReceipt.NAMESPACE, new DeliveryReceiptRequest.Provider());
		// add XMPP Ping (XEP-0199)
		pm.addIQProvider("ping","urn:xmpp:ping", new PingProvider());
		pm.addIQProvider("vCard","vcard-temp", new VCardProvider());
		
		// Offline Message Requests
        pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
                new OfflineMessageRequest.Provider());
		// Group Chat Invitations

		pm.addExtensionProvider("x", "jabber:x:conference",new GroupChatInvitation.Provider());
		ServiceDiscoveryManager.setDefaultIdentity(EMOT_IDENTITY);

		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Data Forms

		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User

		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());

		// MUC Admin

		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		// MUC Owner

		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		// XEP-0115 Entity Capabilities
		pm.addExtensionProvider("c", "http://jabber.org/protocol/caps", new CapsExtensionProvider());

		// ChatStates
		pm.addExtensionProvider("active","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider()); 
		pm.addExtensionProvider("paused","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone","http://jabber.org/protocol/chatstates", new ChatStateExtension.Provider());

		XmppStreamHandler.addExtensionProviders();
	}

	private EmotConfiguration mConfig;
	private ConnectionConfiguration mXMPPConfig;
	private XmppStreamHandler.ExtXMPPConnection mXMPPConnection;
	private XmppStreamHandler mStreamHandler;
	private Thread mConnectingThread;
	private Object mConnectingThreadMutex = new Object();

	private MultiUserChat mGroupChat;
	private MultiUserChat mGroupChat2;
	private ConnectionState mRequestedState = ConnectionState.OFFLINE;
	private ConnectionState mState = ConnectionState.OFFLINE;
	private String mLastError;

	private XMPPServiceCallback mServiceCallBack;
	private Roster mRoster;
	private RosterListener mRosterListener;
	private PacketListener mPacketListener;
	private PacketListener mPresenceListener;
	private PacketListener mServerPingListener;
	private ConnectionListener mConnectionListener;

	private final ContentResolver mContentResolver;

	private AlarmManager mAlarmManager;
	private PacketListener mPongListener;
	private String mPingID;
	private long mPingTimestamp;

	private PendingIntent mPingAlarmPendIntent;
	private PendingIntent mPongTimeoutAlarmPendIntent;
	private static final String PING_ALARM = "com.emot.androidclient.PING_ALARM";
	private static final String PONG_TIMEOUT_ALARM = "com.emot.androidclient.PONG_TIMEOUT_ALARM";
	private Intent mPingAlarmIntent = new Intent(PING_ALARM);
	private Intent mPongTimeoutAlarmIntent = new Intent(PONG_TIMEOUT_ALARM);
	private Service mService;
	private String grpSubject;
	private PongTimeoutAlarmReceiver mPongTimeoutAlarmReceiver = new PongTimeoutAlarmReceiver();
	private PingAlarmReceiver mPingAlarmReceiver = new PingAlarmReceiver();
	
	public SmackableImp(EmotConfiguration config,
			ContentResolver contentResolver,
			Service service) {
		this.mConfig = config; 
		this.mContentResolver = contentResolver;
		this.mService = service;
		this.mAlarmManager = (AlarmManager)mService.getSystemService(Context.ALARM_SERVICE);


	}

	public Service getService(){
		return mService;
	}

	// this code runs a DNS resolver, might be blocking
	private synchronized void initXMPPConnection() {
		Log.i(TAG, "server = "+mConfig.server + " customserver = "+mConfig.customServer + " user = "+mConfig.userName + " ssl = "+mConfig.require_ssl + " resource = " + mConfig.ressource + " password = "+mConfig.password);
		// allow custom server / custom port to override SRV record
		if (mConfig.customServer.length() > 0)
			mXMPPConfig = new ConnectionConfiguration(mConfig.customServer,
					mConfig.port, mConfig.server);
		else
			mXMPPConfig = new ConnectionConfiguration(mConfig.server); // use SRV
		mXMPPConfig.setReconnectionAllowed(false);
		mXMPPConfig.setSendPresence(true);
		mXMPPConfig.setCompressionEnabled(false); // disable for now
		mXMPPConfig.setDebuggerEnabled(mConfig.smackdebug);
		if (mConfig.require_ssl)
			this.mXMPPConfig.setSecurityMode(ConnectionConfiguration.SecurityMode.required);

		// register MemorizingTrustManager for HTTPS
		//		try {
		//			SSLContext sc = SSLContext.getInstance("TLS");
		//			MemorizingTrustManager mtm = EmotApplication.getApp(mService).mMTM;
		//			sc.init(null, new X509TrustManager[] { mtm },
		//					new java.security.SecureRandom());
		//			this.mXMPPConfig.setCustomSSLContext(sc);
		//			this.mXMPPConfig.setHostnameVerifier(mtm.wrapHostnameVerifier(
		//						new org.apache.http.conn.ssl.StrictHostnameVerifier()));
		//		} catch (java.security.GeneralSecurityException e) {
		//			Log.d(TAG, "initialize MemorizingTrustManager: " + e);
		//		}

		this.mXMPPConnection = new XmppStreamHandler.ExtXMPPConnection(mXMPPConfig);
		this.mStreamHandler = new XmppStreamHandler(mXMPPConnection, mConfig.smackdebug);
		mStreamHandler.addAckReceivedListener(new XmppStreamHandler.AckReceivedListener() {
			public void ackReceived(long handled, long total) {
				gotServerPong("" + handled);
			}
		});
		mConfig.reconnect_required = false;
		Log.i(TAG, "Connected = "+mXMPPConnection.isConnected() + " authenticatec = "+mXMPPConnection.isAuthenticated());
		initServiceDiscovery();
	}


	// blocking, run from a thread!
	public  boolean doConnect(boolean create_account) throws EmotXMPPException {



		mRequestedState = ConnectionState.ONLINE;
		updateConnectionState(ConnectionState.CONNECTING);
		if (mXMPPConnection == null || mConfig.reconnect_required)
			initXMPPConnection();
		tryToConnect(create_account);
		// actually, authenticated must be true now, or an exception must have
		// been thrown.
		if (isAuthenticated()) {

			registerMessageListener();
			registerPresenceListener();
			registerPongListener();
			registerServerPingListener();

			sendOfflineMessages();
			sendFailedMessages();
			sendUserWatching();
			// we need to "ping" the service to let it know we are actually
			// connected, even when no roster entries will come in
			updateConnectionState(ConnectionState.ONLINE);
			setAvatar();
		}else throw new EmotXMPPException("SMACK connected, but authentication failed");


		return true;
	}

	private void joinGroups(){

		sStopService = false;
		scheduledExecutorService = Executors.newScheduledThreadPool(1);







		Log.i(TAG, "Login successful");

		String rooms = EmotApplication.getValue(PreferenceConstants.ROOMS, "");
		if(rooms != null && !rooms.equals("")){
			final String roomstoJoin[] = rooms.split(",");
			//						//EmotApplication.setValue(PreferenceConstants.ROOMS, rooms + "," + mGroupChat.getRoom());
			Log.i(TAG, "rooms to join " + roomstoJoin);
			//						List<String> roomstoJoin2 = EmotApplication.getRooms();
			Log.i(TAG, "rooms to join size " +roomstoJoin.length);
			//sStopService = false;
			for(int i = 0; i < roomstoJoin.length; i++){
				mJoined.put(roomstoJoin[i], false);
			}
			int count = 0;
			for(int i = 0; i < roomstoJoin.length; i++){
				count++;
				if(roomstoJoin[i] != null && !roomstoJoin[i].equals("")){
					mGroupChat2 = new MultiUserChat(mXMPPConnection, roomstoJoin[i]);
					Log.i(TAG, "joining room " +roomstoJoin[i]);
					final DiscussionHistory dh = new DiscussionHistory();
					long since = EmotApplication.getLongValue("historySince", -1);
					SimpleDateFormat dateFormater = new SimpleDateFormat(
							"yy-MM-dd HH:mm:ss");
					Date date = new Date(since);
					dh.setSince(date);
					final long timeout = 4000;
					try {


						mGroupChat2.join(mConfig.userName + "@conference.emot-net","",dh, timeout);
						mJoined.put(roomstoJoin[i], true);
						Log.i(TAG, "joining room " + mGroupChat2.getRoom() + "count " +count);


					} catch (XMPPException e) {
						//sStopService = false;
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}

				}
			}


		}
		//mHandlerTask.run();


	}

	// BLOCKING, call on a new Thread!
	private void updateConnectingThread(Thread new_thread) {
		synchronized(mConnectingThreadMutex) {
			if (mConnectingThread == null) {
				mConnectingThread = new_thread;
			} else try {
				Log.d(TAG, "updateConnectingThread: old thread is still running, killing it.");
				mConnectingThread.interrupt();
				mConnectingThread.join(50);
			} catch (InterruptedException e) {
				Log.d(TAG, "updateConnectingThread: failed to join(): " + e);
			} finally {
				mConnectingThread = new_thread;
			}
		}
	}
	private void finishConnectingThread() {
		synchronized(mConnectingThreadMutex) {
			mConnectingThread = null;
		}
	}

	static boolean stopService = false;

	/** Non-blocking, synchronized function to connect/disconnect XMPP.
	 * This code is called from outside and returns immediately. The actual work
	 * is done on a background thread, and notified via callback.
	 * @param new_state The state to transition into. Possible values:
	 * 	OFFLINE to properly close the connection
	 * 	ONLINE to connect
	 * 	DISCONNECTED when network goes down
	 * @param create_account When going online, try to register an account.
	 */
	@Override
	public synchronized void requestConnectionState(ConnectionState new_state, final boolean create_account) {

		Log.i(TAG, "requestConnState: " + mState + " -> " + new_state + (create_account ? " create_account!" : ""));
		mRequestedState = new_state;
		if (new_state == mState)
			return;
		switch (new_state) {
		case ONLINE:
			switch (mState) {
			case RECONNECT_DELAYED:
			case DISCONNECTED:
				// TODO: cancel timer
			case RECONNECT_NETWORK:
			case OFFLINE:
				// update state before starting thread to prevent race conditions
				updateConnectionState(ConnectionState.CONNECTING);

				// register ping (connection) timeout handler: 2*PACKET_TIMEOUT(30s) + 3s
				registerPongTimeout(2*PACKET_TIMEOUT + 3000, "connection");


				Thread t1 = new Thread() {
					@Override
					public void run() {
						updateConnectingThread(this);
						try {
							Log.w(TAG, "Connnnnnnnnnecting");
							doConnect(create_account);
						} catch (IllegalArgumentException e) {
							// this might happen when DNS resolution in ConnectionConfiguration fails
							onDisconnected(e);
						} catch (EmotXMPPException e) {
							onDisconnected(e);
						} finally {
							mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
							finishConnectingThread();
						}
					}
				};
				t1.start();
				

				break;
			case CONNECTING:
			case DISCONNECTING:
				// ignore all other cases
				break;

			}
			break;
		case DISCONNECTED:
			// spawn thread to do disconnect
			if (mState == ConnectionState.ONLINE) {
				// update state before starting thread to prevent race conditions
				updateConnectionState(ConnectionState.DISCONNECTING);

				// register ping (connection) timeout handler: PACKET_TIMEOUT(30s)
				registerPongTimeout(PACKET_TIMEOUT, "forced disconnect");

				new Thread() {
					public void run() {
						updateConnectingThread(this);
						mXMPPConnection.shutdown();
						mStreamHandler.quickShutdown();
						onDisconnected("forced disconnect completed");
						finishConnectingThread();
						//updateConnectionState(ConnectionState.OFFLINE);
					}
				}.start();
			}


			break;
		case OFFLINE:
			switch (mState) {
			case CONNECTING:
			case ONLINE:
				// update state before starting thread to prevent race conditions
				updateConnectionState(ConnectionState.DISCONNECTING);

				// register ping (connection) timeout handler: PACKET_TIMEOUT(30s)
				registerPongTimeout(PACKET_TIMEOUT, "manual disconnect");

				// spawn thread to do disconnect
				new Thread() {
					public void run() {
						updateConnectingThread(this);
						mXMPPConnection.shutdown();
						mStreamHandler.close();
						mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
						// we should reset XMPPConnection the next time
						mConfig.reconnect_required = true;
						finishConnectingThread();
						// reconnect if it was requested in the meantime
						if (mRequestedState == ConnectionState.ONLINE)
							requestConnectionState(ConnectionState.ONLINE);
					}
				}.start();
				break;
			case DISCONNECTING:
				break;
			case RECONNECT_DELAYED:
				// TODO: clear timer
			case RECONNECT_NETWORK:
				updateConnectionState(ConnectionState.OFFLINE);
			}
			break;
		case RECONNECT_NETWORK:
		case RECONNECT_DELAYED:
			switch (mState) {
			case DISCONNECTED:

			case RECONNECT_NETWORK:
				//registerPongTimeout(2*PACKET_TIMEOUT + 3000, "connection");
				//	sendServerPing();

				//	updateConnectionState(ConnectionState.CONNECTING);

				//				 register ping (connection) timeout handler: 2*PACKET_TIMEOUT(30s) + 3s
				//								registerPongTimeout(2*PACKET_TIMEOUT + 3000, "connection");

				//								new Thread() {
				//									@Override
				//									public void run() {
				//										try {
				//											
				//										updateConnectingThread(this);
				//										
				//											Log.i(TAG, "Connnnnnnnnnecting");
				//											doConnect(create_account);
				//											
				//										} catch (IllegalArgumentException e) {
				//											// this might happen when DNS resolution in ConnectionConfiguration fails
				//											onDisconnected(e);
				//										} catch (EmotXMPPException e) {
				//											onDisconnected(e);
				//										} finally {
				//											mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
				//											finishConnectingThread();
				//										}
				//									}
				//								}.start();

			case RECONNECT_DELAYED:
				updateConnectionState(new_state);

				break;
			default:
				throw new IllegalArgumentException("Can not go from " + mState + " to " + new_state);
			}
		}
	}
	@Override
	public void requestConnectionState(ConnectionState new_state) {
		requestConnectionState(new_state, false);
	}

	@Override
	public ConnectionState getConnectionState() {
		return mState;
	}

	// called at the end of a state transition
	private synchronized void updateConnectionState(ConnectionState new_state) {
		if (new_state == ConnectionState.ONLINE || new_state == ConnectionState.CONNECTING)
			mLastError = null;
		Log.d(TAG, "updateConnectionState: " + mState + " -> " + new_state + " (" + mLastError + ")");
		if (new_state == mState)
			return;
		mState = new_state;
		if (mServiceCallBack != null)
			mServiceCallBack.connectionStateChanged();
	}
	private void initServiceDiscovery() {
		// register connection features
		ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(mXMPPConnection);

		// init Entity Caps manager with storage in app's cache dir
		try {
			if (capsCacheDir == null) {
				capsCacheDir = new File(mService.getCacheDir(), "entity-caps-cache");
				capsCacheDir.mkdirs();
				EntityCapsManager.setPersistentCache(new SimpleDirectoryPersistentCache(capsCacheDir));
			}
		} catch (java.io.IOException e) {
			Log.e(TAG, "Could not init Entity Caps cache: " + e.getLocalizedMessage());
		} catch(Exception e){
			//e.printStackTrace();
		}

		// reference PingManager, set ping flood protection to 10s
		PingManager.getInstanceFor(mXMPPConnection).setPingMinimumInterval(10*1000);

		// set Version for replies
		String app_name = mService.getString(com.emot.screens.R.string.app_name);
		String build_revision = mService.getString(com.emot.screens.R.string.build_revision);
		Version.Manager.getInstanceFor(mXMPPConnection).setVersion(
				new Version(app_name, build_revision, "Android"));

		// reference DeliveryReceiptManager, add listener
		DeliveryReceiptManager dm = DeliveryReceiptManager.getInstanceFor(mXMPPConnection);
		dm.enableAutoReceipts();
		dm.addReceiptReceivedListener(new ReceiptReceivedListener() { // DOES NOT WORK IN CARBONS
			public void onReceiptReceived(String fromJid, String toJid, String receiptId) {
				Log.d(TAG, "got delivery receipt for " + receiptId);
				changeMessageDeliveryStatus(receiptId, ChatConstants.DS_ACKED);
			}});
	}

	public void addRosterItem(String user, String alias, String group)
			throws EmotXMPPException {
		tryToAddRosterEntry(user, alias, group);
	}

	public void removeRosterItem(String user) throws EmotXMPPException {
		Log.d(TAG, "removeRosterItem(" + user + ")");

		tryToRemoveRosterEntry(user);
		mServiceCallBack.rosterChanged();
	}

	public void renameRosterItem(String user, String newName)
			throws EmotXMPPException {
		RosterEntry rosterEntry = mRoster.getEntry(user);

		if (!(newName.length() > 0) || (rosterEntry == null)) {
			throw new EmotXMPPException("JabberID to rename is invalid!");
		}
		rosterEntry.setName(newName);
	}

	public void addRosterGroup(String group) {
		mRoster.createGroup(group);
	}

	public void renameRosterGroup(String group, String newGroup) {
		RosterGroup groupToRename = mRoster.getGroup(group);
		groupToRename.setName(newGroup);
	}

	public void moveRosterItemToGroup(String user, String group)
			throws EmotXMPPException {
		tryToMoveRosterEntryToGroup(user, group);
	}

	public void sendPresenceRequest(String user, String type) {
		// HACK: remove the fake roster entry added by handleIncomingSubscribe()
		if ("unsubscribed".equals(type))
			deleteRosterEntryFromDB(user);
		Presence response = new Presence(Presence.Type.valueOf(type));
		response.setTo(user);
		mXMPPConnection.sendPacket(response);
	}

	@Override
	public String changePassword(String newPassword) {
		try {
			new AccountManager(mXMPPConnection).changePassword(newPassword);
			return "OK"; //HACK: hard coded string to differentiate from failure modes
		} catch (XMPPException e) {
			if (e.getXMPPError() != null)
				return e.getXMPPError().toString();
			else
				return e.getLocalizedMessage();
		}
	}

	private void onDisconnected(String reason) {
		unregisterPongListener();

		mLastError = reason;
		String rooms = EmotApplication.getValue(PreferenceConstants.ROOMS, "");
		if(rooms != null && !rooms.equals("")){
			final String roomstoJoin[] = rooms.split(",");
			//						//EmotApplication.setValue(PreferenceConstants.ROOMS, rooms + "," + mGroupChat.getRoom());
			Log.i(TAG, "rooms to join " + roomstoJoin);
			//						List<String> roomstoJoin2 = EmotApplication.getRooms();
			Log.i(TAG, "rooms to join size " +roomstoJoin.length);
			//sStopService = false;
			for(int i = 0; i < roomstoJoin.length; i++){
				mJoined.put(roomstoJoin[i], false);
			}
		}
		updateConnectionState(ConnectionState.DISCONNECTED);

		//		new Thread(new Runnable() {
		//			
		//			@Override
		//			public void run() {
		//				mXMPPConnection.disconnect();
		//				
		//			}
		//		}).start();
	}
	private void onDisconnected(Throwable reason) {
		Log.e(TAG, "onDisconnected: " + reason);
		sStopService = true;
		reason.printStackTrace();
		// iterate through to the deepest exception
		while (reason.getCause() != null)
			reason = reason.getCause();
		onDisconnected(reason.getLocalizedMessage());
	}

	public void initMUC(final String pGroupName){
		Form form = null;
		String roomID = EmotUtils.generateRoomID();
		mGroupChat = new MultiUserChat(mXMPPConnection, roomID+"@conference.emot-net");

		try {
			//mGroupChat.create(pGroupName);

			mGroupChat.create(pGroupName);
			mGroupChat.changeSubject(pGroupName);
			mJoined.put(roomID+"@conference.emot-net", false);
			EmotApplication.setValue(roomID+"@conference.emot-net", pGroupName);
			Intent intent = new Intent();
			intent.setAction("GroupIDGenerated");
			intent.putExtra("groupID", roomID+"@conference.emot-net");
			intent.putExtra("grpSubject", pGroupName);

			mService.sendBroadcast(intent);
			Log.i(TAG, "creating multi user chat2 " +mGroupChat);
			form = mGroupChat.getConfigurationForm();
			Form submitForm = form.createAnswerForm(); 
			Iterator<FormField> fields = form.getFields();
			while(fields.hasNext()){
				Log.i(TAG, "while loop 1111111111111111");
				FormField field = (FormField) fields.next();
				if(!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null){
					submitForm.setDefaultAnswer(field.getVariable()); 
				}
			}
			List<String> owners = new ArrayList<String>(); 
			Log.i(TAG, "creating multi user chat 3" +mGroupChat);
			owners.add("9379475511@emot-net");
			//owners.add("test6@emot-net");
			//submitForm.setAnswer("muc#owner", owners);
			Log.i(TAG, "creating multi user chat 4" +mGroupChat);
			try {
				//mGroupChat.sendConfigurationForm(submitForm);
				FormField field = new FormField("muc#roomconfig_persistentroom");
				field.addValue("1");
				FormField field2 = new FormField("muc#roomconfig_membersonly");
				field2.addValue("1");
				FormField field3 = new FormField("muc#roomconfig_changesubject");
				field3.addValue("1");
				submitForm.addField(field);
				submitForm.addField(field2);
				submitForm.addField(field3);

				mGroupChat.sendConfigurationForm(submitForm);


			} catch (XMPPException e) {
				Log.i(TAG, "Exception " +e.getMessage());
				mService.sendBroadcast(new Intent("Group_Generated_Failed"));
				//e.printStackTrace();
			}
		} catch (XMPPException e1) {
			mService.sendBroadcast(new Intent("Group_Generated_Failed"));
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}


	}

	private void setChatRoomInvitationListener() {
		MultiUserChat.addInvitationListener(mXMPPConnection, new InvitationListener() {

			@Override
			public void invitationReceived(Connection connection,
					String room, String inviter, String reason,
					String unKnown, Message message) {

				//MultiUserChat.decline(mXmppConnection, room, inviter,
				//  "Don't bother me right now");
				// MultiUserChat.decline(mXmppConnection, room, inviter,
				// "Don't bother me right now");
				try {
					Log.i("abc","Invitation Received for room " +room);

					mGroupChat2 = new MultiUserChat(connection, room);

					mGroupChat2.join(mConfig.userName+"@conference.emot-net");
					Log.i(TAG, "Room subject for recieved room is " + mGroupChat2.getSubject());
					mJoined.put(room, true);

					String rooms = EmotApplication.getValue(PreferenceConstants.ROOMS, "");
					Log.i(TAG, "rooms being saved into shared preferences " +rooms);
					if(rooms == null){
						rooms = "";
					}

					EmotApplication.setValue(PreferenceConstants.ROOMS, (rooms + "," + mGroupChat2.getRoom()).trim());
					Log.e("abc","join room successfully");
					// muc.sendMessage("I joined this room!! Bravo!!");
				} catch (XMPPException e) {
					//e.printStackTrace();
					Log.e("abc","join room failed!");
				}
			}
		});
	}

	public void joinUsers(List<Contact> members){
		//mGroupChat = new MultiUserChat(mXMPPConnection, "testroom@conference.emot-net");
		int size = members.size();
		DiscussionHistory dh = new DiscussionHistory();

		Log.i(TAG, "joining user is " +members.get(0).getGJID());

		try {
			mGroupChat.join(mConfig.userName +"@conference.emot-net");
			mJoined.put(mGroupChat.getRoom(), true);
			for(int i=0; i< size; i++){
				Log.i(TAG, "joining user " + members.get(i).getGJID());
				mGroupChat.invite(members.get(i).getGJID(),"");

			}


			String rooms = EmotApplication.getValue(PreferenceConstants.ROOMS, "");
			Log.i(TAG, "rooms being saved into shared preferences " +rooms);
			if(rooms == null){
				rooms = "";
			}
			EmotApplication.setValue(PreferenceConstants.ROOMS, (rooms + "," + mGroupChat.getRoom()).trim());
			//mGroupChat.invite("1234567890@emot-net/Smack", "");
			//mGroupChat.invite("test6@emot-net/Smack", "");
		} catch (Exception e) {
			Log.i(TAG, "Error while joining group chat");
			//e.printStackTrace();
		}	

	}



	//	private List<String> discoverjoinedRooms(final String pUserName){
	//		
	//		Iterator<String> joinedRooms = MultiUserChat.getJoinedRooms(mXMPPConnection, pUserName+"/Smack");
	//		
	//		return joinedRooms;
	//	}
	//	
	private void joinRooms(final Iterator<String> pJoinedRooms){
		while(pJoinedRooms.hasNext()){
			final String room = pJoinedRooms.next();
			if(room.trim().equals("")){
				Log.i(TAG, "No rooms to join");
			}else{
				//join the room to multi user chat object.
			}
		}

	}
	private String getDateString(long milliSeconds) {
		SimpleDateFormat dateFormater = new SimpleDateFormat(
				"yy-MM-dd HH:mm:ss");
		Date date = new Date(milliSeconds);
		return dateFormater.format(date);
	}

	private void tryToConnect(boolean create_account) throws EmotXMPPException {
		try {
			if (mXMPPConnection.isConnected()) {
				try {
					mStreamHandler.quickShutdown(); // blocking shutdown prior to re-connection
				} catch (Exception e) {
					Log.d(TAG, "conn.shutdown() failed: " + e);
				}
			}
			registerRosterListener();
			boolean need_bind = !mStreamHandler.isResumePossible();

			if (mConnectionListener != null)
				mXMPPConnection.removeConnectionListener(mConnectionListener);
			mConnectionListener = new ConnectionListener() {
				public void connectionClosedOnError(Exception e) {
					onDisconnected(e);
				}
				public void connectionClosed() {
					// TODO: fix reconnect when we got kicked by the server or SM failed!
					//onDisconnected(null);
					sStopService = true;
					updateConnectionState(ConnectionState.OFFLINE);
				}
				public void reconnectingIn(int seconds) { }
				public void reconnectionFailed(Exception e) { }
				public void reconnectionSuccessful() {
					Log.i(TAG, "Reconnection success full");
				}
			};
			mXMPPConnection.addConnectionListener(mConnectionListener);
			Thread invListener = new Thread(new Runnable() {

				@Override
				public void run() {
					MultiUserChat.addInvitationListener(mXMPPConnection, new InvitationListener() {

						@Override
						public void invitationReceived(Connection arg0, String arg1, String arg2,
								String arg3, String arg4, Message arg5) {


						}
					});

				}
			});
			mXMPPConnection.connect(need_bind);
			// SMACK auto-logins if we were authenticated before
			Log.i(TAG, "Trying again "+create_account+" .. Connected = "+mXMPPConnection.isConnected() + " authenticatec = "+mXMPPConnection.isAuthenticated());
			if (!mXMPPConnection.isAuthenticated()) {
				if (create_account) {
					Log.d(TAG, "creating new server account...");
					AccountManager am = new AccountManager(mXMPPConnection);
					am.createAccount(mConfig.userName, mConfig.password);
				}

				Log.i(TAG, "user = "+mConfig.userName + " password = "+mConfig.password + " resource = "+mConfig.ressource);
				SASLAuthentication.registerSASLMechanism("DIGEST-MD5", org.jivesoftware.smack.sasl.SASLDigestMD5Mechanism.class);
				SASLAuthentication.supportSASLMechanism("DIGEST-MD5", 0);

				mXMPPConnection.login(mConfig.userName, mConfig.password, mConfig.ressource);
				//registerRosterListener();
				LastActivity activity = LastActivityManager.getLastActivity(mXMPPConnection, "919893435659@emot-net"+"/"+mConfig.ressource);// +"/"+mConfig.ressource
				long l = activity.lastActivity;
				
				long l2 = System.currentTimeMillis() - l*1000;
				Log.i(TAG, "activity is " +activity.lastActivity);
				Log.i(TAG, "activity is " +activity.message);
				Log.i(TAG, "activity is " +activity.getIdleTime());
				Log.i(TAG, "activity time is " + EmotUtils.getTimeSimple(getDateString(l2)));
				
				setChatRoomInvitationListener();
				//mXMPPConnection.login(mConfig.userName, mConfig.password);
			}
			
			//joinGroups();
			//sendOfflineMessages();
			//				scheduledExecutorService.schedule(new Runnable() {
			//
			//							@Override
			//							public void run() {
			//								if(mXMPPConnection != null && !mXMPPConnection.isAuthenticated() && !sStopService){
			//
			//								}else if(sStopService){
			//									
			//									scheduledExecutorService.shutdown();
			//								}else{
			//									
			//									try {
			//										sStopService = true;
			//										
			//										mGroupChat.join(mConfig.userName + "@conference.emot-net", "", dh, timeout);
			//									} catch (XMPPException e) {
			//										sStopService = false;
			//										// TODO Auto-generated catch block
			//										//e.printStackTrace();
			//									}
			//								}
			//
			//							}
			//						}, 5, TimeUnit.SECONDS);
			//initMUC("myroom");

			Log.i(TAG, "Trying again 222"+create_account+" .. Connected = "+mXMPPConnection.isConnected() + " authenticatec = "+mXMPPConnection.isAuthenticated());
			Log.d(TAG, "SM: can resume = " + mStreamHandler.isResumePossible() + " needbind=" + need_bind);
			//Setting status in starting
			mConfig.loadPrefs();
			boolean isAway = EmotApplication.getValue(ApplicationConstants.IS_GOING_TO_ANOTHER_APP_SCREEN, false);
			if(!isAway){
				EmotApplication.setValue(PreferenceConstants.STATUS_MODE, StatusMode.available.name());
			}else{
				EmotApplication.setValue(PreferenceConstants.STATUS_MODE, StatusMode.away.name());
			}

			if (need_bind) {
				mStreamHandler.notifyInitialLogin();
				setStatusFromConfig();
			}

		} catch (Exception e) {
			// actually we just care for IllegalState or NullPointer or XMPPEx.
			//e.printStackTrace();
			throw new EmotXMPPException("tryToConnect failed", e);
		}
	}

	private void tryToMoveRosterEntryToGroup(String userName, String groupName)
			throws EmotXMPPException {

		RosterGroup rosterGroup = getRosterGroup(groupName);
		RosterEntry rosterEntry = mRoster.getEntry(userName);

		removeRosterEntryFromGroups(rosterEntry);

		if (groupName.length() == 0)
			return;
		else {
			try {
				rosterGroup.addEntry(rosterEntry);
			} catch (XMPPException e) {
				throw new EmotXMPPException("tryToMoveRosterEntryToGroup", e);
			}
		}
	}

	private RosterGroup getRosterGroup(String groupName) {
		RosterGroup rosterGroup = mRoster.getGroup(groupName);

		// create group if unknown
		if ((groupName.length() > 0) && rosterGroup == null) {
			rosterGroup = mRoster.createGroup(groupName);
		}
		return rosterGroup;

	}

	private void removeRosterEntryFromGroups(RosterEntry rosterEntry)
			throws EmotXMPPException {
		Collection<RosterGroup> oldGroups = rosterEntry.getGroups();

		for (RosterGroup group : oldGroups) {
			tryToRemoveUserFromGroup(group, rosterEntry);
		}
	}

	private void tryToRemoveUserFromGroup(RosterGroup group,
			RosterEntry rosterEntry) throws EmotXMPPException {
		try {
			group.removeEntry(rosterEntry);
		} catch (XMPPException e) {
			throw new EmotXMPPException("tryToRemoveUserFromGroup", e);
		}
	}

	private void tryToRemoveRosterEntry(String user) throws EmotXMPPException {
		try {
			RosterEntry rosterEntry = mRoster.getEntry(user);

			if (rosterEntry != null) {
				// first, unsubscribe the user
				Presence unsub = new Presence(Presence.Type.unsubscribed);
				unsub.setTo(rosterEntry.getUser());
				mXMPPConnection.sendPacket(unsub);
				// then, remove from roster
				mRoster.removeEntry(rosterEntry);
			}
		} catch (XMPPException e) {
			throw new EmotXMPPException("tryToRemoveRosterEntry", e);
		}
	}

	private void tryToAddRosterEntry(String user, String alias, String group)
			throws EmotXMPPException {
		try {
			mRoster.createEntry(user, alias, new String[] { group });
		} catch (XMPPException e) {
			throw new EmotXMPPException("tryToAddRosterEntry", e);
		}
	}

	private void removeOldRosterEntries() {
		Log.d(TAG, "removeOldRosterEntries()");
		Collection<RosterEntry> rosterEntries = mRoster.getEntries();
		StringBuilder exclusion = new StringBuilder(RosterConstants.JID + " NOT IN (");
		boolean first = true;
		for (RosterEntry rosterEntry : rosterEntries) {
			updateRosterEntryInDB(rosterEntry);
			if (first)
				first = false;
			else
				exclusion.append(",");
			exclusion.append("'").append(rosterEntry.getUser()).append("'");
		}
		exclusion.append(")");
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI, exclusion.toString(), null);
		Log.d(TAG, "deleted " + count + " old roster entries");
	}

	// HACK: add an incoming subscription request as a fake roster entry
	private void handleIncomingSubscribe(Presence request) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, request.getFrom());
		values.put(RosterConstants.ALIAS, request.getFrom());
		values.put(RosterConstants.GROUP, "");

		values.put(RosterConstants.STATUS_MODE, getStatusInt(request));
		values.put(RosterConstants.STATUS_MESSAGE, request.getStatus());
//		Presence unsub = new Presence(Presence.Type.subscribed);
//		unsub.setTo(request.getFrom());
//		mXMPPConnection.sendPacket(unsub);
//		Log.i(TAG, "Sending subscription packet");
		Uri uri = mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		Log.d(TAG, "handleIncomingSubscribe: faked " + uri);
	}



	public void setStatusFromConfig() {
		try{
			mConfig.loadPrefs();
			//SharedPreferences prefs = EmotApplication.getAppContext().getSharedPreferences("emot_prefs", Context.MODE_MULTI_PROCESS);

			//Updating mconfig before updating status
			CarbonManager.getInstanceFor(mXMPPConnection).sendCarbonsEnabled(mConfig.messageCarbons);

			Presence presence = new Presence(Presence.Type.available);
			
			Log.i(TAG, "Setting mode to be sent as " +mConfig.statusMode);
			Mode mode = Mode.valueOf(mConfig.statusMode);
			Log.i(TAG, "Setting mode to be sent as " +mode);
			if("away".equals(mConfig.statusMode)){
				presence.setMode(Mode.away);
			}else if("available".equals(mConfig.statusMode)){
				presence.setMode(Mode.available);
			}
			
			
			presence.setMode(mode);
			
			presence.setStatus(mConfig.statusMessage);
			presence.setPriority(mConfig.priority);
			mXMPPConnection.sendPacket(presence);
			mConfig.presence_required = false;
			OfflineMessageManager ofn = new OfflineMessageManager(mXMPPConnection);
			Log.i(TAG, "offline message size is " +ofn.getMessageCount());
		}catch(Exception e){
			//e.printStackTrace();
		}
	}

	public void sendOfflineMessages() {
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				SEND_OFFLINE_PROJECTION, SEND_OFFLINE_SELECTION,
				null, null);
		final int      _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
		final int      JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int      MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int      MSG_TYPE = cursor.getColumnIndexOrThrow(ChatConstants.CHAT_TYPE);
		final int       TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext()) {
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String message = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			String messageType = cursor.getString(MSG_TYPE);
			long ts = cursor.getLong(TS_COL);
			Log.d(TAG, "sendOfflineMessages: " + toJID + " > " + message);
			Message newMessage =null;
			if(messageType.equals(CHATTYPE)){
				newMessage = new Message(toJID, Message.Type.chat);

			}else{
				newMessage = new Message(toJID, Message.Type.groupchat);
			}
			newMessage.setBody(message);
			DelayInformation delay = new DelayInformation(new Date(ts));
			newMessage.addExtension(delay);
			newMessage.addExtension(new DelayInfo(delay));
			newMessage.addExtension(new DeliveryReceiptRequest());
			if ((packetID != null) && (packetID.length() > 0)) {
				newMessage.setPacketID(packetID);
			} else {
				packetID = newMessage.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}

			if(messageType.equals(CHATTYPE)){
				mXMPPConnection.sendPacket(newMessage);
				Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
						+ "/" + ChatProvider.TABLE_NAME + "/" + _id);
				mContentResolver.update(rowuri, mark_sent,
						null, null);

			}else{
				if(mJoined.get(toJID)){
					Log.i(TAG, "sending message " +newMessage.getBody());
					mXMPPConnection.sendPacket(newMessage);
					Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
							+ "/" + ChatProvider.TABLE_NAME + "/" + _id);
					mContentResolver.update(rowuri, mark_sent,
							null, null);

				}else{
					Log.i(TAG, "not joined in group " +toJID);
				}
			}// must be after marking delivered, otherwise it may override the SendFailListener
		}
		cursor.close();
	}

	public void sendFailedMessages() {
		Log.i(TAG, "-- Sending offline messages --");
		Cursor cursor = mContentResolver.query(ChatProvider.CONTENT_URI,
				SEND_FAILED_PROJECTION, SEND_FAILED_SELECTION,
				null, null);
		final int      _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
		final int      JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
		final int      MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
		final int       TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
		final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
		ContentValues mark_sent = new ContentValues();
		mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		while (cursor.moveToNext()) {
			int _id = cursor.getInt(_ID_COL);
			String toJID = cursor.getString(JID_COL);
			String message = cursor.getString(MSG_COL);
			String packetID = cursor.getString(PACKETID_COL);
			long ts = cursor.getLong(TS_COL);
			Log.d(TAG, "sendOfflineMessages: " + toJID + " > " + message);
			final Message newMessage = new Message(toJID, Message.Type.chat);
			newMessage.setBody(message);
			DelayInformation delay = new DelayInformation(new Date(ts));
			newMessage.addExtension(delay);
			newMessage.addExtension(new DelayInfo(delay));
			newMessage.addExtension(new DeliveryReceiptRequest());
			if ((packetID != null) && (packetID.length() > 0)) {
				newMessage.setPacketID(packetID);
			} else {
				packetID = newMessage.getPacketID();
				mark_sent.put(ChatConstants.PACKET_ID, packetID);
			}
			Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
					+ "/" + ChatProvider.TABLE_NAME + "/" + _id);
			mContentResolver.update(rowuri, mark_sent,
					null, null);
			mXMPPConnection.sendPacket(newMessage);		// must be after marking delivered, otherwise it may override the SendFailListener
		}
		cursor.close();
	}

	public boolean isRunning() {
		ActivityManager activityManager = (ActivityManager) EmotApplication.getAppContext().getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);
		RunningTaskInfo foregroundTaskInfo = activityManager.getRunningTasks(1).get(0);
		if(EmotApplication.getAppContext().getPackageName().equals(foregroundTaskInfo.baseActivity.getPackageName())){
			
			Log.i(TAG,  "foreground activity is "+foregroundTaskInfo.topActivity.getClassName());
			return true;
		}
//		for (RunningTaskInfo task : tasks) {
//			if (EmotApplication.getAppContext().getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName())) 
//				return true;                                  
//		}

		return false;
	}
	private String acknowledgedPacketID;
	public void sendNotacknowledgedMessages(String jid) {
		try{
			Log.i(TAG, " -- sending notacknowledgedMessages to "+jid+" -- ");
			Cursor cursor = mContentResolver.query(
					ChatProvider.CONTENT_URI,
					SEND_UNACKNOWLEDGED_PROJECTION, 
					SEND_UNACKNOWLEDGED_SELECTION + " AND "+ChatProvider.ChatConstants.JID +"='"+jid+"'",
					null, 
					ChatProvider.ChatConstants.DATE + " ASC limit 50"
					);
			Log.i(TAG, "msges found= "+cursor.getCount());
			final int      _ID_COL = cursor.getColumnIndexOrThrow(ChatConstants._ID);
			final int      JID_COL = cursor.getColumnIndexOrThrow(ChatConstants.JID);
			final int      MSG_COL = cursor.getColumnIndexOrThrow(ChatConstants.MESSAGE);
			final int       TS_COL = cursor.getColumnIndexOrThrow(ChatConstants.DATE);
			final int      MSG_TYPE = cursor.getColumnIndexOrThrow(ChatConstants.CHAT_TYPE);
			final int PACKETID_COL = cursor.getColumnIndexOrThrow(ChatConstants.PACKET_ID);
			ContentValues mark_sent = new ContentValues();
			mark_sent.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
			while (cursor.moveToNext()) {
				int _id = cursor.getInt(_ID_COL);
				String toJID = cursor.getString(JID_COL);
				String message = cursor.getString(MSG_COL);
				String packetID = cursor.getString(PACKETID_COL);

				String msgType = cursor.getString(MSG_TYPE);
				long ts = cursor.getLong(TS_COL);
				Log.i(TAG, "sendnotacknowledgedMessages: " + toJID + " > " + message);
				Message newMessage = null;
				if(msgType.equals(CHATTYPE)){
					newMessage = new Message(toJID, Message.Type.chat);
				}else{
					newMessage = new Message(toJID, Message.Type.groupchat);
				}
				newMessage.setBody(message);
				DelayInformation delay = new DelayInformation(new Date(ts));
				newMessage.addExtension(delay);
				newMessage.addExtension(new DelayInfo(delay));
				newMessage.addExtension(new DeliveryReceiptRequest());
				if ((packetID != null) && (packetID.length() > 0)) {
					newMessage.setPacketID(packetID);
				} else {
					packetID = newMessage.getPacketID();
					mark_sent.put(ChatConstants.PACKET_ID, packetID);
				}
				Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY
						+ "/" + ChatProvider.TABLE_NAME + "/" + _id);
				mContentResolver.update(rowuri, mark_sent,
						null, null);
				if(msgType.equals(CHATTYPE)){
					mXMPPConnection.sendPacket(newMessage);
				}else{
					if(mJoined.get(toJID)){
						mXMPPConnection.sendPacket(newMessage);
					}
				}// must be after marking delivered, otherwise it may override the SendFailListener
			}
			cursor.close();
		}catch(Exception e){
			//e.printStackTrace();
		}
	}

	public static void sendOfflineMessage(ContentResolver cr, String toJID, String message, String type) {
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DIRECTION, ChatConstants.OUTGOING);
		values.put(ChatConstants.JID, toJID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.CHAT_TYPE, type);
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_NEW);
		values.put(ChatConstants.DATE, System.currentTimeMillis());

		cr.insert(ChatProvider.CONTENT_URI, values);
	}

	public void sendReceipt(String toJID, String id) {
		Log.d(TAG, "sending XEP-0184 ack to " + toJID + " id=" + id);
		final Message ack = new Message(toJID, Message.Type.normal);
		ack.addExtension(new DeliveryReceipt(id));
		mXMPPConnection.sendPacket(ack);
	}

	public void sendMessage(String toJID, String message) {
		Log.i(TAG, "Sending message");
		final Message newMessage = new Message(toJID, Message.Type.chat);
		newMessage.setBody(message);
		newMessage.addExtension(new DeliveryReceiptRequest());
		if (isAuthenticated()) {
			addChatMessageToDB(ChatConstants.OUTGOING, toJID, "",message, ChatConstants.DS_SENT_OR_READ,
					System.currentTimeMillis(), newMessage.getPacketID(), CHATTYPE, null );
			mXMPPConnection.sendPacket(newMessage);
		} else {
			// send offline -> store to DB
			addChatMessageToDB(ChatConstants.OUTGOING, toJID,"", message, ChatConstants.DS_NEW,
					System.currentTimeMillis(), newMessage.getPacketID(),CHATTYPE, null);
		}
	}

	public boolean isAuthenticated() {
		if (mXMPPConnection != null) {
			return (mXMPPConnection.isConnected() && mXMPPConnection
					.isAuthenticated());
		}
		return false;
	}

	public void registerCallback(XMPPServiceCallback callBack) {
		Log.i(TAG, "Callback registering !!!");
		this.mServiceCallBack = callBack;
		mService.registerReceiver(mPingAlarmReceiver, new IntentFilter(PING_ALARM));
		mService.registerReceiver(mPongTimeoutAlarmReceiver, new IntentFilter(PONG_TIMEOUT_ALARM));
	}

	public void unRegisterCallback() {
		Log.d(TAG, "unRegisterCallback()");
		// remove callbacks _before_ tossing old connection
		try {
			mXMPPConnection.getRoster().removeRosterListener(mRosterListener);
			mXMPPConnection.removePacketListener(mPacketListener);
			mXMPPConnection.removePacketListener(mPresenceListener);

			mXMPPConnection.removePacketListener(mPongListener);
			unregisterPongListener();
		} catch (Exception e) {
			// ignore it!
			//e.printStackTrace();
		}
		requestConnectionState(ConnectionState.OFFLINE);
		//setStatusOffline();
		mService.unregisterReceiver(mPingAlarmReceiver);
		mService.unregisterReceiver(mPongTimeoutAlarmReceiver);
		this.mServiceCallBack = null;
	}

	public String getNameForJID(String jid) {
		if (null != this.mRoster.getEntry(jid) && null != this.mRoster.getEntry(jid).getName() && this.mRoster.getEntry(jid).getName().length() > 0) {
			return this.mRoster.getEntry(jid).getName();
		} else {
			return jid;
		}			
	}

	private void setStatusOffline() {
		ContentValues values = new ContentValues();
		values.put(RosterConstants.STATUS_MODE, StatusMode.away.ordinal());
		mContentResolver.update(RosterProvider.CONTENT_URI, values, null, null);
	}

	private void registerRosterListener() {
		// flush roster on connecting.
		Log.i(TAG, "-------- Registerng all rosters --------");
		mRoster = mXMPPConnection.getRoster();
		Collection<RosterEntry> rosters = mRoster.getEntries();
		for(RosterEntry rstr: rosters){
			Presence p = mRoster.getPresence(rstr.getUser());
			Log.i(TAG, "Roster status " + rstr.getStatus() + " Presence ="+p.getStatus());
		}

		mRoster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);

		if (mRosterListener != null)
			mRoster.removeRosterListener(mRosterListener);

		mRosterListener = new RosterListener() {
			private boolean first_roster = true;

			public void entriesAdded(Collection<String> entries) {
				Log.i(TAG, "entriesAdded(" + entries + ")");

				ContentValues[] cvs = new ContentValues[entries.size()];
				int i = 0;
				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					cvs[i++] = getContentValuesForRosterEntry(rosterEntry);
				}
				mContentResolver.bulkInsert(RosterProvider.CONTENT_URI, cvs);
				// when getting the roster in the beginning, remove remains of old one
				if (first_roster) {
					removeOldRosterEntries();
					first_roster = false;
					mServiceCallBack.rosterChanged();
				}
				Log.d(TAG, "entriesAdded() done");
			}

			public void entriesDeleted(Collection<String> entries) {
				Log.d(TAG, "entriesDeleted(" + entries + ")");

				for (String entry : entries) {
					deleteRosterEntryFromDB(entry);
				}
				mServiceCallBack.rosterChanged();
			}

			public void entriesUpdated(Collection<String> entries) {
				Log.d(TAG, "entriesUpdated(" + entries + ")");

				for (String entry : entries) {
					RosterEntry rosterEntry = mRoster.getEntry(entry);
					updateRosterEntryInDB(rosterEntry);
				}
				mServiceCallBack.rosterChanged();
			}

			public void presenceChanged(Presence presence) {
				Log.i(TAG, "presenceChanged(" + presence.getFrom() + "): "+presence +" status"+ presence.getStatus());


				String jabberID = getBareJID(presence.getFrom());

				RosterEntry rosterEntry = mRoster.getEntry(jabberID);
				Log.i(TAG, "Roster status = "+rosterEntry.getStatus() );
				if (rosterEntry != null) {
					updateRosterEntryInDB(rosterEntry);
					mServiceCallBack.rosterChanged();
				}
				//Sending Chat status as gone
				Log.i(TAG, "presence mode is " +presence.isAway());
				if(getStatusInt(presence)!=StatusMode.available.ordinal()){
					mServiceCallBack.chatStateChanged(ChatState.gone.ordinal(), jabberID);
				}else{
					mServiceCallBack.chatStateChanged(ChatState.active.ordinal(), jabberID);
				}
//				if(presence.isAway()){
//					mServiceCallBack.chatStateChanged(ChatState.gone.ordinal(), jabberID);
//				}else{
//					mServiceCallBack.chatStateChanged(ChatState.active.ordinal(), jabberID);
//				}

				sendNotacknowledgedMessages(jabberID);
			}
		};
		mRoster.addRosterListener(mRosterListener);
	}
	private String getBareGJID(String from) {
		String[] res = from.split("/");
		return res[1].toLowerCase();
	}
	private String getBareJID(String from) {
		String[] res = from.split("/");
		return res[0].toLowerCase();
	}

	public boolean changeMessageDeliveryStatus(String packetID, int new_status) {
		ContentValues cv = new ContentValues();
		cv.put(ChatConstants.DELIVERY_STATUS, new_status);
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME);
		return mContentResolver.update(rowuri, cv,
				ChatConstants.PACKET_ID + " = ? AND " +
						ChatConstants.DELIVERY_STATUS + " != " + ChatConstants.DS_ACKED + " AND " +
						ChatConstants.DIRECTION + " = " + ChatConstants.OUTGOING,
						new String[] { packetID }) > 0;
	}

	protected boolean is_user_watching = false;
	public void setUserWatching(boolean user_watching) {
		if (is_user_watching == user_watching)
			return;
		is_user_watching = user_watching;
		if (mXMPPConnection != null && mXMPPConnection.isAuthenticated())
			sendUserWatching();
	}

	protected void sendUserWatching() {
		IQ toggle_google_queue = new IQ() {
			public String getChildElementXML() {
				// enable g:q = start queueing packets = do it when the user is gone
				return "<query xmlns='google:queue'><" + (is_user_watching ? "disable" : "enable") + "/></query>";
			}
		};
		toggle_google_queue.setType(IQ.Type.SET);
		mXMPPConnection.sendPacket(toggle_google_queue);
	}

	/** Check the server connection, reconnect if needed.
	 *
	 * This function will try to ping the server if we are connected, and try
	 * to reestablish a connection otherwise.
	 */
	public void sendServerPing() {
		if (mXMPPConnection == null || !mXMPPConnection.isAuthenticated()) {
			Log.i(TAG, "Ping: requested, but not connected to server.");
			requestConnectionState(ConnectionState.ONLINE, false);
			return;
		}
		if (mPingID != null) {
			Log.i(TAG, "Ping: requested, but still waiting for " + mPingID);
			return; // a ping is still on its way
		}

		if (mStreamHandler.isSmEnabled()) {
			Log.i(TAG, "Ping: sending SM request");
			mPingID = "" + mStreamHandler.requestAck();
		} else {
			Ping ping = new Ping();
			ping.setType(Type.GET);
			ping.setTo(mConfig.server);
			mPingID = ping.getPacketID();
			Log.i(TAG, "Ping: sending ping " + mPingID);
			mXMPPConnection.sendPacket(ping);
		}

		// register ping timeout handler: PACKET_TIMEOUT(30s) + 3s
		registerPongTimeout(PACKET_TIMEOUT + 3000, mPingID);
	}

	private void gotServerPong(String pongID) {
		Log.i(TAG, "pong ID : "+pongID + " ping ID : "+mPingID);
		long latency = System.currentTimeMillis() - mPingTimestamp;
		if (pongID != null && pongID.equals(mPingID)){
			Log.i(TAG, String.format("Ping: server latency %1.3fs", latency/1000.));
		}
		else{
			Log.i(TAG, String.format("Ping: server latency %1.3fs (estimated)", latency/1000.));
		}
			
		mPingID = null;
		mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
	}

	/** Register a "pong" timeout on the connection. */
	private void registerPongTimeout(long wait_time, String id) {
		mPingID = id;
		mPingTimestamp = System.currentTimeMillis();
		Log.i(TAG, String.format("Ping: registering timeout for %s: %1.3fs", id, wait_time/1000.));
		mAlarmManager.set(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + wait_time,
				mPongTimeoutAlarmPendIntent);
	}

	/**
	 * BroadcastReceiver to trigger reconnect on pong timeout.
	 */
	private class PongTimeoutAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			Log.i(TAG, "Ping: timeout for " + mPingID);
			onDisconnected("Ping timeout");
		}
	}

	/**
	 * BroadcastReceiver to trigger sending pings to the server
	 */
	private class PingAlarmReceiver extends BroadcastReceiver {
		public void onReceive(Context ctx, Intent i) {
			Log.i(TAG, "Received PING broadcast");
			sendServerPing();
		}
	}

	/**
	 * Registers a smack packet listener for IQ packets, intended to recognize "pongs" with
	 * a packet id matching the last "ping" sent to the server.
	 *
	 * Also sets up the AlarmManager Timer plus necessary intents.
	 */
	private void registerPongListener() {
		// reset ping expectation on new connection
		mPingID = null;

		if (mPongListener != null)
			mXMPPConnection.removePacketListener(mPongListener);

		mPongListener = new PacketListener() {

			@Override
			public void processPacket(Packet packet) {
				if (packet == null) return;

				gotServerPong(packet.getPacketID());
			}

		};
		
		

		mXMPPConnection.addPacketListener(mPongListener, new PacketTypeFilter(IQ.class));
		mPingAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPingAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		mPongTimeoutAlarmPendIntent = PendingIntent.getBroadcast(mService.getApplicationContext(), 0, mPongTimeoutAlarmIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		//		mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
		//				System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES, AlarmManager.INTERVAL_FIFTEEN_MINUTES, mPingAlarmPendIntent);
		mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 
				System.currentTimeMillis() + 60*1000, 60*1000, mPingAlarmPendIntent);
	}
	
	private PacketFilter mPingPacketFilter = new PacketFilter() {
		
		@Override
		public boolean accept(Packet packet) {
			if(packet instanceof Ping){
				Log.i(TAG, "Received server ping");
				Log.i(TAG,"Server ping packet is " +packet.toXML());
				return true;
			}else{
			return false;
			}
		}
	};
	
	class Pong1 extends IQ{
		
		Pong1(){
			this.setType(Type.RESULT);
		}

		@Override
		public String getChildElementXML() {
			StringBuffer sb = new StringBuffer();
//			sb.append("<ping xmlns='urn:xmpp:ping'/>");
//			sb.append("<error type='cancel'>");
//			sb.append("<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>");
//			sb.append("</error>");
			return sb.toString();
			
		}
		
	}
	private void registerServerPingListener(){
		
		PacketTypeFilter filter = new PacketTypeFilter(Ping.class);
		
		mServerPingListener = new PacketListener() {
			
			@Override
			public void processPacket(Packet packet) {
				Log.i(TAG, "Received Server ping");
				if(packet == null){
					return;
				}
				
				Pong1 p1 = new Pong1();
				String from = packet.getFrom();
				
				
				String to = packet.getTo();
				String packetID = packet.getPacketID();
				to = getBareJID(to);
				Log.i(TAG, "Server ping to is " +to);
				p1.setFrom(to);
				p1.setTo(from);
				p1.setPacketID(packetID);
				Log.i(TAG, "Sending pong for server ping");
				Log.i(TAG, "packet pong is for server ping" +p1.toXML());
				mXMPPConnection.sendPacket(p1);
				
			}
		};
		
		mXMPPConnection.addPacketListener(mServerPingListener, mPingPacketFilter);
		
	}
	private void unregisterPongListener() {
		mAlarmManager.cancel(mPingAlarmPendIntent);
		mAlarmManager.cancel(mPongTimeoutAlarmPendIntent);
	}

	private void registerMessageListener() {
		// do not register multiple packet listeners
		if (mPacketListener != null)
			mXMPPConnection.removePacketListener(mPacketListener);

		PacketTypeFilter filter = new PacketTypeFilter(Message.class);

		mPacketListener = new PacketListener() {
			public void processPacket(Packet packet) {
				Log.i(TAG, "packet is " +packet.toXML());

				try {

					if (packet instanceof Message) {
						Message msg = (Message) packet;

						String from = msg.getFrom();
						String fromJID = getBareJID(msg.getFrom());
						if(msg.getSubject() != null){
							Log.i(TAG, "saving to SP " + fromJID + "= " + msg.getSubject());
							Intent intent = new Intent();
							intent.putExtra("grpID", fromJID);
							intent.putExtra("newSubject", msg.getSubject());
							intent.setAction("GroupSubjectChanged");
							//addChatMessageToDB(ChatConstants.OUTGOING, fromJID, msg.getSubject(), "", ChatConstants.DS_SENT_OR_READ,
							//		System.currentTimeMillis(), "sss", GROUPCHATTYPE, "");
							mService.sendBroadcast(intent);
							EmotApplication.setValue(fromJID, msg.getSubject());
						}
						grpSubject = EmotApplication.getValue(fromJID, "default");
						Log.i(TAG, "fromJID is " +fromJID);
						Log.i(TAG, "grpSubject is " +grpSubject +"mSmackable is " +this);
						Log.i(TAG, "message properties " + msg.getPropertyNames());
						Log.i(TAG, "message extensions " + msg.getExtensions());
						Log.i(TAG, "room nickname " +msg.getProperty("roomName"));
						PacketExtension extension = msg.getExtension("http://jabber.org/protocol/chatstates");
						Log.i(TAG, "Extension  = "+extension +" composing  = "+ msg.getProperty("composing") + " propetries = "+msg.getPropertyNames().size());

						if (extension != null) {
							String value = ChatState.valueOf(extension.getElementName()).name();
							Log.i(TAG, "Value = " + value);
							mServiceCallBack.chatStateChanged(ChatState.valueOf(extension.getElementName()).ordinal(), fromJID);
							if (value.equals("composing")) {
								Log.i(TAG, "COMPOSING MESSAGE " + value);
								//mServiceCallBack.messageComposing(msg.getFrom(), true);
							} else {
								Log.i(TAG, "STOPPED COMPOSING MESSAGE "+ value);
								//mServiceCallBack.messageComposing(msg.getFrom(), false);
							}
						}





						Log.i(TAG, "Chat coming from" +fromJID);
						int direction = ChatConstants.INCOMING;
						Carbon cc = CarbonManager.getCarbon(msg);

						// extract timestamp
						long ts;
						DelayInfo timestamp = (DelayInfo)msg.getExtension("delay", "urn:xmpp:delay");
						if (timestamp == null)
							timestamp = (DelayInfo)msg.getExtension("x", "jabber:x:delay");
						if (cc != null) // Carbon timestamp overrides packet timestamp
							timestamp = cc.getForwarded().getDelayInfo();
						if (timestamp != null)
							ts = timestamp.getStamp().getTime();
						else
							ts = System.currentTimeMillis();

						// try to extract a carbon
						if (cc != null) {
							Log.i(TAG, "carbon: " + cc.toXML());
							msg = (Message)cc.getForwarded().getForwardedPacket();

							// outgoing carbon: fromJID is actually chat peer's JID
							if (cc.getDirection() == Carbon.Direction.sent) {
								fromJID = getBareJID(msg.getTo());
								direction = ChatConstants.OUTGOING;
							} else {
								fromJID = getBareJID(msg.getFrom());

								// hook off carbonated delivery receipts
								DeliveryReceipt dr = (DeliveryReceipt)msg.getExtension(
										DeliveryReceipt.ELEMENT, DeliveryReceipt.NAMESPACE);
								if (dr != null) {
									Log.d(TAG, "got CC'ed delivery receipt for " + dr.getId());
									changeMessageDeliveryStatus(dr.getId(), ChatConstants.DS_ACKED);
								}
							}

							String chatMessage = msg.getBody();

							// display error inline
							if (msg.getType() == Message.Type.error) {
								if (changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_FAILED)){
									Log.i(TAG, "message failed !!!");
									//mServiceCallBack.messageError(fromJID, msg.getError().toString(), (cc != null));
									sendFailedMessages();
								}
								return; // we do not want to add errors as "incoming messages"
							}

							// ignore empty messages
							if (chatMessage == null) {
								Log.d(TAG, "empty message.");
								return;
							}

							// carbons are old. all others are new
							int is_new = (cc == null) ? ChatConstants.DS_NEW : ChatConstants.DS_SENT_OR_READ;
							if (msg.getType() == Message.Type.error)
								is_new = ChatConstants.DS_FAILED;
							if(msg.getType() == Message.Type.chat ){

								boolean isPacketPresent = addChatMessageToDB(direction, fromJID, "",chatMessage, is_new, ts, msg.getPacketID(), CHATTYPE, null);
								if (direction == ChatConstants.INCOMING && !isPacketPresent)
									mServiceCallBack.newMessage(fromJID,chatMessage, (cc != null), false, getBareGJID(from));
							}else if(msg.getType() == Message.Type.groupchat ){
								EmotApplication.setLongValue("historySince", ts);
								boolean isPacketPresent = addChatMessageToDB(direction, fromJID, grpSubject,chatMessage, is_new, ts, msg.getPacketID(), GROUPCHATTYPE, null);
								if (direction == ChatConstants.INCOMING && !isPacketPresent)
									mServiceCallBack.newMessage(fromJID, chatMessage, (cc != null), true, getBareGJID(from));	
							}
						}

						String chatMessage = msg.getBody();

						Log.i(TAG, "message received is "+msg.getBody() + "type is " + msg.getType());
						// display error inline
						if (msg.getType() == Message.Type.error) {
							if (changeMessageDeliveryStatus(msg.getPacketID(), ChatConstants.DS_FAILED)){
								//mServiceCallBack.messageError(fromJID, msg.getError().toString(), (cc != null));
								//sendFailedMessages();
							}
							return; // we do not want to add errors as "incoming messages"
						}

						// ignore empty messages
						if (chatMessage == null) {
							Log.d(TAG, "empty message.");
							return;
						}

						// carbons are old. all others are new
						int is_new = (cc == null) ? ChatConstants.DS_NEW : ChatConstants.DS_SENT_OR_READ;
						if (msg.getType() == Message.Type.error)
							is_new = ChatConstants.DS_FAILED;

						//addChatMessageToDB(direction, fromJID, chatMessage, is_new, ts, msg.getPacketID());
						if (direction == ChatConstants.INCOMING){
							if(msg.getType() == Message.Type.chat ){
								boolean isPacketPresent = addChatMessageToDB(direction, fromJID, "",chatMessage, is_new, ts, msg.getPacketID(), CHATTYPE, null);
								if(!isPacketPresent){
									mServiceCallBack.newMessage(fromJID, chatMessage, (cc != null), false, getBareGJID(from));
								}
							}else if(msg.getType() == Message.Type.groupchat ){

								Log.i(TAG, "Message from group member " +getBareGJID(from));
								EmotApplication.setLongValue("historySince", ts);
								boolean isPacketPresent = addChatMessageToDB(direction, fromJID, grpSubject,chatMessage, is_new, ts, msg.getPacketID(), GROUPCHATTYPE, getBareGJID(from));
								if(!isPacketPresent){
									mServiceCallBack.newMessage(getBareJID(fromJID), chatMessage, (cc != null), true, getBareGJID(from));
								}
							}else{
								Log.i(TAG, "Unknown message type");
							}
						}
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from processPacket :(
					Log.e(TAG, "failed to process packet:");
					//e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPacketListener, filter);
	}

	private void registerPresenceListener() {
		// do not register multiple packet listeners
		if (mPresenceListener != null)
			mXMPPConnection.removePacketListener(mPresenceListener);

		mPresenceListener = new PacketListener() {
			public void processPacket(Packet packet) {
				try {
					Presence p = (Presence) packet;
					switch (p.getType()) {
					case subscribe:
						handleIncomingSubscribe(p);
						break;
					case unsubscribe:
						break;
					}
				} catch (Exception e) {
					// SMACK silently discards exceptions dropped from processPacket :(
					Log.e(TAG, "failed to process presence:");
					//e.printStackTrace();
				}
			}
		};

		mXMPPConnection.addPacketListener(mPresenceListener, new PacketTypeFilter(Presence.class));
	}

	private boolean addChatMessageToDB(int direction, String JID, String grpSubject,
			String message, int delivery_status, long ts, String packetID, String chatType, String messageSenderinGroup) {

		if(direction == ChatConstants.INCOMING){
			Cursor oldChat = mContentResolver.query(
					ChatProvider.CONTENT_URI, 
					new String[]{ChatProvider.ChatConstants.PACKET_ID}, 
					ChatProvider.ChatConstants.PACKET_ID+" = '"+packetID+"' and "+ChatProvider.ChatConstants.MESSAGE+ " = '"+message+"'", 
					null, 
					null
					);
			if(oldChat.getCount()>0){
				Log.i(TAG, "Packet ID already present");
				return true;
			}
		}

		ContentValues values = new ContentValues();

		values.put(ChatConstants.DIRECTION, direction);
		values.put(ChatConstants.JID, JID);
		values.put(ChatConstants.MESSAGE, message);
		values.put(ChatConstants.GRP_SUBJECT, grpSubject);
		values.put(ChatConstants.DELIVERY_STATUS, delivery_status);
		values.put(ChatConstants.DATE, ts);
		values.put(ChatConstants.PACKET_ID, packetID);
		values.put(ChatConstants.CHAT_TYPE, chatType);
		values.put(ChatConstants.MESSAGE_SENDER_IN_GROUP, messageSenderinGroup);

		mContentResolver.insert(ChatProvider.CONTENT_URI, values);
		return false;
	}

	private String getDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = new Date();
		
		return dateFormat.format(date);
	}

	private ContentValues getContentValuesForRosterEntry(final RosterEntry entry) {
		final ContentValues values = new ContentValues();

		values.put(RosterConstants.JID, entry.getUser());
		values.put(RosterConstants.ALIAS, getName(entry));
		Log.i(TAG, "mRoster is " +mRoster);
		Presence presence = mRoster.getPresence(entry.getUser());
		int currentStatus = getStatusInt(presence);
		Log.i(TAG, "available ordinal val "+StatusMode.available.ordinal());
		values.put(RosterConstants.STATUS_MODE, currentStatus);
		Log.i(TAG, "Presence for = " + " " + entry.getUser() +" "+ presence);

		mConfig.loadPrefs();
		
		int last_status = EmotApplication.getValue(PreferenceConstants.LAST_STATUS_JID+entry.getUser(), -1);
		Log.i(TAG, "LAST STATUS: "+last_status + " Current status: "+currentStatus );
//		if(currentStatus != StatusMode.available.ordinal() && last_status!=currentStatus){
//			Log.i(TAG, "AWAY STATUS");
//			values.put(RosterConstants.LAST_SEEN, getDateTime());
//		}
		
		values.put(RosterConstants.LAST_SEEN, getDateTime());
		
		
		EmotApplication.setValue(PreferenceConstants.LAST_STATUS_JID+entry.getUser(), currentStatus);
		
		if (presence.getType() == Presence.Type.error) {
			values.put(RosterConstants.STATUS_MESSAGE, presence.getError().toString());
		} else if(presence.getStatus()!=null){
			values.put(RosterConstants.STATUS_MESSAGE, presence.getStatus());
		}
		values.put(RosterConstants.GROUP, getGroup(entry.getGroups()));
		VCard vCard = new VCard();
		//Log.i(TAG, "B4 try catch. user = "+entry.getUser());

		try {
			vCard.load(mXMPPConnection, entry.getUser());
			byte[] avatar = vCard.getAvatar();
			//Log.i(TAG, "Avatar = "+avatar);
			if(avatar!=null){
				values.put(RosterConstants.AVATAR, avatar);
			}
		} catch (XMPPException e) {
			//e.printStackTrace();
		}
		Log.i(TAG, entry.getUser() +" presence values = "+presence.getStatus() + " curr status = "+currentStatus+" Presence mode= " + presence.getMode());
		return values;
	}

	private void deleteRosterEntryFromDB(final String jabberID) {
		int count = mContentResolver.delete(RosterProvider.CONTENT_URI,
				RosterConstants.JID + " = ?", new String[] { jabberID });
		Log.d(TAG, "deleteRosterEntryFromDB: Deleted " + count + " entries");
	}

	private void updateRosterEntryInDB(final RosterEntry entry) {
		upsertRoster(getContentValuesForRosterEntry(entry), entry.getUser());
	}

	private void upsertRoster(final ContentValues values, String jid) {
		if (mContentResolver.update(RosterProvider.CONTENT_URI, values,
				RosterConstants.JID + " = ?", new String[] { jid }) == 0) {
			mContentResolver.insert(RosterProvider.CONTENT_URI, values);
		}
	}

	private String getGroup(Collection<RosterGroup> groups) {
		for (RosterGroup group : groups) {
			return group.getName();
		}
		return "";
	}

	private String getName(RosterEntry rosterEntry) {
		String name = rosterEntry.getName();
		if (name != null && name.length() > 0) {
			return name;
		}
		name = StringUtils.parseName(rosterEntry.getUser());
		if (name.length() > 0) {
			return name;
		}
		return rosterEntry.getUser();
	}

	private StatusMode getStatus(Presence presence) {
		if (presence.getType() == Presence.Type.subscribe)
			return StatusMode.subscribe;
		if (presence.getType() == Presence.Type.available) {
			if (presence.getMode() != null) {
				return StatusMode.valueOf(presence.getMode().name());
			}
			return StatusMode.available;
		}
		return StatusMode.away;
	}

	private int getStatusInt(final Presence presence) {
		return getStatus(presence).ordinal();
	}

	@Override
	public String getLastError() {
		return mLastError;
	}

	//	public void setAvatar() {
	//		try{
	//			mConfig.loadPrefs();
	//			if(!EmotApplication.getPrefs().getBoolean(PreferenceConstants.AVATAR_UPDATED, false)){
	//				Bitmap bmp = UpdateProfileScreen.getAvatar();
	//				//new UpdateAvatarTask(bmp).execute();
	//
	//				//Directly
	//				VCard vCard = new VCard();
	//
	//				bmp = Bitmap.createScaledBitmap(bmp, 120, 120, false);
	//				try {
	//					ByteArrayOutputStream stream = new ByteArrayOutputStream();
	//					bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
	//					Log.i(TAG, "size of bitmap = "+ImageHelper.sizeOf(bmp));
	//					byte[] bytes = stream.toByteArray();
	//					String encodedImage = StringUtils.encodeBase64(bytes);
	//					vCard.setAvatar(bytes);
	//					//vCard.setEncodedImage(encodedImage);
	//					//					vCard.setField("PHOTO", 
	//					//							"<TYPE>image/jpeg</TYPE><BINVAL>"
	//					//									+ encodedImage + 
	//					//									"</BINVAL>", 
	//					//									true);
	//					vCard.save(mXMPPConnection);
	//					//EmotApplication.setValue(PreferenceConstants.USER_AVATAR, encodedImage);
	//				}  catch (XMPPException e) {
	//					Log.i(TAG, "XMPP EXCEPTION  ----------- ");
	//					//e.printStackTrace();
	//				}	catch(Exception e){
	//					Log.i(TAG, " EXCEPTION  ------ ");
	//					//e.printStackTrace();
	//				}finally{
	//					Editor c = EmotApplication.getPrefs().edit();
	//					c.putBoolean(PreferenceConstants.AVATAR_UPDATED, true);
	//					c.commit();
	//					Log.i(TAG, "Setting preference value ...");
	//				}
	//			}else{
	//				Log.i(TAG, "Avatar already updated");
	//			}
	//		}catch(Exception e){
	//			//e.printStackTrace();
	//		}
	//	}

	public void setAvatar() {
		Bitmap bitmap = UpdateProfileScreen.getAvatar();
		Log.i(TAG, "BMP: "+bitmap);
		//new UpdateAvatarTask(bitmap).execute();


		new Thread(new Runnable() {

			@Override
			public void run() {
				try{
					mConfig.loadPrefs();
					if(!EmotApplication.getPrefs().getBoolean(PreferenceConstants.AVATAR_UPDATED, false)){
						Bitmap bmp = UpdateProfileScreen.getAvatar();
						//new UpdateAvatarTask(bmp).execute();

						//Directly
						VCard vCard = new VCard();

						bmp = Bitmap.createScaledBitmap(bmp, 120, 120, false);
						try {
							ByteArrayOutputStream stream = new ByteArrayOutputStream();
							bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
							Log.i(TAG, "size of bitmap = "+ImageHelper.sizeOf(bmp));
							byte[] bytes = stream.toByteArray();
							String encodedImage = StringUtils.encodeBase64(bytes);
							vCard.setAvatar(bytes);
							//vCard.setEncodedImage(encodedImage);
							//					vCard.setField("PHOTO", 
							//							"<TYPE>image/jpeg</TYPE><BINVAL>"
							//									+ encodedImage + 
							//									"</BINVAL>", 
							//									true);
							vCard.save(mXMPPConnection);
							//EmotApplication.setValue(PreferenceConstants.USER_AVATAR, encodedImage);
						}  catch (XMPPException e) {
							Log.i(TAG, "XMPP EXCEPTION  ----------- ");
							//e.printStackTrace();
						}	catch(Exception e){
							Log.i(TAG, " EXCEPTION  ------ ");
							//e.printStackTrace();
						}finally{
							Editor c = EmotApplication.getPrefs().edit();
							c.putBoolean(PreferenceConstants.AVATAR_UPDATED, true);
							c.commit();
							Log.i(TAG, "Setting preference value ...");
						}
					}else{
						Log.i(TAG, "Avatar already updated");
					}
				}catch(Exception e){
					//e.printStackTrace();
				}
			}
		}).start();

	}

	public class UpdateAvatarTask extends AsyncTask<Void, Void, Boolean>{

		private Bitmap bmp;

		public UpdateAvatarTask(Bitmap bmp){
			this.bmp = bmp;
		}



		@Override
		protected Boolean doInBackground(Void... params) {
			//SmackAndroid.init(EmotApplication.getAppContext());
			//ProviderManager.getInstance().addIQProvider("vCard","vcard-temp", new VCardProvider());
			//EmotApplication.configure(ProviderManager.getInstance());
			new Thread(new Runnable() {

				@Override
				public void run() {

				}
			}).start();
			VCard vCard = new VCard();
			bmp = Bitmap.createScaledBitmap(bmp, 120, 120, false);
			try {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
				Log.i(TAG, "size of bitmap = "+ImageHelper.sizeOf(bmp));
				byte[] bytes = stream.toByteArray();
				String encodedImage = StringUtils.encodeBase64(bytes);
				vCard.setAvatar(bytes);
				//vCard.setEncodedImage(encodedImage);
				//				vCard.setField("PHOTO", 
				//						"<TYPE>image/jpeg</TYPE><BINVAL>"
				//								+ encodedImage + 
				//								"</BINVAL>", 
				//								true);
				vCard.save(mXMPPConnection);
				//EmotApplication.setValue(PreferenceConstants.USER_AVATAR, encodedImage);
			}  catch (XMPPException e) {
				Log.i(TAG, "XMPP EXCEPTION  ----------- ");
				//e.printStackTrace();
				return false;
			}	catch(Exception e){
				Log.i(TAG, " EXCEPTION  ------ ");
				//e.printStackTrace();
				return false;
			}finally{
				Editor c = EmotApplication.getPrefs().edit();
				c.putBoolean(PreferenceConstants.AVATAR_UPDATED, true);
				c.commit();
				Log.i(TAG, "Setting preference value ...");
			}


			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if(result){
				//Handle Result
			}else{
				//Handle error
			}
		}

	}

	@Override
	public void sendChatState(String user, String state) {
		Log.i(TAG, "Sending Chat state");
		final Message newMessage = new Message(user, Message.Type.normal);
		newMessage.addExtension(new ChatStateExtension(ChatState.valueOf(state)));
		if (isAuthenticated()) {
			mXMPPConnection.sendPacket(newMessage);
		}
	}
	private String mGrpMsgIDfrmSender;
	private Map<String, Boolean> mJoined = new HashMap<String, Boolean>();; 
	public void sendGroupMessage(String message, String toJID) {
		Log.i(TAG, "Sending group message " +message + "jid is " +toJID);

		try {

			Log.i(TAG, "Sending group message " +message + "jid is " +toJID);
			final Message newMessage = new Message(toJID, Message.Type.groupchat);
			newMessage.setFrom(mConfig.userName+"@emot-net");
			newMessage.setBody(message);
			grpSubject = EmotApplication.getValue(toJID, "default");
			newMessage.addExtension(new DeliveryReceiptRequest());
			if (isAuthenticated()) {
				Log.i(TAG, "mXMPPConnection is " +mXMPPConnection);
				mGroupChat = new MultiUserChat(mXMPPConnection, toJID);
				EmotApplication.setLongValue("historySince", System.currentTimeMillis());
				Log.i(TAG, "grpsubject in sending is " +grpSubject +"mSmackable is " +this);
				if(mJoined.get(toJID)){
					addChatMessageToDB(ChatConstants.OUTGOING, mGroupChat.getRoom(), grpSubject,message, ChatConstants.DS_SENT_OR_READ,
							System.currentTimeMillis(), newMessage.getPacketID(), GROUPCHATTYPE, mConfig.userName+"@conference.emot-net" );
					Log.i(TAG, "sending grp name " + mGroupChat.getRoom());
					newMessage.setProperty("roomName", mGroupChat.getRoom());
					mXMPPConnection.sendPacket(newMessage);
				}else{
					Log.i(TAG, "not joined in group " +toJID);
					addChatMessageToDB(ChatConstants.OUTGOING, toJID, grpSubject,message, ChatConstants.DS_NEW,
							System.currentTimeMillis(), newMessage.getPacketID(),GROUPCHATTYPE, mConfig.userName+"@conference.emot-net");
				}


			} else {
				// send offline -> store to DB
				addChatMessageToDB(ChatConstants.OUTGOING, toJID, grpSubject,message, ChatConstants.DS_NEW,
						System.currentTimeMillis(), newMessage.getPacketID(),GROUPCHATTYPE, mConfig.userName+"@conference.emot-net");
			}
		} catch (Exception e) {
			Log.i(TAG, "Error sending message on group chat");
			//e.printStackTrace();
		}finally{

			Log.i(TAG, "Sending message");

		}
		//final Message newMessage = new Message(toJID, Message.Type.chat);
		//newMessage.setBody(message);
		//newMessage.addExtension(new DeliveryReceiptRequest());


	}

	@Override
	public void createGroup(String grpName) {
		// TODO Auto-generated method stub

	}

	private Date getDate(long milliSeconds) {
		SimpleDateFormat dateFormater = new SimpleDateFormat(
				"yy-MM-dd HH:mm:ss");
		Date date = new Date(milliSeconds);
		return date;
	}

	ScheduledExecutorService scheduledExecutorService; 
	ScheduledExecutorService scheduledConnectionService = Executors.newScheduledThreadPool(1);
	private static boolean sStopService = false;

	public void joinGroup(final String grpName, long pdate) {

		final DiscussionHistory dh = new DiscussionHistory();
		Date date = getDate(pdate);
		dh.setSince(date);
		final long timeout = 4000;


		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if(mXMPPConnection != null && mXMPPConnection.isAuthenticated()){
						mGroupChat = new MultiUserChat(mXMPPConnection, grpName);
						mGroupChat.join(mConfig.userName + "@conference.emot-net", "", dh, timeout);
						mJoined.put(grpName, true);
						RoomInfo info = MultiUserChat.getRoomInfo(mXMPPConnection, grpName);
						Log.i(TAG, "Occupants in the group are " +mGroupChat.getMembers().iterator().next().getJid());
						Log.i(TAG, "Group subject is " +info.getSubject());
					}else{
						try {
							//doConnect(false);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
				}

			}
		}).start();

	}

	@Override
	public void leaveGroup(String grpName) {
		mGroupChat.leave();

	}

	@Override
	public String getGroupSubject() {
		// TODO Auto-generated method stub
		return "SSS";//EmotApplication.getValue(mGroupChat.getRoom(), "default");
	}

	@Override
	public List<String> getGroupMembers() {
		Log.i(TAG, "Called getGroupMembers()");
		List<String> members = new ArrayList<String>();

		try {
			Iterator<Affiliate> i =	mGroupChat.getMembers().iterator();
			while(i.hasNext()){
				members.add(i.next().getJid());
			}
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		Log.i(TAG, "members found to be returned are " +members);
		members.add(mConfig.userName+"@emot-net");
		return members;
	}

	@Override
	public void changeGroupSubject(String grpID, String grpSubject) {
		try {
			MultiUserChat muc = new MultiUserChat(mXMPPConnection, grpID);
			if(mJoined.get(grpID)){
				muc.changeSubject(grpSubject);
			}else{
				muc.join(mConfig.userName + "@conference.emot-net");
				muc.changeSubject(grpSubject);
				mJoined.put(grpID, true);
			}
			EmotApplication.setValue(grpID, grpSubject);
			sendGroupMessage("Group Subject has been changed to " +grpSubject, grpID);
			//			addChatMessageToDB(ChatConstants.OUTGOING, grpID, grpSubject, "Group Subject has been changed to " +grpSubject, ChatConstants.DS_SENT_OR_READ,
			//					System.currentTimeMillis(), "sss", GROUPCHATTYPE, "");
			Toast.makeText(getService(), "changed group subject to " +grpSubject, Toast.LENGTH_LONG).show();
			mService.sendBroadcast(new Intent().setAction("GROUP_SUBJECT_CHANGED_SUCCESS"));
		} catch (XMPPException e) {
			Toast.makeText(getService(), "Could not change subject because of " +e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			//e.printStackTrace();
		}

	}

	@Override
	public void setLastActivity() {
		try {
			LastActivity activity = LastActivityManager.getLastActivity(mXMPPConnection, "919379475511@emot-net" +"/"+mConfig.ressource);
			activity.setLastActivity(System.currentTimeMillis());
		} catch (XMPPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
