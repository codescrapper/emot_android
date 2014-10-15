package com.emot.androidclient.service;

interface IXMPPGroupChatService {
	void sendMessage(String user, String message);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
}