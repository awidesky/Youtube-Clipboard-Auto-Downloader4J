package com.awidesky.YoutubeClipboardAutoDownloader;

public enum LoadingStatus {

	CHECKING_FFMPEG("Checking ffmpeg installation...", 17), 
	CHECKING_YDL("Checking youtube-dl installation...", 42),
	READING_PROPERTIES("Reading properties...", 77), 
	PREPARING_THREADS("Preparing threads...", 83), 
	LOADING_WINDOW("Loading window components...", 100);
	
	
	private String status;
	private int progress;
	
	private LoadingStatus(String status, int progress) {
		this.status = status;
		this.progress = progress;
	}

	public String getStatus() {
		return status;
	}

	public int getProgress() {
		return progress;
	}
}
