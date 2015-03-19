package com.emot.androidclient.util;

import com.emot.screens.R;

public enum StatusMode {
	offline(R.string.status_offline, R.drawable.sb_message),
	dnd(R.string.status_dnd, R.drawable.sb_message),
	xa(R.string.status_xa, R.drawable.sb_message),
	away(R.string.status_away, R.drawable.sb_message),
	available(R.string.status_available, R.drawable.sb_message),
	chat(R.string.status_chat, R.drawable.sb_message),
	subscribe(0 /* not a status you can set */, R.drawable.sb_message);

	private final int textId;
	private final int drawableId;

	StatusMode(int textId, int drawableId) {
		this.textId = textId;
		this.drawableId = drawableId;
	}

	public int getTextId() {
		return textId;
	}

	public int getDrawableId() {
		return drawableId;
	}

	public String toString() {
		return name();
	}

	public static StatusMode fromString(String status) {
		return StatusMode.valueOf(status);
	}

}
