package io.github.awidesky.YoutubeAudioAutoDownloader.util.workers;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.awidesky.YoutubeAudioAutoDownloader.Config;
import io.github.awidesky.YoutubeAudioAutoDownloader.Main;
import io.github.awidesky.YoutubeAudioAutoDownloader.enums.ClipBoardOption;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class ClipBoardListeningThread extends Thread implements ClipboardOwner {

	private LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	private Set<ClipBoardListeningThread.InputHistory> history = new HashSet<>();
	private Logger logger = Main.getLogger("[ClipBoardChecker] ");
	private final int resetInterval;
	
	/**
	 * @param clipboardOwnershipResetInterval check the clipboard every {@code checkInterval} milliseconds.
	 * */
	public ClipBoardListeningThread(int clipboardOwnershipResetInterval) {
		super("ClipBoardChecker");
		this.resetInterval = clipboardOwnershipResetInterval;
		this.setDaemon(true);
	}

	@Override
	public void run() {
		while(true) {
			try {
				Runnable r = null;
				if (resetInterval == -1) {
					r = queue.take();
				} else {
					r = queue.poll(resetInterval, TimeUnit.MILLISECONDS);
					if(r == null) { // reset Clipboard ownership regularly
						Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
						try {
							if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
								cb.setContents(new StringSelection((String) cb.getData(DataFlavor.stringFlavor)), this);
							}
						} catch (UnsupportedFlavorException | IOException e) {
							logger.log("Unable to reset ");
							logger.log(e);
						}
						continue;
					}
				}
				r.run();
			} catch (InterruptedException e) {
				 logger.log("ClipBoardCheckerThread Interrupted!");
				 logger.log(e);
			}
		}
	}
	
	public void submit(FlavorEvent e) {
		long l = System.currentTimeMillis();
		queue.offer(() -> checkClipBoard(l));
	}

	private void checkClipBoard(long now) {
		history.removeIf(h -> now - h.timeStamp > 100);
		logger.logVerbose("[debug] Executing clipboard checking process fired by system clipboard listner...");
		
		if (Config.getClipboardListenOption() == ClipBoardOption.NOLISTEN) {
			logger.logVerbose("[debug] Clipboard ignored due to ClipboardListenOption == \"" + ClipBoardOption.NOLISTEN.getString() + "\"");
			return;
		}
		
		try {
			//https://stackoverflow.com/questions/51797673/in-java-why-do-i-get-java-lang-illegalstateexception-cannot-open-system-clipboaS
			Thread.sleep(50);	//Wait small amount of time for the clipboard to be "ready"

			Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (!cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
				logger.logVerbose("[debug] Non-String Clipboard input!");
				logger.logVerbose("[debug] Clipboard data Flavor(s) : " + Arrays.stream(cb.getAvailableDataFlavors())
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
			cb.setContents(new StringSelection(data), this); // reset clipboard ownership so that we won't miss another
															 // clipboard event

			logger.logVerbose("[debug] Clipboard : " + data);
			if (!Config.isLinkAcceptable(data)) {
				logger.logVerbose("[debug] " + data + " is not acceptable!");
				return;
			}
			
			if (history.stream().anyMatch(h -> h.sameHash(data))) {
				logger.logVerbose("[debug] Duplicate input, ignore : " + data);
				history.add(new InputHistory(data, System.currentTimeMillis()));
				return;
			}

			if (Config.getClipboardListenOption() == ClipBoardOption.ASK) {
				boolean d = SwingDialogs.confirm("Download link from clipboard?", "Link : " + data);
				logger.logVerbose("[GUI.linkAcceptChoose] " + (d ? "Accepted" : "Declined") + " download link " + data);
				history.add(new InputHistory(data, System.currentTimeMillis()));
				if (!d) return;
			}
			
			Arrays.stream(data.split(Pattern.quote("\n"))).forEach(Main::submitDownload);
			history.add(new InputHistory(data, System.currentTimeMillis()));
			
		} catch (InterruptedException | HeadlessException | IllegalStateException | UnsupportedFlavorException | IOException | NullPointerException e1) {
			SwingDialogs.error("Error when checking clipboard!", "%e%", e1, true);
		}
	}
	
	private static class InputHistory {

		private final static String hash(String input) {
			try {
				byte[] hash = MessageDigest.getInstance("SHA-1").digest(input.getBytes("UTF-8")); //TODO : SHA-516 for security
				StringBuffer ret = new StringBuffer();
				for (byte b : hash) {
					ret.append(String.format("%02x", b));
				}
				return ret.toString();
			} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
				SwingDialogs.error("Clipboard input hash error", "%e%", e, true);
				return "";
			}
		}
		
		final String hash;
		final long timeStamp;
		
		public InputHistory(String hash, long timeStamp) {
			this.hash = hash(hash);
			this.timeStamp = timeStamp;
		}
		
		public boolean sameHash(String plainString) {
			return hash.equals(InputHistory.hash(plainString));
		}
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		logger.logVerbose("Lost ownership from clipboard \"" + clipboard.getName() + "\"");
	}
	
}
