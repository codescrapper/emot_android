package com.emot.screen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.content.res.AssetManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
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
import com.emot.gps.GPSTracker;
import com.emot.model.EmotApplication;
import com.emot.persistence.ContactUpdater;
import com.emot.persistence.EmoticonDBHelper;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import net.grandcentrix.tray.AppPreferences;


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
	private String mVCode;
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
	String callnumber = null;
    private String cipher;
    private boolean booleans = false;
    private String otp_number;

	GPSTracker gps;
	boolean call_state = false;
	PhoneStateListener callStateListener;
	TelephonyManager telephonyManager;
	String imei = "", mcc = "", mnc = "", latitude = "0.0", longitude = "0.0",
			brand_name = "", os = "", model_num = "", gmail_id = "";
    private String app_id;
    private String access_toke;
    private String mobilenumber;

    @Override
	protected void onDestroy() {
		Log.i(TAG, "Activity destroy called !!!");
		super.onDestroy();
		if(pd != null){
			pd.dismiss();
		}
		unregisterReceiver(mSMSReciever);

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

    private BroadcastReceiver mSMSReciever =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){

                Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
                SmsMessage[] msgs = null;
                String msg_from;
                if (bundle != null){
                    //---retrieve the SMS message received---
                    try{
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        msgs = new SmsMessage[pdus.length];
                        for(int i=0; i<msgs.length; i++){
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            msg_from = msgs[i].getOriginatingAddress();
                            if(msg_from.contains("EMOTRG")) {

								Toast.makeText(getApplicationContext(),"Verified Successfully", Toast.LENGTH_LONG).show();
                                String msgBody = msgs[i].getMessageBody();
                                Toast.makeText(getApplicationContext(),msgBody.substring(0,6), Toast.LENGTH_LONG).show();
								if(msgBody.substring(0,6).equals(mVCode)){
									Toast.makeText(getApplicationContext(),"Verified Successfully", Toast.LENGTH_LONG).show();
									mEnterVerificationCode.setText(msgBody.substring(0,6));
									mSendVerificationCode.performClick();

								}
                            }else{
                               // Toast.makeText(getApplicationContext(),"Sender is " +msg_from, Toast.LENGTH_LONG).show();
                            }
                        }
                    }catch(Exception e){
//                            Log.d("Exception caught",e.getMessage());
                    }
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
		setContentView(com.emot.screen.R.layout.layout_register_screen);
		//Need to uncomment
		createUICallback();
		initializeUI();


		//Register for call receive
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.provider.Telephony.SMS_RECEIVED");

		registerReceiver(mSMSReciever, filter);
        TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		new EmoticonDBHelper(EmotApplication.getAppContext()).createDatabase();
		suggestCountryOnEntry();
        Account[] accounts = AccountManager.get(this).getAccountsByType(
                "com.google");
        for (Account account : accounts) {

            gmail_id = account.name;

        }
        gps = new GPSTracker(this);
        imei = mngr.getDeviceId();

        String networkOperator = mngr.getNetworkOperator();

        if (networkOperator != null) {
            mcc = networkOperator.substring(0, 3);
            mnc = networkOperator.substring(3);
        }
        if (EmotApplication.isConnectionAvailable(getApplicationContext())) {
            if (gps.canGetLocation()) {
                Location l = gps.getLocation();

                if (l != null) {
                    latitude = "" + l.getLatitude();
                    longitude = "" + l.getLongitude();
                }
            }
        }
        brand_name = Build.BRAND;
        model_num = Build.MODEL;
        os = android.os.Build.VERSION.RELEASE;
		setOnClickListeners();





	//			new EmoticonDBHelper(EmotApplication.getAppContext()).createDatabase();
		//		EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase().execSQL(EmoticonDBHelper.SQL_CREATE_TABLE_EMOT);
			//	EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase().execSQL("insert into emots select * from emoticons");
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

    private void readFromJSON(){
        try {
            AssetManager am = getResources().getAssets();

            InputStream is = am.open("mobile_verification");
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int ctr;
            try {
                ctr = is.read();
                while (ctr != -1) {
                    byteArrayOutputStream.write(ctr);
                    ctr = is.read();
                }
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {

                String body = "{\n" +
                        "   \"template_vars\": {\n" +
                        "       \"code\": "+ "\""+ RN()+"\"" +"\n" +
                        "   },\n" +
                        "   \"address\": {\n" +
                        "       \"sms\": {\n" +
                        "           \"mobile\": " + "\"" + mMobileNumber + "\"" + "\n" +
                        "       }\n" +
                        "   },\n" +
                        "   \"template_name\": \"OTP\"\n" +
                        "}";

                JSONObject jObject = new JSONObject(body);
                System.out.println("jsonarray from file is main1" +jObject.toString());
                JSONObject jObjectResult = jObject.getJSONObject("address");
                System.out.println("jsonarray from file is main" +jObjectResult.toString());
                JSONObject jsonSMS = jObjectResult.getJSONObject("sms");
                System.out.println("jsonarray from file is sms" +jsonSMS.toString());
                String jsonMobile = jsonSMS.getString("mobile");

                System.out.println("jsonarray from file is mobile" +jsonMobile);


                System.out.println("jsonarray from file is " +jObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }


        } catch (IOException e) {
            System.out.println("jsonarray from file is exception");
            e.printStackTrace();
        }
    }


	private void sendRegistrationRequest() {
        pd.show();
        pd.setCancelable(false);

        mMobileNumber = mEnterMobile.getText().toString().replaceAll("[^\\d.]", "");
        if (isNumberValid(mEnterMobile.getText().toString(), EmotApplication.getValue(PreferenceConstants.COUNTRY_CODE, ""))) {
            String url = WebServiceConstants.HTTP + "://" +
                    WebServiceConstants.SERVER_IP
                    + WebServiceConstants.PATH_API ;/*+ WebServiceConstants.OP_SETCODE
                    + WebServiceConstants.GET_QUERY + WebServiceConstants.DEVICE_TYPE +
                    "=" + mMobileNumber;*/

			mVCode = verificationCode();
            if (EmotApplication.isConnectionAvailable(Registration.this)) {



                String body = "{\n" +
                        "   \"template_vars\": {\n" +
                        "       \"code\": "+ "\""+ mVCode+"\"" +"\n" +
                        "   },\n" +
                        "   \"address\": {\n" +
                        "       \"sms\": {\n" +
                        "           \"mobile\": " + "\"" + mMobileNumber + "\"" + "\n" +
                        "       }\n" +
                        "   },\n" +
                        "   \"template_name\": \"OTP\"\n" +","+
						"   \"name\": \"EMOT_OTP\"\n" +
                        "}";


                try {
                    JSONObject jsonObject1 = new JSONObject(body);










                    URL wsURL = new URL(url);

                    EmotHTTPClient asy = new EmotHTTPClient(wsURL, jsonObject1.toString(),taskCompletedRunnable);
					Log.i("Register", "Send Registration Request");
                    asy.execute();
                }catch (MalformedURLException e) {

                    //e.printStackTrace();
                }catch (JSONException e){
					Log.i("Register", "Send Registration Request JSON Exception");

                }

            } else {
                Toast.makeText(Registration.this, "No Internet Connection",
                        Toast.LENGTH_SHORT).show();

            }

        } else {
            Intent in = new Intent();
            Toast.makeText(getApplicationContext(), ApplicationConstants.THREE, Toast.LENGTH_LONG).show();

        }






		/*	URL wsURL = null;
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
*/
    }


    TaskCompletedRunnable taskCompletedRunnable = new TaskCompletedRunnable() {

        @Override
        public void onTaskComplete(String result) {
            pd.hide();
            viewMobileBlock.setVisibility(View.GONE);
            viewVerificationBlock.setVisibility(View.VISIBLE);
            mRetry.setEnabled(true);
            /////
            retryCounter = new RetryCounter(30000, 1000);
            //retryCounter.start();
            Log.i("Registration", "callback called");
            try {
                JSONObject resultJson = new JSONObject(result);

                Log.i("TAG", "callback called");
                String status = resultJson.getString("status");
                if(status.equals("true")){
                    Log.i("Registration", "status us true");
                    Toast.makeText(Registration.this, "You have been registered successfully", Toast.LENGTH_LONG).show();
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




	private void setOnClickListeners() {

		mSubmitNumber.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reclaimMemory();
                sendRegistrationRequest();
				//pd.hide();
				viewMobileBlock.setVisibility(View.GONE);
				viewVerificationBlock.setVisibility(View.VISIBLE);
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
						WebServiceConstants.CHAT_SERVER+":"+WebServiceConstants.HTTP_PORT
						+WebServiceConstants.REGISTER_PATH_API;

				URL wsURL = null;

				try {
					wsURL = new URL(url);
				} catch (MalformedURLException e) {

					//e.printStackTrace();
				}
				ArrayList<NameValuePair> reqContent = new ArrayList<NameValuePair>();
				mRN = RN();




                String body = "{\n" +
                        "\"mobile\" : "+ "\"" +mMobileNumber+"\""+","+"\n" +
                        "\"password\" : "+ "\""+ mRN+"\"" +"\n" +
                        "}";
                JSONObject jo = null;
                try {
                     jo = new JSONObject(body);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

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
						String status = null;
						try {
							Log.i(TAG, result);
							JSONObject resultJson = new JSONObject(result);
							 status = resultJson.getString("status");

							if(status.equals("true")){
								//EmotApplication.setValue(PreferenceConstants.USER_APPID, resultJson.getString("appid"));
								EmotApplication.setValue(PreferenceConstants.USER_APPID, "12345");
								EmotApplication.setValue(PreferenceConstants.JID, mMobileNumber+"@"+WebServiceConstants.CHAT_DOMAIN);
								EmotApplication.setValue(PreferenceConstants.PASSWORD, mRN);
								EmotApplication.setValue(PreferenceConstants.CUSTOM_SERVER, WebServiceConstants.CHAT_SERVER);
								EmotApplication.setValue(PreferenceConstants.RESSOURCE, WebServiceConstants.CHAT_DOMAIN);
								//need to check
								/*Editor e = EmotApplication.getPrefs().edit();
								e.putBoolean(PreferenceConstants.REQUIRE_SSL, false);
								e.commit();*/
								AppPreferences preferences = EmotApplication.getPrefs();
								preferences.put(PreferenceConstants.REQUIRE_SSL, false);

								//Register and bind
								registerXMPPService();
								bindXMPPService();


							}else{
								throw new Exception("Registration failed");
							}
						}catch(JSONException e){
							Toast.makeText(Registration.this, "JSON Exception "+e.toString() , Toast.LENGTH_LONG).show();
						} catch (Exception e) {
							//e.printStackTrace();
							/*registerXMPPService();
							bindXMPPService();*/
							pd.cancel();
							Toast.makeText(Registration.this, "Something went wrong. Please try again later." +status , Toast.LENGTH_LONG).show();
						}
					}

					@Override
					public void onTaskError(String error) {
						pd.cancel();
						Log.i(TAG, "Error "+error);
						Toast.makeText(Registration.this, error, Toast.LENGTH_LONG).show();
					}

				};
				System.out.println("Registration wsURL is " +wsURL );
				EmotHTTPClient registrationHTTPClient =new EmotHTTPClient(wsURL,jo.toString(), taskCompletedRunnable);
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

    private String verificationCode(){
        Random ran = new Random();
        return Integer.toString(100000 + ran.nextInt(99999));
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
					//pd.cancel();
					Log.i(TAG, " ---- Connected ----");
					serviceAdapter.unregisterUICallback(rosterCallback);
					openContactScreen();
					//openContactScreen1();
				}
			}
		};
	}

	private void initializeUI() {
		mEnterMobile = (EditText)findViewById(com.emot.screen.R.id.enterNumber);
		mCountrySelector = (AutoCompleteTextView)findViewById(com.emot.screen.R.id.countryselector);
		mSubmitNumber = (Button)findViewById(com.emot.screen.R.id.submitNumber);
		mEnterVerificationCode = (EditText)findViewById(com.emot.screen.R.id.verificationCode);
		mSendVerificationCode = (Button)findViewById(com.emot.screen.R.id.sendVerificationCode);
		mRetry = (Button)findViewById(com.emot.screen.R.id.Retry);
		mRetry.setEnabled(false);
		pd = new ProgressDialog(Registration.this);
		pd.setMessage("Loading");
		viewMobileBlock = findViewById(com.emot.screen.R.id.viewRegisterMobileBlock);
		viewVerificationBlock = findViewById(com.emot.screen.R.id.viewRegisterVerificationBlock);
	}

	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mConfig = EmotConfiguration.getConfig();
		Log.i(TAG, "USERNAME = "+mConfig.jabberID + " password = "+mConfig.password);
		xmppServiceIntent = new Intent(Registration.this, XMPPService.class);
		xmppServiceIntent.setAction("com.emot.androidclient.service.XMPPService");

		xmppServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected() " + "service " +service.toString() + "rostercallback" +rosterCallback);
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

	private void openContactScreen1(){
		startActivity(new Intent(EmotApplication.getAppContext(), LastChatScreen.class));
		finish();

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
		Log.i(TAG, "bind XMPPService");
		bindService(xmppServiceIntent, xmppServiceConnection, BIND_AUTO_CREATE);
	}



}
