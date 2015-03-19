package com.emot.screens;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import com.emot.androidclient.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.emot.adapters.ContactArrayAdapter;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.data.RosterProvider.RosterConstants;
import com.emot.emotobjects.Contact;
import com.emot.model.EmotApplication;




public class GroupInfo extends Activity{
	
	private TextView currentSubject;
	private ArrayList<String> currentMembers;
	private String subject;
	
	private ContactArrayAdapter contactAdapter;
	private ListView currentList;
	private ShowContacts showContactsThread;
	private ImageView changeGroup;
	private ProgressDialog mProgress;
	private EditText enterNewGroupSubject;
	private String currentGrpID;
	private Button mChangeSubject;
	private BroadcastReceiver mGroupSubjectChanged = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			GroupInfo.this.finish();
			
		}
	};
	
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mGroupSubjectChanged);
		super.onDestroy();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		registerReceiver(mGroupSubjectChanged, new IntentFilter("GROUP_SUBJECT_CHANGED_SUCCESS"));
		setContentView(R.layout.group_info);
		changeGroup = (ImageView)findViewById(R.id.changeSubject);
		enterNewGroupSubject = (EditText)findViewById(R.id.EnterSubject);
		currentSubject = (TextView)findViewById(R.id.currentGrpSubject);
		currentList = (ListView)findViewById(R.id.listviewMembers);
		subject = getIntent().getStringExtra("currentSubject");
		mChangeSubject = (Button)findViewById(R.id.changeSubjectButton);
		currentGrpID = getIntent().getStringExtra("grpID");
		currentMembers = getIntent().getStringArrayListExtra("currentMembers");
		Log.i(TAG, "currentMembers are " +currentMembers);
		contacts = new ArrayList<Contact>();
		contactAdapter = new ContactArrayAdapter(EmotApplication.getAppContext(), R.layout.contact_row, contacts);
		currentList.setAdapter(contactAdapter);
		currentSubject.setText(subject);
		
		
		setOnclickListeners();
		
		
		
	}
	
	
	private void setOnclickListeners(){
		changeGroup.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				enterNewGroupSubject.setVisibility(View.VISIBLE);
				
			}
		});
		mChangeSubject.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction("GROUP_SUBJECT_CHANGED");
				Log.i(TAG, "grpID in groupInfo is " +currentGrpID);
				
				intent.putExtra("newGrpSubject", enterNewGroupSubject.getText().toString());
				intent.putExtra("grpID", currentGrpID);
				sendBroadcast(intent);
				
			}
		});
	}
	
	
	public void refreshContacts(){
		Log.i(TAG, "Refreshing contacts !!!!");
		showContactsThread = new ShowContacts();
		showContactsThread.execute();
	}

	@Override
	protected void onResume() {
		mProgress = new ProgressDialog(this);
		
		refreshContacts();
		super.onResume();
	}
	
	private static final String TAG = "GroupInfo";
	final static private String[] CONTACT_PROJECTION = new String[] {
		RosterConstants.ALIAS, RosterConstants.STATUS_MESSAGE,
		RosterConstants.AVATAR, RosterConstants.JID};
	private ArrayList<Contact> contacts;
	public class ShowContacts extends AsyncTask<Void, Contact, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {
			try{
				Cursor cr = getContentResolver().query(RosterProvider.CONTENT_URI, CONTACT_PROJECTION, null, null, null);
				
				Log.i(TAG, "contacts found  = "+cr.getCount());
				while (cr.moveToNext()) {
					if(currentMembers.contains((String)cr.getString(cr.getColumnIndex(RosterConstants.JID)))){
				    Contact contact = new Contact(cr.getString(cr.getColumnIndex(RosterConstants.ALIAS)), cr.getString(cr.getColumnIndex(RosterConstants.JID)));
				    contact.setStatus(cr.getString(cr.getColumnIndex(RosterConstants.STATUS_MESSAGE)));
				    contact.setAvatar(cr.getBlob(cr.getColumnIndex(RosterConstants.AVATAR)));
				    publishProgress(contact);
					}
				}
				cr.close();
				Log.i(TAG, "time 2");
				return true;
			}catch(Exception e){
				//e.printStackTrace();
				return false;
			}

		}

		protected void onProgressUpdate(Contact... contact){
			
			contacts.add(contact[0]);
			contactAdapter.notifyDataSetChanged();
			Log.i(TAG, "Adding contact ...");
			return;
		}

		protected void onPostExecute(Boolean resp) {
			if(!resp){
				Toast.makeText(EmotApplication.getAppContext(), "Sorry encountered some error while fetching contacts. Please try again later.", Toast.LENGTH_LONG).show();
			}else{
				if(mProgress != null){
					mProgress.dismiss();
				}
			}
		}
	}

}
