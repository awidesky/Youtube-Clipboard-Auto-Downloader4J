package io.github.awidesky.YoutubeAudioAutoDownloader.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * An Simple Logger class that prints log to a <code>PrintWriter</code>.
 * 
 * @author Eugene Hong
 * */
public class SimpleLogger extends AbstractLogger {
	
	private PrintWriter logTo;
	
	public SimpleLogger(OutputStream os) {
		this(os, true, Charset.defaultCharset());
	}
	
	public SimpleLogger(OutputStream os, Charset cs) {
		this(os, true, cs);
	}
	
	public SimpleLogger(OutputStream os, boolean autoFlush) {
		this(os, autoFlush, Charset.defaultCharset());
	}
	
	public SimpleLogger(OutputStream os, boolean autoFlush, Charset cs) {
		logTo = new PrintWriter(os, autoFlush, cs);
	}

	/**
	 * Prints a empty line without prefix
	 * */
	@Override
	public void newLine() {
		logTo.println();
	}

	/**
	 * Logs a String.
	 * Each lines will be printed with prefix.
	 * */
	@Override
	public void log(String data) {
		logTo.println(getPrefix() + data);
	}

	@Override
	public void close() throws IOException {
		logTo.flush();
		logTo.close();
	}

}
