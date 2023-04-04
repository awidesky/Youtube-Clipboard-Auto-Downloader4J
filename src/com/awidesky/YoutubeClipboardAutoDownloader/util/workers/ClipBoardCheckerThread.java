package com.awidesky.YoutubeClipboardAutoDownloader.util.workers;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.Config;
import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.enums.ClipBoardOption;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;

public class ClipBoardCheckerThread extends Thread {

	private LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	private Logger logger = Main.getLogger("[ClipBoardChecker] ");
	private String clipboardBefore = "";
	
	public ClipBoardCheckerThread() {
		this.setDaemon(true);
	}

	@Override
	public void run() {
		while(true) {
			 try {
			 	 queue.take().run();
			} catch (InterruptedException e) {
				 logger.log("ClipBoardCheckerThread Interrupted!");
				 logger.log(e);
			}
		}
	}
	
	public void submit(FlavorEvent e) { queue.offer(this::checkClipBoard); }

	private void checkClipBoard() {
		if (Config.getClipboardListenOption() == ClipBoardOption.NOLISTEN) {
			logger.logVerbose("[debug] clipboard ignored due to ClipboardListenOption == \"" + ClipBoardOption.NOLISTEN.getString() + "\"");
			return;
		}
		try {
			Thread.sleep(50);

			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (!cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
				logger.logVerbose("[debug] Non-String Clipboard input!");
				logger.logVerbose("[debug] clipboard data Flavor(s) : " + Arrays.stream(cb.getAvailableDataFlavors())
						.map(DataFlavor::getHumanPresentableName).collect(Collectors.joining(", ")));
				logger.logVerbose("[debug] These can be represented as :");
				Arrays.stream(cb.getAvailableDataFlavors()).map(t -> {
					try {
						return cb.getData(t);
					} catch (UnsupportedFlavorException | IOException e) {
						logger.log(e);
						return null;
					}
				}).filter(Objects::nonNull).forEach(o -> logger.logVerbose("[debug] \t" + o));
				return;
			}
			final String data = (String) cb.getData(DataFlavor.stringFlavor);
			cb.setContents(new StringSelection(data), null); // reset clipboard ownership so that we won't miss another
															 // clipboard event
			if (clipboardBefore.equals(data)) {
				logger.logVerbose("[debug] Duplicate input, ignore : " + data);
				clipboardBefore = "";
				return;
			}
			clipboardBefore = data;
			
			logger.logVerbose("[debug] Clipboard : " + data);
			if (!Config.isLinkAcceptable(data)) {
				logger.logVerbose("[debug] " + data + " is not acceptable!");
				return;
			}

			if (Config.getClipboardListenOption() == ClipBoardOption.ASK) {
				boolean d = SwingDialogs.confirm("Download link from clipboard?", "Link : " + data);
				logger.logVerbose("[GUI.linkAcceptChoose] " + (d ? "Accepted" : "Declined") + " download link " + data);
				if (!d)
					return;
			}

			Arrays.stream(data.split(Pattern.quote("\n"))).forEach(Main::submitDownload);

		} catch (InterruptedException | HeadlessException | UnsupportedFlavorException | IOException e1) {
			SwingDialogs.error("Error when checking clipboard!", "%e%", e1, true);
		}
	}
	
}