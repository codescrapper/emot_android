package com.emot.androidclient.util;

import java.math.BigInteger;
import java.security.SecureRandom;

public class EmotUtils {

	public static String generateRoomID(){
		SecureRandom random = new SecureRandom();


		return new BigInteger(130, random).toString(32);


	}

}
