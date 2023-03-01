package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.Closeable;
import java.text.DateFormat;
import java.util.Date;



/**
 * An Abstract class for a Logger object.
 * 
 * @author Eugene Hong
 * */
public interface Logger extends Closeable, AutoCloseable {


	/**
	 * a Simple logger stub that just prints to console 
	 * */
	public static final Logger consoleLogger = new AbstractLogger() {
		{
			setPrefix("[consoleLogger] ");
		}
		
		@Override
		public void newLine() {
			System.out.println();
		}
		
		@Override
		public void log(String data) {
			printPrefix();
			System.out.println(data);
		}

		private void printPrefix() {
			if(datePrefix != null) System.out.print("[" + datePrefix.format(new Date()) + "]");
			if(prefix != null) System.out.print(prefix);
		}
		
		@Override
		public void close() {}

	}; 

	
	/**
	 * Set date information prefix for this <code>Logger</code> instance.
	 * if argument is <code>null</code>, no date information prefix is appended,
	 * */
	public void setDatePrefix(DateFormat datePrefix);

	/**
	 * Set additional prefix for this <code>Logger</code> instance.
	 * if argument is <code>null</code>, no date information prefix is appended,
	 * */
	public void setPrefix(String prefix);
	
	/**
	 * Print a newLine character without printing prefixes.
	 * 
	 * */
	public abstract void newLine();

	/**
	 * Logs a String.
	 * */
	public abstract void log(String data);
	/**
	 * Logs an <code>Exception</code> 
	 * */
	public void log(Exception e);
	/**
	 * Logs an array of <code>Object</code>s
	 * */
	public void log(Object... objs);
	
	
	/**
	 * Set verbosity of this <code>Logger</code> object.
	 * */
	public void setVerbose(boolean verbose);
	/**
	 * Log in verbose mode.
	 * If <code>this.verbose</code> is <code>true</code>, argument <code>data</code> is logged, otherwise it doesn't 
	 * */
	public void logVerbose(String data);
	
}
