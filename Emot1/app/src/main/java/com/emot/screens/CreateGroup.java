package com.emot.screens;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import com.emot.androidclient.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.emot.adapters.SelectContactArrayAdapter;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.data.RosterProvider.RosterConstants;
import com.emot.emotobjects.Contact;
import com.emot.model.EmotApplication;

public class CreateGroup extends EmotActivity {
	
	private ListView mContactList;
	private ArrayList<Contact> contacts;
	private SelectContactArrayAdapter contactAdapter;
	private ShowContacts showContactsThread;
	private Button createGroup;
	
	private EditText grpName;
	private SearchView participantSelector;
	private static final String TAG = "CreateGroup";
	final static private String[] CONTACT_PROJECTION = new String[] {
		RosterConstants.ALIAS, RosterConstants.STATUS_MESSAGE,
		RosterConstants.AVATAR, RosterConstants.JID};
	private BroadcastReceiver mGroupCreatedReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals("GROUP_CREATED_ERROR")){
				Toast.makeText(getApplicationContext(), "Not connected to network", Toast.LENGTH_LONG).show();
				CreateGroup.this.finish();
			}
			
		}
	};
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mGroupCreatedReceiver);
		super.onDestroy();
	}



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_group_screen);
		
		registerReceiver(mGroupCreatedReceiver, new IntentFilter("GROUP_CREATED_ERROR"));
		mContactList = (ListView)findViewById(R.id.listviewContact);
		
		createGroup = (Button)findViewById(R.id.createGroup);
		grpName = (EditText)findViewById(R.id.grpname);
		participantSelector = (SearchView)findViewById(R.id.search_view);
		SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		
		participantSelector.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
		
		contacts = new ArrayList<Contact>();
		contactAdapter = new SelectContactArrayAdapter(EmotApplication.getAppContext(), R.layout.contact_row, contacts);
		mContactList.setAdapter(contactAdapter);
		setOnClickListeners();
		
		
	}
	
	
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshContacts();
		participantSelector.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			
			@Override
			public boolean onQueryTextSubmit(String query) {
				Log.i(TAG, "Sstring change : "+query);
            	contactAdapter.getFilter().filter(query);
                return true; 
			}
			
			@Override
			public boolean onQueryTextChange(String arg0) {
				Log.i(TAG, "Sstring submit : "+arg0);
				contactAdapter.getFilter().filter(arg0);
				return true;
			}
		});
		
	}



	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		this.finish();
	}



	private void setOnClickListeners(){
		
		createGroup.setOnClickListener(new View.OnClickListener() {
			
			
			ArrayList<Contact> selectedContacts = new ArrayList<Contact>();
			@Override
			public void onClick(View v) {
				
				
				for(int i=0; i < contacts.size(); i++){
					if(contacts.get(i).isSelected()){
						Log.i(TAG, "selected contacts is " + contacts.get(i).getName());
						selectedContacts.add(contacts.get(i));
						
					}
				}
				
				if(grpName.getText().toString() != null && grpName.getText().toString().length() != 0){
				//if(selectedContacts.size() > 0){
					Intent i = new Intent(CreateGroup.this, GroupChatScreen.class);
					
					i.putParcelableArrayListExtra("groupmembers", selectedContacts);
					i.putExtra(GroupChatScreen.INTENT_GRPCHAT_SUBJECT, grpName.getText().toString());
					i.putExtra("creategroup?", true);
					startActivity(i);
					selectedContacts.clear();
					
				//}else{
				//	Toast.makeText(CreateGroup.this, "Please select at least one group member", Toast.LENGTH_LONG).show();
				//}
				}else{
					Toast.makeText(CreateGroup.this, "Please enter group name", Toast.LENGTH_LONG).show();
				}
			}
		});
	}
	
	
	
	public void refreshContacts(){
		Log.i(TAG, "Refreshing contacts !!!!");
//		showContactsThread = new ShowContacts();
//		showContactsThread.execute();
		
		
		//Doing this on UI thread
		Cursor cr = getContentResolver().query(RosterProvider.CONTENT_URI, CONTACT_PROJECTION, null, null, null);
		
		Log.i(TAG, "contacts found  = "+cr.getCount());
		while (cr.moveToNext()) {
		    Contact contact = new Contact(cr.getString(cr.getColumnIndex(RosterConstants.ALIAS)), cr.getString(cr.getColumnIndex(RosterConstants.JID)));
		    contact.setStatus(cr.getString(cr.getColumnIndex(RosterConstants.STATUS_MESSAGE)));
		    contact.setAvatar(cr.getBlob(cr.getColumnIndex(RosterConstants.AVATAR)));
		    contacts.add(contact);
		}
		cr.close();
		contactAdapter.notifyDataSetChanged();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.create_group, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	

	public class ShowContacts extends AsyncTask<Void, Contact, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {
			try{
				Cursor cr = getContentResolver().query(RosterProvider.CONTENT_URI, CONTACT_PROJECTION, null, null, null);
				
				Log.i(TAG, "contacts found  = "+cr.getCount());
				while (cr.moveToNext()) {
				    Contact contact = new Contact(cr.getString(cr.getColumnIndex(RosterConstants.ALIAS)), cr.getString(cr.getColumnIndex(RosterConstants.JID)));
				    contact.setStatus(cr.getString(cr.getColumnIndex(RosterConstants.STATUS_MESSAGE)));
				    contact.setAvatar(cr.getBlob(cr.getColumnIndex(RosterConstants.AVATAR)));
				    publishProgress(contact);
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
			}
		}
	}
}
