package io.github.awidesky.YoutubeClipboardAutoDownloader.enums;

import java.util.stream.Stream;

public enum PlayListOption {

	YES("yes", "--yes-playlist"),
	NO("no", "--no-playlist"),
	ASK("ask", null);
	
	private String comboBoxForm;
	private String commandArgForm;
	
	private PlayListOption(String toCombo, String toArgm) {
		this.comboBoxForm = toCombo;
		this.commandArgForm = toArgm;
	}

	public String toComboBox() { return comboBoxForm; }
	public String toCommandArgm() { return commandArgForm; }
	
	public static String[] getComboBoxList() { return Stream.of(values()).map(PlayListOption::toComboBox).toArray(String[]::new); }
	
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
