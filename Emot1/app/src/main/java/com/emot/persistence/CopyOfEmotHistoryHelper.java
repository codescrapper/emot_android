package com.emot.persistence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.emot.androidclient.util.Log;

import com.emot.emotobjects.CurrentEmot;

public class CopyOfEmotHistoryHelper extends SQLiteOpenHelper {
	private static final String TAG = "EmotHistoryHelper";
	private static final int DATABASE_VERSION = 1;

	public CopyOfEmotHistoryHelper(Context context) {
		super(context, EmotDBHelper.DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	
	public Cursor getEmotHistory(final String entryID){
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor cursor = db.rawQuery("SELECT " + DBContract.EmotHistoryEntry.EMOTS + " from " +
				DBContract.EmotHistoryEntry.TABLE_NAME + " where " + 
				DBContract.EmotHistoryEntry.ENTRY_ID + " = '" + entryID  + "'", null);
		
		//db.execSQL(CREATE_EMOT_TABLE);
		
		Log.d(TAG, "starttime ... ");
		//Cursor cursor = db.rawQuery("SELECT * FROM emots WHERE tags MATCH 'apple OR bat';", null);
		Log.d(TAG, "querytime ... "+cursor.getCount());
		int i = 0;
		if (cursor != null) {
			while (cursor.moveToNext()) {
	    		//Log.d(TAG, cursor.getString(0));
	    		i++;
	    	}
		}
		//cursor.close();
		Log.d(TAG, "endtime ... " + i);
		Log.d(TAG, "Ran queries ...");
		
		return cursor;
		
	}
	
	public void insertChat(final String entryID, final String chat, final String date, final String time, final String location){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBContract.EmotHistoryEntry.ENTRY_ID, entryID);
		contentValues.put(DBContract.EmotHistoryEntry.EMOTS, chat);
		contentValues.put(DBContract.EmotHistoryEntry.DATETIME, date);	
		contentValues.put(DBContract.EmotHistoryEntry.EMOT_LOCATION, location);
		long i = db.insert(DBContract.EmotHistoryEntry.TABLE_NAME, null, contentValues);
		if(i < 0){
			Log.i(TAG,"Could not insert chat");
		}else{
			Log.i(TAG,"Chat inserted successfully "+contentValues.get(DBContract.EmotHistoryEntry.EMOTS));
		}

	}
	
	private  List<String> getUsers(final SQLiteDatabase db){
		
		String sql = "SELECT DISTINCT " + DBContract.EmotHistoryEntry.ENTRY_ID +
					 " from " + DBContract.EmotHistoryEntry.TABLE_NAME;
		Log.i(TAG,sql);
		Cursor usersList = db.rawQuery(sql, null);
		usersList.moveToFirst();
		List<String> users = new ArrayList<String>();
		Log.i(TAG,"sersList.isAfterLast() :" + usersList.isAfterLast());
		while(usersList.isAfterLast() == false){
			
			users.add(usersList.getString(usersList.getColumnIndex(DBContract.EmotHistoryEntry.ENTRY_ID)));
			usersList.moveToNext();
		}
		
		return users;
		
	}
	
	public List<CurrentEmot> getCurrentEmots(){
		SQLiteDatabase db = this.getReadableDatabase();
		List<CurrentEmot> currentEmots = new ArrayList<CurrentEmot>();
		CurrentEmot currentEmot = new CurrentEmot(); 
		String user = null;
		String chat = null;
		Cursor userLastEmot = null;
		String sql = null;
		List<String> usersList = getUsers(db);
		Log.i(TAG,"userList size is " +usersList.size());
		Iterator<String> iterator = usersList.iterator();
		while(iterator.hasNext()){
			user = iterator.next();
			Log.i(TAG,"user is " +user);
			sql = prepareSQLforEntryID(user);
			userLastEmot = db.rawQuery(sql, null);
			userLastEmot.moveToFirst();
			chat = userLastEmot.getString(userLastEmot.getColumnIndex(DBContract.EmotHistoryEntry.EMOTS));
			Log.i(TAG,"chat  is " +chat);
			currentEmot.setUserLastEmot(chat);
			currentEmot.setUserName(user);
			currentEmots.add(currentEmot);
		}
		
		
		return currentEmots;
		
		
	}
	
	//prepare SQL statements for user id, actually termed as entryID
	private String prepareSQLforEntryID(final String entryID){
		
		String sql = "SELECT " + DBContract.EmotHistoryEntry.EMOTS + " from " +
				DBContract.EmotHistoryEntry.TABLE_NAME + " where " + 
				DBContract.EmotHistoryEntry.ENTRY_ID + " = '" + entryID  + "'";
		
		return sql;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		//db.execSQL(sql)
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS contacts");
		onCreate(db);

	}

}
