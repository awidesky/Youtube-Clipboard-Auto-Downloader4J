package com.awidesky.YoutubeClipboardAutoDownloader.enums;

import java.util.Arrays;
import java.util.stream.Stream;

public enum ClipBoardOption {

	AUTOMATIC("download link automatically"),
	ASK("ask when a link is found"),
	NOLISTEN("stop listening clipboard");
	
	private String description;

	private ClipBoardOption(String description) { this.description = description; }

	public String getString() { return description; }
	public static String[] getComboBoxStrings() { return Stream.of(values()).map(ClipBoardOption::getString).toArray(String[]::new); }

	public static ClipBoardOption get(String clipboardListenOption) {
		return Arrays.stream(ClipBoardOption.values()).filter(op -> op.description.equals(clipboardListenOption)).findAny()
				.orElseThrow(() -> new RuntimeException("Invalid parameter : ClipBoardOption.get(" + clipboardListenOption + ")"));
	}
}
