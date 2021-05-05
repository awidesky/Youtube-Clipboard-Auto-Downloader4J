package com.awidesky.YoutubeClipboardAutoDownloader;

public class Config { 
	
	private static String saveto;
	private static String format;
	private static String quality;
	private static PlayListOption playlistOption;
	private static String fileNameFormat;
	private static String clipboardListenOption;
	
	
	public static String getSaveto() {
		
		return saveto;
		
	}
	
	public static synchronized void setSaveto(String saveto) {
		
		Config.saveto = saveto;
		
	}
	
	public static String getFormat() {
		
		return format;
		
	}
	
	public static synchronized void setFormat(String extension) {
		
		Config.format = extension;
		
	}
	
	public static String getQuality() {
		
		return quality;
		
	}
	
	public static synchronized void setQuality(String quality) {
		
		Config.quality = quality;
		
	}

	public static PlayListOption getPlaylistOption() {
		return playlistOption;
	}
	
	public static synchronized void setPlaylistOption(String playlist) {

		Config.playlistOption = PlayListOption.get(playlist);
		
	}


	public static String getFileNameFormat() {
		return fileNameFormat;
	}


	public static synchronized void setFileNameFormat(String fileNameFormat) {
		Config.fileNameFormat = fileNameFormat;
	}

	public static String getClipboardListenOption() {
		return clipboardListenOption;
	}

	public static void setClipboardListenOption(String clipboardListenOption) {
		Config.clipboardListenOption = clipboardListenOption;
	}

	public static String status() {
		return String.format(" properties :\n downloadpath-%s\n format-%s\n quality-%s\n playlistoption-%s\n filenameformat-%s\n clipboardListenOption-%s", Config.saveto, Config.format, Config.quality, Config.playlistOption, Config.fileNameFormat, Config.clipboardListenOption);
	}
	
}
