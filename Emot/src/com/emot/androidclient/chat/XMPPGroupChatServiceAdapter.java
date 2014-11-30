package com.emot.androidclient.chat;

import java.util.ArrayList;
import java.util.List;

import android.os.RemoteException;
import android.util.Log;

import com.emot.androidclient.service.IXMPPChatService;
import com.emot.androidclient.service.IXMPPGroupChatService;
import com.emot.emotobjects.Contact;

public class XMPPGroupChatServiceAdapter {

	private static final String TAG = "yaxim.XMPPCSAdapter";
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
						e.printStackTrace();
					}
				}
			});
			
		
		t.start();
	}
	
	public void joinExistingGroup(final String grpName, final boolean isCreateGrp, final long date){
		
		try {
			xmppGrpServiceStub.joinGroup(grpName, isCreateGrp, date);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public String sendMessage(String user, String message) {
		String piD = "";
		try {
			Log.i(TAG, "Called sendMessage(): " + grpJabberID + ": " + message);
			 piD =  xmppGrpServiceStub.sendGroupMessage(user, message);
		} catch (RemoteException e) {
			Log.e(TAG, "caught RemoteException: " + e.getMessage());
		}
		
		return piD;
	}
	
	public boolean isServiceAuthenticated() {
		try {
			return xmppGrpServiceStub.isAuthenticated();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void clearNotifications(String Jid) {
		try {
			xmppGrpServiceStub.clearNotifications(Jid);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}