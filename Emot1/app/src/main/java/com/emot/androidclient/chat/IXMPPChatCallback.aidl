package com.emot.androidclient.chat;

/*
	IPC interface for XMPPService to send broadcasts to UI
*/

interface IXMPPChatCallback {
	void chatStateChanged(int chatState, String from);
}
