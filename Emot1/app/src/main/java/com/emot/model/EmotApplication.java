package com.emot.model;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.receipts.DeliveryReceipt;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.search.UserSearch;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import com.emot.androidclient.util.Log;

public class EmotApplication extends Application {

	private static final String TAG = EmotApplication.class.getSimpleName();
	private static Context context;
	private static SharedPreferences prefs;
	private static List<String> roomJIDs = new ArrayList<String>();
	public static final String XMPP_IDENTITY_NAME = "emot";
	public static final String XMPP_IDENTITY_TYPE = "phone";

	public void onCreate() {
		super.onCreate();

		EmotApplication.context = getApplicationContext();
		prefs = getAppContext().getSharedPreferences("emot_prefs", Context.MODE_MULTI_PROCESS);
	}
	
	public static void addRooms(final String room){
		roomJIDs.add(room);
	}
	
	public static List<String> getRooms(){
		return roomJIDs;
	}
	
	public static EmotApplication getApp(Context ctx) {
		return (EmotApplication)ctx.getApplicationContext();
	}

	public static Context getAppContext() {
		return EmotApplication.context;
	}
	
	public static SharedPreferences getPrefs(){
		return prefs;
	}

	public static boolean setValue(String k, String v) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(k, v); 
		editor.commit();
		return true;
	}
	
	public static boolean setValue(String k, int v) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(k, v); 
		editor.commit();
		return true;
	}
	
	public static boolean setBooleanValue(String k, boolean v) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(k, v); 
		editor.commit();
		return true;
	}
	
	public static boolean setLongValue(String k, long v) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(k, v); 
		editor.commit();
		return true;
	}
	
	public static long getLongValue(String k , long d){
		
		return prefs.getLong(k, d);
	}

	public static String getValue(String k, String d) {
		return prefs.getString(k, d);
	}
	
	public static int getValue(String emoticonSize, Integer i) {
		return prefs.getInt(emoticonSize, i);
	}
	
	public static boolean getValue(String k, Boolean i) {
		return prefs.getBoolean(k,i);
	}

	public static String getDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = new Date();
		return dateFormat.format(date);
	}

	public static String randomId() {
		return new BigInteger(130, new SecureRandom()).toString(32);
	}
	
}
