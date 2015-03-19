package com.emot.screens;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import com.emot.androidclient.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.emot.adapters.CurrentEmotsAdapter;
import com.emot.emotobjects.CurrentEmot;
import com.emot.persistence.EmotHistoryHelper;


public class Emots extends Activity {
	
	private ListView emotsList;
	private EmotHistoryHelper emotHistoryDB;
	private CurrentEmotsAdapter currentEmotsAdapter;
	private List<CurrentEmot> currentEmotsList;
	private class EmotHistoryListTask extends AsyncTask<EmotHistoryHelper, Void, List<CurrentEmot>>{

		

		@Override
		protected void onPostExecute(List<CurrentEmot> result) {
			
			currentEmotsList = new ArrayList<CurrentEmot>();
			currentEmotsList = result;
			currentEmotsAdapter = new CurrentEmotsAdapter(Emots.this, currentEmotsList);
			Log.i("Emots", currentEmotsList.get(0).getUserLastEmot());
			emotsList.setAdapter(currentEmotsAdapter);
			currentEmotsAdapter.notifyDataSetChanged();
			
			
		}

		@Override
		protected List<CurrentEmot> doInBackground(EmotHistoryHelper... params) {
			EmotHistoryHelper emotHistory = params[0];
			List<CurrentEmot> CEmotsList = emotHistory.getCurrentEmots();
			return CEmotsList;
		}

		
		
		
	}
	
	
	
	private void setOnClickListeners(){
		
		emotsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				TextView user = (TextView)view.findViewById(R.id.username);
				String userName = user.getText().toString();
				Intent intent = new Intent(Emots.this, ChatScreen.class);
				intent.putExtra("chat_friend", userName);
				startActivity(intent);
				
				
			}
		});
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_emots);
		initUI();
		setOnClickListeners();
		
		
		
		
		 emotHistoryDB = new EmotHistoryHelper(Emots.this);
		 EmotHistoryListTask emotHistoryTask = new EmotHistoryListTask();
		 emotHistoryTask.execute(new EmotHistoryHelper[]{emotHistoryDB});
		
	}
	
	//Bring XML UI elements to java control
	private void initUI() {
		emotsList = (ListView)findViewById(R.id.emotListView);
		
	}

	@Override
	protected void onResume() {
		
		super.onResume();
	}
	
	

}
