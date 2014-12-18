package com.emot.screens;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smackx.pubsub.GetItemsRequest;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.emot.androidclient.chat.XMPPChatServiceAdapter;
import com.emot.androidclient.chat.XMPPGroupChatServiceAdapter;
import com.emot.androidclient.data.ChatProvider;
import com.emot.androidclient.data.EmotConfiguration;
import com.emot.androidclient.data.ChatProvider.ChatConstants;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.service.IXMPPChatService;
import com.emot.androidclient.service.IXMPPGroupChatService;
import com.emot.androidclient.service.XMPPService;
import com.emot.androidclient.util.ConnectionState;
import com.emot.androidclient.util.StatusMode;
import com.emot.common.EmotEditText;
import com.emot.common.EmotTextView;
import com.emot.emotobjects.Contact;
import com.emot.model.EmotApplication;
import com.emot.screens.R.color;

public class GroupChatScreen extends ActionBarActivity {

	private ImageView sendButton;
	private String messageID = "";
	private EmotEditText chatEntry;
	private TextView userTitle;
	private View emotSuggestion;
	private LinearLayout emotSuggestionLayout;
	private static String TAG = "GroupChatScreen";
	private ListView chatView;
	private List<Contact> grpchatmembers;
	private String chatAlias;
	private Intent mServiceIntent;
	private ServiceConnection mServiceConnection;
	private XMPPGroupChatServiceAdapter mServiceAdapter;
	private String lastSeen;
	private long mDate;
	private  String grpName = "grpname";
	private boolean isCreateGrp;
	private static final int DELAY_NEWMSG = 2000;
	public final static String INTENT_GRPCHAT_MEMBERS = "groupmembers";
	public final static String INTENT_GRPCHAT_NAME = "grpName";

	private static final String[] PROJECTION_FROM = new String[] {
		ChatProvider.ChatConstants._ID, ChatProvider.ChatConstants.DATE,
		ChatProvider.ChatConstants.DIRECTION,
		ChatProvider.ChatConstants.PACKET_ID,
		ChatProvider.ChatConstants.JID, ChatProvider.ChatConstants.MESSAGE,
		ChatProvider.ChatConstants.DELIVERY_STATUS ,
		ChatProvider.ChatConstants.MESSAGE_SENDER_IN_GROUP};

	private static final int[] PROJECTION_TO = new int[] { 
		//R.id.chat_date, R.id.chat_from, R.id.chat_message 
	};


