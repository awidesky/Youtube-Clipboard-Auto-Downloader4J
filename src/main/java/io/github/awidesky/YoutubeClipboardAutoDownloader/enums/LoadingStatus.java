package io.github.awidesky.YoutubeClipboardAutoDownloader.enums;

public enum LoadingStatus {

	PREPARING_THREADS("Preparing threads...", 0), 
	READING_PROPERTIES("Reading properties...", 7), 
	CHECKING_FFMPEG("Checking ffmpeg installation...", 20), 
	CHECKING_YTDLP("Checking yt-dlp installation...", 39),
	LOADING_WINDOW("Loading window components...", 72);
	
	private String status;
	private int progress;
	
	private LoadingStatus(String status, int progress) {
		this.status = status;
		this.progress = progress;
	}

	public String getStatus() { return status; }
	public int getProgress() { return progress; }
}
