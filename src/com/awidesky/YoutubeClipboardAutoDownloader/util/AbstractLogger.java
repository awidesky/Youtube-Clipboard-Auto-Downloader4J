package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Arrays;


/**
 * An Abstract class for a Logger object.
 * 
 * @author Eugene Hong
 * */
public abstract class AbstractLogger implements Logger{

	protected boolean verbose = false;
	protected DateFormat datePrefix = null;
	protected String prefix = null;

	
	/**
	 * Set date information prefix for this <code>Logger</code> instance.
	 * if argument is <code>null</code>, no date information prefix is appended,
	 * */
	@Override
	public void setDatePrefix(DateFormat datePrefix) {
		this.datePrefix = datePrefix;
	}

	/**
	 * Set additional prefix for this <code>Logger</code> instance.
	 * if argument is <code>null</code>, no date information prefix is appended,
	 * */
	@Override
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	/**
	 * Logs an <code>Exception</code> 
	 * */
	@Override
	public void log(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		log(sw.toString());
	}
	/**
	 * Logs an array of <code>Object</code>s
	 * */
	@Override
	public void log(Object... objs) {
		Arrays.stream(objs).map(Object::toString).forEach(this::log);
	}
	
	
	/**
	 * Set verbosity of this <code>Logger</code> object.
	 * */
	@Override
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	/**
	 * Log in verbose mode.
	 * If <code>this.verbose</code> is <code>true</code>, argument <code>data</code> is logged, otherwise it doesn't 
	 * */
	@Override
	public void logVerbose(String data) {
		if(verbose) log(data);
	}
	
}
