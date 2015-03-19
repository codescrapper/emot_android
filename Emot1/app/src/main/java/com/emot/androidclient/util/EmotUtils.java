package com.emot.androidclient.util;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import com.emot.androidclient.util.Log;

import com.emot.constants.ApplicationConstants;
import com.emot.model.EmotApplication;

public class EmotUtils {

	private static final String TAG = EmotUtils.class.getSimpleName();
	private static int DIVISION_FACTOR = 16;

	public static String generateRoomID(){
		SecureRandom random = new SecureRandom();
		return new BigInteger(130, random).toString(32);
	}

	
	public static String getTimeSimple() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = new Date();
		return getTimeSimple(dateFormat.format(date));
	}
	
	public static String getTimeSimple(String dt){
		try {
			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date inputDate;
			inputDate = fmt.parse(dt);
			
			Calendar smsTime = Calendar.getInstance();
		    smsTime.setTimeInMillis(inputDate.getTime());
		    Calendar now = Calendar.getInstance();
		    final String timeFormatString = "h:mm aa";
		    final String dateTimeFormatString = "EEEE, MMMM d, h:mm aa";
		    final long HOURS = 60 * 60 * 60;
		    if(now.get(Calendar.DATE) == smsTime.get(Calendar.DATE) ){
		        return "Today " + DateFormat.format(timeFormatString, smsTime);
		    }else if(now.get(Calendar.DATE) - smsTime.get(Calendar.DATE) == 1 ){
		        return "Yesterday " + DateFormat.format(timeFormatString, smsTime);
		    }else if(now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)){
		        return DateFormat.format(dateTimeFormatString, smsTime).toString();
		    }else
		        return DateFormat.format("MMMM dd yyyy, h:mm aa", smsTime).toString();
		} catch (ParseException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static byte[] resizeEmoticon(byte[] img){
		Bitmap inputImg = BitmapFactory.decodeByteArray(img , 0, img.length);
		int size = getEmoticonSize();
		Bitmap bmp = Bitmap.createScaledBitmap(inputImg, size, size, false);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
		byte outputImage[] = stream.toByteArray();
		return outputImage;
	}
	
	private static int getEmoticonSize(){
		int size = 0;
		size = EmotApplication.getValue(PreferenceConstants.EMOTICON_SIZE, 0);
		if(size==0){
			DisplayMetrics metrics = EmotApplication.getAppContext().getResources().getDisplayMetrics();
			int width = metrics.widthPixels;
			int height = metrics.heightPixels;
			Log.i(TAG, "width = "+width + " height = "+height);
			size = width/DIVISION_FACTOR;
			EmotApplication.setValue(PreferenceConstants.EMOTICON_SIZE, size);
		}
		return size;
	}
	
	public static String replaceTag(String message){
		String new_message = "";
		int sindx = message.indexOf(ApplicationConstants.EMOT_TAGGER_START);
		while(sindx >= 0){
			new_message = new_message + message.substring(0, sindx);
			new_message = new_message + "☐";
			int eindx = message.indexOf(ApplicationConstants.EMOT_TAGGER_END) + ApplicationConstants.EMOT_TAGGER_END.length();
			message = message.substring(eindx, message.length());
			
			sindx = message.indexOf(ApplicationConstants.EMOT_TAGGER_START);
		}
		new_message = new_message + message;
		return new_message;
	}
}
