package com.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

public abstract class TaskBufferedLogger extends TaskLogger implements FlushableLogger {

	private StringWriter buffer = new StringWriter();

	
	public TaskBufferedLogger(boolean verbose) {
		super(verbose);
	}

	

	@Override
	public void newLine() {
		buffer.append(System.lineSeparator());
	}
	
	@Override
	public void log(String data) {
		Consumer<PrintWriter> task = getLogTask(data);
		PrintWriter pw = new PrintWriter(buffer);
		task.accept(pw);
	}
	
	@Override
	public void flush() {
		queueLogTask(getLogTask(buffer.toString()));
	}

}
