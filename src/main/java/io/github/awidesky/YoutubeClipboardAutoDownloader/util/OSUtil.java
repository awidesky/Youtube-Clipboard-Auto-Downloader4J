package io.github.awidesky.YoutubeClipboardAutoDownloader.util;

public class OSUtil {

	private static final String OS = System.getProperty("os.name").toLowerCase();

    public static boolean isWindows() { return OS.contains("windows"); }
    public static boolean isMac() { return OS.contains("mac"); }
    public static boolean isLinux() { return OS.contains("linux"); }
    
}
