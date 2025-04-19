package io.github.awidesky.YoutubeClipboardAutoDownloader;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import io.github.awidesky.YoutubeClipboardAutoDownloader.gui.TaskStatusModel;
import io.github.awidesky.guiUtil.TaskLogger;

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
	
	private AtomicReference<String> err = new AtomicReference<String>(null);
	
	/** Is this task finished or failed? */
	private Status st = Status.RUNNING;
	
	private Future<?> fu;
	private Process p;
	
	public TaskData(int num, TaskLogger logger, boolean audioMode) {
		this.taskNum.set(num);
		this.logger = logger;
		this.audioMode.set(audioMode);
	}
	

	public String getUrl() { return url.get(); }
	public void setUrl(String url) { this.url.set(url); }
	
	public String getVideoName() { return videoName.get(); }
	public void setVideoName(String videoName) {
		this.videoName.set(videoName);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	public String getStatus() { return status.get(); }
	public void setStatus(String status) {
		if(this.status.get().equals(status)) return;
		this.status.set(status);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	
	public int getProgress() { return progress.get(); }
	public void setProgress(int progress) {
		this.progress.set(progress);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}
	
	public String getDest() { return dest.get(); }
	public void setDest(String dest) {
		this.dest.set(dest);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}

	public boolean isChecked() { return checked.get(); }
	public void setChecked(boolean checked) {
		this.checked.set(checked);
		SwingUtilities.invokeLater(() -> TaskStatusModel.getinstance().updated(this));
	}

	public int getTaskNum() { return taskNum.get(); }

	public void finished() {
		st = Status.DONE;
		setStatus("Done!");
		setProgress(100);
		fu = null;
		p = null;
		logger.close();
	}
	
	public void failed() {
		st = Status.FAILED;
		kill();
		setStatus("ERROR");
		setProgress(-1);
		setVideoName(url.get());
		fu = null;
		p = null;
		logger.close();
	}

	public boolean isFinished() { return st == Status.DONE; } //TODO : future.wait to see exceptions
	public boolean isRunning() { return st == Status.RUNNING; }
	public boolean isFailed() { return st == Status.FAILED; }

	public void setTotalNumVideo(int vdnum) { totalNumOfVideo.set(vdnum); }
	public int getTotalNumVideo() { return totalNumOfVideo.get(); }
	
	public void setNowVideoNum(int now) { videoNum.set(now); }
	public int getNowVideoNum() { return videoNum.get(); }

	public boolean isAudioMode() { return audioMode.get(); }

	public void addErrorMessage(String errorMessage) { err.set(errorMessage); }

	public void kill() {
		if(!isFinished()) {
			if (p != null) p.destroy();
			if (fu != null) fu.cancel(true);
			logger.log("[Canceled] Task number " + taskNum + " has killed!");
		}
	}

	public String getProgressToolTip() {
		return progress + "%" + ( (totalNumOfVideo.get() > 1) ? " (" + videoNum.get() + "/" + totalNumOfVideo.get() + ")" : "" );
	}
	public String getStatusToolTip() {
		String e = err.get();
		return e == null ? status.get() : err.get();
	}

	public void setFuture(Future<?> submit) { fu = submit; }

	public void setProcess(Process p) { this.p = p;	}

	@Override
	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println("Task #" + taskNum);
		pw.println("URL : " + url.get());
		pw.println("Status : " + status.get());
		pw.println("Progress : " + progress.get() + "%");
		pw.println("Destination : " + dest.get());
		if(totalNumOfVideo.get() > 1) pw.println("playlist index : " + videoNum.get() + " of " + totalNumOfVideo.get());
		pw.println("Download mode : " + (audioMode.get() ? "audio only" : "video"));
		
		if(err.get() != null) {
			pw.println();
			pw.println("yt-dlp error :");
			pw.println(err.get());
		}
		
		pw.flush(); pw.close();
		return sw.toString();
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
		return Objects.equals(dest.get(), other.dest.get())	&& Objects.equals(url.get(), other.url.get());
	}
	
	private static enum Status { RUNNING, DONE, FAILED; }

}
