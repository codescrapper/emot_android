package com.emot.androidclient.service;
import java.util.List;
import com.emot.emotobjects.Contact;
interface IXMPPGroupChatService {
	void sendGroupMessage(String user, String message, String tag);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
	boolean createGroup(String grpName,inout List<Contact> members);
	void joinGroup(String grpName, boolean isCreateGroup, long date);
	void leaveGroup(String grpName);
	String getGroupSubject();
	List<String> getGroupMembers();
}