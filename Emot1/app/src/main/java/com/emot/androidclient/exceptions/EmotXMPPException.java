package com.emot.androidclient.exceptions;

public class EmotXMPPException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public EmotXMPPException(String message) {
		super(message);
	}

	public EmotXMPPException(String message, Throwable cause) {
		super(message, cause);
	}
}
