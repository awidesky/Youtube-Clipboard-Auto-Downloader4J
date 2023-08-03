package io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec;

public class OSUtil {

	private static final String OS = System.getProperty("os.name");

    public static boolean isWindows() { return OS.contains("Windows"); }
    public static boolean isMac() { return OS.contains("Mac"); }
    public static boolean isLinux() { return OS.contains("Linux"); }
    
}
