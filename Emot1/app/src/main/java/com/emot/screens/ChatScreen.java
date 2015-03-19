package com.emot.screens;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.ChatState;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.emot.androidclient.chat.IXMPPChatCallback;
import com.emot.androidclient.chat.IXMPPChatCallback.Stub;
import com.emot.androidclient.chat.XMPPChatServiceAdapter;
import com.emot.androidclient.data.ChatProvider;
import com.emot.androidclient.data.ChatProvider.ChatConstants;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.data.RosterProvider.RosterConstants;
import com.emot.androidclient.service.IXMPPChatService;
import com.emot.androidclient.service.XMPPService;
import com.emot.androidclient.util.EmotUtils;
import com.emot.androidclient.util.Log;
import com.emot.androidclient.util.StatusMode;
import com.emot.common.EmotEditText;
import com.emot.common.EmotTextView;
import com.emot.common.ImageHelper;
import com.emot.model.EmotApplication;
import com.emot.screens.R.color;

public class ChatScreen extends EmotActivity {
	
	private ImageView sendButton;
	private EmotEditText chatEntry;
	private TextView userTitle;
	
	private static String TAG = "ChatScreen";
	private ListView chatView;
	private String chatFriend;
	private String chatAlias;
	private Messenger mMessengerService = null;
	private boolean mMessengerServiceConnected = false;
	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	private XMPPChatServiceAdapter mServiceAdapter;
	private String lastSeen;
	private static final int DELAY_NEWMSG = 2000;
	public final static String INTENT_CHAT_FRIEND = "chat_friend";
	private Stub chatCallback;
	private String ONLINE_STATUS = "online";
	private String TYPING_STATUS = "typing ...";
	private String LAST_SEEN = "";
	private final static int INTERVAL = 1000 * 30;
	Handler mHandler = new Handler();
	private View emotSuggestion;
	PendingIntent pIntent; 
	Runnable mHandlerTask = new Runnable(){
	     @Override 
	     public void run() {
	          setFriendStatusDB();
	          mHandler.postDelayed(mHandlerTask, INTERVAL);
	     }
	};
	
	
	long TYPE_PAUSE_DIFF = 2000;
	
	long lastTypingState = 0;
	
	
	@Override
	protected void onStop() {
		
		
		super.onStop();
	}
	private static final String[] PROJECTION_FROM = new String[] {
			ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
			ChatProvider.ChatConstants.DIRECTION,
			ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
			ChatProvider.ChatConstants.DELIVERY_STATUS };

	private static final int[] PROJECTION_TO = new int[] { 
		//R.id.chat_date, R.id.chat_from, R.id.chat_message 
	};

