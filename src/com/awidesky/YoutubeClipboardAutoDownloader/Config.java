package com.awidesky.YoutubeClipboardAutoDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;
import com.awidesky.YoutubeClipboardAutoDownloader.gui.GUI;

public class Config { 
	
	private static String saveto = null;
	private static String format = null;
	private static String quality = null;
	private static PlayListOption playlistOption;
	private static String fileNameFormat = null;
	private static ClipBoardOption clipboardListenOption = null;
	private static List<String> acceptableLinks = new ArrayList<>();
	
	
	public static String getSaveto() {
		
		return saveto;
		
	}
	
	public static synchronized void setSaveto(String saveto) {
		
		File file = new File(saveto);
		if((file.exists() || file.mkdirs()) && file.isDirectory()) {
			Config.saveto = saveto;
		} else {
			GUI.error("Download Path is invalid!", "Invalid path : " + saveto, null, false);
		}
		
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
		
		if(quality.matches("0\\(best\\)|9\\(worst\\)")) {
			quality = quality.substring(0, 1);
		}
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

	public static ClipBoardOption getClipboardListenOption() {
		return clipboardListenOption;
	}

	public static void setClipboardListenOption(String clipboardListenOption) {
		Config.clipboardListenOption = ClipBoardOption.get(clipboardListenOption);
	}

	public static void addAcceptableList(String s) {
		acceptableLinks.add(s);
	}
	
	public static boolean isLinkAcceptable(String s) {
		return acceptableLinks.stream().anyMatch(valid -> s.startsWith(valid));
	}
	
	public static String getAcceptedLinkStr() {
		return acceptableLinks.stream().collect(Collectors.joining(System.lineSeparator()));
	}
	
	
	public static String status() {
		return String.format(" properties :\n downloadpath-%s\n format-%s\n quality-%s\n playlistoption-%s\n filenameformat-%s\n clipboardListenOption-%s\n Accepted links starts by :\n%s", Config.saveto, Config.format, Config.quality, Config.playlistOption, Config.fileNameFormat, Config.clipboardListenOption.getString(), Config.getAcceptedLinkStr());
	}
	
}
