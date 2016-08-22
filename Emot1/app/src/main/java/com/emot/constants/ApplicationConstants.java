package com.emot.constants;

public  final class ApplicationConstants {
	
	public static final String VERIFICATION_SALT = "37&#^%@YDK!";
	public static final String HTTP_POST = "POST";
	public static final String EMOT_TAGGER_START = "<etag>";
	public static final String EMOT_TAGGER_END = "</etag>";
	
	//Broadcast Constants
	
	public static final String WENT_ON_STOP = "wentOnStop";
	public static final String APP_ID = "cebd2311a0924dada381676";
	public static final String ACCESS_TOKEN = "80517d2323285af3fc58151eb4a6db3f4e919e42";
	public static final String HOME_PRESSED = "homePressed";
	public static final String GOING_AWAY = "goingAway";
	public static final String USER_STATUS_CHANGED = "userStatusChanged";
	public static final String IS_GOING_TO_ANOTHER_APP_SCREEN = "isGoingToAnotherAppScreen";


	public static String BASE_URL = "https://www.cognalys.com/api/v2/request_missed_call/";
	public static String BASE_URL2 ="https://www.cognalys.com/api/v2/confirm_verification/";
	public static String ONE = "MISSING CREDENTIALS";
	public static String TWO = "MISSING REQUIRED VALUES";
	public static String THREE = "MISSING PROPER NUMBER";
	public static String FOUR = "VERIFICATION SUCCESS";
	public static String FIVE="NUMBER IS NOT CORRECT";
	public static String SIX="MOBLIE NUMBER VERIFICATION CANCELED";
	public static String SEVEN="NETWORK ERROR CANNOT BE VERIFIED";
	public static String EIGHT="MOBLIE NUMBER VERIFICATION FAILED, NO INTERNET";

	//GroupChat
	public static final String GROUP_CHAT_DOMAIN = "@conference.localhost";
	public static final String CHAT_DOMAIN = "@localhost";


}
