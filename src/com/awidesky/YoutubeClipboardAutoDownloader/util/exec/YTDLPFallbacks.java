package com.awidesky.YoutubeClipboardAutoDownloader.util.exec;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;
import com.awidesky.YoutubeClipboardAutoDownloader.util.Logger;
import com.awidesky.YoutubeClipboardAutoDownloader.util.SwingDialogs;

public class YTDLPFallbacks {

	private static Logger log = Main.getLogger("[runFixCommand] ");
	private static FixCommand[] fixArr = new FixCommand[] {
		new FixCommand("ERROR: unable to download video data: HTTP Error 403: Forbidden", YoutubeAudioDownloader.getYtdlpPath() + "yt-dlp", "--rm-cache-dir")
	};
	
	public static boolean runFixCommand(String err) {
		List<FixCommand> list = Arrays.stream(fixArr).filter(f -> err.startsWith(f.error)).toList();

		if(list.isEmpty()) {
			log.log("no available fix for : " + err);
			return false;
		}
		if (list.size() > 1) {
			return list.stream().filter(f -> err.equals(f.error)).map(FixCommand::runFixCommand).allMatch(b-> b);
		} else {
			return list.get(0).runFixCommand();
		}
	}
	
	
	static class FixCommand {
		
		private final String error;
		private final String[] command;
		
		FixCommand(String error, String... command) {
			this.error = error;
			this.command = command;
		}
		
		public boolean runFixCommand() {
			log.newLine();
			log.log("Found known error : \"" + error + "\"");
			log.log("Trying to fix error automatically by executing \"" + Arrays.stream(command).collect(Collectors.joining(" ")) + "\"");
			
			// start process
			try {
				log.log("Executing ended with exit code : " + ProcessExecutor.runNow(log, null, command));
			} catch (Exception e) {
				SwingDialogs.error("Error!", "Error when fixing yt-dlp problem!\n%e%", e, false);
				return false;
			}
			return true;
		}
	}
}
