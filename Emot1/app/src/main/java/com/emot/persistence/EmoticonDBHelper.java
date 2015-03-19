package com.emot.persistence;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import com.emot.androidclient.util.EmotUtils;
import com.emot.model.EmotApplication;
import com.emot.screens.R;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import com.emot.androidclient.util.Log;

public class EmoticonDBHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "emoticon.db";
	private static final String DATABASE_PATH = Environment.getDataDirectory().toString()+"/data/com.emot.screens/databases/";
	private static final String TAG = EmoticonDBHelper.class.getSimpleName();
	private static EmoticonDBHelper emoticonHelperInstance;
	private static HashMap<String, Bitmap> emotCache = new HashMap<String, Bitmap>();

	Context context;

	public static final String SQL_CREATE_TABLE_EMOT = "CREATE VIRTUAL TABLE" +
			" " + DBContract.EmotsDBEntry.TABLE_NAME +
			" USING fts3 " +
			" (" + DBContract.EmotsDBEntry._ID + " INTEGER PRIMARY KEY autoincrement," +
			DBContract.EmotsDBEntry.EMOT_HASH + " VARCHAR(20)," +
			DBContract.EmotsDBEntry.EMOT_IMG + " BLOB," +
			DBContract.EmotsDBEntry.TAGS + " TEXT" + ")";

	public EmoticonDBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this.context = context;
	}

	public EmoticonDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	public static EmoticonDBHelper getInstance(Context context ) {

		if(emoticonHelperInstance == null){
			emoticonHelperInstance = new EmoticonDBHelper(context);
		}
		Log.i(TAG, "In Instance get!!!");
		return emoticonHelperInstance;


	}

	public void copyDataBase() throws IOException{

		//Open your local db as the input stream
		InputStream myInput = context.getAssets().open(DATABASE_NAME);

		// Path to the just created empty db
		String outFileName = DATABASE_PATH + DATABASE_NAME;
		File databaseFile = new File( DATABASE_PATH);
        // check if databases folder exists, if not create one and its subfolders
        if (!databaseFile.exists()){
            databaseFile.mkdir();
        }

		//Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		//transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer))>0){
			myOutput.write(buffer, 0, length);
		}

		//Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();

	}

	public void createDatabase(){
		//db.execSQL("create table emoticons (_id INTEGER PRIMARY KEY autoincrement, emot_hash varchar(20), emot_img BLOB, tags TEXT)");
		boolean dbExist = checkDataBase();
		if(dbExist){
			Log.i(TAG, "DATABASE EXISTS");
			//do nothing - database already exist
		}else{
			Log.i(TAG, "DATABASE DOES NOT EXISTS");
			try {
				copyDataBase();
				Log.i(TAG, "Copied database successfully !!!");
			} catch (IOException e) {
				Log.i(TAG, "Error copying database");
				//e.printStackTrace();
			}
		}
	}

	private boolean checkDataBase(){
		Log.i(TAG, "Checking DBBBB");
		SQLiteDatabase checkDB = null;
		try{
			String dbPath = DATABASE_PATH + DATABASE_NAME;
			checkDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
		}catch(Exception e){
			//database does't exist yet.
			Log.i(TAG, "Exception caught !!!");
			//e.printStackTrace();
		}
		if(checkDB != null){
			checkDB.close();
		}
		return checkDB != null ? true : false;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}
	
	public static Bitmap getEmot(byte[] emotImg, byte[] emotImgLrg, String emot_hash){
		if(emotImg==null){
			Log.i(TAG, "Pulling image from large");
			emotImg = EmotUtils.resizeEmoticon(emotImgLrg);
			ContentValues values = new ContentValues();
			values.put(DBContract.EmotsDBEntry.EMOT_IMG, emotImg);
			getInstance(EmotApplication.getAppContext()).getWritableDatabase().update(
					DBContract.EmotsDBEntry.TABLE_NAME, 
					values, 
					DBContract.EmotsDBEntry.EMOT_HASH+"='"+emot_hash+"'", 
					null
			);
		}else{
			Log.i(TAG, "Pulling image from small");
		}
		return BitmapFactory.decodeByteArray(emotImg , 0, emotImg.length);
	}
	
	public static Bitmap getEmotImg(String emot_hash){
		Bitmap emot_img = emotCache.get(emot_hash);
		if(emot_img==null){
			Log.i(TAG, "emot image from DB");
			Cursor cr = EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getReadableDatabase().query(
					DBContract.EmotsDBEntry.TABLE_NAME, 
					new String[] {DBContract.EmotsDBEntry.EMOT_IMG, DBContract.EmotsDBEntry.EMOT_IMG_LARGE} , 
					DBContract.EmotsDBEntry.EMOT_HASH+" match '"+emot_hash+"';", 
					null, null, null, null, null);
			
			while (cr.moveToNext())
			{
				byte[] emotImg = cr.getBlob(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_IMG));
				byte[] emotImgLrg = cr.getBlob(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_IMG_LARGE));
				emot_img = EmoticonDBHelper.getEmot(emotImg, emotImgLrg, emot_hash);
			}
			cr.close();
			//Set to some default if not found
			if(emot_img == null){
				Log.i(TAG, "Emoticon not found !!!");
				emot_img = BitmapFactory.decodeResource(EmotApplication.getAppContext().getResources(), R.drawable.ic_question);
			}
			emotCache.put(emot_hash, emot_img);
		}else{
			Log.i(TAG, "emot image from hash");
		}
		return emot_img;
	}

}
