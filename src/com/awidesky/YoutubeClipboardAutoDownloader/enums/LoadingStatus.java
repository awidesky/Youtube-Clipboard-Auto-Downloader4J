package com.awidesky.YoutubeClipboardAutoDownloader.enums;

public enum LoadingStatus {

	PREPARING_THREADS("Preparing threads...", 17), 
	CHECKING_YTDLP("Checking yt-dlp installation...", 32),
	CHECKING_FFMPEG("Checking ffmpeg installation...", 53), 
	READING_PROPERTIES("Reading properties...", 83), 
	LOADING_WINDOW("Loading window components...", 100);
	
	private String status;
	private int progress;
	
	private LoadingStatus(String status, int progress) {
		this.status = status;
		this.progress = progress;
	}

	public String getStatus() { return status; }
	public int getProgress() { return progress; }
}
