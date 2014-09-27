package com.emot.screens;

import java.io.ByteArrayOutputStream;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.emot.model.EmotUser;
import com.emot.services.ChatService;
import com.emot.services.ChatService.ProfileBinder;

public class UpdateProfileScreen extends ActionBarActivity {
	private EditText editStatus;
	private Button saveButton;
	private ImageView imageAvatar;
	private static final int CAMERA_REQUEST = 1;
	private static final int PICK_FROM_GALLERY = 2;
	private ChatService chatService;
	boolean mBound = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.profile);
		editStatus = (EditText)findViewById(R.id.editTextStatus);
		editStatus.setText(EmotUser.getStatus());
		saveButton = (Button)findViewById(R.id.buttonProfileSave);
		imageAvatar = (ImageView)findViewById(R.id.imageAvatar);
		imageAvatar.setImageBitmap(EmotUser.getAvatar());

		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mBound) {
					chatService.updateStatus(editStatus.getText().toString());
				}
			}
		});

		final String[] option = new String[] { "Take from Camera", "Select from Gallery" };
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.select_dialog_item, option);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Select Option");
		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				Log.e("Selected Item", String.valueOf(which));
				if (which == 0) {
					callCamera();
				}
				if (which == 1) {
					callGallery();
				}

			}
		});
		final AlertDialog dialog = builder.create();

		imageAvatar.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.show();
			}
		});

	}
	
	@Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, ChatService.class);
        intent.putExtra("request_code", ChatService.REQUEST_PROFILE_UPDATE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

	public void callCamera() {
		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		cameraIntent.putExtra("crop", "true");
		cameraIntent.putExtra("aspectX", 0);
		cameraIntent.putExtra("aspectY", 0);
		cameraIntent.putExtra("outputX", 200);
		cameraIntent.putExtra("outputY", 150);
		startActivityForResult(cameraIntent, CAMERA_REQUEST);

	}

	public void callGallery() {
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		intent.putExtra("crop", "true");
		intent.putExtra("aspectX", 0);
		intent.putExtra("aspectY", 0);
		intent.putExtra("outputX", 200);
		intent.putExtra("outputY", 150);
		intent.putExtra("return-data", true);
		startActivityForResult(Intent.createChooser(intent, "Complete action using"), PICK_FROM_GALLERY);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK)
			return;

		switch (requestCode) {
		case CAMERA_REQUEST:

			Bundle extras = data.getExtras();

			if (extras != null) {
				Bitmap yourImage = extras.getParcelable("data");
				// convert bitmap to byte
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				yourImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byte imageInByte[] = stream.toByteArray();
				Log.e("output before conversion", imageInByte.toString());
				chatService.updateAvatar(yourImage);
			}
			break;
		case PICK_FROM_GALLERY:
			Bundle extras2 = data.getExtras();

			if (extras2 != null) {
				Bitmap yourImage = extras2.getParcelable("data");
				// convert bitmap to byte
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				yourImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
				byte imageInByte[] = stream.toByteArray();
				Log.e("output before conversion", imageInByte.toString());
				chatService.updateAvatar(yourImage);
			}

			break;
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			ProfileBinder binder = (ProfileBinder) service;
			chatService = binder.getService();
			mBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};
}