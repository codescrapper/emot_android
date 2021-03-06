package com.emot.androidclient.service;

import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import com.emot.androidclient.util.Log;
import android.widget.Toast;

import com.emot.androidclient.data.EmotConfiguration;
import com.emot.androidclient.util.EmotUtils;
import com.emot.androidclient.util.LogConstants;
import com.emot.constants.ApplicationConstants;
import com.emot.constants.WebServiceConstants;
import com.emot.screens.ChatScreen;
import com.emot.screens.GroupChatScreen;
import com.emot.screens.R;

public abstract class GenericService extends Service {

	private static final String TAG = GenericService.class.getSimpleName();
	private static final String APP_NAME = "emot";
	private static final int MAX_TICKER_MSG_LEN = 50;

	private NotificationManager mNotificationMGR;
	private Notification mNotification;
	private Vibrator mVibrator;
	private Intent mNotificationIntent;
	private Intent mGrpNotificationIntent;
	protected WakeLock mWakeLock;
	//private int mNotificationCounter = 0;
	
	private Map<String, Integer> notificationCount = new HashMap<String, Integer>(2);
	private Map<String, Integer> notificationId = new HashMap<String, Integer>(2);
	protected static int SERVICE_NOTIFICATION = 1;
	private int lastNotificationId = 2;

	protected EmotConfiguration mConfig;
	

