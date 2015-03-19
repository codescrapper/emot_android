package com.emot.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import com.emot.androidclient.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.emot.emotobjects.ChatMessage;
import com.emot.screens.R;

public class GroupChatListArrayAdapter extends BaseAdapter {  
	private Activity mContext;  
	private List<ChatMessage> mList;
	private LinearLayout mMessageContainer;
	//private RelativeLayout mMessageContainer;
	private ChatMessage mChatMessage;
	private LayoutInflater mLayoutInflater = null;  
	public GroupChatListArrayAdapter(Activity context, List<ChatMessage> list) {  
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
	public Object getItem(int pos) {  
		return mList.get(pos);  
	}  
	@Override  
	public long getItemId(int position) {  
		return position;  
	}  
	@Override  
	public View getView(int position, View convertView, ViewGroup parent) {  
		View v = convertView;  
		ListViewHolder viewHolder;  
		Log.i("GroupChatListArrayAdapter", "convertView " +convertView);
		//mMessageContainer.setGravity(Gravity.RIGHT);
		mChatMessage = mList.get(position);

		//  if (convertView != null) {  
		LayoutInflater li = (LayoutInflater) mContext  
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
		if(!mChatMessage.isRight() ){
			v = li.inflate(R.layout.grp_chat_row_left, null); 
			viewHolder = new ListViewHolder(v, true);  
			v.setTag(viewHolder); 
			viewHolder.mChatTextRight.setText(mChatMessage.getmMessage());
			viewHolder.mDateTimeRight.setText(mChatMessage.getmTime());
			viewHolder.mUserName.setText(mChatMessage.getUser());
		}else{
			v = li.inflate(R.layout.grp_chat_row_right, null); 
			viewHolder = new ListViewHolder(v, false);  
			v.setTag(viewHolder); 
			viewHolder.mChatText.setText(mChatMessage.getmMessage());
			viewHolder.mDateTime.setText(mChatMessage.getmTime());
		}



		//   } //else {  
			//  viewHolder = (CompleteListViewHolder) v.getTag();  
			// }
		//  if(!mChatMessage.isRight() ){
		Log.i("GroupChatListArrayAdapter", mChatMessage.getmTime() +"Left Aigned");

		//   viewHolder.mChatTextRight.setText(mChatMessage.getmMessage());
		//    viewHolder.mDateTimeRight.setText(mChatMessage.getmTime());
		// viewHolder.mDateTime.setGravity(Gravity.RIGHT);
		//  }else{
		Log.i("GroupChatListArrayAdapter", mChatMessage.getmMessage() + "Right Aigned");



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
class ListViewHolder {  
	public TextView mChatText; 
	public TextView mChatTextRight; 
	public TextView mDateTime;
	public TextView mDateTimeRight;
	public TextView mUserName;
	public ListViewHolder(View base, boolean left) {  

		mDateTimeRight = (TextView)base.findViewById(R.id.chatDateRight);
		mChatTextRight = (TextView) base.findViewById(R.id.chatContentRight);
		mUserName = (TextView) base.findViewById(R.id.grpUserName);

		  
		mChatText = (TextView) base.findViewById(R.id.chatContent); 
		mDateTime = (TextView)base.findViewById(R.id.chatDate);
	}


}  