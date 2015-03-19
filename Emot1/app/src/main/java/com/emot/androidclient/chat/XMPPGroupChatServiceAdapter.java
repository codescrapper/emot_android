package com.emot.androidclient.chat;

import java.util.ArrayList;
import java.util.List;

import android.os.RemoteException;
import com.emot.androidclient.util.Log;

import com.emot.androidclient.service.IXMPPGroupChatService;
import com.emot.emotobjects.Contact;

public class XMPPGroupChatServiceAdapter {

	private static final String TAG = XMPPGroupChatServiceAdapter.class.getName();
	private IXMPPGroupChatService xmppGrpServiceStub;
	private String grpJabberID;
	private boolean createGroup;

	public XMPPGroupChatServiceAdapter(IXMPPGroupChatService xmppServiceStub,
			String jabberID) {
		Log.i(TAG, "New XMPPChatServiceAdapter construced");
		this.xmppGrpServiceStub = xmppServiceStub;
		this.grpJabberID = jabberID;
		
	}
	
	public void createGroup(final String grpName, final List<Contact> members){
		Log.i(TAG, "Called createGroup(): " + grpJabberID + ": " + grpName);
		 
		Log.i(TAG, "Members are " +members);
		
			Thread t = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						Log.i(TAG, "members before sending to service " +members);
					 xmppGrpServiceStub.createGroup(grpName, members);
					Log.i(TAG, "members after sending to service " +members);// TODO Auto-generated method stub
					} catch (RemoteException e) {
						Log.i(TAG, "Remote Exception occured " +e.getMessage());
						//e.printStackTrace();
						
					}
				}
			});
			
		
		t.start();
		
	}
	
	public void leaveGroup(final String grpName){
		try {
			Log.i(TAG, "leaving group " +grpName);
			xmppGrpServiceStub.leaveGroup(grpName);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
	}
	
	public void joinExistingGroup(final String grpName, final boolean isCreateGrp, final long date){
		
		try {
			xmppGrpServiceStub.joinGroup(grpName, isCreateGrp, date);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
	}
	
	public String getGroupSubject(){
		String subject = "";
		
		try {
			subject = xmppGrpServiceStub.getGroupSubject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return subject;
	}
	
	public List<String> getGroupMembers(){
		List<String> members = new ArrayList<String>();
		
		try {
			members = xmppGrpServiceStub.getGroupMembers();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return members;
		
	}
	
	
	public void sendMessage(String user, String message, String tag) {
		
		try {
			Log.i(TAG, "Called sendMessage(): " + user + ": " + message);
			 xmppGrpServiceStub.sendGroupMessage(user, message, tag);
		} catch (RemoteException e) {
			Log.e(TAG, "caught RemoteException: " + e.getMessage());
		}
		
		
	}
	
	public boolean isServiceAuthenticated() {
		try {
			return xmppGrpServiceStub.isAuthenticated();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
		return false;
	}

	public void clearNotifications(String Jid) {
		try {
			xmppGrpServiceStub.clearNotifications(Jid);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
}
