package com.emot.androidclient.chat;

import android.os.RemoteException;
import android.util.Log;

import com.emot.androidclient.service.IXMPPChatService;
import com.emot.androidclient.service.IXMPPGroupChatService;

public class XMPPGroupChatServiceAdapter {

	private static final String TAG = "yaxim.XMPPCSAdapter";
	private IXMPPGroupChatService xmppGrpServiceStub;
	private String grpJabberID;

	public XMPPGroupChatServiceAdapter(IXMPPGroupChatService xmppServiceStub,
			String jabberID) {
		Log.i(TAG, "New XMPPChatServiceAdapter construced");
		this.xmppGrpServiceStub = xmppServiceStub;
		this.grpJabberID = jabberID;
	}

	public void sendMessage(String user, String message) {
		try {
			Log.i(TAG, "Called sendMessage(): " + grpJabberID + ": " + message);
			xmppGrpServiceStub.sendMessage(user, message);
		} catch (RemoteException e) {
			Log.e(TAG, "caught RemoteException: " + e.getMessage());
		}
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
