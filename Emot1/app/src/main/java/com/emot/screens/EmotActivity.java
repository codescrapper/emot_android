package com.emot.screens;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.emot.androidclient.data.EmotConfiguration;
import com.emot.constants.ApplicationConstants;
import com.emot.model.EmotApplication;

public  class EmotActivity extends ActionBarActivity{

	protected boolean isGoingToAnotherAppScreen = false;
	private EmotConfiguration mConfig = EmotConfiguration.getConfig();
	@Override
	protected void onStop() {
		mConfig.loadPrefs();
		isGoingToAnotherAppScreen = EmotApplication.getValue(ApplicationConstants.IS_GOING_TO_ANOTHER_APP_SCREEN, false);
		if(!isGoingToAnotherAppScreen){
			Log.i("", "isGoingToAnotherAppScreen is sending broadcast true");
			Intent intent = new Intent();
			intent.putExtra(ApplicationConstants.GOING_AWAY, true);
			intent.setAction(ApplicationConstants.USER_STATUS_CHANGED);
			sendBroadcast(intent);
			}
		super.onStop();
	}
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mConfig.loadPrefs();
		isGoingToAnotherAppScreen = true;
		EmotApplication.setBooleanValue(ApplicationConstants.IS_GOING_TO_ANOTHER_APP_SCREEN, isGoingToAnotherAppScreen);
		Log.i("", "isGoingToAnotherAppScreen is" +isGoingToAnotherAppScreen);
		
		super.onCreate(savedInstanceState);
	}


	@Override
	protected void onPause() {
		mConfig.loadPrefs();
		isGoingToAnotherAppScreen = false;
		EmotApplication.setBooleanValue(ApplicationConstants.IS_GOING_TO_ANOTHER_APP_SCREEN, isGoingToAnotherAppScreen);
		Log.i("", "isGoingToAnotherAppScreen is" +isGoingToAnotherAppScreen);
		super.onPause();
	}
	@Override
	protected void onResume() {
		mConfig.loadPrefs();
		isGoingToAnotherAppScreen = true;
		EmotApplication.setBooleanValue(ApplicationConstants.IS_GOING_TO_ANOTHER_APP_SCREEN, isGoingToAnotherAppScreen);
		Log.i("", "isGoingToAnotherAppScreen is" +isGoingToAnotherAppScreen);
		super.onResume();
	}
	
	

	

	

	

	
	
	
	

}
