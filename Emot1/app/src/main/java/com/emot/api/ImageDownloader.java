package com.emot.api;

import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import com.emot.androidclient.util.Log;

import com.emot.common.BitmapHandler;
import com.emot.constants.WebServiceConstants;

public class ImageDownloader extends AsyncTask<Void, Void, Bitmap> {
	
	private final static String TAG = ImageDownloader.class.getSimpleName();
	private String imgUrl;
	private BitmapHandler bitmapHandler;
	
	public ImageDownloader(String imgUrl, BitmapHandler bitmapHandler){
		this.imgUrl = imgUrl;
		this.bitmapHandler = bitmapHandler;
	}

	@Override
	protected Bitmap doInBackground(Void... param) {
		return downloadBitmap(WebServiceConstants.HTTP + "://" + WebServiceConstants.SERVER_IP  +this.imgUrl);
	}

	@Override
	protected void onPreExecute() {
		Log.i(TAG, "onPreExecute Called");
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		Log.i(TAG, "onPostExecute Called");
		this.bitmapHandler.processImage(result);
	}

	private Bitmap downloadBitmap(String url) {
		// initilize the default HTTP client object
		final DefaultHttpClient client = new DefaultHttpClient();

		//forming a HttoGet request 
		final HttpGet getRequest = new HttpGet(url);
		try {

			HttpResponse response = client.execute(getRequest);

			//check 200 OK for success
			final int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != HttpStatus.SC_OK) {
				Log.w("ImageDownloader", "Error " + statusCode + " while retrieving bitmap from " + url);
				return null;

			}

			final HttpEntity entity = response.getEntity();
			if (entity != null) {
				InputStream inputStream = null;
				try {
					// getting contents from the stream 
					inputStream = entity.getContent();

					// decoding stream data back into image Bitmap that android understands
					final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

					return bitmap;
				} finally {
					if (inputStream != null) {
						inputStream.close();
					}
					entity.consumeContent();
				}
			}
		} catch (Exception e) {
			// You Could provide a more explicit error message for IOException
			getRequest.abort();
			Log.e("ImageDownloader", "Something went wrong while" +" retrieving bitmap from " + url + e.toString());
		} 

		return null;
	}
	
}
