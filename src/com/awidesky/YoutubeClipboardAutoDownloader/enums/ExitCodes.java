package com.awidesky.YoutubeClipboardAutoDownloader.enums;

public enum ExitCodes {

	SUCCESSFUL("Program exited successfully as user intended", 0),
	PROJECTPATHNOTFOUND("Unable to locate project root library(where YoutubeAudioAutoDownloader-resources folder exists)", 100),
	INITLOADINGFRAMEFAILED("LoadingFrame initiation failed", -1),
	FFMPEGNOTEXISTS("Failed to find ffmpeg", -2),
	YOUTUBEDNOTEXISTS("Failed to find youtube-dl", -3);
	
	private String msg;
	private int code;

	private ExitCodes(String msg, int code) {
		this.msg = msg;
		this.code = code;
	}

	public String getMsg() { return msg; }
	public int getCode() { return code; }

}
