package com.emot.androidclient.service;

interface IXMPPGroupChatService {
	void sendGroupMessage(String user, String message);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
}