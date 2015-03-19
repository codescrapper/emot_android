package com.emot.constants;

public final class WebServiceConstants {

	public static final String HTTP = "http";
	public static final String SERVER_IP = "ec2-54-165-232-122.compute-1.amazonaws.com";
	//public static final String CHAT_SERVER = "ec2-54-85-148-36.compute-1.amazonaws.com";
	public static final String CHAT_SERVER = "emot-426416851.us-east-1.elb.amazonaws.com";
	public static final int CHAT_PORT = 5222; 
	//public static final String SERVER_PORT = "8000";
	public static final String OP_GET = "GET";
	public static final String OP_POST = "POST";
	public static final String PATH_API = "/api";
	public static final String OP_REGISTER = "/register/";
	public static final String OP_SETCODE = "/setcode/";
	public static final String OP_GETCONTACT = "/getemotters/";
	public static final String GET_QUERY = "?";
	public static final String DEVICE_TYPE = "mobile";
	public static final String CHAT_DOMAIN = "emot-net";


	public final class WSRegisterParamConstants{

		public static final String REQUEST = "request";
		public static final String MOBILE = "mobile";
		public static final String APP_VERSION = "app_version";
		public static final String CLIENT_OS = "client_os";
		public static final String VERIFICATION_CODE = "code";
		public static final String S = "s";
		public static final String HASH = "hash";

	}



}
