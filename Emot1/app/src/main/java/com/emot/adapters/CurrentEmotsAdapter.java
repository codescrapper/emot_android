package com.emot.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import com.emot.androidclient.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.emot.emotobjects.CurrentEmot;
import com.emot.screens.R;

public class CurrentEmotsAdapter extends BaseAdapter {
	
	private Activity mContext;
	private List<CurrentEmot> mList;
	private LayoutInflater mLayoutInflater = null;
	
	public CurrentEmotsAdapter(Activity context, List<CurrentEmot> list) {  
        mContext = context;  
        mList = list;  
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
   }

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;  
		CompleteListViewHolder viewHolder;
		if (convertView == null) {  
            LayoutInflater li = (LayoutInflater) mContext  
                      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
            v = li.inflate(R.layout.emot_list_row, null);  
            viewHolder = new CompleteListViewHolder();
            viewHolder.user = (TextView)v.findViewById(R.id.text_contact_name);
            viewHolder.lastEmot = (TextView)v.findViewById(R.id.text_contact_status);
            v.setTag(viewHolder);  
       } else {  
            viewHolder = (CompleteListViewHolder) v.getTag();  
       } 
		Log.i("EmotsAdapter", "position "+mList.get(position).getUserName());
       viewHolder.user.setText(mList.get(position).getUserName());
       viewHolder.lastEmot.setText(mList.get(position).getUserLastEmot());
       return v;  
   
		
		
	}
	class CompleteListViewHolder {  
	     public TextView user;
	     public TextView lastEmot;
	     

}
}
