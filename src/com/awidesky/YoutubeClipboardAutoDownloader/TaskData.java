package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;

public class TaskData {
	
	private String videoName = "null"; 
	private String status = "";
	private int progress = 0;
	private String dest = "";
	private int taskNum;
	private int totalNumOfVideo = 1;
	private int videoNum = 0;
	
	private Future<?> fu;
	private Process p;
	
	public TaskData(int num) {
		this.taskNum = num;
	}
	

	public String getVideoName() {
		return videoName;
	}
	
	public void setVideoName(String videoName) {
		this.videoName = videoName;
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
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
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}

	public int getTaskNum() {
		return taskNum;
	}

	public void done() {
		fu = null;
		p = null;
		setStatus("Done!");
	}

	public boolean isNotDone() {
		return !status.equals("Done!");
	}

	public void setTotalNumVideo(int vdnum) {
		totalNumOfVideo = vdnum;
	}

	public int getTotalNumVideo() {
		return totalNumOfVideo;
	}
	
	public void increaseVideoNum() {
		videoNum++;
	}
	
	public int getVideoNum() {
		return videoNum;
	}

	public void kill() {
		if(isNotDone()) {
			if (p != null) p.destroy();
			if (fu != null) fu.cancel(true);
			Main.log("[Task" + taskNum + "|Canceled] Task number " + taskNum + " has killed!");
		}
	}

	public String getProgressToolTip() {
		return progress + "%" + ( (totalNumOfVideo > 1) ? " (" + videoNum + "/" + totalNumOfVideo + ")" : "" );
	}


	public void setFuture(Future<?> submit) {
		fu = submit;
	}


	public void setProcess(Process p) {
		this.p = p;
	}
	
}