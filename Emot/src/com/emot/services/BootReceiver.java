package com.emot.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver
{
    
    public void onReceive(Context context, Intent intent)
    {
        
           // Your code to execute when Boot Completd
    	 if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
             Intent i = new Intent();
             i.setAction("com.emot.androidclient.service.XMPPService");
            // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
             context.startService(i);
         }
    }

	
}
       