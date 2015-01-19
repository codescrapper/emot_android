package com.emot.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import com.emot.androidclient.util.Log;

import com.emot.androidclient.XMPPRosterServiceAdapter;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.util.PreferenceConstants;
import com.emot.api.EmotHTTPClient;
import com.emot.common.TaskCompletedRunnable;
import com.emot.constants.WebServiceConstants;
import com.emot.model.EmotApplication;

public class ContactUpdater {

	private static String TAG = ContactUpdater.class.getSimpleName();
	private static HashMap<String, String> contacts;
	private static TaskCompletedRunnable taskCompleteHandler;
	private static XMPPRosterServiceAdapter serviceAdapter;
	
	@SuppressLint("InlinedApi")
	private final static String[] FROM_COLUMN = {
		Build.VERSION.SDK_INT
		>= Build.VERSION_CODES.HONEYCOMB ?
				Contacts.DISPLAY_NAME_PRIMARY :
					Contacts.DISPLAY_NAME
	};


	public static void updateContacts(final TaskCompletedRunnable taskCompleteHandler, XMPPRosterServiceAdapter serviceAdapter){

		ContactUpdater.taskCompleteHandler = taskCompleteHandler;
		ContactUpdater.serviceAdapter = serviceAdapter;
		new GetContacts().execute();
	}


	
	public static class GetContacts extends AsyncTask<Void, Void, JSONArray>{
		
		@Override
		protected JSONArray doInBackground(Void... vals) {
			try{
				contacts = getContacts(EmotApplication.getAppContext().getContentResolver());
				JSONArray numbers = new JSONArray();
				Set<String> numberSet = contacts.keySet();
				for(String number: numberSet){
					numbers.put(number);
				}
				return numbers;
			}catch(Exception e){
				//e.printStackTrace();
				return null;
			}
			
		}
		
		@Override
		protected void onPostExecute(JSONArray numbers) {
			if(numbers!=null && numbers.length()>0){
				Log.i(TAG, "Calling API ... "+numbers.toString());
				URL cUrl;
				try{
					cUrl = new URL(WebServiceConstants.HTTP + "://"+ 
							WebServiceConstants.SERVER_IP
							+WebServiceConstants.PATH_API+WebServiceConstants.OP_GETCONTACT);
					Log.i(TAG, "URL "+cUrl);
				}catch(MalformedURLException e){
					//e.printStackTrace();
					return;
				}
				Log.i(TAG, "Looking for numbers : "+numbers.toString());
				ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("appid", EmotApplication.getValue(PreferenceConstants.USER_APPID, "")));
				params.add(new BasicNameValuePair("number_list", numbers.toString()));
				EmotHTTPClient contactCall = new EmotHTTPClient(cUrl, params , new TaskCompletedRunnable() {

					@Override
					public void onTaskComplete(String result) {
						//Put this in database
						//result = "[{\"hbjh\"}]";
						//Log.i(TAG, "Result "+result.toString());

						SQLiteDatabase db = EmotDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase();
						try {
							AddRoster addRoster = new AddRoster(new JSONArray(result));
							addRoster.execute();
						}catch (JSONException e1) {
							e1.printStackTrace();
						}finally{
							db.close();
						}
						Log.i(TAG, "Db ran ... ");
					}

					@Override
					public void onTaskError(String error) {
						Log.i(TAG, "Error while getting numbers"+error);
					}
				});
				contactCall.execute();
			}else{
				ContactUpdater.taskCompleteHandler.onTaskComplete(null);
			}
		}
		
