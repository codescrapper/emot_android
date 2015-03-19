package com.emot.adapters;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.emot.androidclient.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.emot.common.ImageHelper;
import com.emot.emotobjects.Contact;
import com.emot.screens.R;

public class ContactArrayAdapter extends ArrayAdapter<Contact> implements Filterable{

	private ArrayList<Contact> contacts;
	private ArrayList<Contact> ocontacts;
	private static final String TAG = ContactArrayAdapter.class.getSimpleName();

	public ContactArrayAdapter(Context context, int resource, ArrayList<Contact> contacts) {
		super(context, resource, contacts);
		this.contacts = contacts;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if(view==null) {
			LayoutInflater vi = LayoutInflater.from(this.getContext());
			view=vi.inflate(R.layout.contact_row, null);
		}

		TextView name = (TextView)view.findViewById(R.id.text_contact_name);
		TextView mobile = (TextView)view.findViewById(R.id.text_contact_number);
		ImageView profile = (ImageView)view.findViewById(R.id.image_contact_profile);
		TextView status = (TextView)view.findViewById(R.id.text_contact_status);
		name.setText(contacts.get(position).getName());
		mobile.setText(contacts.get(position).getJID());
		status.setText(contacts.get(position).getStatus());
		Bitmap bitmap = null;
		if(contacts.get(position).getAvatar()!=null){
			bitmap = BitmapFactory.decodeByteArray(contacts.get(position).getAvatar() , 0, contacts.get(position).getAvatar().length);
			//Log.i(TAG, "Bitmap  = "+bitmap);
		}
		if(bitmap==null){
			bitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.drawable.blank_user_image);
		}

		profile.setImageBitmap(ImageHelper.getRoundedCornerBitmap(bitmap, 10));

		return view;
	}

	@Override
	public Filter getFilter() {
		Log.i(TAG, "get filter called !!!");
		Filter filter = new Filter() {

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {

				contacts = (ArrayList<Contact>) results.values;
				notifyDataSetChanged();
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				Log.i(TAG, "Filtering through : "+constraint);
				if(ocontacts==null){
					ocontacts = new ArrayList<Contact>(contacts);
				}
				FilterResults results = new FilterResults();
				ArrayList<Contact> filtered_results = new ArrayList<Contact>();

				constraint = constraint.toString().toLowerCase();
				for (int i = 0; i < ocontacts.size(); i++) {
					if (ocontacts.get(i).getName().toLowerCase().contains((constraint.toString())))  {
						filtered_results.add(ocontacts.get(i));
					}
				}

				results.count = filtered_results.size();
				results.values = filtered_results;
				Log.e("VALUES", results.values.toString());

				return results;
			}
			
		};

		return filter;
	}
	
	@Override
	public int getCount(){
		return contacts.size();
	}
	
	public Contact getItem(int position){
		return contacts.get(position);
	}
}
