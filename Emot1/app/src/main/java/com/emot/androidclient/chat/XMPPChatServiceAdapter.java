package com.emot.androidclient.chat;

import android.os.RemoteException;
import com.emot.androidclient.util.Log;

import com.emot.androidclient.service.IXMPPChatService;

public class XMPPChatServiceAdapter {

	private static final String TAG = XMPPChatServiceAdapter.class.getSimpleName();
	private IXMPPChatService xmppServiceStub;
	private String jabberID;

	public XMPPChatServiceAdapter(IXMPPChatService xmppServiceStub,
			String jabberID) {
		Log.i(TAG, "New XMPPChatServiceAdapter construced");
		this.xmppServiceStub = xmppServiceStub;
		this.jabberID = jabberID;
	}

	public void sendMessage(String user, String message) {
		try {
			Log.i(TAG, "Called sendMessage(): " + jabberID + ": " + message);
			xmppServiceStub.sendMessage(user, message);
		} catch (RemoteException e) {
			Log.e(TAG, "caught RemoteException: " + e.getMessage());
		}
	}
	
	public boolean isServiceAuthenticated() {
		try {
			return xmppServiceStub.isAuthenticated();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
		return false;
	}

	public void clearNotifications(String Jid) {
		try {
			xmppServiceStub.clearNotifications(Jid);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void registerUICallback(IXMPPChatCallback uiCallback) {
		try {
			xmppServiceStub.registerChatCallback(uiCallback);
		} catch (RemoteException e) {
			//e.printStackTrace();
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}
	}

	public void unregisterUICallback(IXMPPChatCallback uiCallback) {
		try {
			xmppServiceStub.unregisterChatCallback(uiCallback);
		} catch (RemoteException e) {
			//e.printStackTrace();
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}
	}

	public void sendChatState(String chatFriend, String state) {
		try {
			xmppServiceStub.sendChatState(chatFriend, state);
		} catch (RemoteException e) {
			//e.printStackTrace();
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}
	}
}
