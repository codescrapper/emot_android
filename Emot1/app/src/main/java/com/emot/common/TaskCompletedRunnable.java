package com.emot.common;

public interface TaskCompletedRunnable {
	
	public void onTaskComplete(String result);
	public void onTaskError(String error);

}
