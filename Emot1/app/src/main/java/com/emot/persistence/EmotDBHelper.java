package com.emot.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.emot.androidclient.util.Log;

public class EmotDBHelper extends SQLiteOpenHelper{

	private static final int DATABASE_VERSION = 1;
	public static final String DATABASE_NAME = "emot.db";
	private static final String TAG = EmotDBHelper.class.getSimpleName();
	private static final  String SQL_CREATE_TABLE_EMOTHISTORY = "CREATE TABLE IF NOT EXISTS " +
			DBContract.EmotHistoryEntry.TABLE_NAME +
			" (" + DBContract.EmotHistoryEntry._ID + " INTEGER PRIMARY KEY," +
			DBContract.EmotHistoryEntry.ENTRY_ID + " VARCHAR(20)," +
			DBContract.EmotHistoryEntry.EMOTS + " TEXT," +
			DBContract.EmotHistoryEntry.DATETIME + " TEXT," +
			DBContract.EmotHistoryEntry.EMOT_LOCATION + " TEXT " +" )";
	
	private static final  String SQL_CREATE_TABLE_GROUPEMOTHISTORY = "CREATE TABLE IF NOT EXISTS " +
			DBContract.GroupEmotHistoryEntry.TABLE_NAME +
			" (" + DBContract.GroupEmotHistoryEntry._ID + " INTEGER PRIMARY KEY," +
			DBContract.GroupEmotHistoryEntry.ENTRY_ID + " VARCHAR(20)," +
			DBContract.GroupEmotHistoryEntry.GROUP_NAME + " TEXT," +
			DBContract.GroupEmotHistoryEntry.EMOTS + " TEXT," +
			DBContract.GroupEmotHistoryEntry.DATETIME + " TEXT," +
			DBContract.GroupEmotHistoryEntry.EMOT_LOCATION + " TEXT " +" )";
	
//	private static final String SQL_CREATE_TABLE_CONTACTDETAILS = "CREATE TABLE IF NOT EXISTS " +
//			DBContract.ContactsDBEntry.TABLE_NAME +
//			" (" + DBContract.ContactsDBEntry._ID + " INTEGER PRIMARY KEY autoincrement," +
//			DBContract.ContactsDBEntry.CURRENT_STATUS + " VARCHAR(100) NULL," +
//			DBContract.ContactsDBEntry.LAST_SEEN + " DATETIME NULL," +
//			DBContract.ContactsDBEntry.EMOT_NAME + " VARCHAR(100) NULL," +
//			DBContract.ContactsDBEntry.CONTACT_NAME + " VARCHAR(100) NULL," +
//			DBContract.ContactsDBEntry.MOBILE_NUMBER + " VARCHAR(20) UNIQUE," +
//			DBContract.ContactsDBEntry.PROFILE_THUMB + " BLOB NULL," +
//			DBContract.ContactsDBEntry.SUBSCRIBED + " BOOLEAN DEFAULT 0 NOT NULL," +
//			DBContract.ContactsDBEntry.PROFILE_IMG + " VARCHAR(100) NULL" + " )";
	
	
	private static EmotDBHelper emotDBHelperInstance;
	
	private EmotDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		
	}

	public static EmotDBHelper getInstance(Context context ) {
		
		if(emotDBHelperInstance == null){
			emotDBHelperInstance = new EmotDBHelper(context);
		}
		Log.d(TAG, "In Instance get!!!");
		return emotDBHelperInstance;
		
	
	}
	//SQLiteDatabase db;
	
	public Cursor getEmotHistory(final String entryID){
		 //db = this.getReadableDatabase();
		
		Cursor cursor = emotDBHelperInstance.getReadableDatabase().rawQuery("SELECT " + DBContract.EmotHistoryEntry.EMOTS + "," +
				DBContract.EmotHistoryEntry.EMOT_LOCATION + "," +
				DBContract.EmotHistoryEntry.DATETIME + " from " +
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
		//cursor.close();
		return cursor;
		
	}
	
	public Cursor getGroupEmotHistory(final String pGroupName){
		
		Cursor cursor = emotDBHelperInstance.getReadableDatabase().rawQuery("SELECT " + DBContract.GroupEmotHistoryEntry.EMOTS + "," +
				DBContract.GroupEmotHistoryEntry.EMOT_LOCATION + "," +
				DBContract.GroupEmotHistoryEntry.ENTRY_ID + "," +
				DBContract.GroupEmotHistoryEntry.DATETIME + " from " +
				DBContract.GroupEmotHistoryEntry.TABLE_NAME + " where " + 
				DBContract.GroupEmotHistoryEntry.GROUP_NAME + " = '" + pGroupName  + "'", null);
		
		//db.execSQL(CREATE_EMOT_TABLE);
		
		Log.d(TAG, "starttime ... ");
		//Cursor cursor = db.rawQuery("SELECT * FROM emots WHERE tags MATCH 'apple OR bat';", null);
		Log.d(TAG, "querytime ... "+cursor.getCount());
		
		
		//cursor.close();
		
		Log.d(TAG, "Ran queries ...");
		
		return cursor;
		
		
	}
	
	public void insertChat(final String entryID, final String chat, final String date, final String time, final String location){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBContract.EmotHistoryEntry.ENTRY_ID, entryID);
		contentValues.put(DBContract.EmotHistoryEntry.EMOTS, chat);
		contentValues.put(DBContract.EmotHistoryEntry.DATETIME, time);	
		contentValues.put(DBContract.EmotHistoryEntry.EMOT_LOCATION, location);
		long i = db.insert(DBContract.EmotHistoryEntry.TABLE_NAME, null, contentValues);
		if(i < 0){
			Log.i(TAG,"Could not insert chat");
		}else{
			Log.i(TAG,"Chat inserted successfully "+contentValues.get(DBContract.EmotHistoryEntry.EMOTS));
		}

	}
	
	public void insertGroupChat(final String entryID, final String groupName, final String chat, final String date, final String time, final String location){
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(DBContract.GroupEmotHistoryEntry.ENTRY_ID, entryID);
		contentValues.put(DBContract.GroupEmotHistoryEntry.GROUP_NAME, entryID);
		contentValues.put(DBContract.GroupEmotHistoryEntry.EMOTS, chat);
		contentValues.put(DBContract.GroupEmotHistoryEntry.DATETIME, time);	
		contentValues.put(DBContract.GroupEmotHistoryEntry.EMOT_LOCATION, location);
		long i = db.insert(DBContract.GroupEmotHistoryEntry.TABLE_NAME, null, contentValues);
		if(i < 0){
			Log.i(TAG,"Could not insert chat");
		}else{
			Log.i(TAG,"Chat inserted successfully "+contentValues.get(DBContract.EmotHistoryEntry.EMOTS));
		}

	}
	
	public Cursor runQuery(final SQLiteDatabase db, final String sql){
		
		Cursor result = db.rawQuery(sql, null);
		return result;
	}
	
	public void insert(final String tableName, final ContentValues contentValues){
		SQLiteDatabase db = this.getWritableDatabase();
		long i = db.insert(tableName, null, contentValues);
		if(i < 0){
			Log.i(TAG,"Could not insert records into table "+tableName);
		}else{
			Log.i(TAG,"Records inserted successfully into table "+tableName);
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(SQL_CREATE_TABLE_EMOTHISTORY);
		//db.execSQL(SQL_CREATE_TABLE_CONTACTDETAILS);
		Log.d(TAG, "Tables created !!!");
		db.execSQL(SQL_CREATE_TABLE_GROUPEMOTHISTORY);
		
        
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db);
	}
	
}
