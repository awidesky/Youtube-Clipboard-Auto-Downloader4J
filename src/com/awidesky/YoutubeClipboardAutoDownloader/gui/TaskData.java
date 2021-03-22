package com.awidesky.YoutubeClipboardAutoDownloader.gui;

import javax.swing.SwingUtilities;

public class TaskData{
	
	private String videoName = ""; 
	private String status = "";
	private int progress = 0;
	private String dest = "";
	private int taskNum;
	private String url; /** Show as tooltip */ //TODO: tooltip
	
	
	public TaskData(int num, String url) {
		this.taskNum = num;
		this.url = url;
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
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	
	public int getProgress() {
		return progress;
	}
	
	public void setProgress(int progress) {
		this.progress = progress;
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
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

	public String getUrl() {
		return url;
	}


	public void done() {
		setStatus("Done!");
	}

}