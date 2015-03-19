package com.emot.persistence;

import android.provider.BaseColumns;

//cannot be subclassed
public final class DBContract {

	public DBContract(){}

	public static abstract class EmotHistoryEntry implements BaseColumns{

		public static final String TABLE_NAME = "emothistory";
		public static final String ENTRY_ID = "mobile";
		public static final String DATETIME = "date";
		public static final String EMOTS = "emots";
		public static final String EMOT_LOCATION = "emotlocation";
	}
	
	public static abstract class GroupEmotHistoryEntry implements BaseColumns{
		
		public static final String TABLE_NAME = "group_emot_history";
		public static final String GROUP_NAME = "group_name";
        public static final String DATETIME = "date";
        public static final String ENTRY_ID = "mobile";
        public static final String EMOTS = "emots";
        public static final String EMOT_LOCATION = "emotlocation";
		
		
	}

	public static abstract class EmotsDBEntry implements BaseColumns{

		public static final String TABLE_NAME = "emots";
		public static final String EMOT_IMG_LARGE = "emot_img_large";
		public static final String EMOT_IMG = "emot_img";
		public static final String TAGS = "tags";
		public static final String EMOT_HASH = "emot_hash";
		public static final String LAST_USED = "last_used";
	}

}
