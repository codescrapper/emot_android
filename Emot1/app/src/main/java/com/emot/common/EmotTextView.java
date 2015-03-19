package com.emot.common;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import com.emot.androidclient.util.Log;
import android.widget.TextView;

import com.emot.androidclient.util.EmotUtils;
import com.emot.constants.ApplicationConstants;
import com.emot.model.EmotApplication;
import com.emot.persistence.DBContract;
import com.emot.persistence.EmoticonDBHelper;
import com.emot.screens.R;

public class EmotTextView extends TextView {

	private static final String TAG = EmotTextView.class.getSimpleName();
	private PlaceEmot placeEmotTask;
	private Spannable emotText = null;

	public EmotTextView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	public EmotTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EmotTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public void setText(CharSequence text, BufferType type) {
		
//		SpannableStringBuilder builder = new SpannableStringBuilder(text);
//		super.setText((Spannable)builder, type);
		Log.i(TAG, "EMOT TEXT = "+emotText + " TEXT = "+text);
		updateEmots(text, type);
//		if(emotText==null || emotText.toString().equals("")){
//			Log.i(TAG, "starting text = "+text);
//			super.setText(text, type);
//			placeEmotTask = new PlaceEmot(text, type);
//			placeEmotTask.execute();
//			
//		}else{
//			super.setText(emotText, type);
//		}
	}
	
	public void updateEmots(CharSequence text, BufferType bufferType){
		Spannable spannable = new SpannableStringBuilder(text);
		Log.i(TAG, "String = " + spannable);
		//Log.i(TAG, "len = "+spannable.length());
		int len = spannable.length();
		int curr = 0;
		boolean findStartTag = true;
		int startFound = 0;
		while(curr < len-ApplicationConstants.EMOT_TAGGER_START.length()){
			if(findStartTag){
				String foundStr = "";
			    for(int j=0; j<ApplicationConstants.EMOT_TAGGER_START.length(); j++){
			    	foundStr = foundStr + spannable.charAt(curr+j);
			    }
				//Log.i(TAG, "start Found string = "+foundStr);
				if(foundStr.equals(ApplicationConstants.EMOT_TAGGER_START)){
					startFound = curr;
					curr = curr + ApplicationConstants.EMOT_TAGGER_START.length() - 1;
					findStartTag = false;
				}else{
					curr++;
				}
			}else{
				String foundStr = "";
				for(int j=0; j<ApplicationConstants.EMOT_TAGGER_END.length(); j++){
			    	foundStr = foundStr + spannable.charAt(curr+j);
			    }
				//Log.i(TAG, "end Found string = "+foundStr);
				if(foundStr.equals(ApplicationConstants.EMOT_TAGGER_END)){
					int endFound = curr + ApplicationConstants.EMOT_TAGGER_END.length();
					findStartTag = true;
					//Log.i(TAG, "start - end : "+ spannable.charAt(startFound) + spannable.charAt(endFound-1));
					//DB QUERY TO GET IMAGE
					String emot_hash = spannable.subSequence(startFound + ApplicationConstants.EMOT_TAGGER_START.length(), endFound - ApplicationConstants.EMOT_TAGGER_END.length()).toString();
					Bitmap emot_img = EmoticonDBHelper.getEmotImg(emot_hash);
					replaceWithEmot(spannable, startFound, endFound, emot_img);
					curr = curr+6;
				}else{
					curr++;
				}
			}
		}
		
		this.emotText = spannable;
		Log.i(TAG, "SETTING emotext = "+this.emotText + " .For text = "+text);
		super.setText(spannable, bufferType);
		//Log.i(TAG, "Updating emots in emottextview ");
	}
	
	public void replaceWithEmot(Spannable spannable, int start, int end, Bitmap emot){
		//Log.i(TAG, "Spanneblae "+spannable);
        boolean set = true;
        for (ImageSpan span : spannable.getSpans(start, end, ImageSpan.class)){
            if (spannable.getSpanStart(span) >= start && spannable.getSpanEnd(span) <= end){
                spannable.removeSpan(span);
            }else {
                set = false;
                break;
            }
        }
        if (set) {
            spannable.setSpan(new ImageSpan(EmotApplication.getAppContext(), emot), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
	}
	
	
	public class PlaceEmot extends AsyncTask<Void, Void, Spannable>{
		private CharSequence text;
		private BufferType bufferType;
		
		public PlaceEmot(CharSequence text, BufferType bufferType){
			this.text = text;
			this.bufferType = bufferType;
		}
		
		@Override
		protected Spannable doInBackground(Void... params) {
			SpannableStringBuilder builder = new SpannableStringBuilder(text);
			updateEmots(builder);
			Log.i(TAG, "thread text = "+builder.toString());
			return builder;
		}
		
		public void updateEmots(Spannable spannable){
			//Log.i(TAG, "String = " + spannable);
			//Log.i(TAG, "len = "+spannable.length());
			int len = spannable.length();
			int curr = 0;
			boolean findStartTag = true;
			int startFound = 0;
			while(curr < len-ApplicationConstants.EMOT_TAGGER_START.length()){
				if(findStartTag){
					String foundStr = "";
				    for(int j=0; j<ApplicationConstants.EMOT_TAGGER_START.length(); j++){
				    	foundStr = foundStr + spannable.charAt(curr+j);
				    }
					//Log.i(TAG, "start Found string = "+foundStr);
					if(foundStr.equals(ApplicationConstants.EMOT_TAGGER_START)){
						startFound = curr;
						curr = curr + ApplicationConstants.EMOT_TAGGER_START.length() - 1;
						findStartTag = false;
					}else{
						curr++;
					}
				}else{
					String foundStr = "";
					for(int j=0; j<ApplicationConstants.EMOT_TAGGER_END.length(); j++){
				    	foundStr = foundStr + spannable.charAt(curr+j);
				    }
					//Log.i(TAG, "end Found string = "+foundStr);
					if(foundStr.equals(ApplicationConstants.EMOT_TAGGER_END)){
						int endFound = curr + ApplicationConstants.EMOT_TAGGER_END.length();
						findStartTag = true;
						//Log.i(TAG, "start - end : "+ spannable.charAt(startFound) + spannable.charAt(endFound-1));
						//DB QUERY TO GET IMAGE
						String emot_hash = spannable.subSequence(startFound + ApplicationConstants.EMOT_TAGGER_START.length(), endFound - ApplicationConstants.EMOT_TAGGER_END.length()).toString();
						Bitmap emot_img = EmoticonDBHelper.getEmotImg(emot_hash);
						replaceWithEmot(spannable, startFound, endFound, emot_img);
						curr = curr+6;
					}else{
						curr++;
					}
				}
			}
			//Log.i(TAG, "Updating emots in emottextview ");
		}
		
		public void replaceWithEmot(Spannable spannable, int start, int end, Bitmap emot){
			//Log.i(TAG, "Spanneblae "+spannable);
	        boolean set = true;
	        for (ImageSpan span : spannable.getSpans(start, end, ImageSpan.class)){
	            if (spannable.getSpanStart(span) >= start && spannable.getSpanEnd(span) <= end){
	                spannable.removeSpan(span);
	            }else {
	                set = false;
	                break;
	            }
	        }
	        if (set) {
	            spannable.setSpan(new ImageSpan(EmotApplication.getAppContext(), emot), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        }
		}
		
		@Override
		protected void onPostExecute(Spannable result) {
			//super.onPostExecute(result);
			Log.i(TAG, "emot text = "+this.text+" span = "+result);
			EmotTextView.this.emotText = result;
			EmotTextView.super.setText(result, this.bufferType);
		}
		
	}

}
