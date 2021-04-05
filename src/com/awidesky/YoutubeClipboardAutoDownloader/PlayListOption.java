package com.awidesky.YoutubeClipboardAutoDownloader;

public enum PlayListOption {

	YES("yes", "--yes-playlist"),
	NO("no", "--no-playlist");
	
	
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
	
	public static PlayListOption get(String s) {
		
		switch(s) {
		
		case "yes":
		case "--yes-playlist":
			return YES;
		
		case "no":
		case "--no-playlist":
			return NO;
		
		default:
			throw new RuntimeException("Invalid parameter : PlayListOption.get(" + s + ")");
		}
		
	}
	
}
