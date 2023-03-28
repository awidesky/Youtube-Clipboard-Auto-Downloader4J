package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;

public class Config { 
	
	private static String saveto = YoutubeAudioDownloader.getProjectpath();
	private static String format = "mp3";
	private static String quality = "0";
	private static PlayListOption playlistOption = PlayListOption.NO;
	private static String fileNameFormat = "%(title)s.%(ext)s";
	private static ClipBoardOption clipboardListenOption = ClipBoardOption.AUTOMATIC;
	private static List<String> acceptableLinks = new ArrayList<>(List.of("https://www.youtu", "youtube.com"));
	
	public static String getSaveto() { return saveto; }
	public static synchronized void setSaveto(String saveto) { Config.saveto = saveto; }
	
	public static String getFormat() { return format; }
	public static synchronized void setFormat(String extension) { Config.format = extension; }
	
	public static String getQuality() { return quality; }
	public static synchronized void setQuality(String quality) {
		if(quality.matches("0\\(best\\)|9\\(worst\\)")) {
			quality = quality.substring(0, 1);
		}
		Config.quality = quality;
	}

	public static PlayListOption getPlaylistOption() { return playlistOption; }
	public static synchronized void setPlaylistOption(String playlist) { Config.playlistOption = PlayListOption.get(playlist); }

	public static String getFileNameFormat() { return fileNameFormat; }
	public static synchronized void setFileNameFormat(String fileNameFormat) { Config.fileNameFormat = fileNameFormat; }

	public static ClipBoardOption getClipboardListenOption() { return clipboardListenOption; }
	public static void setClipboardListenOption(String clipboardListenOption) { Config.clipboardListenOption = ClipBoardOption.get(clipboardListenOption); }

	public static void addAcceptableList(String s) { acceptableLinks.add(s); }
	public static boolean isLinkAcceptable(String s) { return acceptableLinks.stream().anyMatch(valid -> s.startsWith(valid)); }
	public static String getAcceptedLinkStr(String delimiter) { return acceptableLinks.stream().collect(Collectors.joining(delimiter)); }
	
	public static String status() {
		return String.format("\n\tdownloadpath-%s\n\tformat-%s\n\tquality-%s\n\tplaylistoption-%s\n\tfilenameformat-%s\n\tclipboardListenOption-%s\n\tAccepted links starts by :\n\t    %s\n",
				Config.saveto, Config.format, Config.quality, Config.playlistOption, Config.fileNameFormat, Config.clipboardListenOption.getString(), Config.getAcceptedLinkStr(System.lineSeparator() + "\t    "));
	}
	
}
