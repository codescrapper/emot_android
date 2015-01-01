package com.emot.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.view.ViewPager.LayoutParams;
import android.text.Spannable;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.emot.androidclient.util.EmotUtils;
import com.emot.constants.ApplicationConstants;
import com.emot.model.Emot;
import com.emot.model.EmotApplication;
import com.emot.persistence.DBContract;
import com.emot.persistence.EmoticonDBHelper;
import com.emot.screens.R;

public class EmotEditText extends EditText {

	private static final String TAG = EmotEditText.class.getSimpleName();
	private UpdateEmotSuggestions updateEmotTask;

	//Suggestion views
	
	private LinearLayout emotSuggestionLayout;
	private LinearLayout emotRecentLayout;
	private View scrollEmotSuggestionLayout;
	private View scrollEmotRecentLayout;
	private View toggleLastEmot;
	private boolean recent_emot_clicked = false;
	
	private HashMap<String, Boolean> suggestedEmots = new HashMap<String, Boolean>();
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
	
	public void addEmot(Emot emot){
		Spannable spannable = getText();
		int start = spannable.length();
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
		
		Log.i(TAG, "111 " + getText().toString());
		append(appendString);
		Log.i(TAG, "222 " + getText().toString());
		//spannable.setSpan(new ImageSpan(EmotApplication.getAppContext(), emot), start+1, start+2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		replaceWithEmot(start, start+appendString.length(), emot.getEmotImg());
		lastEmotIndex.push(getText().length()-1);
		Log.i(TAG, "333 " + getText().toString());
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
							final Emot emot = new Emot(hash, emotBmp);
							ImageView view = new ImageView(EmotApplication.getAppContext());
							view.setId(0);
							RelativeLayout.LayoutParams params = getEmotParams();
							view.setLayoutParams(params);
							view.setImageBitmap(emot.getEmotImg());
							view.setDrawingCacheEnabled(true);
							view.setOnClickListener(new OnClickListener() {

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
							emotRecentLayout.addView(view);
						}
						cr.close();
					//}
						
					scrollEmotRecentLayout.setVisibility(View.VISIBLE);
					scrollEmotSuggestionLayout.setVisibility(View.GONE);
					toggleLastEmot.setBackgroundColor(EmotApplication.getAppContext().getResources().getColor(R.color.dark_grey));
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
		
		if(!lastEmotIndex.isEmpty() && lastSpace<lastEmotIndex.peek()){
			if(txt.length()-1<lastEmotIndex.peek()){
				return;
			}
			Log.i(TAG, "lastEmotIndex = "+lastEmotIndex.peek());
			lastWord = txt.substring(lastEmotIndex.peek());
		}else{
			lastWord = txt.substring(lastSpace);
		}
		Log.i(TAG, "lastWord = "+lastWord);
		
		//Start emoticon suggestiononly if word length greater than 3
		if(lastWord.length()>2){
			if(updateEmotTask==null){
				updateEmotTask = new UpdateEmotSuggestions(lastWord);
				updateEmotTask.execute();
			}else{
				updateEmotTask.cancel(true);
				updateEmotTask = new UpdateEmotSuggestions(lastWord);
				updateEmotTask.execute();
			}
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
	
	public class UpdateEmotSuggestions extends AsyncTask<Void, Emot, Void>{
		private String text;

		public UpdateEmotSuggestions(String s){
			this.text = s;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Cursor cr = EmoticonDBHelper.getInstance(EmotApplication.getAppContext()).getReadableDatabase().query(
					DBContract.EmotsDBEntry.TABLE_NAME, 
					new String[] {DBContract.EmotsDBEntry.EMOT_IMG, DBContract.EmotsDBEntry.EMOT_HASH, DBContract.EmotsDBEntry.EMOT_IMG_LARGE}, 
					DBContract.EmotsDBEntry.TAGS+" match '"+text+"*';", 
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
				publishProgress(emot);
			}
			cr.close();
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Emot... values) {
			final Emot emot = values[0];
			ImageView view = new ImageView(EmotApplication.getAppContext());
			view.setId(0);
			RelativeLayout.LayoutParams params = getEmotParams();
			view.setLayoutParams(params);
			view.setImageBitmap(emot.getEmotImg());
			view.setDrawingCacheEnabled(true);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.i(TAG, "Image clicked !!!");
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
			if(!suggestedEmots.containsKey(emot.getEmotHash())){
				EmotEditText.this.emotSuggestionLayout.addView(view, 0);
				suggestedEmots.put(emot.getEmotHash(), true);
			}
		}

	}
	
	private RelativeLayout.LayoutParams getEmotParams(){
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.setMargins(40, 0, 40, 0);
		return params;
	}

}
