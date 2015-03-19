package com.emot.screens;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import com.emot.androidclient.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.emot.androidclient.IXMPPRosterCallback;
import com.emot.androidclient.IXMPPRosterCallback.Stub;
import com.emot.androidclient.XMPPRosterServiceAdapter;
import com.emot.androidclient.data.EmotConfiguration;
import com.emot.androidclient.service.IXMPPRosterService;
import com.emot.androidclient.service.XMPPService;
import com.emot.androidclient.util.ConnectionState;
import com.emot.androidclient.util.PreferenceConstants;
import com.emot.api.EmotHTTPClient;
import com.emot.common.TaskCompletedRunnable;
import com.emot.constants.ApplicationConstants;
import com.emot.constants.WebServiceConstants;
import com.emot.model.EmotApplication;
import com.emot.persistence.ContactUpdater;
import com.emot.persistence.EmoticonDBHelper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;



public class Registration extends ActionBarActivity {

	private static final String TAG = Registration.class.getSimpleName();
	private EditText mEnterMobile;
	private Button mRetry;
	private Spinner mCountryList;
	private Button mSubmitNumber;
	private EditText mEnterVerificationCode;
	private Button mSendVerificationCode;
	private String mMobileNumber;
	private SecureRandom mRandom = new SecureRandom();
	private String mRN;
	private ProgressDialog pd;
	private View viewMobileBlock;
	private View viewVerificationBlock;
	private AutoCompleteTextView mCountrySelector;
	private static Map<String, String> mCountryCode = new HashMap<String, String>();
	private static Map<String, Integer> mCountryCallingCodeMap = new HashMap<String, Integer>();
	private static PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
	private Intent xmppServiceIntent;
	private ServiceConnection xmppServiceConnection;
	private XMPPRosterServiceAdapter serviceAdapter;
	private Stub rosterCallback;
	private EmotConfiguration mConfig;
	private static String OTP_STARTING_DIGITS = "12345";

	@Override
	protected void onDestroy() {
		Log.i(TAG, "Activity destroy called !!!");
		super.onDestroy();
		if(pd != null){
			pd.dismiss();
		}
		unregisterReceiver(receiver);
		
		unbindXMPPService();
	}
	private static volatile boolean receivedMissedCall;
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Toast.makeText(getApplicationContext(), "received", Toast.LENGTH_SHORT);
			String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

			if(state==null)
				return;

