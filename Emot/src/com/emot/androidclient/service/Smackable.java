package com.emot.androidclient.service;


import java.util.List;

import android.app.Service;

import com.emot.androidclient.exceptions.EmotXMPPException;
import com.emot.androidclient.util.ConnectionState;
import com.emot.emotobjects.Contact;


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
	void setLastActivity();
	void setStatusFromConfig();
	void sendMessage(String user, String message);
	void sendChatState(String user, String state);
	
	void sendGroupMessage(String message, String tag);
	void createGroup(String grpName);
	void sendServerPing();
	void setUserWatching(boolean user_watching);
	void initMUC(String grpName);
	void registerCallback(XMPPServiceCallback callBack);
	void unRegisterCallback();
	void setAvatar();
	void joinUsers(List<Contact> members);
	void joinGroup(String grpName, long date);
	String getGroupSubject();
	Service getService();
	void changeGroupSubject(String grpSubject, String grpID);
	List<String> getGroupMembers();
	void leaveGroup(String grpName);
	String getNameForJID(String jid);
	boolean isRunning();
}
