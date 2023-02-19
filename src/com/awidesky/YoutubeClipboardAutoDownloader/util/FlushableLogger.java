package com.awidesky.YoutubeClipboardAutoDownloader.util;

/**
 * A <code>Logger</code> class that support flush operation.
 * */
public interface FlushableLogger extends Logger { //TODO : use this for YTD

	public abstract void flush();
	
}
