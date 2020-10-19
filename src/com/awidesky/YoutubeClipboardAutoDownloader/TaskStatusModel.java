package com.awidesky.YoutubeClipboardAutoDownloader;

public class TaskStatusModel {

	public TaskStatusModel(String videoName, String status, int progress, String dest) {
		this.videoName = videoName;
		this.status = status;
		this.progress = progress;
		this.dest = dest;
	}
	
	private String videoName;
	private String status;
	private int progress;
	private String dest;
	
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
	}
	
	public String getDest() {
		return dest;
	}
	
	public void setDest(String dest) {
		this.dest = dest;
	}
	
	
	
}
