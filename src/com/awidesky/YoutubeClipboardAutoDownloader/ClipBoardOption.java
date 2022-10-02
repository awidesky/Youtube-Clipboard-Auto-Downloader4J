package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.stream.Stream;

public enum ClipBoardOption {

	AUTOMATIC("Download link automatically"),
	ASK("Ask when a link is found"),
	NOLISTEN("Stop listening clipboard");
	
	private String str;

	private ClipBoardOption(String str) {
		this.str = str;
	}

	public String getString() {
		return str;
	}

	
	public static String[] getComboBoxList() {
		return Stream.of(values()).map(ClipBoardOption::getString).toArray(String[]::new);
	}

	public static ClipBoardOption get(String clipboardListenOption) {
		switch(clipboardListenOption) {
		
		case "Download link automatically":
			return AUTOMATIC;
		
		case "Ask when a link is found":
			return ASK;
			
		case "Stop listening clipboard":
			return NOLISTEN;
		
		default:
			throw new RuntimeException("Invalid parameter : ClipBoardOption.get(" + clipboardListenOption + ")");
		}
	}
}
