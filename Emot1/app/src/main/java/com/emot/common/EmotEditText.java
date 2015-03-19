package com.emot.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import com.emot.androidclient.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.emot.constants.ApplicationConstants;
import com.emot.model.Emot;
import com.emot.model.EmotApplication;
import com.emot.persistence.DBContract;
import com.emot.persistence.EmoticonDBHelper;
import com.emot.screens.R;

public class EmotEditText extends EditText {

	private static final String TAG = EmotEditText.class.getSimpleName();

	private static final String COLON_REPLACEMENT = "zyx";

	//Suggestion views
	
	private LinearLayout emotSuggestionLayout;
	private LinearLayout emotRecentLayout;
	private View scrollEmotSuggestionLayout;
	private View scrollEmotRecentLayout;
	private View toggleLastEmot;
	private boolean recent_emot_clicked = false;
	
	private HashMap<String, View> suggestedEmots = new HashMap<String, View>();
	Stack<Integer> lastEmotIndex = new Stack<Integer>();

	public EmotEditText(Context context) {
		super(context);
	}

	public EmotEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EmotEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void replaceWithEmot(int start, int end, Bitmap emot){
		Spannable spannable = getText();
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
        Log.i(TAG, "len2 = "+spannable.length());
	}
	
	public boolean isLastEmotLocation(int loc){
		if(lastEmotIndex.isEmpty()){
			return false;
		}
		return loc==lastEmotIndex.peek();
	}
	
	
	public void updateEmots(CharSequence text, int startPoint){
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
					Log.i(TAG, "Replacing with emoticon !!!");
					replaceWithEmot(startPoint+startFound, startPoint+endFound, emot_img);
					curr = curr+6;
				}else{
					curr++;
				}
			}
		}
		
		Log.i(TAG, "For text = "+text);
		//Log.i(TAG, "Updating emots in emottextview ");
	}
	
	private Activity context;
	
	public void addEmot(Emot emot){
		try{
			Spannable spannable = getText();
			int start = getSelectionStart();
			
			Log.i(TAG, "len1 = "+start + " String = "+getText().toString());
			String appendString = ApplicationConstants.EMOT_TAGGER_START + emot.getEmotHash() + ApplicationConstants.EMOT_TAGGER_END;
			if(start>0 && getText().charAt(start-1) != ' '){
				while(start>0 && getText().charAt(start-1) != ' ' && !isLastEmotLocation(start-1)){
					Log.i(TAG, "@@@@@@@@@ len1 = "+start + " String = "+getText().toString());
					start--;
					dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				}
				//setText(getText().toString().substring(0, start));
				//setSelection(getText().length());
			}
			int end = getSelectionEnd();
			
			Log.i(TAG, "111 " + getText().toString());
			getText().replace(Math.min(start, end), Math.max(start, end), appendString, 0, appendString.length());
			Log.i(TAG, "222 " + getText().toString());
			//spannable.setSpan(new ImageSpan(EmotApplication.getAppContext(), emot), start+1, start+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			replaceWithEmot(start, start+appendString.length(), emot.getEmotImg());
			lastEmotIndex.push(getText().length()-1);
			Log.i(TAG, "333 " + getText().toString());
		}catch(Exception e){
			//e.printStackTrace();
		}
		
	}

	public void addSmiles(Context context, Spannable spannable) {
		//spannable.setSpan(new ImageSpan(context, R.drawable.blank_user_image),0, 0,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    boolean hasChanges = false;
	    Map<Pattern, Integer> emoticons = new HashMap<Pattern, Integer>();
	    emoticons.put(Pattern.compile(Pattern.quote(":)")), R.drawable.blank_user_image);
	    for (Entry<Pattern, Integer> entry : emoticons.entrySet()) {
	        Matcher matcher = entry.getKey().matcher(spannable);
	        while (matcher.find()) {
	            boolean set = true;
	            for (ImageSpan span : spannable.getSpans(matcher.start(),
	                    matcher.end(), ImageSpan.class))
	                if (spannable.getSpanStart(span) >= matcher.start()
	                        && spannable.getSpanEnd(span) <= matcher.end())
	                    spannable.removeSpan(span);
	                else {
	                    set = false;
	                    break;
	                }
	            if (set) {
	                hasChanges = true;
	                spannable.setSpan(new ImageSpan(context, entry.getValue()),
	                        matcher.start(), matcher.end(),
	                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	            }
	            Log.i(TAG, "Matcher start = " + matcher.start() + " end = "+matcher.end());
	        }
	    }
	}
	
	public void setContext(Context ctx){
	
		if(ctx instanceof Activity){
			context = (Activity)ctx;
		}
	}
	
	public void setEmotSuggestBox(View view){
		emotSuggestionLayout = (LinearLayout) view.findViewById(R.id.viewEmotSuggestionLayout);
		emotRecentLayout = (LinearLayout) view.findViewById(R.id.viewEmotRecentLayout);
		toggleLastEmot = view.findViewById(R.id.buttonRecentEmots);
		scrollEmotSuggestionLayout = view.findViewById(R.id.scrollEmotSuggestionLayout);
		scrollEmotRecentLayout = view.findViewById(R.id.scrollEmotRecentLayout);
		
		toggleLastEmot.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(scrollEmotRecentLayout.getVisibility()==View.VISIBLE){
					Log.i(TAG, "opening suggestion layout");
					scrollEmotRecentLayout.setVisibility(View.GONE);
					scrollEmotSuggestionLayout.setVisibility(View.VISIBLE);
					toggleLastEmot.setBackgroundColor(EmotApplication.getAppContext().getResources().getColor(R.color.white));
				}else{
					Log.i(TAG, "opening recent layout");
					
					//Remove if condition to get latest emots every time
					//if(emotRecentLayout.getChildCount()<=0){
						emotRecentLayout.removeAllViews();
						
						String selection = DBContract.EmotsDBEntry.LAST_USED + " != ''";
						Cursor cr = EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getReadableDatabase().query(
								DBContract.EmotsDBEntry.TABLE_NAME, 
								new String[] {DBContract.EmotsDBEntry.EMOT_IMG, DBContract.EmotsDBEntry.EMOT_HASH, DBContract.EmotsDBEntry.EMOT_IMG_LARGE}, 
								selection, 
								null, 
								null, 
								null, 
								DBContract.EmotsDBEntry.LAST_USED + " desc", 
								"20"
						);
						Log.i(TAG, "count = "+cr.getCount());
						while (cr.moveToNext())
						{
							String hash = cr.getString(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_HASH));
							byte[] emotImg = cr.getBlob(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_IMG));
							byte[] emotImgLrg = cr.getBlob(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_IMG_LARGE));
							Log.i(TAG, "emot img "+emotImg);
							Log.i(TAG, "Emot hash is "+hash);
							Bitmap emotBmp = EmoticonDBHelper.getEmot(emotImg, emotImgLrg, hash);
							Emot emot = new Emot(hash, emotBmp);
							View view = getEmotSuggestView(emot);
							emotRecentLayout.addView(view);
						}
						cr.close();
					//}
						
					scrollEmotRecentLayout.setVisibility(View.VISIBLE);
					scrollEmotSuggestionLayout.setVisibility(View.GONE);
					toggleLastEmot.setBackgroundColor(EmotApplication.getAppContext().getResources().getColor(R.color.list_row_pressed_bg));
				}
				
			}
		});
	}
	
	public void refillSuggestedEmots(){
		emotSuggestionLayout.removeAllViews();
	}
	
	public void clearSuggestion(){
		emotSuggestionLayout.removeAllViews();
		suggestedEmots.clear();
	}
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if(before==0){
			updateEmots(s.subSequence(start, start+count), start);
		}
		Log.i(TAG, "Chnage : "+s+" start = "+start + " before = "+before + " count = "+count );
		if(emotSuggestionLayout==null){
			super.onTextChanged(s, start, before, count);
			return;
		}
		
		String txt = s.toString();
		int lastSpace = txt.lastIndexOf(" ");
		if(txt.length()==0)
			return;
		if(lastSpace==-1)
			lastSpace = 0;
		String lastWord;
		if(!lastEmotIndex.isEmpty() && lastEmotIndex.peek()>txt.length()){
			lastEmotIndex.pop();
		}
		Log.i(TAG, "text = "+txt + " length="+txt.length());
		
		int findFrom = 0;
		if(!lastEmotIndex.isEmpty() && lastSpace<lastEmotIndex.peek()){
			if(txt.length()-1<lastEmotIndex.peek()){
				return;
			}
			Log.i(TAG, "lastEmotIndex = "+lastEmotIndex.peek());
			findFrom = lastEmotIndex.peek() + 1;
		}else{
			if(lastSpace==0){
				findFrom = lastSpace;
			}else{
				findFrom = lastSpace + 1;
			}
			
		}
		lastWord = txt.substring(findFrom);
		Log.i(TAG, "lastWord = "+lastWord);
		
		//Start emoticon suggestion only if word length greater than 3
		if(lastWord.length()>2 || (lastWord.length()>1 && lastWord.startsWith(":"))){
			//Applying different technique for stupid android phones
			String newLastWord = lastWord.replaceAll(":", COLON_REPLACEMENT);
			new Thread(new UpdateRunnable(newLastWord)).start();
			
		}
		
		//Log.i(TAG, "Text changed 222" + s.toString());
		
		//Showing suggested emots on text change
		if(!recent_emot_clicked){
			scrollEmotRecentLayout.setVisibility(View.GONE);
			scrollEmotSuggestionLayout.setVisibility(View.VISIBLE);
			toggleLastEmot.setBackgroundColor(EmotApplication.getAppContext().getResources().getColor(R.color.white));
		}
		recent_emot_clicked = false;
	}
	
	private class UpdateRunnable implements Runnable{
		private String lastWord;
		public UpdateRunnable(String lastWord){
			this.lastWord = lastWord;
		}
		
		public void run() {
			Log.i(TAG, "Matching last word : "+lastWord);
			Cursor cr = EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getReadableDatabase().query(
					DBContract.EmotsDBEntry.TABLE_NAME, 
					new String[] {DBContract.EmotsDBEntry.EMOT_IMG, DBContract.EmotsDBEntry.EMOT_HASH, DBContract.EmotsDBEntry.EMOT_IMG_LARGE}, 
					DBContract.EmotsDBEntry.TAGS+" match '"+lastWord+"*';", 
					null, null, null, null, null
			);
			while (cr.moveToNext())
			{
				String hash = cr.getString(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_HASH));
				byte[] emotImg = cr.getBlob(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_IMG));
				byte[] emotImgLrg = cr.getBlob(cr.getColumnIndex(DBContract.EmotsDBEntry.EMOT_IMG_LARGE));
				Log.i(TAG, "emot img "+emotImg);
				Log.i(TAG, "Emot hash is "+hash);
				Bitmap emotBmp = EmoticonDBHelper.getEmot(emotImg, emotImgLrg, hash);
				Emot emot = new Emot(hash, emotBmp);
				Log.i(TAG, "Is instance of chatscreen");
				context.runOnUiThread(new UiUpdateRunnable(emot));
			}
			cr.close();
		}
		
		private class UiUpdateRunnable implements Runnable{

			private Emot emot;
			public UiUpdateRunnable(Emot emot){
				this.emot = emot;
			}
			
			@Override
			public void run() {

				View view = getEmotSuggestView(emot);
				
				if(!suggestedEmots.containsKey(emot.getEmotHash())){
					EmotEditText.this.emotSuggestionLayout.addView(view, 0);
					suggestedEmots.put(emot.getEmotHash(), view);
					Log.i(TAG, "adding view = "+view);
				}else{
					View oldView = suggestedEmots.get(emot.getEmotHash());
					Log.i(TAG, "old view = "+oldView);
					EmotEditText.this.emotSuggestionLayout.removeView(oldView);
					EmotEditText.this.emotSuggestionLayout.addView(view, 0);
					suggestedEmots.put(emot.getEmotHash(), view);
				}
				Log.i(TAG, "Emoticon added");
			
			}
			
		}
	}
	
	private View getEmotSuggestView(final Emot emot){
		View emotView = context.getLayoutInflater().inflate(R.layout.emot_suggestion_view, null);

		ImageView iView = (ImageView)emotView.findViewById(R.id.icon_emot);
		iView.setImageBitmap(emot.getEmotImg());
		iView.setDrawingCacheEnabled(true);
		emotView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.i(TAG, "Image clicked !!!");
				recent_emot_clicked = true;
				EmotEditText.this.addEmot(emot);
				ContentValues values = new ContentValues();
				int time = (int) (System.currentTimeMillis());
				values.put(DBContract.EmotsDBEntry.LAST_USED, time);
				EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getReadableDatabase().update(
						DBContract.EmotsDBEntry.TABLE_NAME, 
						values, 
						DBContract.EmotsDBEntry.EMOT_HASH+"='"+emot.getEmotHash()+"'", 
						null
				);
			}
		});
		
		return emotView;
	}

}
