package com.emot.androidclient.service;

public interface XMPPServiceCallback {
	void newMessage(String from, String messageBody, boolean silent_notification, boolean grpchat, String msgSenderinGrp);
	void messageError(String from, String errorBody, boolean silent_notification);
	void connectionStateChanged();
	void rosterChanged(); // TODO: remove that!
	void chatStateChanged(int ordinal, String from);
}
