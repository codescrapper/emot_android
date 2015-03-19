package com.emot.androidclient.service;

import com.emot.androidclient.chat.IXMPPChatCallback;

interface IXMPPChatService {
	void sendMessage(String user, String message);
	boolean isAuthenticated();
	void clearNotifications(String Jid);
	void registerChatCallback(IXMPPChatCallback callback);
	void unregisterChatCallback(IXMPPChatCallback callback);
	void sendChatState(String user, String state);
}