			//phone is ringing
			if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){           
				//get caller's number
				Bundle bundle = intent.getExtras();
				String callerPhoneNumber= bundle.getString("incoming_number").replaceAll("[^\\d.]", "");
				Log.i(TAG, "number = "+callerPhoneNumber.substring(callerPhoneNumber.length()-10, callerPhoneNumber.length()-5));
				if(!callerPhoneNumber.substring(callerPhoneNumber.length()-10, callerPhoneNumber.length()-5).equals(OTP_STARTING_DIGITS)){
					
					return;
				}
				callerPhoneNumber = callerPhoneNumber.substring(callerPhoneNumber.length()-5, callerPhoneNumber.length());
				//Log.i(TAG, "Received call from "+callerPhoneNumber);

				if(viewVerificationBlock!=null && mEnterVerificationCode!=null && viewVerificationBlock.getVisibility()==View.VISIBLE){
					mEnterVerificationCode.setText(callerPhoneNumber);
					mSendVerificationCode.performClick();
					receivedMissedCall = true;
					retryCounter.cancel();
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		Log.i(TAG, getSHA(EmotApplication.getAppContext()));

		if(EmotApplication.getValue(PreferenceConstants.USER_APPID, "")!=""){
			startActivity(new Intent(this, LastChatScreen.class));
			finish();
		}

		String[] locales = Locale.getISOCountries();
		for (String countryCode : locales) {
			Locale obj = new Locale("", countryCode);
			mCountryCode.put(obj.getDisplayCountry(), obj.getCountry());
			mCountryCallingCodeMap.put(obj.getCountry(), phoneUtil.getCountryCodeForRegion(obj.getCountry()));
			//Log.i(TAG, obj.getDisplayCountry()+"  -  "+obj.getCountry()+"  -  "+phoneUtil.getCountryCodeForRegion(obj.getCountry()));
		}
		setContentView(R.layout.layout_register_screen);
		createUICallback();
		initializeUI();


		//Register for call receive
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.PHONE_STATE");
		registerReceiver(receiver, filter);

		new EmoticonDBHelper(EmotApplication.getAppContext()).createDatabase();
		suggestCountryOnEntry();
		setOnClickListeners();
		
		//		new EmoticonDBHelper(EmotApplication.getAppContext()).createDatabase();
		//		EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase().execSQL(EmoticonDBHelper.SQL_CREATE_TABLE_EMOT);
		//		EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase().execSQL("insert into emots select * from emoticons");
	}
	
	

	public String getSHA(Context context) {
		String sign=null;
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
			for (Signature signature : info.signatures) {
				MessageDigest md = MessageDigest.getInstance("SHA");
				md.update(signature.toByteArray());
				sign = Base64 .encodeToString(md.digest(), Base64.DEFAULT);
			}
		} catch (Exception e) {
		} 
		return sign;
	}


	private boolean isNumberValid(final String pNumber, String code){
		boolean isValid = false;
		//PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
		try {
			PhoneNumber numberProto = phoneUtil.parse(pNumber, code);
			isValid = phoneUtil.isValidNumber(numberProto); 
		} catch (NumberParseException e) {
			System.err.println("NumberParseException was thrown: " + e.toString());
		}
		return isValid;
	}
	
	private void reclaimMemory(){
		ActivityManager am = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
		try{
		List<RunningServiceInfo> rsi = am.getRunningServices(20);
		if(rsi != null){
		for(RunningServiceInfo a: rsi){
			ComponentName n = a.service;
			
			if(!n.getPackageName().equals("com.google.android.gms")){
				Log.i(TAG, "Killing process " + n.getPackageName());
			    am.killBackgroundProcesses(n.getPackageName());
			}
		}
		}
		}catch(Exception e){
			
		}
	}
	private CountDownTimer retryCounter;
	class  RetryCounter extends CountDownTimer{
		
	     public RetryCounter(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			// TODO Auto-generated constructor stub
		}

		public void onTick(long millisUntilFinished) {
	    	 
	    	
	         mRetry.setText("Retrying in.. " + millisUntilFinished / 1000);
	    	 
	     }

	     public void onFinish() {
	    	 if(pd != null){
	    		 pd.dismiss();
	    	 }
	    	
	    	 reclaimMemory();
	    	 sendRegistrationRequest();
	    	 mRetry.setEnabled(false);
	    	
	     }
	  }
	private void sendRegistrationRequest(){
		pd.show();
		pd.setCancelable(false);
	    
		mMobileNumber = mEnterMobile.getText().toString().replaceAll("[^\\d.]", "");
		if(isNumberValid(mEnterMobile.getText().toString(), EmotApplication.getValue(PreferenceConstants.COUNTRY_CODE, ""))){
			String url = WebServiceConstants.HTTP + "://"+ 
					WebServiceConstants.SERVER_IP
					+WebServiceConstants.PATH_API+WebServiceConstants.OP_SETCODE
					+WebServiceConstants.GET_QUERY+WebServiceConstants.DEVICE_TYPE+
					"="+mMobileNumber;
			URL wsURL = null;
			Log.d(TAG, "wsurl is  " +wsURL);
			try {
				wsURL = new URL(url);
			} catch (MalformedURLException e) {

				//e.printStackTrace();
			}
			Log.d(TAG, "wsurl is  " +wsURL);
			TaskCompletedRunnable taskCompletedRunnable = new TaskCompletedRunnable() {

				@Override
				public void onTaskComplete(String result) {
					pd.hide();
					viewMobileBlock.setVisibility(View.GONE);
					viewVerificationBlock.setVisibility(View.VISIBLE);
					mRetry.setEnabled(true);
					/////
					retryCounter = new RetryCounter(30000, 1000);
					retryCounter.start();
					Log.i("Registration", "callback called");
					try {
						JSONObject resultJson = new JSONObject(result);

						Log.i("TAG", "callback called");
						String status = resultJson.getString("status");
						if(status.equals("true")){
							Log.i("Registration", "status us true");
							//Toast.makeText(Registration.this, "You have been registered successfully", Toast.LENGTH_LONG).show();
						}else{
							Toast.makeText(Registration.this, "Error in Registration", Toast.LENGTH_LONG).show();
							Log.i(TAG, "registration status is " +status);
							Log.d(TAG, "message from server " + resultJson.getString("message"));

						}
					}
					catch (JSONException e) {

						//e.printStackTrace();
					}catch(Exception e){
						//e.printStackTrace();
					}

				}

				@Override
				public void onTaskError(String error) {
					pd.cancel();
					Toast.makeText(Registration.this, error, Toast.LENGTH_LONG).show();
				}
			};

			EmotHTTPClient registrationHTTPClient = new EmotHTTPClient(wsURL, null, taskCompletedRunnable);
			registrationHTTPClient.execute(new Void[]{});
		}else{
			pd.cancel();
			Toast.makeText(Registration.this, "Mobile Number is invalid", Toast.LENGTH_LONG).show();
		}

	}

	private void setOnClickListeners() {

		mSubmitNumber.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				reclaimMemory();
			    sendRegistrationRequest();
							}

		});

		mSendVerificationCode.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				pd.show();
				pd.setCancelable(false);
				
				
				//Remove this before relase
				String vCode = "";
				if(Log.IS_DEBUG && mEnterVerificationCode.getText().length()==4){
					 vCode = mEnterVerificationCode.getText().toString();	
				}else{
					 vCode = OTP_STARTING_DIGITS + mEnterVerificationCode.getText().toString();	
				}
				
				String url = WebServiceConstants.HTTP + "://"+ 
						WebServiceConstants.SERVER_IP
						+WebServiceConstants.PATH_API+WebServiceConstants.OP_REGISTER;

				URL wsURL = null;
				Log.d(TAG, "wsurl is  " +wsURL);
				try {
					wsURL = new URL(url);
				} catch (MalformedURLException e) {

					//e.printStackTrace();
				}
				ArrayList<NameValuePair> reqContent = new ArrayList<NameValuePair>();
				mRN = RN();

				String s = "1|"+"android|" +vCode+ "|" + mMobileNumber+ "|" + "register|"+ mRN +"|"+ ApplicationConstants.VERIFICATION_SALT;
				String ht = hText(s);
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.REQUEST, "register"));
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.MOBILE, mMobileNumber));
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.APP_VERSION, "1"));
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.CLIENT_OS, "android"));
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.VERIFICATION_CODE, vCode));
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.S, mRN));
				reqContent.add(new BasicNameValuePair(WebServiceConstants.WSRegisterParamConstants.HASH, ht));

				TaskCompletedRunnable taskCompletedRunnable = new TaskCompletedRunnable() {

					@Override
					public void onTaskComplete(String result) {
						try {
							Log.i(TAG, result);
							JSONObject resultJson = new JSONObject(result);
							String status = resultJson.getString("status");
							if(status.equals("success")){
								EmotApplication.setValue(PreferenceConstants.USER_APPID, resultJson.getString("appid"));
								EmotApplication.setValue(PreferenceConstants.JID, mMobileNumber+"@"+WebServiceConstants.CHAT_DOMAIN);
								EmotApplication.setValue(PreferenceConstants.PASSWORD, mRN);
								EmotApplication.setValue(PreferenceConstants.CUSTOM_SERVER, WebServiceConstants.CHAT_SERVER);
								EmotApplication.setValue(PreferenceConstants.RESSOURCE, WebServiceConstants.CHAT_DOMAIN);
								Editor e = EmotApplication.getPrefs().edit();
								e.putBoolean(PreferenceConstants.REQUIRE_SSL, false);
								e.commit();

								//Register and bind
								registerXMPPService();
								bindXMPPService();

							}else{
								throw new Exception("Registration failed");
							}
						} catch (Exception e) {
							//e.printStackTrace();
							pd.cancel();
							Toast.makeText(Registration.this, "Something went wrong. Please try again later.", Toast.LENGTH_LONG).show();
						}
					}

					@Override
					public void onTaskError(String error) {
						pd.cancel();
						Log.i(TAG, "Error "+error);
						Toast.makeText(Registration.this, error, Toast.LENGTH_LONG).show();
					}

				};
				EmotHTTPClient registrationHTTPClient = new EmotHTTPClient(wsURL, reqContent, taskCompletedRunnable);
				registrationHTTPClient.execute(new Void[]{});

			}
		});

		mCountrySelector.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String countryCode = String.valueOf(mCountryCallingCodeMap.get(mCountryCode.get(mCountrySelector.getText().toString())));
				mEnterMobile.setText("+"+countryCode+"-");
				EmotApplication.setValue(PreferenceConstants.COUNTRY_PHONE_CODE, countryCode);
				EmotApplication.setValue(PreferenceConstants.COUNTRY_CODE, mCountryCode.get(mCountrySelector.getText().toString()));
				Log.i(TAG, "ph code = "+mCountryCode.get(mCountrySelector.getText().toString()) + " c code="+countryCode);
				mEnterMobile.requestFocus();
				mEnterMobile.setSelection(mEnterMobile.getText().length());
			}
		});
	}



	private String hText(final String input){

		MessageDigest md;
		String hashtext = "";
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] messageDigest = md.digest(input.getBytes());
			BigInteger number = new BigInteger(1, messageDigest);
			hashtext = number.toString(16);
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}
			//System.out.println("hastext is " +hashtext);
		} catch (NoSuchAlgorithmException e) {

			//e.printStackTrace();
		}

		return hashtext;	
	}

	private String RN(){
		return new BigInteger(130, mRandom).toString(32);
	}

	//	private void addItemsOnCountrySpinner() {
	//
	//
	//		List<String> list = new ArrayList<String>();
	//		for(String key : mCountryCode.keySet()){
	//			list.add(key);
	//		}
	//		Collections.sort(list);
	//
	//		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
	//				android.R.layout.simple_spinner_item, list);
	//		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	//		mCountryList.setAdapter(dataAdapter);
	//	}

	private void suggestCountryOnEntry(){

		List<String> list = new ArrayList<String>();
		for(String key : mCountryCode.keySet()){
			list.add(key);
		}
		Collections.sort(list);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>  
		(this,android.R.layout.select_dialog_item,list);
		mCountrySelector.setThreshold(1);//will start working from first character  
		mCountrySelector.setAdapter(adapter);//setting the adapter data into the AutoCompleteTextView  
	}

	private void createUICallback() {
		rosterCallback = new IXMPPRosterCallback.Stub() {
			@Override
			public void connectionStateChanged(final int connectionstate) throws RemoteException {
				Log.i(TAG, "Connection state changed to "+connectionstate);
				if(connectionstate == ConnectionState.ONLINE.ordinal()){
					Log.i(TAG, " ---- Connected ----");
					serviceAdapter.unregisterUICallback(rosterCallback);
					openContactScreen();
				}
			}
		};
	}

	private void initializeUI() {
		mEnterMobile = (EditText)findViewById(R.id.enterNumber);
		mCountrySelector = (AutoCompleteTextView)findViewById(R.id.countryselector);
		mSubmitNumber = (Button)findViewById(R.id.submitNumber);
		mEnterVerificationCode = (EditText)findViewById(R.id.verificationCode);
		mSendVerificationCode = (Button)findViewById(R.id.sendVerificationCode);
		mRetry = (Button)findViewById(R.id.Retry);
		mRetry.setEnabled(false);
		pd = new ProgressDialog(Registration.this);
		pd.setMessage("Loading");
		viewMobileBlock = findViewById(R.id.viewRegisterMobileBlock);
		viewVerificationBlock = findViewById(R.id.viewRegisterVerificationBlock);
	}

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mConfig = EmotConfiguration.getConfig();
		Log.i(TAG, "USERNAME = "+mConfig.jabberID + " password = "+mConfig.password);
		xmppServiceIntent = new Intent(this, XMPPService.class);
		xmppServiceIntent.setAction("com.emot.services.XMPPSERVICE");

		xmppServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				serviceAdapter = new XMPPRosterServiceAdapter(
						IXMPPRosterService.Stub.asInterface(service));
				serviceAdapter.registerUICallback(rosterCallback);
				Log.i(TAG, "getConnectionState(): " + serviceAdapter.getConnectionState());
				//invalidateOptionsMenu();	// to load the action bar contents on time for access to icons/progressbar
				//ConnectionState cs = serviceAdapter.getConnectionState();

				serviceAdapter.connect();
				//openContactScreen();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
				pd.cancel();
				Toast.makeText(Registration.this, "Sorry we encountered error while registering. Please try again later", Toast.LENGTH_LONG).show();
			}
		};
	}

	private void openContactScreen(){
		Thread waitForConnection = new Thread(new Runnable() {

			@Override
			public void run() {

				ContactUpdater.updateContacts(new TaskCompletedRunnable() {
					@Override
					public void onTaskComplete(String result) {
						//Contacts updated in SQLite. You might want to update UI
						pd.cancel();
						startActivity(new Intent(EmotApplication.getAppContext(), LastChatScreen.class));
						finish();
					}

					@Override
					public void onTaskError(String error) {
						pd.cancel();
					}
				}, serviceAdapter);
			}
		});
		waitForConnection.start();
	}

	private void unbindXMPPService() {
		try {
			unbindService(xmppServiceConnection);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(xmppServiceIntent, xmppServiceConnection, BIND_AUTO_CREATE);
	}




}