	@Override
	public void onCreate() {
		
		Log.i(TAG, "called onCreate()");
		super.onCreate();
		mConfig = EmotConfiguration.getConfig();
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mWakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, APP_NAME);
		addNotificationMGR();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "called onDestroy()");
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "called onStartCommand()");
		return START_STICKY;
	}

	private void addNotificationMGR() {
		mNotificationMGR = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mNotificationIntent = new Intent(this, ChatScreen.class);
		mGrpNotificationIntent = new Intent(this, GroupChatScreen.class);
	}

	protected void notifyClient(String fromJid, String fromUserName, String message,
			boolean showNotification, boolean silent_notification, boolean is_error, boolean grpchat, String messageSenderInGrp) {
		if (!showNotification) {
			if (is_error)
				//shortToastNotify(getString(R.string.notification_error) + " " + message);
				// only play sound and return
			try {
				if (!silent_notification && !Uri.EMPTY.equals(mConfig.notifySound))
					RingtoneManager.getRingtone(getApplicationContext(), mConfig.notifySound).play();
			} catch (NullPointerException e) {
				// ignore NPE when ringtone was not found
			}
			return;
		}
		mWakeLock.acquire();

		// Override silence when notification is created initially
		// if there is no open notification for that JID, and we get a "silent"
		// one (i.e. caused by an incoming carbon message), we still ring/vibrate,
		// but only once. As long as the user ignores the notifications, no more
		// sounds are made. When the user opens the chat window, the counter is
		// reset and a new sound can be made.
		if (silent_notification && !notificationCount.containsKey(fromJid)) {
			silent_notification = false;		
		}

		setNotification(fromJid, fromUserName, message, is_error, grpchat, messageSenderInGrp);
		setLEDNotification();
		if (!silent_notification)
			mNotification.sound = mConfig.notifySound;
		
		int notifyId = 0;
		if (notificationId.containsKey(fromJid)) {
			notifyId = notificationId.get(fromJid);
		} else {
			lastNotificationId++;
			notifyId = lastNotificationId;
			notificationId.put(fromJid, Integer.valueOf(notifyId));
		}

		// If vibration is set to "system default", add the vibration flag to the 
		// notification and let the system decide.
		if(!silent_notification && "SYSTEM".equals(mConfig.vibraNotify)) {
			mNotification.defaults |= Notification.DEFAULT_VIBRATE;
		}
		mNotificationMGR.notify(notifyId, mNotification);
		
		// If vibration is forced, vibrate now.
		if(!silent_notification && "ALWAYS".equals(mConfig.vibraNotify)) {
			mVibrator.vibrate(400);
		}
		mWakeLock.release();
	}
	
	private void setNotification(String fromJid, String fromUserId, String message, boolean is_error, boolean grpchat, String msgSenderinGrp) {
		message = EmotUtils.replaceTag(message);
		Log.i(TAG, "New message = "+message);
		int mNotificationCounter = 0;
		if (notificationCount.containsKey(fromJid)) {
			mNotificationCounter = notificationCount.get(fromJid);
		}
		mNotificationCounter++;
		notificationCount.put(fromJid, mNotificationCounter);
		String author;
		if (null == fromUserId || fromUserId.length() == 0) {
			author = fromJid;
		} else {
			author = fromUserId;
		}
		author = author.replaceAll("@"+ WebServiceConstants.CHAT_DOMAIN, "");
		if(grpchat){
			author = msgSenderinGrp + "@" + fromJid;
		}
		
		String title = getString(R.string.notification_message, author);
		String ticker;
		if (is_error) {
			title = getString(R.string.notification_error);
			ticker = title;
			message = author + ": " + message;
		} else
		if (mConfig.ticker) {
			int newline = message.indexOf('\n');
			int limit = 0;
			String messageSummary = message;
			if (newline >= 0)
				limit = newline;
			if (limit > MAX_TICKER_MSG_LEN || message.length() > MAX_TICKER_MSG_LEN)
				limit = MAX_TICKER_MSG_LEN;
			if (limit > 0)
				messageSummary = message.substring(0, limit) + " [...]";
			ticker = title + ":\n" + messageSummary;
		} else
			ticker = getString(R.string.notification_anonymous_message);
		mNotification = new Notification(R.drawable.sb_message, ticker,
				System.currentTimeMillis());
		mNotification.defaults = 0;
		Uri userNameUri = Uri.parse(fromJid);
		if(!grpchat){
		mNotificationIntent.setData(userNameUri);
		Log.i(TAG, "Notificaiton from user = "+fromUserId);
		mNotificationIntent.putExtra(ChatScreen.INTENT_CHAT_FRIEND, fromJid);
		mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		//need to set flag FLAG_UPDATE_CURRENT to get extras transferred
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		mNotification.setLatestEventInfo(this, title, message, pendingIntent);
		if (mNotificationCounter > 1)
			mNotification.number = mNotificationCounter;
		mNotification.flags = Notification.FLAG_AUTO_CANCEL;
		}else{
			mGrpNotificationIntent.setData(userNameUri);
			Log.i(TAG, "Notificaiton from user = "+fromUserId);
			mGrpNotificationIntent.putExtra("grpName", fromJid);
			mGrpNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			
			//need to set flag FLAG_UPDATE_CURRENT to get extras transferred
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					mGrpNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			mNotification.setLatestEventInfo(this, title, message, pendingIntent);	
		}
	}
	
	

	private void setLEDNotification() {
		if (mConfig.isLEDNotify) {
			mNotification.ledARGB = Color.MAGENTA;
			mNotification.ledOnMS = 300;
			mNotification.ledOffMS = 1000;
			mNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
		}
	}

	protected void shortToastNotify(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}
	protected void shortToastNotify(Throwable e) {
		//e.printStackTrace();
		while (e.getCause() != null)
			e = e.getCause();
		shortToastNotify(e.getMessage());
	}

	public void resetNotificationCounter(String userJid) {
		notificationCount.remove(userJid);
	}

	protected void logError(String data) {
		if (LogConstants.LOG_ERROR) {
			Log.e(TAG, data);
		}
	}

	protected void logInfo(String data) {
		if (LogConstants.LOG_INFO) {
			Log.i(TAG, data);
		}
	}

	public void clearNotification(String Jid) {
		int notifyId = 0;
		if (notificationId.containsKey(Jid)) {
			notifyId = notificationId.get(Jid);
			mNotificationMGR.cancel(notifyId);
		}
	}

}
