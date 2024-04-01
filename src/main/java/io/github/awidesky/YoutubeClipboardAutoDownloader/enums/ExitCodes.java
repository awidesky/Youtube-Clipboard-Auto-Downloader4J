package io.github.awidesky.YoutubeClipboardAutoDownloader.enums;

public enum ExitCodes {

	SUCCESSFUL("Program exited successfully as user intended", 0),
	FFMPEGNOTEXISTS("Failed to find ffmpeg installation", 200),
	YOUTUBEDNOTEXISTS("Failed to find yt-dlp installation", 300),
	INVALIDCOMMANDARGS("Invalid command line argument(s)", -1),
	EDTFAILED("Task execution from GUI event dispatch thread has failed", -2),
	UNKNOWNERROR("Unknown Error(e.g. unhandled Exception/JVM Error)", -100);
	
	private String msg;
	private int code;

	private ExitCodes(String msg, int code) {
		this.msg = msg;
		this.code = code;
	}

	public String getMsg() { return msg; }
	public int getCode() { return code; }

	@Override
	public String toString() {
		return code + " (" + msg + ")";
	}

}
