package com.emot.model;

import android.graphics.Bitmap;

public class Emot {
	private String emotHash;
	private Bitmap emotImg;
	private String emotTags;
	
	public Emot(String hash, Bitmap bitmap) {
		this.emotHash = hash;
		this.emotImg = bitmap;
	}

	public String getEmotHash() {
		return emotHash;
	}
	
	public void setEmotHash(String emotHash) {
		this.emotHash = emotHash;
	}
	
	public Bitmap getEmotImg() {
		return emotImg;
	}
	
	public void setEmotImg(Bitmap emotImg) {
		this.emotImg = emotImg;
	}
}
