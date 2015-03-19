package com.emot.emotobjects;

import com.emot.constants.WebServiceConstants;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable{
	private String name;
	private String jid;
	private String profileImgUrl;
	private String status;
	private byte[] avatar;
	private boolean selected;
	
	public Contact(String name, String jid){
		this.setName(name);
		this.setJID(jid);
	}

	public String getName() {
		return name.replaceAll("@"+ WebServiceConstants.CHAT_DOMAIN, "");
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJID() {
		return jid;
	}
	
	public String getGJID(){
		String gjid = jid;
		gjid = jid.substring(0, jid.lastIndexOf('@'));
		
		return gjid+"@emot-net/Smack";
	}
	
	

	public void setJID(String jid) {
		this.jid = jid;
	}

	public String getProfileImgUrl() {
		return profileImgUrl;
	}

	public void setProfileImgUrl(String profileImgUrl) {
		this.profileImgUrl = profileImgUrl;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public byte[] getAvatar() {
		return avatar;
	}

	public void setAvatar(byte[] avatar) {
		this.avatar = avatar;
	}
	
	public boolean isSelected() {
		return selected;
	}
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(name);
		dest.writeString(jid);
		
	}
	
	private Contact(Parcel in){
		this.name = in.readString();
		this.jid = in.readString();
	}
	
	public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
	    public Contact createFromParcel(Parcel in) {
	     return new Contact(in);
	    }

	    public Contact[] newArray(int size) {
	     return new Contact[size];
	    }
	  };
}
