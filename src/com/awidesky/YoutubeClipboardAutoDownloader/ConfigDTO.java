package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.Serializable;

public class ConfigDTO implements Serializable{ //TODO: Use Properties instead!

	private static final long serialVersionUID = 1184293966501697035L;
	
	private String saveto;
	private String format;
	private String quality;
	private String playlistOption;
	
	public ConfigDTO(String saveto, String extension, String quality, String playlistOption) {

		this.saveto = saveto;
		this.format = extension;
		this.quality = quality;
		this.setPlaylistOption(playlistOption);
	}

	
	public String getSaveto() {
		
		return saveto;
		
	}
	
	public void setSaveto(String saveto) {
		
		this.saveto = saveto;
		
	}
	
	public String getFormat() {
		
		return format;
		
	}
	
	public void setFormat(String extension) {
		
		this.format = extension;
		
	}
	
	public String getQuality() {
		
		return quality;
		
	}
	
	public void setQuality(String quality) {
		
		this.quality = quality;
		
	}


	public String getPlaylistOption() {
		
		return (Boolean.parseBoolean(playlistOption)) ? "--yes-playlist" : "--no-playlist";
		
	}


	public void setPlaylistOption(String playlistOption) {
		
		this.playlistOption = playlistOption;
		
	}
	
}
