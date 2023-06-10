package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.PlayListOption;

public class Config { 
	
	private static final Pattern QUALITYPATTERN = Pattern.compile("0\\(best\\)|9\\(worst\\)");
	
	private static String saveto = getDefaultSaveto();
	private static String format = getDefaultFormat();
	private static String quality = getDefaultQuality();
	private static PlayListOption playlistOption = getDefaultPlaylistOption();
	private static String fileNameFormat = getDefaultFileNameFormat();
	private static ClipBoardOption clipboardListenOption = getDefaultClipboardListenOption();
	private static List<String> acceptableLinks = new ArrayList<>(List.of("https://www.youtu", "youtube.com"));
	
	public static String getSaveto() { return saveto; }
	public static synchronized void setSaveto(String saveto) { Config.saveto = saveto; }
	
	public static String getFormat() { return format; }
	public static synchronized void setFormat(String extension) { Config.format = extension; }
	
	public static String getQuality() { return quality; }
	public static synchronized void setQuality(String quality) {
		if(QUALITYPATTERN.matcher(quality).matches()) {
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
	
	
	// Default config values
	public static String getDefaultSaveto() { return System.getProperty("user.home"); }
	public static String getDefaultFormat() { return "mp3"; }
	public static String getDefaultQuality() { return "0"; }
	public static PlayListOption getDefaultPlaylistOption() { return PlayListOption.NO; }
	public static String getDefaultFileNameFormat() { return "%(title)s.%(ext)s"; }
	public static ClipBoardOption getDefaultClipboardListenOption() { return ClipBoardOption.AUTOMATIC; }
}
