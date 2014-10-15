package com.emot.androidclient.service;


import com.emot.androidclient.exceptions.EmotXMPPException;
import com.emot.androidclient.util.ConnectionState;


public interface Smackable {
	boolean doConnect(boolean create_account) throws EmotXMPPException;
	boolean isAuthenticated();
	void requestConnectionState(ConnectionState new_state);
	void requestConnectionState(ConnectionState new_state, boolean create_account);
	ConnectionState getConnectionState();
	String getLastError();

	void addRosterItem(String user, String alias, String group) throws EmotXMPPException;
	void removeRosterItem(String user) throws EmotXMPPException;
	void renameRosterItem(String user, String newName) throws EmotXMPPException;
	void moveRosterItemToGroup(String user, String group) throws EmotXMPPException;
	void renameRosterGroup(String group, String newGroup);
	void sendPresenceRequest(String user, String type);
	void addRosterGroup(String group);
	String changePassword(String newPassword);
	
	void setStatusFromConfig();
	void sendMessage(String user, String message);
	void sendGroupMessage(String message);
	void sendServerPing();
	void setUserWatching(boolean user_watching);
	
	void registerCallback(XMPPServiceCallback callBack);
	void unRegisterCallback();
	void setAvatar(String file);
	
	String getNameForJID(String jid);
}
