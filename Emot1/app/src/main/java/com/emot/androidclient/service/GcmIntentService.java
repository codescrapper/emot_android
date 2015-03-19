package com.emot.androidclient.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.emot.androidclient.util.Log;
import com.emot.screens.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
    String mes;
    private Handler handler;
   public GcmIntentService() {
       super("GcmIntentService");
   }

   @Override
   public void onCreate() {
       // TODO Auto-generated method stub
       super.onCreate();
       handler = new Handler();
   }
   
   private Notification mNotification;
   private Intent mNotificationIntent;
   NotificationManager notificationManager;
   @Override
   protected void onHandleIntent(Intent intent) {
       Bundle extras = intent.getExtras();
       notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
       GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
       // The getMessageType() intent parameter must be the intent you received
       // in your BroadcastReceiver.
       String messageType = gcm.getMessageType(intent);

      mes = extras.getString("title");
      String ticker = getString(R.string.notification_anonymous_message);
      mNotification = new NotificationCompat.Builder(getApplicationContext())
      .setContentTitle("Progress")
      .setContentText(mes)
      .setTicker("Notification!")
      .setWhen(System.currentTimeMillis())
      .setDefaults(Notification.DEFAULT_SOUND)
      .setAutoCancel(true)
      .setSmallIcon(R.drawable.ic_launcher)
      .build();
      
      notificationManager.notify(1, mNotification);
     
      showToast();
      Log.i("GCM", "Received : (" +messageType+")  "+extras.getString("title"));

       GcmBroadcastReceiver.completeWakefulIntent(intent);

   }

   public void showToast(){
       handler.post(new Runnable() {
           public void run() {
               Toast.makeText(getApplicationContext(),mes , Toast.LENGTH_LONG).show();
           }
        });

   }}