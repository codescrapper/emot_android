package com.emot.androidclient.util;

public class Log {
	public static boolean IS_DEBUG = true;
	
	public static void i(String tag, String text){
		if(IS_DEBUG){
			android.util.Log.i(tag, text);
		}
	}
	
	public static void d(String tag, String text){
		if(IS_DEBUG){
			android.util.Log.d(tag, text);
		}
	}
	
	public static void e(String tag, String text){
		if(IS_DEBUG){
			android.util.Log.d(tag, text);
		}
	}
	
	public static void w(String tag, String text){
		if(IS_DEBUG){
			android.util.Log.w(tag, text);
		}
	}
}
