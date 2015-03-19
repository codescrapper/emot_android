package com.emot.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.NameValuePair;

import android.os.AsyncTask;
import com.emot.androidclient.util.Log;

import com.emot.common.TaskCompletedRunnable;
import com.emot.constants.ApplicationConstants;

public class EmotHTTPClient extends AsyncTask<Void, Void, String>{

	private static final String TAG = EmotHTTPClient.class.getSimpleName();
	private URL url;
	private ArrayList<NameValuePair> reqContent;
	private TaskCompletedRunnable taskCompletedRunnable;

	public EmotHTTPClient(final URL pUrl){

		this.url = pUrl;
	}

	public EmotHTTPClient(final URL pUrl, final ArrayList<NameValuePair> pRequestContents, final TaskCompletedRunnable pTaskCompletedRunnable){

		this.url = pUrl;
		this.reqContent = pRequestContents;
		this.taskCompletedRunnable = pTaskCompletedRunnable;
	}

	public EmotHTTPClient(final URL pUrl, final ArrayList<NameValuePair> pRequestContents, final String pVerificationSalt,final TaskCompletedRunnable pTaskCompletedRunnable){

		this.url = pUrl;
		this.reqContent = pRequestContents;
		this.taskCompletedRunnable = pTaskCompletedRunnable;
	}

	private String getQuery(final List<NameValuePair> params)
			throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder();
		boolean first = true;

		for (NameValuePair pair : params) {
			if (first)
				first = false;
			else
				result.append("&");

			result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
			result.append("=");
			Log.d(TAG, pair.getName() + " = "+pair.getValue());
			result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
		}
		//Log.d(TAG, "Post data: " + result.toString());
		return result.toString();
	}
	
	public URL createGETURL(final String pUrl){
		URL url = null;
		try {
			url = new URL(pUrl);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return url;
	}

	@Override
	protected String doInBackground(Void... params) {



		InputStream in = null;
		HttpURLConnection urlConnection;
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String result = null;
		String line;
		try {
			//url = new URL("http://192.168.0.104:8000/api/register/");
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.addRequestProperty("Content-type","application/x-www-form-urlencoded");
			System.setProperty("http.keepAlive", "false");

			if (reqContent != null) {
				urlConnection.setDoOutput(true);
				urlConnection.setRequestMethod(ApplicationConstants.HTTP_POST);

				urlConnection.setRequestProperty("Content-Length",
						Integer.toString(getQuery(reqContent).getBytes().length));

				PrintWriter out = new PrintWriter(
						urlConnection.getOutputStream());
				out.print(getQuery(reqContent));
				out.close();
			}
			Log.d(TAG, "connection : " + urlConnection.toString());
			int status = urlConnection.getResponseCode();
			Log.d(TAG, "status =" + status);
			System.out.println("status= " +status);
			if (urlConnection.getErrorStream() != null) {
				in = urlConnection.getErrorStream();
				Log.d(TAG, "Error : " );
			}

			if (status == HttpsURLConnection.HTTP_OK) {
				in = urlConnection.getInputStream();
				String contentEncoding = urlConnection
						.getHeaderField("Content-Encoding");
				Log.d(TAG, "Content encoding : " + contentEncoding);
				System.out.println("Content encoding : " + contentEncoding);
				br = new BufferedReader(new InputStreamReader(in));
				//Log.i(TAG, "Single Response String is " +br.readLine() + " ready value = "+br.ready());
				while (/*br.ready() && */(line = br.readLine()) != null) {
					Log.i(TAG, "One line read ... "+line);
					sb.append(line);
				}
				result = sb.toString();
				
				Log.i(TAG, "Response String is " +sb);
				if (contentEncoding != null
						&& contentEncoding.equalsIgnoreCase("gzip")) {
					in = new GZIPInputStream(in);
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch(IOException e){
			//e.printStackTrace();
		} 

		return result;
	}

	@Override
	protected void onPostExecute(String result) {
		Log.i("Registration", "in postExecute");
		Log.i("task Val", taskCompletedRunnable.toString());
		if(result!=null){
			taskCompletedRunnable.onTaskComplete(result);
		}else{
			taskCompletedRunnable.onTaskError("Something went wrong while processing your request. Please try again later.");
			Log.e(TAG, "Server not responding. Received null result");
		}
	}

}
