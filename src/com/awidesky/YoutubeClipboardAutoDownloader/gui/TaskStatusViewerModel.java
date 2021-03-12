package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import java.util.function.Consumer;

public class TaskStatusViewerModel {

	private String videoName = ""; 
	private String status = "";
	private int progress = 0;
	private String dest = "";
	private Runnable whenDone;
	private Consumer<Integer> processUpdater; //what will this do?
	private int taskNum;
	
	
	public TaskStatusViewerModel(int num) {
		this.taskNum = num;
		
	}
	

	public String getVideoName() {
		return videoName;
	}
	
	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}
	
	public String getStatus() {
		return status;
	}
	
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getProgress() {
		return progress;
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
		processUpdater.accept(progress);
	}
	
	public String getDest() {
		return dest;
	}
	
	public void setDest(String dest) {
		this.dest = dest;
	}

	public int getTaskNum() {
		return taskNum;
	}

	public void done() {
		
		status = "Done!";
		whenDone.run();
		
	}

	public void setWhenDone(Runnable whenDone) {
		
		this.whenDone = whenDone;
		
	}

	public void setProcessUpdater(Consumer<Integer> processUpdater) {
		
		this.processUpdater = processUpdater;
		
	}
	
	public Consumer<Integer> getProcessUpdater() {
		
		return this.processUpdater;
		
	}
	
	
}
