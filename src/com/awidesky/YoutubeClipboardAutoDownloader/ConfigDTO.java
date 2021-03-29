package com.awidesky.YoutubeClipboardAutoDownloader;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.PlayListOption;

public class ConfigDTO { 
	
	private String saveto;
	private String format;
	private String quality;
	private PlayListOption playlistOption;
	private String fileNameFormat;
	
	public ConfigDTO(String saveto, String extension, String quality, String playlistOption, String nameFormat) {

		this.saveto = saveto;
		this.format = extension;
		this.quality = quality;
		setPlaylistOption(playlistOption);
		this.fileNameFormat = nameFormat;
		
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


	public PlayListOption getPlaylistOption() {
		
		return playlistOption;
		
	}
	
	public void setPlaylistOption(String playlistOption2) {

		this.playlistOption = PlayListOption.get(playlistOption2);
		
	}


	public String getFileNameFormat() {
		return fileNameFormat;
	}


	public void setFileNameFormat(String fileNameFormat) {
		this.fileNameFormat = fileNameFormat;
	}

	@Override
	public String toString() {
		return String.format(" properties :\n downloadpath-%s\n format-%s\n quality-%s\n playlistoption-%s\n filenameformat-%s", saveto, format, quality, playlistOption, fileNameFormat);
	}
	
}
