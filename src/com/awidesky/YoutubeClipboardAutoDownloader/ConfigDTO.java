package com.awidesky.YoutubeClipboardAutoDownloader;

public class ConfigDTO { 
	
	private static String saveto;
	private static String format;
	private static String quality;
	private static PlayListOption playlistOption;
	private static String fileNameFormat;
	
	
	public static String getSaveto() {
		
		return saveto;
		
	}
	
	public static synchronized void setSaveto(String saveto) {
		
		ConfigDTO.saveto = saveto;
		
	}
	
	public static String getFormat() {
		
		return format;
		
	}
	
	public static synchronized void setFormat(String extension) {
		
		ConfigDTO.format = extension;
		
	}
	
	public static String getQuality() {
		
		return quality;
		
	}
	
	public static synchronized void setQuality(String quality) {
		
		ConfigDTO.quality = quality;
		
	}


	public static PlayListOption getPlaylistOption() {
		
		return playlistOption;
		
	}
	
	public static synchronized void setPlaylistOption(String playlist) {

		ConfigDTO.playlistOption = PlayListOption.get(playlist);
		
	}


	public static String getFileNameFormat() {
		return fileNameFormat;
	}


	public static synchronized void setFileNameFormat(String fileNameFormat) {
		ConfigDTO.fileNameFormat = fileNameFormat;
	}

	public static String status() {
		return String.format(" properties :\n downloadpath-%s\n format-%s\n quality-%s\n playlistoption-%s\n filenameformat-%s", ConfigDTO.saveto, ConfigDTO.format, ConfigDTO.quality, ConfigDTO.playlistOption, ConfigDTO.fileNameFormat);
	}
	
}