		public HashMap<String, String> getContacts(ContentResolver cr)
		{
			HashMap<String, String> cntcts = new HashMap<String, String>();
			Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
			while (phones.moveToNext())
			{
				String name=phones.getString(phones.getColumnIndex(FROM_COLUMN[0]));
				String ph = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				if(ph.charAt(0)=='0'){
					ph = ph.substring(1, ph.length());
					ph = EmotApplication.getValue(PreferenceConstants.COUNTRY_PHONE_CODE, "") + ph;
				}else if(ph.charAt(0)=='+'){
					//Country code already present. Do nothing
				}else{
					ph = EmotApplication.getValue(PreferenceConstants.COUNTRY_PHONE_CODE, "") + ph;
				}
				String mobile = ph.replaceAll("[^\\d.]", "");
				String prev_name = cntcts.get(mobile);
				//Log.i(TAG, "mobile = "+mobile + " prev_name = "+prev_name + " name = "+name);
				if(prev_name!=null && !prev_name.trim().equals("")){
					name = prev_name;
				}
				cntcts.put(mobile,  name);
			}
			return cntcts;
		}
	}
	
	public static class AddRoster extends AsyncTask<Void, Void, Void>{

		private JSONArray emotters;
		
		public AddRoster(JSONArray emotters){
			this.emotters = emotters;
		}
		
		@Override
		protected Void doInBackground(Void... params) {

			//Wait till connected
			if(ContactUpdater.serviceAdapter==null || !ContactUpdater.serviceAdapter.isAuthenticated()){
				return null;
			}
//			while (ContactUpdater.serviceAdapter==null || !ContactUpdater.serviceAdapter.isAuthenticated()){
//				Log.i(TAG, "Wait for connection to establish");
//				try {
//					Thread.sleep(5000);
//				} catch (InterruptedException e) {
//					//e.printStackTrace();
//				}
//			}
			
			int len = emotters.length();
			for(int i=0; i<len; i++){
				try {
					
					JSONObject emotter = emotters.getJSONObject(i);
					
					//Database entry
					ContentValues cvs = new ContentValues();
					cvs.put(RosterProvider.RosterConstants.JID, emotter.getString("mobile")+"@"+WebServiceConstants.CHAT_DOMAIN);
					cvs.put(RosterProvider.RosterConstants.ALIAS, contacts.get(emotter.getString("mobile")));
					
					
					if(EmotApplication.getAppContext().getContentResolver().update(RosterProvider.CONTENT_URI, cvs, RosterProvider.RosterConstants.JID+" = '"+emotter.getString("mobile")+"@"+WebServiceConstants.CHAT_DOMAIN+"'", null)==0){
						cvs.put(RosterProvider.RosterConstants.STATUS_MODE, "");
						cvs.put(RosterProvider.RosterConstants.GROUP, "");
						EmotApplication.getAppContext().getContentResolver().insert(RosterProvider.CONTENT_URI, cvs);
						Log.i(TAG, "Putting in DB "+emotter.getString("mobile"));
					}
					
//					Cursor cr = EmotApplication.getAppContext().getContentResolver().query(RosterProvider.CONTENT_URI, new String[] {RosterProvider.RosterConstants.JID} , RosterProvider.RosterConstants.JID+" = '"+emotter.getString("mobile")+"@"+WebServiceConstants.CHAT_DOMAIN+"'", null, null);
//					if(cr.getCount()==0){
//						EmotApplication.getAppContext().getContentResolver().insert(RosterProvider.CONTENT_URI, cvs);
//						//updateProfileBitmap(emotter.getString("profile_image"), emotter.getString("mobile"));
//						Log.i(TAG, "Putting in DB "+emotter.getString("mobile"));
//					}else{
//						Log.i(TAG, "Already in DB "+emotter.getString("mobile"));
//					}
					
					
					Log.i(TAG, "Service adapter value = "+ContactUpdater.serviceAdapter);
					if(ContactUpdater.serviceAdapter!=null && ContactUpdater.serviceAdapter.isAuthenticated()){
						Log.i(TAG, "Adding roster "+emotter.getString("mobile"));
						ContactUpdater.serviceAdapter.addRosterItem(emotter.getString("mobile")+"@"+WebServiceConstants.CHAT_DOMAIN, contacts.get(emotter.getString("mobile")), "Buddies");
						ContactUpdater.serviceAdapter.sendPresenceRequest(emotter.getString("mobile")+"@"+WebServiceConstants.CHAT_DOMAIN, "subscribe");
					}
					//cr.close();
				} catch (JSONException e) {
					//e.printStackTrace();
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			ContactUpdater.taskCompleteHandler.onTaskComplete(null);
		}
		
	}
	
}