	//
	// private class EmotHistoryTask extends AsyncTask<EmotDBHelper,
	// ChatMessage, Void>{
	//
	//
	//
	// @Override
	// protected void onPostExecute(Void result) {
	//
	// Log.i(TAG, "chats = "+chatList.size());
	// //chatView.setSelection(chatList.size()-1);
	// }
	//
	// @Override
	// protected void onProgressUpdate(ChatMessage... values) {
	// if(values!=null){
	// chatList.add(values[0]);
	// chatlistAdapter.notifyDataSetChanged();
	// }
	// super.onProgressUpdate(values);
	// }
	//
	// @Override
	// protected Void doInBackground(EmotDBHelper... params) {
	// EmotDBHelper emotHistory = params[0];
	// Cursor result = emotHistory.getEmotHistory(chatFriend);
	// boolean valid = result.moveToFirst();
	// String chat = "";
	// String location = "";
	// String datetime = "";
	// boolean emotlocation = false;
	// if(valid && result != null && result.getCount() > 0){
	// for(int i = 0; i < result.getCount(); i++){
	// chat =
	// result.getString(result.getColumnIndex(DBContract.EmotHistoryEntry.EMOTS));
	// location =
	// result.getString(result.getColumnIndex(DBContract.EmotHistoryEntry.EMOT_LOCATION));
	// datetime =
	// result.getString(result.getColumnIndex(DBContract.EmotHistoryEntry.DATETIME));
	// result.moveToNext();
	// Log.i(TAG, "chat from DB is " +chat);
	// ChatMessage newChat = null;
	// if(location.equals("left")){
	// newChat = new ChatMessage(chat,datetime, false);
	// }else if(location.equals("right")){
	// newChat = new ChatMessage(chat,datetime, true);
	// }
	// publishProgress(newChat);
	// }
	//
	// }
	// result.close();
	// return null;
	// }
	//
	// }
	//

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (hasWindowFocus()) unbindXMPPService();
		
	}
	@Override
	protected void onResume() {
		super.onResume();
		needs_to_bind_unbind = true;
		//startRepeatingTask();
	}
	protected boolean needs_to_bind_unbind = false;
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (!needs_to_bind_unbind)
			return;
		if (hasFocus)
			bindXMPPService();
		else
			unbindXMPPService();
		needs_to_bind_unbind = false;
	}
	@Override
	protected void onPause() {
		super.onPause();
		needs_to_bind_unbind = true;
		//stopRepeatingTask();
	}
	
	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, XMPPService.class);
		Uri chatURI = Uri.parse(chatFriend);
		mServiceIntent.setData(chatURI);
		mServiceIntent.setAction("com.emot.services.XMPPSERVICE");

		mServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				mServiceAdapter = new XMPPChatServiceAdapter(
						IXMPPChatService.Stub.asInterface(service), chatFriend);

				mServiceAdapter.clearNotifications(chatFriend);
				mServiceAdapter.registerUICallback(chatCallback);
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}

		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
			mServiceAdapter.unregisterUICallback(chatCallback);
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "Service wasn't bound!");
		}
	}

	private void bindXMPPService() {
		bindService(mServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case android.R.id.home:
			Log.i(TAG, "back pressed");
			this.finish();
			return true;
		case R.id.action_copy_text:
			putText(messageToCopyList);
			for(View currentView : currentlySelectedViewList){
				currentView.setBackgroundColor(0x00000000);
			}
			Iterator<Integer> iter = selectedRow.iterator();
			while (iter.hasNext()) {
			   int i = iter.next();

			  
			        iter.remove();
			}
			messageToCopyList.clear();
			
			return true;
		case R.id.action_settings:
			
			startActivity(new Intent(this, UpdateProfileScreen.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar ab = getSupportActionBar();
		ab.setHomeButtonEnabled(true);
		ab.setDisplayHomeAsUpEnabled(true);
		Intent incomingIntent = getIntent();
		
		
		chatFriend = incomingIntent
				.getStringExtra(ChatScreen.INTENT_CHAT_FRIEND);
		setAliasFromDB();

		Log.i(TAG, "chatFriend is " + chatFriend);
		if (chatFriend == null) {
			Toast.makeText(EmotApplication.getAppContext(),
					"Incorrect username", Toast.LENGTH_LONG).show();
			finish();
		}

		setContentView(R.layout.activity_chat_screen);
		chatView = (ListView) findViewById(R.id.chatView);
		sendButton = (ImageView) findViewById(R.id.dove_send);
		userTitle = (TextView) findViewById(R.id.username);
		emotSuggestion = findViewById(R.id.viewSuggestBox);
		

		chatEntry = (EmotEditText) findViewById(R.id.editTextStatus);
		chatEntry.setEmotSuggestBox(emotSuggestion);
		chatEntry.setContext(ChatScreen.this);
		userTitle.setText(chatFriend);

		ab.setTitle(chatAlias);
		ab.setSubtitle(lastSeen);
		/*
		 * if(incomingIntent != null){
		 * 
		 * userName = incomingIntent.getStringExtra("USERNAME");
		 * userTitle.setText(userName); }
		 */
		// handler = new Handler();
		// emotHistoryDB = EmotDBHelper.getInstance(ChatScreen.this);
		// EmotHistoryTask eht = new EmotHistoryTask();
		// eht.execute(new EmotDBHelper[]{emotHistoryDB});
		// chatList.add("Howdy");

		sendButton.setEnabled(true);
		setOnclickListeners();
		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String sendText = chatEntry.getText().toString();
				if(sendText.trim().equals("")){
					return;	
				}
				sendMessage(sendText);
			}
		});
		
		registerXMPPService();

		// chatView.setAdapter(chatlistAdapter);
		setChatWindowAdapter();

		
		chatCallback = new IXMPPChatCallback.Stub() {
		
			@Override
			public void chatStateChanged(int state, String from) throws RemoteException {
				Log.i(TAG, "new chat state = "+state + " from = "+from + " composing val = "+ChatState.composing.ordinal()+ " gone val = "+ChatState.gone.ordinal());
				if(state == ChatState.composing.ordinal() && chatFriend.equals(from)){
					lastSeen = TYPING_STATUS;
				}else if(state == ChatState.gone.ordinal() && chatFriend.equals(from)){
					lastSeen = LAST_SEEN + " " + EmotUtils.getTimeSimple();
				}else{
					lastSeen = ONLINE_STATUS;
				}
				runOnUiThread(new Runnable(){
			        @Override
			        public void run(){
			        	getSupportActionBar().setSubtitle(lastSeen);
			        }
			    });
			}
		};
		
		chatEntry.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
			    if(hasFocus){
			    	//mServiceAdapter.sendChatState(chatFriend, ChatState.composing.toString());
			    }else {
			    	//mServiceAdapter.sendChatState(chatFriend, ChatState.paused.toString());
			    }
			}
		});
		
		chatEntry.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				Log.i(TAG, "Sys time: "+System.currentTimeMillis() + " last time: "+lastTypingState);
				if(System.currentTimeMillis() - lastTypingState > TYPE_PAUSE_DIFF){
					lastTypingState = System.currentTimeMillis();
					mServiceAdapter.sendChatState(chatFriend, ChatState.composing.toString());
					new Handler().postDelayed(
							new Runnable() {

								@Override
								public void run() {
									mServiceAdapter.sendChatState(chatFriend, ChatState.paused.toString());
								}
							}, TYPE_PAUSE_DIFF);

				}
				Log.i(TAG, "text changed after");
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				Log.i(TAG, "text changed before");
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
				Log.i(TAG, "text changed");
			}
		});
		
		// setChatWindowAdapter();

		// new
		// EmoticonDBHelper(EmotApplication.getAppContext()).createDatabase();

		// EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase().execSQL(EmoticonDBHelper.SQL_CREATE_TABLE_EMOT);
		// EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getWritableDatabase().execSQL("INSERT INTO emots select * from emoticons");

		// ContentValues cvs = new ContentValues();
		// String imgHash = EmotApplication.randomId();
		// Log.i(TAG, "Image hash  = "+ imgHash);
		// cvs.put(DBContract.EmotsDBEntry.EMOT_HASH, imgHash);
		// cvs.put(DBContract.EmotsDBEntry.TAGS, "asin yes no what sad");
		// cvs.put(DBContract.EmotsDBEntry.EMOT_IMG,
		// ImageHelper.getByteArray(BitmapFactory.decodeResource(EmotApplication.getAppContext().getResources(),R.drawable.mad)));
		// EmoticonDBHelper.getInstance(ChatScreen.this).getWritableDatabase().insertWithOnConflict("emoticons",
		// null, cvs, SQLiteDatabase.CONFLICT_REPLACE);
		//
		// cvs = new ContentValues();
		// imgHash = EmotApplication.randomId();
		// Log.i(TAG, "Image hash  = "+ imgHash);
		// cvs.put(DBContract.EmotsDBEntry.EMOT_HASH, imgHash);
		// cvs.put(DBContract.EmotsDBEntry.TAGS, "hello happy angry");
		// cvs.put(DBContract.EmotsDBEntry.EMOT_IMG,
		// ImageHelper.getByteArray(BitmapFactory.decodeResource(EmotApplication.getAppContext().getResources(),R.drawable.sad)));
		// EmoticonDBHelper.getInstance(ChatScreen.this).getWritableDatabase().insertWithOnConflict("emoticons",
		// null, cvs, SQLiteDatabase.CONFLICT_REPLACE);

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat_screen, menu);
		return true;
	}
	private Set<Integer> selectedRow = new HashSet<Integer>();
	private View currentlySelectedView;
	private List<View> currentlySelectedViewList = new ArrayList<View>();
	private int currPosition;
	private Map<Integer,String> messageToCopyList = new HashMap<Integer,String>();
	private StringBuilder messageToCopy = new StringBuilder();
	
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void putText(final Map<Integer,String> messages){
		StringBuilder sb = new StringBuilder();
		Iterator itr  = messages.entrySet().iterator();
		while(itr.hasNext()){
			Map.Entry pairs = (Map.Entry)itr.next();
			if(sb.length() != 0){
				sb.append("\n" +(String)pairs.getValue());
			}else{
				sb.append((String)pairs.getValue());
			}
			itr.remove();
			
		}
		
	    int sdk = android.os.Build.VERSION.SDK_INT;
	    if(sdk < android.os.Build.VERSION_CODES. HONEYCOMB) {
	        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	        clipboard.setText(sb.toString());
	    } else {
	        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
	        android.content.ClipData clip = ClipData.newPlainText("simple text",sb.toString());
	        clipboard.setPrimaryClip(clip);
	    }
	}
	
	
	private void delText(){
	    int sdk = android.os.Build.VERSION.SDK_INT;
	    if(sdk < android.os.Build.VERSION_CODES. HONEYCOMB) {
	        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
	        clipboard.setText(null);
	    } else {
	        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); 
	        android.content.ClipData clip = ClipData.newPlainText("simple text",null);
	        clipboard.setPrimaryClip(clip);
	    }
	}
	
	private void setOnclickListeners(){
		chatView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Cursor cursor = (Cursor)parent.getItemAtPosition(position);

				currentlySelectedView = view;
				Log.i(TAG, "Current view is " +parent.getChildAt(position));
				String message = "";
				currPosition = position;
				if(!selectedRow.contains(currPosition)){
					selectedRow.add(currPosition);
					message = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
					Toast.makeText(EmotApplication.getAppContext(),
							"Selected. Copy from top right icon.", Toast.LENGTH_LONG).show();
					
						messageToCopyList.put(currPosition, message);
					
//					if(messageToCopy.length() != 0){
//						messageToCopy
//					messageToCopy.append("\n" +message);
//					}else{
//						messageToCopy.append(message);	
//					}
					currentlySelectedView.setSelected(true);
					currentlySelectedViewList.add(currentlySelectedView);
					currentlySelectedView.setBackgroundColor(color.darkgreen);

				}else{
					Log.i(TAG, "Deselecting");
					currentlySelectedView.setBackgroundColor(0x00000000);
					messageToCopyList.remove(currPosition);
					selectedRow.remove(currPosition);
				}

				return false;
			}
		});
	}
	
	void startRepeatingTask()
	{
	    mHandlerTask.run(); 
	}

	void stopRepeatingTask()
	{
	    mHandler.removeCallbacks(mHandlerTask);
	}

	private void sendMessage(String message) {
		chatEntry.setText(null);
		mServiceAdapter.sendMessage(chatFriend, message);
		delText();
		if (!mServiceAdapter.isServiceAuthenticated()) {
			// Show single tick and try later
			// showToastNotification(R.string.toast_stored_offline);
		}
	}

	private void setAliasFromDB() {
		chatAlias = chatFriend.split("@")[0];
		lastSeen = "";
		String selection = RosterProvider.RosterConstants.JID + "='" + chatFriend + "'";
		String[] projection = new String[] {
				RosterProvider.RosterConstants.ALIAS,
				RosterProvider.RosterConstants.LAST_SEEN,
				RosterProvider.RosterConstants.STATUS_MODE,
				RosterProvider.RosterConstants.AVATAR};
		Cursor cursor = EmotApplication
				.getAppContext()
				.getContentResolver()
				.query(RosterProvider.CONTENT_URI, projection, selection, null,
						null);
		Log.i(TAG, "users found length = " + cursor.getCount());
		byte[] avatar = null;
		while (cursor.moveToNext()) {
			int mode = cursor
					.getInt(cursor
							.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE));
			String name = cursor.getString(cursor
					.getColumnIndex(RosterProvider.RosterConstants.ALIAS));
			String last_seen = cursor.getString(cursor
					.getColumnIndex(RosterProvider.RosterConstants.LAST_SEEN));
			Log.i(TAG, "last seen = "+last_seen);
			if (mode == StatusMode.available.ordinal()) {
				lastSeen = ONLINE_STATUS;
			} else if (last_seen != null) {
				lastSeen = LAST_SEEN + " "+ EmotUtils.getTimeSimple(last_seen);
			} else {
				lastSeen = "";
			}
			chatAlias = name;
			Log.i(TAG, "chat alias : " + chatAlias);
			Log.i(TAG, "last seen : " + lastSeen);
			
			//Seting avatar image
			avatar = cursor.getBlob(cursor.getColumnIndex(RosterConstants.AVATAR));
		}
		cursor.close();
		
		Bitmap bitmap;
		if(avatar!=null){
			bitmap = BitmapFactory.decodeByteArray(avatar , 0, avatar.length);
		}else{
			bitmap = BitmapFactory.decodeResource(EmotApplication.getAppContext().getResources(), R.drawable.blank_user_image);
		}
		Resources res = getResources();
		BitmapDrawable bd = new BitmapDrawable(res, ImageHelper.getRoundedCornerBitmap(bitmap, 10));
    	getSupportActionBar().setLogo(bd);
	}
	
	public void setFriendStatusDB(){
		Log.i(TAG, "Setting friend status from DB");
		String selection = RosterProvider.RosterConstants.JID + "='" + chatFriend + "'";
		String[] projection = new String[] {
				RosterProvider.RosterConstants.LAST_SEEN,
				RosterProvider.RosterConstants.STATUS_MODE };
		Cursor cursor = EmotApplication
				.getAppContext()
				.getContentResolver()
				.query(RosterProvider.CONTENT_URI, projection, selection, null,
						null);
		Log.i(TAG, "users found length = " + cursor.getCount());
		while (cursor.moveToNext()) {
			int mode = cursor
					.getInt(cursor
							.getColumnIndex(RosterProvider.RosterConstants.STATUS_MODE));
			String last_seen = cursor.getString(cursor
					.getColumnIndex(RosterProvider.RosterConstants.LAST_SEEN));
			if (mode == StatusMode.available.ordinal()) {
				if(!getSupportActionBar().getSubtitle().toString().equals(TYPING_STATUS)){
					getSupportActionBar().setSubtitle(ONLINE_STATUS);
				}
			} else if (last_seen != null) {
				getSupportActionBar().setSubtitle(LAST_SEEN + " " + EmotUtils.getTimeSimple(last_seen));
			} else {
				getSupportActionBar().setSubtitle("");
			}
			Log.i(TAG, "chat alias : " + chatAlias);
			Log.i(TAG, "last seen : " + lastSeen);
		}
		cursor.close();
	}

	
	private void markAsReadDelayed(final int id, final int delay) {
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(delay);
				} catch (Exception e) {
				}
				markAsRead(id);
			}
		}.start();
	}

	private void markAsRead(int id) {
		Uri rowuri = Uri.parse("content://" +
				"" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME + "/" + id);
		Log.d(TAG, "markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		getContentResolver().update(rowuri, values, null, null);
	}

	private void setChatWindowAdapter() {
		String selection = ChatConstants.JID + "='" + chatFriend + "'";
		//String selection = ChatConstants.JID + "='" + "kkii" + "'";
		Cursor cursor = managedQuery(ChatProvider.CONTENT_URI, PROJECTION_FROM,
				selection, null, null);
		ListAdapter adapter = new ChatScreenAdapter(cursor, PROJECTION_FROM,
				PROJECTION_TO, chatFriend, chatFriend);
		chatView.setAdapter(adapter);
	}

	class ChatScreenAdapter extends SimpleCursorAdapter {
		String mScreenName, mJID;

		public ChatScreenAdapter(Cursor cursor, String[] from, int[] to,
				String JID, String screenName) {
			super(ChatScreen.this, R.layout.chat_row, cursor, from, to);
			mScreenName = screenName;
			mJID = JID;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ChatItemWrapper wrapper = null;
			Cursor cursor = this.getCursor();
			cursor.moveToPosition(position);

			long dateMilliseconds = cursor.getLong(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DATE));

			int _id = cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants._ID));
			String date = getDateString(dateMilliseconds);
			String message = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
			boolean from_me = (cursor.getInt(cursor
					.getColumnIndex(ChatProvider.ChatConstants.DIRECTION)) == ChatConstants.OUTGOING);
			String jid = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.JID));
			int delivery_status = cursor
					.getInt(cursor
							.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.chat_row, null);
				wrapper = new ChatItemWrapper(row);
				row.setTag(wrapper);
			} else {
				wrapper = (ChatItemWrapper) row.getTag();
			}

			if (!from_me && delivery_status == ChatConstants.DS_NEW) {
				markAsReadDelayed(_id, DELAY_NEWMSG);
			}

			String from = jid;
			if (jid.equals(mJID))
				from = mScreenName;
			wrapper.populateFrom(date, from_me, from, message, delivery_status);
			// cursor.close();
			return row;
		}
	}

	private String getDateString(long milliSeconds) {
		SimpleDateFormat dateFormater = new SimpleDateFormat(
				"yy-MM-dd HH:mm:ss");
		Date date = new Date(milliSeconds);
		return dateFormater.format(date);
	}

	public class ChatItemWrapper {
		public EmotTextView mChatTextLeft;
		public View chatBoxLeft;
		public TextView mDateTimeLeft;
		public ImageView mChatTickLeft;

		public EmotTextView mChatTextRight;
		public View chatBoxRight;
		public TextView mDateTimeRight;
		public ImageView mChatTickRight;

		ChatItemWrapper(View base) {
			chatBoxLeft = base.findViewById(R.id.messageContainerLeft);
			mDateTimeLeft = (TextView) base.findViewById(R.id.chatDateLeft);
			mChatTextLeft = (EmotTextView) base
					.findViewById(R.id.chatContentLeft);
			mChatTickLeft = (ImageView) base.findViewById(R.id.chatTickLeft);

			chatBoxRight = base.findViewById(R.id.messageContainerRight);
			mDateTimeRight = (TextView) base.findViewById(R.id.chatDateRight);
			mChatTextRight = (EmotTextView) base
					.findViewById(R.id.chatContentRight);
			mChatTickRight = (ImageView) base.findViewById(R.id.chatTickRight);
		}

		void populateFrom(String date, boolean from_me, String from,
				String message, int delivery_status) {
			// Log.i(TAG, "populateFrom(" + from_me + ", " + from + ", " +
			// message + ")");

			if (from_me) {
				chatBoxRight.setVisibility(View.GONE);
				chatBoxLeft.setVisibility(View.VISIBLE);
				String nd = EmotUtils.getTimeSimple(date);
				mDateTimeLeft.setText(nd);
				mChatTextLeft.setText(message);
				switch (delivery_status) {
				case ChatConstants.DS_NEW:
					mChatTickLeft.setImageDrawable(getResources().getDrawable(
							R.drawable.wait_tick));
					break;
				case ChatConstants.DS_SENT_OR_READ:
					mChatTickLeft.setImageDrawable(getResources().getDrawable(
							R.drawable.single_tick));
					break;
				case ChatConstants.DS_ACKED:
					mChatTickLeft.setImageDrawable(getResources().getDrawable(
							R.drawable.double_tick));
					break;
				case ChatConstants.DS_FAILED:
//					mChatTickLeft.setImageDrawable(getResources().getDrawable(
//							R.drawable.fail_tick));
					//Keeping Single tick for case of failed to fool user
					mChatTickLeft.setImageDrawable(getResources().getDrawable(
							R.drawable.single_tick));
					break;
				}
			} else {
				chatBoxLeft.setVisibility(View.GONE);
				chatBoxRight.setVisibility(View.VISIBLE);
				String nd = EmotUtils.getTimeSimple(date);
				mDateTimeRight.setText(nd);
				mChatTextRight.setText(message);
//				switch (delivery_status) {
//				case ChatConstants.DS_NEW:
//					mChatTickRight.setImageDrawable(getResources().getDrawable(
//							R.drawable.wait_tick));
//					break;
//				case ChatConstants.DS_SENT_OR_READ:
//					mChatTickRight.setImageDrawable(getResources().getDrawable(
//							R.drawable.single_tick));
//					break;
//				case ChatConstants.DS_ACKED:
//					mChatTickRight.setImageDrawable(getResources().getDrawable(
//							R.drawable.double_tick));
//					break;
//				case ChatConstants.DS_FAILED:
//					mChatTickRight.setImageDrawable(getResources().getDrawable(
//							R.drawable.fail_tick));
//					break;
//				}
			}

			// getMessageView().setText(message);
			// getMessageView().setTextSize(TypedValue.COMPLEX_UNIT_SP,
			// chatWindow.mChatFontSize);
			// getDateView().setTextSize(TypedValue.COMPLEX_UNIT_SP,
			// chatWindow.mChatFontSize*2/3);
			// getFromView().setTextSize(TypedValue.COMPLEX_UNIT_SP,
			// chatWindow.mChatFontSize*2/3);
		}
	}

}
