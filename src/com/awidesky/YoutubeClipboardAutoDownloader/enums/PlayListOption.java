package com.awidesky.YoutubeClipboardAutoDownloader.enums;

import java.util.stream.Stream;

public enum PlayListOption {

	YES("yes", "--yes-playlist"),
	NO("no", "--no-playlist"),
	ASK("ask", null);
	
	private String toComboBox;
	private String toCommandArgm;
	
	private PlayListOption(String toCombo, String toArgm) {
		this.toComboBox = toCombo;
		this.toCommandArgm = toArgm;
	}

	public String toComboBox() {
		return toComboBox;
	}

	public String toCommandArgm() {
		return toCommandArgm;
	}
	
	public static String[] getComboBoxList() {
		return Stream.of(values()).map(PlayListOption::toComboBox).toArray(String[]::new);
	}
	
	public static PlayListOption get(String s) {
		
		switch(s) {
		
		case "yes":
		case "--yes-playlist":
			return YES;
		
		case "no":
		case "--no-playlist":
			return NO;
			
		case "ask":
			return ASK;
		
		default:
			throw new RuntimeException("Invalid parameter : PlayListOption.get(" + s + ")");
		}
		
	}
	
}
