package com.emot.screens;

import java.util.ArrayList;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SearchView.OnQueryTextListener;
import com.emot.androidclient.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.emot.adapters.ContactArrayAdapter;
import com.emot.androidclient.IXMPPRosterCallback.Stub;
import com.emot.androidclient.XMPPRosterServiceAdapter;
import com.emot.androidclient.data.EmotConfiguration;
import com.emot.androidclient.data.RosterProvider;
import com.emot.androidclient.data.RosterProvider.RosterConstants;
import com.emot.androidclient.service.IXMPPRosterService;
import com.emot.androidclient.service.XMPPService;
import com.emot.androidclient.util.ConnectionState;
import com.emot.common.TaskCompletedRunnable;
import com.emot.emotobjects.Contact;
import com.emot.model.EmotApplication;
import com.emot.persistence.ContactUpdater;

public class ContactScreen extends EmotActivity{
	private ListView listviewContact;
	private static String TAG = ContactScreen.class.getSimpleName();
	private ArrayList<Contact> contacts;
	private ShowContacts showContactsThread;

	private Intent xmppServiceIntent;
	private ServiceConnection xmppServiceConnection;
	private XMPPRosterServiceAdapter serviceAdapter;
	private Stub rosterCallback;
	private EmotConfiguration mConfig;
	private ContactArrayAdapter contactAdapter;
	private Button inviteButton;
	
	final static private String[] CONTACT_PROJECTION = new String[] {
		RosterConstants.ALIAS, RosterConstants.STATUS_MESSAGE,
		RosterConstants.AVATAR, RosterConstants.JID};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contacts);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		getSupportActionBar().setTitle("Contacts");
		
		listviewContact = (ListView)findViewById(R.id.listview_contact);
		inviteButton = (Button)findViewById(R.id.buttonInvite);
		contacts = new ArrayList<Contact>();
		contactAdapter = new ContactArrayAdapter(EmotApplication.getAppContext(), R.layout.contact_row, contacts);
		listviewContact.setAdapter(contactAdapter);
		refreshContacts();

		Intent i = new Intent(this, XMPPService.class);
		//i.setAction("com.emot.services.ChatService");
		this.startService(i);
		listviewContact.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//startActivity(new Intent(EmotApplication.getAppContext(), UpdateProfileScreen.class));


				String jid = contactAdapter.getItem(position).getJID();
				Intent chatIntent = new Intent(ContactScreen.this, ChatScreen.class);
				chatIntent.putExtra(ChatScreen.INTENT_CHAT_FRIEND, jid);
				startActivity(chatIntent);

				//startChatActivity(mobile+"@"+WebServiceConstants.CHAT_DOMAIN, "alias", null);

			}
		});
		inviteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, EmotApplication.getAppContext().getResources().getString(R.string.invite_text));
                startActivity(Intent.createChooser(shareIntent, "Share"));
			}
		});
		registerXMPPService();
	}
	
	public void refreshContacts(){
		Log.i(TAG, "Refreshing contacts !!!!");
		//showContactsThread = new ShowContacts();
		//showContactsThread.execute();
		
		
		try{
			Log.i(TAG, "Starting Query ...");
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
			Log.i(TAG, "time 2");
		}catch(Exception e){
			//e.printStackTrace();
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		//unbindXMPPService();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//bindXMPPService();
	}

	@Override
	protected void onStop() {
		if(showContactsThread!=null){
			showContactsThread.cancel(true);
		}
		super.onStop();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
		Log.i(TAG, "Action bar creating menu");
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu_actions, menu);
	    SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView search = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        search.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        search.setOnQueryTextListener(new OnQueryTextListener() { 
            @Override 
            public boolean onQueryTextChange(String query) {
            	Log.i(TAG, "Sstring change : "+query);
            	contactAdapter.getFilter().filter(query);
                return true; 
            }

			@Override
			public boolean onQueryTextSubmit(String arg0) {
				Log.i(TAG, "Sstring submit : "+arg0);
				contactAdapter.getFilter().filter(arg0);
				return true;
			} 
 
        });
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_profile:
	            startActivity(new Intent(ContactScreen.this, UpdateProfileScreen.class));
	            return true;
	        case android.R.id.home:
	        	Log.i(TAG, "back pressed");
	            this.finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}



	public class ShowContacts extends AsyncTask<Void, Contact, Boolean>{

		@Override
		protected Boolean doInBackground(Void... params) {
			try{
				Log.i(TAG, "Starting Query ...");
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


	private void registerXMPPService() {
		Log.i(TAG, "called startXMPPService()");
		mConfig = EmotConfiguration.getConfig();
		xmppServiceIntent = new Intent(this, XMPPService.class);
		xmppServiceIntent.setAction("com.emot.services.XMPPSERVICE");

		xmppServiceConnection = new ServiceConnection() {

			public void onServiceConnected(ComponentName name, IBinder service) {
				Log.i(TAG, "called onServiceConnected()");
				serviceAdapter = new XMPPRosterServiceAdapter(
						IXMPPRosterService.Stub.asInterface(service));
				//serviceAdapter.registerUICallback(rosterCallback);
				Log.i(TAG, "getConnectionState(): " + serviceAdapter.getConnectionState());
				ConnectionState cs = serviceAdapter.getConnectionState();
				//				updateConnectionState(cs);
				//				updateRoster();

				// when returning from prefs to main activity, apply new config
				if (mConfig.reconnect_required && cs == ConnectionState.ONLINE) {
					// login config changed, force reconnection
					Log.i(TAG, "--------- RECONNECTING CONTACTSCREEN ----------");
					serviceAdapter.disconnect();
					serviceAdapter.connect();
				} else if (mConfig.presence_required && isConnected()){
					Log.i(TAG, "--------- SETTING STATUS CONTACTSCREEN ----------");
					serviceAdapter.setStatusFromConfig();
				}
				//updateContacts();
				
				// handle server-related intents after connecting to the backend
				//handleJabberIntent();
			}

			public void onServiceDisconnected(ComponentName name) {
				Log.i(TAG, "called onServiceDisconnected()");
			}
		};
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

	private boolean isConnected() {
		return serviceAdapter != null && serviceAdapter.isAuthenticated();
	}
	
	private boolean isConnecting() {
		return serviceAdapter != null && serviceAdapter.getConnectionState() == ConnectionState.CONNECTING;
	}

}
