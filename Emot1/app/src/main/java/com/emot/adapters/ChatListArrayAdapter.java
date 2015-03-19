package com.emot.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import com.emot.androidclient.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.emot.common.EmotTextView;
import com.emot.emotobjects.ChatMessage;
import com.emot.screens.R;


public class ChatListArrayAdapter extends ArrayAdapter<ChatMessage> {  
	private Activity mContext;  
	private ArrayList<ChatMessage> mList;
	private LinearLayout mMessageContainer;
	//private RelativeLayout mMessageContainer;
	private ChatMessage mChatMessage;
	private LayoutInflater mLayoutInflater = null;  
	private static final String TAG = ChatListArrayAdapter.class.getSimpleName();
	
	public ChatListArrayAdapter(Activity context, int resource, ArrayList<ChatMessage> list) { 
		super(context, resource, list);
		mContext = context;  
		mList = list;  
		mLayoutInflater = (LayoutInflater) mContext  
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
	}  
	@Override  
	public int getCount() {  
		return mList.size();  
	}  
	@Override  
	public ChatMessage getItem(int pos) {  
		return mList.get(pos);  
	}  
	@Override  
	public long getItemId(int position) {  
		return position;  
	}  
	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {  
		View v = convertView;  

		CompleteListViewHolder viewHolder;  
		//Log.i("ChatListArrayAdapter", "convertView " +convertView);
		//mMessageContainer.setGravity(Gravity.RIGHT);
		mChatMessage = mList.get(position);

		//  if (convertView != null) {  

		//              LayoutInflater li = (LayoutInflater) mContext  
		//                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
		
		//Check for view already inflated
		
		if(v==null) {
			LayoutInflater li = LayoutInflater.from(this.getContext());
			//LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
			v = li.inflate(R.layout.chat_row, null); 
		}
		
		
		
		/*
		LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
		if(!mChatMessage.isRight() ){
			v = li.inflate(R.layout.chat_row_left, null); 
			viewHolder = new CompleteListViewHolder(v, true); 
		}else{
			v = li.inflate(R.layout.chat_row, null); 
			viewHolder = new CompleteListViewHolder(v, false); 
		}
		*/
		
		viewHolder = new CompleteListViewHolder(v);
		v.setTag(viewHolder); 
		if(mChatMessage.isRight() ){
			//Log.i(TAG, "IS RIGHT = "+mChatMessage.getmMessage());
			viewHolder.chatBoxLeft.setVisibility(View.GONE);
			viewHolder.chatBoxRight.setVisibility(View.VISIBLE);
			viewHolder.mChatTextRight.setText(mChatMessage.getmMessage());
			viewHolder.mDateTimeRight.setText(mChatMessage.getmTime());
		}else{
			Log.i(TAG, "IS NOT RIGHT = "+mChatMessage.getmMessage());
			viewHolder.chatBoxRight.setVisibility(View.GONE);
			viewHolder.chatBoxLeft.setVisibility(View.VISIBLE);
			viewHolder.mChatTextLeft.setText(mChatMessage.getmMessage(), TextView.BufferType.SPANNABLE);
			viewHolder.mDateTimeLeft.setText(mChatMessage.getmTime());
		}



		//   } //else {  
		//  viewHolder = (CompleteListViewHolder) v.getTag();  
		// }
		//  if(!mChatMessage.isRight() ){
		//Log.i("ChatListArrayAdapter", mChatMessage.getmTime() +"Left Aigned");

		//   viewHolder.mChatTextRight.setText(mChatMessage.getmMessage());
		//    viewHolder.mDateTimeRight.setText(mChatMessage.getmTime());
		// viewHolder.mDateTime.setGravity(Gravity.RIGHT);
		//  }else{
		//Log.i("ChatListArrayAdapter", mChatMessage.getmMessage() + "Right Aigned");



		//mMessageContainer.setGravity(Gravity.RIGHT); 
		// viewHolder.mChatTextRight.setVisibility(View.VISIBLE);
		//  viewHolder.mChatText.setVisibility(View.GONE);
		//	  android.widget.RelativeLayout.LayoutParams params =(RelativeLayout.LayoutParams)viewHolder.mChatText.getLayoutParams();
		// params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		// params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		//params.addRule(RelativeLayout.ALIGN_PARENT_TOP);

		// viewHolder.mDateTime.setGravity(Gravity.RIGHT);
		//  }





		return v;  
	}  
}  
class CompleteListViewHolder {  
	public EmotTextView mChatTextLeft; 
	public View chatBoxLeft;
	public TextView mDateTimeLeft;
	
	public EmotTextView mChatTextRight; 
	public View chatBoxRight;
	public TextView mDateTimeRight;
	
	public CompleteListViewHolder(View base) {  
		chatBoxLeft = base.findViewById(R.id.messageContainerLeft);
		mDateTimeLeft = (TextView)base.findViewById(R.id.chatDateLeft);
		mChatTextLeft = (EmotTextView) base.findViewById(R.id.chatContentLeft); 
		
		chatBoxRight = base.findViewById(R.id.messageContainerRight);
		mDateTimeRight = (TextView)base.findViewById(R.id.chatDateRight);
		mChatTextRight = (EmotTextView) base.findViewById(R.id.chatContentRight);
	}


}  