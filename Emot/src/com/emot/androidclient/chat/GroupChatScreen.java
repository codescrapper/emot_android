package com.emot.androidclient.chat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.emot.adapters.GroupChatListArrayAdapter;
import com.emot.androidclient.chat.XMPPChatServiceAdapter;
import com.emot.androidclient.service.IXMPPChatService;
import com.emot.constants.IntentStrings;
import com.emot.emotobjects.ChatMessage;
import com.emot.model.EmotApplication;
import com.emot.persistence.DBContract;
import com.emot.persistence.EmotDBHelper;
import com.emot.services.ChatService;
import com.emot.screens.R;
public class GroupChatScreen extends Activity{
	

	private Chat chat;
	private ImageView sendButton;
	private EditText chatEntry;
	private TextView userTitle;
	private EmotDBHelper emotHistoryDB;
	private static String TAG = "GroupChatScreen";
	private List<String>mUserList;

	private String mGroupName;


	

	private class GroupEmotHistoryTask extends AsyncTask<EmotDBHelper, Void, Cursor>{



		@Override
		protected void onPostExecute(Cursor result) {
			Log.i(TAG, "In GroupEmotHistoryTask post Execute");
			boolean valid  = result.moveToFirst();
			String chat = "";
			String location = "";
			String datetime = "";
			String user = "";
			
			if(valid && result != null && result.getCount() > 0){
				for(int i = 0; i < result.getCount(); i++){
				 chat = result.getString(result.getColumnIndex(DBContract.GroupEmotHistoryEntry.EMOTS));
				 location = result.getString(result.getColumnIndex(DBContract.GroupEmotHistoryEntry.EMOT_LOCATION));
				 datetime = result.getString(result.getColumnIndex(DBContract.GroupEmotHistoryEntry.DATETIME));
				 user = result.getString(result.getColumnIndex(DBContract.GroupEmotHistoryEntry.ENTRY_ID));
				 result.moveToNext();
				Log.i(TAG, "chat from DB is " +chat);
				if(location.equals("left")){
				chatList.add(new ChatMessage(user, chat,datetime, false));
				grpchatlistAdapter.notifyDataSetChanged();
				}else if(location.equals("right")){
					chatList.add(new ChatMessage(user,chat,datetime, true));
					grpchatlistAdapter.notifyDataSetChanged();
				}
				
				
				}
				
				result.close();
			}else{
				//chatList.add("DB unfriendly");
			}

		}

		@Override
		protected Cursor doInBackground(EmotDBHelper... params) {
			EmotDBHelper emotHistory = params[0];
			Cursor emotHistoryCursor = emotHistory.getGroupEmotHistory(mGroupName);
			return emotHistoryCursor;
		}




	}

	@Override
	protected void onResume() {
		
		super.onResume();
		/**/	
	}





	@Override
	protected void onPause() {
		
		super.onPause();
	}

	@Override
	protected void onStop() {
		
		super.onStop();
	
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		if (mMessengerServiceConnected) {

            unbindService(mChatServiceConnection);

            chatList.clear();

            mMessengerServiceConnected = false;

}
	}



	private ListView chatView;
	
	
	@Override
	protected void onStart() {
		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.emot.services.ChatService");
		startService(serviceIntent);
		super.onStart();
		 Intent chatservice = new Intent("com.emot.services.ChatService");
		 bindService(chatservice, mChatServiceConnection, Context.BIND_AUTO_CREATE);
	}



	private Messenger mMessengerService = null;
	private boolean mMessengerServiceConnected = false;
	private GroupChatListArrayAdapter grpchatlistAdapter;
	private XMPPGroupChatServiceAdapter mServiceAdapter;
	ArrayList<ChatMessage> chatList;
	
	
	private ServiceConnection mChatServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mMessengerService = null;
			mMessengerServiceConnected = false;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			

			Log.i(TAG, "called onServiceConnected()");
			mServiceAdapter = new XMPPGroupChatServiceAdapter(
					IXMPPChatService.Stub.asInterface(service),
					mGroupName);
			
