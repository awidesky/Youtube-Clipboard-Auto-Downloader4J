package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.Objects;
import java.util.concurrent.Future;

import javax.swing.SwingUtilities;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;

public class TaskData {
	
	private String videoName = "null"; 
	private String url = "null"; 
	private String status = "";
	private int progress = 0;
	private String dest = "";
	private boolean checked = false;
	private int taskNum;
	private int totalNumOfVideo = 1;
	private int videoNum = 0;
	
	private Future<?> fu;
	private Process p;
	
	public TaskData(int num) {
		this.taskNum = num;
	}
	

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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
		if(this.status.equals(status)) return;
		
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

	public boolean isChecked() {
		return checked;
	}


	public void setChecked(boolean checked) {
		this.checked = checked;
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}


	public int getTaskNum() {
		return taskNum;
	}

	public void done() {
		fu = null;
		p = null;
		setProgress(100);
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
	
	public void setNowVideoNum(int now) {
	  videoNum = now;
	}
	
	
	public int getNowVideoNum() {
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


	@Override
	public String toString() {
		return "Task : " + taskNum + " dest : " + dest + "video url) : " + url;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof TaskData)) {
			return false;
		}
		TaskData other = (TaskData) obj;
		return Objects.equals(dest, other.dest)	&& Objects.equals(url, other.url);
	}
	
}