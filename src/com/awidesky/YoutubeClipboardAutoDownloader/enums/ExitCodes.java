package com.awidesky.YoutubeClipboardAutoDownloader.enums;

public enum ExitCodes {

	SUCCESSFUL("Program exited successfully as user intended", 0),
	PROJECTPATHNOTFOUND("Unable to locate project root library(\"YoutubeAudioAutoDownloader-resources\" folder)", 100),
	EDTFAILED("GUI event dispatch thread task execution has failed", -1),
	FFMPEGNOTEXISTS("Failed to find ffmpeg installation", -2),
	YOUTUBEDNOTEXISTS("Failed to find yt-dlp installation", -3);
	
	private String msg;
	private int code;

	private ExitCodes(String msg, int code) {
		this.msg = msg;
		this.code = code;
	}

	public String getMsg() { return msg; }
	public int getCode() { return code; }

}