			mServiceAdapter.clearNotifications(mGroupName);
			//updateContactStatus();
		
			
		}
	};
	
	
	
	private final Messenger mMessenger = new Messenger(new IncomingHandler());
	class IncomingHandler extends Handler{

		@Override
		public void handleMessage(android.os.Message pMessage) {
		
			super.handleMessage(pMessage);
			Log.i(TAG, "received message from service");
			Bundle b = pMessage.getData();
			String s = b.getString("chat");
			String u = b.getString("user");
			String time = b.getString("time");
			
			chatList.add(new ChatMessage(u,s,time, false));
			grpchatlistAdapter.notifyDataSetChanged();
			
		}
		
		
		
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent incomingIntent = getIntent();
		String userName = "";
		mGroupName = incomingIntent.getStringExtra(IntentStrings.CHAT_FRIEND);
		Log.i(TAG, "chatFriend is " +mGroupName);
//		if (mGroupName==null){
//			Toast.makeText(EmotApplication.getAppContext(), "Incorrect username", Toast.LENGTH_LONG).show();
//			finish();
//		}

		setContentView(R.layout.grp_chat_screen);
		chatView = (ListView)findViewById(R.id.chatView);
		sendButton = (ImageView)findViewById(R.id.dove_send);
		userTitle = (TextView)findViewById(R.id.username);

		chatEntry = (EditText)findViewById(R.id.editTextStatus);
		userTitle.setText(mGroupName);
		/*if(incomingIntent != null){

			userName = incomingIntent.getStringExtra("USERNAME");
			userTitle.setText(userName);
		}*/
		chatList = new ArrayList<ChatMessage>();
		
		emotHistoryDB = EmotDBHelper.getInstance(GroupChatScreen.this);
		GroupEmotHistoryTask eht = new GroupEmotHistoryTask();
		eht.execute(new EmotDBHelper[]{emotHistoryDB});
		//chatList.add("Howdy");
		grpchatlistAdapter = new GroupChatListArrayAdapter(this, chatList);  
		
			sendButton.setEnabled(true);
		
		sendButton.setOnClickListener(new View.OnClickListener() {
			String chat;
			@Override
			public void onClick(View v) {
				try {
					Log.i(TAG, "Send onClick called");
					SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					Date now = new Date();
					String strDate = sdfDate.format(now);
					final String dateTime[] = strDate.split(" ");
					Bundle data = new Bundle();
					
					data.putString("chat_friend", mGroupName);
					data.putString("chat", chatEntry.getText().toString());
					//data.putCharSequence("chat", chatEntry.getText().toString());
					android.os.Message msg = android.os.Message.obtain();
					msg.setData(data);
					msg.replyTo = mMessenger;
					mMessengerService.send(msg);
					chat = chatEntry.getText().toString();
					new Thread(new Runnable() {

						@Override
						public void run() {
							emotHistoryDB.insertGroupChat("me",mGroupName, chat, dateTime[0], dateTime[1], "right");

						}
					}).start(); 
					chatList.add(new ChatMessage("me",chat, dateTime[1],true));
					grpchatlistAdapter.notifyDataSetChanged();
					chatEntry.setText("");
					
				} catch (Exception ex) { 
				}

			}
		});
		chatView.setAdapter(grpchatlistAdapter);


		if(EmotApplication.getConnection() != null){
			ChatManager current_chat  = EmotApplication.getConnection().getChatManager();
			
			runOnUiThread(new Thread(new Runnable() {
				@Override
				public void run() {
					if(chat != null){
						sendButton.setEnabled(true);
					}

				}
			}));
		}
		
		//EmotApplication.startConnection();

		//startActivity(new Intent(EmotApplication.getAppContext(), ContactScreen.class));
		//startActivity(new Intent(EmotApplication.getAppContext(), UpdateProfileScreen.class));



	}
	
	
	

}
