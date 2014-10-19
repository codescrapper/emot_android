package com.emot.androidclient.service;

/*
	IPC interface for methods on XMPPService called by an activity
*/

import com.emot.androidclient.IXMPPRosterCallback;

interface IXMPPRosterService {
	/* hack: use int because enums are not trivially parcellable */
	int getConnectionState();
	String getConnectionStateString();
	
	/* xmpp methods */
	
	void setStatusFromConfig();
	void disconnect();
	void connect();
	void addRosterItem(String user, String alias, String group);
	void addRosterGroup(String group);
	void renameRosterGroup(String group, String newGroup);
	void removeRosterItem(String user);
	void sendPresenceRequest(String user, String type);
	void renameRosterItem(String user, String newName);
	void moveRosterItemToGroup(String user, String group);
	String changePassword(String newPassword);
	void setAvatar();
	
	/* callback methods */
	
	void registerRosterCallback(IXMPPRosterCallback callback);
	void unregisterRosterCallback(IXMPPRosterCallback callback);
}