	@Override
	protected void onStop() {
		mCursor.close();
		super.onStop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(mCursor == null){
			
		}
		localCache();
		bindXMPPService();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unbindXMPPService();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.group_chat, menu);
		return true;
	}

	
	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mServiceIntent = new Intent(this, XMPPService.class);
		Uri chatURI = Uri.parse(grpName);
		mServiceIntent.setData(chatURI);
		mServiceIntent.putExtra("isforgrpchat", true);
		mServiceIntent.putExtra("sinceDate", mDate);
		mServiceIntent.setAction("com.emot.services.XMPPSERVICE");

		mServiceConnection = new ServiceConnection() {



			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				mServiceAdapter = new XMPPGroupChatServiceAdapter(
						IXMPPGroupChatService.Stub.asInterface(service), grpName);

				mServiceAdapter.clearNotifications(grpName);
				if(isCreateGrp){


					mServiceAdapter.createGroup(grpName, grpchatmembers);

				}
				mServiceAdapter.joinExistingGroup(grpName, isCreateGrp, mDate);
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}

		};
	}

	private void unbindXMPPService() {
		try {
			unbindService(mServiceConnection);
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
		case R.id.leave_group:
			Log.i(TAG, "leaving the group");
			mServiceAdapter.leaveGroup(grpName);
		case R.id.action_copy_text:
			myClip = ClipData.newPlainText("text", messageToCopy.toString());
			myClipboard.setPrimaryClip(myClip);
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
	}
	ClipboardManager myClipboard;
	ClipData myClip;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar ab = getSupportActionBar();
		ab.setHomeButtonEnabled(true);
		ab.setDisplayHomeAsUpEnabled(true);
		Intent incomingIntent = getIntent();
		 myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
		
		grpName = incomingIntent
				.getStringExtra("grpName");
		isCreateGrp = incomingIntent.getBooleanExtra("creategroup?", false);
		grpchatmembers =  incomingIntent.getParcelableArrayListExtra("groupmembers");
		Log.i(TAG, "group chat members are " +grpchatmembers);
		setAliasFromDB();

		Log.i(TAG, "groupName is " + grpName);
		if (grpName == null) {
			Toast.makeText(EmotApplication.getAppContext(),
					"Incorrect username", Toast.LENGTH_LONG).show();
			finish();
		}

		setContentView(R.layout.activity_chat_screen);
		chatView = (ListView) findViewById(R.id.chatView);
		sendButton = (ImageView) findViewById(R.id.dove_send);
		userTitle = (TextView) findViewById(R.id.username);
		emotSuggestion = findViewById(R.id.viewEmotSuggestion);
		emotSuggestionLayout = (LinearLayout) findViewById(R.id.viewEmotSuggestionLayout);

		chatEntry = (EmotEditText) findViewById(R.id.editTextStatus);
		chatEntry.setEmotSuggestionLayout(emotSuggestionLayout);
		userTitle.setText(grpName);
		//chatEntry.addTextChangedListener(groupMessageWatcher);
		ab.setTitle(chatAlias);
		ab.setSubtitle(lastSeen);

		sendButton.setEnabled(true);

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				messageID = sendMessage(chatEntry.getText().toString(), occupantTag);
			}
		});

		// chatView.setAdapter(chatlistAdapter);
		setChatWindowAdapter();

		chatEntry.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				emotSuggestion.setVisibility(View.VISIBLE);
			}
		});
		chatView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				Cursor cursor = (Cursor)parent.getItemAtPosition(position);
				
				currentlySelectedView = view;
				Log.i(TAG, "Current view is " +parent.getChildAt(position));
				String message = "";
				
				if(!selectedRow.contains(position)){
					selectedRow.add(position);
				 message = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
				 Toast.makeText(EmotApplication.getAppContext(),
							"messge selected " + message, Toast.LENGTH_LONG).show();
				messageToCopy.append("\n" +message);
				currentlySelectedView.setSelected(true);
				currentlySelectedView.setBackgroundColor(color.darkgreen);
				
				}else{
					Log.i(TAG, "Deselecting");
					currentlySelectedView.setBackgroundColor(0x00000000);
					selectedRow.remove(position);
				}
				
				return false;
			}
		});
		
		
		registerXMPPService();


	}
	private Set<Integer> selectedRow = new HashSet<Integer>();
	private View currentlySelectedView;
	private StringBuilder messageToCopy = new StringBuilder();

	private TextWatcher groupMessageWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if(s.length() > 0 && s.charAt(start) =='@'){
				occupantTag = "";
				isStartListeningForTag = true;
				return;
			}
			if(s.length() > 0 && isStartListeningForTag && s.charAt(start) == ' ' ){
				isStartListeningForTag = false;	
			}
			if(s.length() > 0 && isStartListeningForTag)
				occupantTag(s.charAt(start));
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {


		}

		@Override
		public void afterTextChanged(Editable s) {


		}
	};

	private boolean isStartListeningForTag;

	/**
	 * @param message
	 * check if group message intended to be sent has any occupant tagged
	 */
	private void isTagPresentInMessage(final String message){

		//find all occurences of '@'


	}

	private String occupantTag = "";

	private void occupantTag(final char s){
		occupantTag = occupantTag.trim();
		occupantTag =  occupantTag + s;

		Log.i(TAG, "ocupant tag is " +occupantTag);

	}

	private String sendMessage(String message, String tag) {
		chatEntry.setText(null);

		if (!mServiceAdapter.isServiceAuthenticated()) {
			// Show single tick and try later
			// showToastNotification(R.string.toast_stored_offline);
		}
		return mServiceAdapter.sendMessage(grpName, message, occupantTag);
	}

	private void setAliasFromDB() {
		chatAlias = grpName.split("@")[0];
		lastSeen = "";
		String selection = RosterProvider.RosterConstants.JID + "='"
				+ grpName + "'";
		;
		String[] projection = new String[] {
				RosterProvider.RosterConstants.ALIAS,
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
			String name = cursor.getString(cursor
					.getColumnIndex(RosterProvider.RosterConstants.ALIAS));
			String last_seen = cursor.getString(cursor
					.getColumnIndex(RosterProvider.RosterConstants.LAST_SEEN));
			if (mode == StatusMode.available.ordinal()) {
				lastSeen = "online";
			} else if (last_seen != null) {
				lastSeen = last_seen;
			} else {
				lastSeen = "away";
			}
			chatAlias = name;
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
		Uri rowuri = Uri.parse("content://" + ChatProvider.AUTHORITY + "/"
				+ ChatProvider.TABLE_NAME + "/" + id);
		Log.d(TAG, "markAsRead: " + rowuri);
		ContentValues values = new ContentValues();
		values.put(ChatConstants.DELIVERY_STATUS, ChatConstants.DS_SENT_OR_READ);
		getContentResolver().update(rowuri, values, null, null);
	}
	Cursor mCursor;
private void localCache(){
	mCursor = managedQuery(ChatProvider.CONTENT_URI, PROJECTION_FROM,
			ChatConstants.JID + "='" + grpName + "'", null, null);
}
	private void setChatWindowAdapter() {
		String selection = ChatConstants.JID + "='" + grpName + "'";
		Cursor cursor = managedQuery(ChatProvider.CONTENT_URI, PROJECTION_FROM,
				selection, null, null);
		ListAdapter adapter = new ChatScreenAdapter(cursor, PROJECTION_FROM,
				PROJECTION_TO, grpName, grpName);
		

		chatView.setAdapter(adapter);
	}

	private List<String> mList = new ArrayList<String>();
	class ChatScreenAdapter extends SimpleCursorAdapter {
		String mScreenName, mJID;

		public ChatScreenAdapter(Cursor cursor, String[] from, int[] to,
				String JID, String screenName) {
			super(GroupChatScreen.this, R.layout.chat_row, cursor, from, to);
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
			mDate = dateMilliseconds;
			String date = getDateString(dateMilliseconds);
			String message = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.MESSAGE));
			mList.add(message);
			boolean from_me =false;
			String msgSender = cursor.getString(cursor.getColumnIndex(ChatProvider.ChatConstants.MESSAGE_SENDER_IN_GROUP));
			Log.i(TAG, "Message Sender in Group is " +msgSender);
			if(msgSender.equals(EmotConfiguration.getConfig().userName + "@conference.emot-net")){
				Log.i(TAG, "Comparing if sender is same as reciever");
				from_me = true;	
			}

			String jid = cursor.getString(cursor
					.getColumnIndex(ChatProvider.ChatConstants.JID));
			int delivery_status = cursor
					.getInt(cursor
							.getColumnIndex(ChatProvider.ChatConstants.DELIVERY_STATUS));

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.grpchat_row, null);
				wrapper = new ChatItemWrapper(row);
				row.setTag(wrapper);
			} else {
				wrapper = (ChatItemWrapper) row.getTag();
			}

			if (delivery_status == ChatConstants.DS_NEW) {
				markAsReadDelayed(_id, DELAY_NEWMSG);
			}

			String from = jid;
			if (jid.equals(mJID))
				from = mScreenName;
			wrapper.populateFrom(date, from_me, msgSender, message, delivery_status);
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

	private Date getDate(long milliSeconds) {
		SimpleDateFormat dateFormater = new SimpleDateFormat(
				"yy-MM-dd HH:mm:ss");
		Date date = new Date(milliSeconds);
		return date;
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

		private TextView mGrpMember;

		ChatItemWrapper(View base) {
			chatBoxLeft = base.findViewById(R.id.messageContainerLeft);
			mDateTimeLeft = (TextView) base.findViewById(R.id.chatDateLeft);
			mChatTextLeft = (EmotTextView) base
					.findViewById(R.id.chatContentLeft);
			mChatTickLeft = (ImageView) base.findViewById(R.id.chatTickLeft);

			chatBoxRight = base.findViewById(R.id.messageContainerRight);
			mGrpMember = (TextView)base.findViewById(R.id.grpMemberName2);
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
				mDateTimeLeft.setText(date);
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
					mChatTickLeft.setImageDrawable(getResources().getDrawable(
							R.drawable.fail_tick));
					break;
				}
			} else {
				chatBoxLeft.setVisibility(View.GONE);
				chatBoxRight.setVisibility(View.VISIBLE);
				mDateTimeRight.setText(date);
				mChatTextRight.setText(message);
				Log.i(TAG, "mGrpmeber is " +mGrpMember);
				mGrpMember.setText(from);
				switch (delivery_status) {
				case ChatConstants.DS_NEW:
					mChatTickRight.setImageDrawable(getResources().getDrawable(
							R.drawable.wait_tick));
					break;
				case ChatConstants.DS_SENT_OR_READ:
					mChatTickRight.setImageDrawable(getResources().getDrawable(
							R.drawable.single_tick));
					break;
				case ChatConstants.DS_ACKED:
					mChatTickRight.setImageDrawable(getResources().getDrawable(
							R.drawable.double_tick));
					break;
				case ChatConstants.DS_FAILED:
					mChatTickRight.setImageDrawable(getResources().getDrawable(
							R.drawable.fail_tick));
					break;
				}
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
