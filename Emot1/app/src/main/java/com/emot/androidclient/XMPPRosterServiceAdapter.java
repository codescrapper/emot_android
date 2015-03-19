package com.emot.androidclient;


import java.io.File;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.os.RemoteException;
import com.emot.androidclient.util.Log;

import com.emot.androidclient.service.IXMPPRosterService;
import com.emot.androidclient.util.ConnectionState;
import com.emot.common.ImageHelper;
import com.emot.model.EmotApplication;

public class XMPPRosterServiceAdapter {
	
	private static final String TAG = XMPPRosterServiceAdapter.class.getSimpleName();
	private IXMPPRosterService xmppServiceStub;
	
	public XMPPRosterServiceAdapter(IXMPPRosterService xmppServiceStub) {
		Log.i(TAG, "New XMPPRosterServiceAdapter construced");
		this.xmppServiceStub = xmppServiceStub;
	}
	
	public void setStatusFromConfig() {
		try {
			xmppServiceStub.setStatusFromConfig();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}

	public void addRosterItem(String user, String alias, String group) {
		try {
			xmppServiceStub.addRosterItem(user, alias, group);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void renameRosterGroup(String group, String newGroup){
		try {
			xmppServiceStub.renameRosterGroup(group, newGroup);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void renameRosterItem(String contact, String newItemName){
		try {
			xmppServiceStub.renameRosterItem(contact, newItemName);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	
	public void moveRosterItemToGroup(String user, String group){
		try {
			xmppServiceStub.moveRosterItemToGroup(user, group);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void addRosterGroup(String group){
		try {
			xmppServiceStub.addRosterGroup(group);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void removeRosterItem(String user) {
		try {
			xmppServiceStub.removeRosterItem(user);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void disconnect() {
		try {
			xmppServiceStub.disconnect();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void connect() {
		try {
			xmppServiceStub.connect();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}

	public void registerUICallback(IXMPPRosterCallback uiCallback) {
		try {
			xmppServiceStub.registerRosterCallback(uiCallback);
		} catch (RemoteException e) {
			//e.printStackTrace();
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}
	}

	public void unregisterUICallback(IXMPPRosterCallback uiCallback) {
		try {
			xmppServiceStub.unregisterRosterCallback(uiCallback);
		} catch (RemoteException e) {
			//e.printStackTrace();
		} catch (NullPointerException e) {
			//e.printStackTrace();
		}
	}

	public ConnectionState getConnectionState() {
		try {
			return ConnectionState.values()[xmppServiceStub.getConnectionState()];
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
		return ConnectionState.OFFLINE;
	}

	public String getConnectionStateString() {
		try {
			return xmppServiceStub.getConnectionStateString();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
		return null;
	}

	public boolean isAuthenticated() {
		return getConnectionState() == ConnectionState.ONLINE;
	}

	public void sendPresenceRequest(String user, String type) {
		try {
			xmppServiceStub.sendPresenceRequest(user, type);
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}

	public String changePassword(String newPassword) {
		try {
			return xmppServiceStub.changePassword(newPassword);
		} catch (RemoteException e) {
			//e.printStackTrace();
			return "service connection failure.";
		}
	}
	
	public void setAvatar(){
		try {
			xmppServiceStub.setAvatar();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public void setAvatar(Bitmap bmp){
		try {
			Log.i(TAG, "cache directory : "+ImageHelper.getCacheDir(EmotApplication.getAppContext()));
			String filePath = ImageHelper.getCacheDir(EmotApplication.getAppContext());
			saveBitmap(bmp, "profile", filePath, false);
			//Save file bmp to file path
			xmppServiceStub.setAvatar();
		} catch (RemoteException e) {
			//e.printStackTrace();
		}
	}
	
	public static File saveBitmap(Bitmap bitmap, String filename, String path, boolean recycle) {
        FileOutputStream out=null;
        try {
            File f = new File(path,filename);
            if(!f.exists()) {
                f.createNewFile();
            }
            out = new FileOutputStream(f);
            if(bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                return f;
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not save bitmap : " + e.getMessage());
        } finally {
            try{
                out.close();
            } catch(Throwable ignore) {}
            if(recycle) {
                bitmap.recycle();
            }
        }
        return null;
    }
}
