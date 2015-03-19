package com.emot.androidclient;

/*
	IPC interface for XMPPService to send broadcasts to UI
*/

interface IXMPPRosterCallback {
	void connectionStateChanged(int connectionstate);
}
