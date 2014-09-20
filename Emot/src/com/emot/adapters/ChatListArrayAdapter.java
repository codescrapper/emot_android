package com.emot.adapters;

import java.util.List;  
import android.app.Activity;  
import android.content.Context;  
import android.view.Gravity;
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  

import android.widget.AbsListView.LayoutParams;
import android.widget.BaseAdapter;  
import android.widget.LinearLayout;
import android.widget.TextView;  

import com.emot.emotobjects.ChatMessage;
import com.emot.screens.R; 


public class ChatListArrayAdapter extends BaseAdapter {  
     private Activity mContext;  
     private List<ChatMessage> mList;
     private LinearLayout mMessageContainer;
     private ChatMessage mChatMessage;
     private LayoutInflater mLayoutInflater = null;  
     public ChatListArrayAdapter(Activity context, List<ChatMessage> list) {  
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
          CompleteListViewHolder viewHolder;  
          if (convertView == null) {  
               LayoutInflater li = (LayoutInflater) mContext  
                         .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
               v = li.inflate(R.layout.chat_row, null);  
               mMessageContainer = (LinearLayout)v.findViewById(R.id.messageContainer);
               
               viewHolder = new CompleteListViewHolder(v);  
               v.setTag(viewHolder);  
          } else {  
               viewHolder = (CompleteListViewHolder) v.getTag();  
          }
          mChatMessage = mList.get(position);
          if(!mChatMessage.isRight() ){
          mMessageContainer.setGravity(Gravity.LEFT);
          }else{
        	  mMessageContainer.setGravity(Gravity.RIGHT);  
          }
          viewHolder.mChatText.setText(mChatMessage.getmMessage());  
          
          
          return v;  
     }  
}  
class CompleteListViewHolder {  
     public TextView mChatText;  
     public CompleteListViewHolder(View base) {  
    	 mChatText = (TextView) base.findViewById(R.id.chatContent);  
     }  
}  