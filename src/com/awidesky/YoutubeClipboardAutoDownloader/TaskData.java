package com.awidesky.YoutubeClipboardAutoDownloader;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import com.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;
import com.awidesky.YoutubeClipboardAutoDownloader.util.TaskLogger;

public class TaskData {
	
	public final TaskLogger logger;
	
	private AtomicReference<String> videoName = new AtomicReference<String>("null"); 
	private AtomicReference<String> url = new AtomicReference<String>("null");
	private AtomicReference<String> status = new AtomicReference<String>("");
	private AtomicInteger progress = new AtomicInteger(0);
	private AtomicReference<String> dest = new AtomicReference<String>("");
	private AtomicBoolean checked = new AtomicBoolean(false);
	private AtomicInteger taskNum = new AtomicInteger();
	private AtomicInteger totalNumOfVideo = new AtomicInteger(1);
	private AtomicInteger videoNum = new AtomicInteger(1);
	private AtomicBoolean audioMode = new AtomicBoolean(true);
	
	/** Is this task finished or failed? */
	private boolean isDone = false;
	
	private Future<?> fu;
	private Process p;
	
	public TaskData(int num, TaskLogger logger, boolean audioMode) {
		this.taskNum.set(num);
		this.logger = logger;
		this.audioMode.set(audioMode);
	}
	

	public String getUrl() {
		return url.get();
	}

	public void setUrl(String url) {
		this.url.set(url);
	}

	public String getVideoName() {
		return videoName.get();
	}
	
	public void setVideoName(String videoName) {
		this.videoName.set(videoName);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	
	public String getStatus() {
		return status.get();
	}
	
	public void setStatus(String status) {
		if(this.status.get().equals(status)) return;
		
		this.status.set(status);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	
	public int getProgress() {
		return progress.get();
	}
	
	public void setProgress(int progress) {
		this.progress.set(progress);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	
	public String getDest() {
		return dest.get();
	}
	
	public void setDest(String dest) {
		this.dest.set(dest);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}

	public boolean isChecked() {
		return checked.get();
	}


	public void setChecked(boolean checked) {
		this.checked.set(checked);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}


	public int getTaskNum() {
		return taskNum.get();
	}

	public void finished() {
		isDone = true;
		setStatus("Done!");
		setProgress(100);
		fu = null;
		p = null;
		logger.close();
	}
	
	public void failed() {
		isDone = true;
		kill();
		setStatus("ERROR");
		setProgress(-1);
		fu = null;
		p = null;
		logger.close();
	}

	public boolean isNotDone() {
		return !isDone;
	}

	public void setTotalNumVideo(int vdnum) {
		totalNumOfVideo.set(vdnum);
	}

	public int getTotalNumVideo() {
		return totalNumOfVideo.get();
	}
	
	public void setNowVideoNum(int now) {
	  videoNum.set(now);
	}
	
	
	public int getNowVideoNum() {
		return videoNum.get();
	}

	public boolean isAudioMode() {
		return audioMode.get();
	}


	public void kill() {
		if(isNotDone()) {
			if (p != null) p.destroy();
			if (fu != null) fu.cancel(true);
			logger.log("[Canceled] Task number " + taskNum + " has killed!");
		}
	}

	public String getProgressToolTip() {
		return progress + "%" + ( (totalNumOfVideo.get() > 1) ? " (" + videoNum.get() + "/" + totalNumOfVideo.get() + ")" : "" );
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