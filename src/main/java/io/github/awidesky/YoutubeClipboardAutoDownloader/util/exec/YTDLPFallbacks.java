package io.github.awidesky.YoutubeClipboardAutoDownloader.util.exec;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.YoutubeClipboardAutoDownloader.Main;
import io.github.awidesky.YoutubeClipboardAutoDownloader.YoutubeClipboardAutoDownloader;
import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SwingDialogs;

public class YTDLPFallbacks {

	private static Logger log = Main.getLogger("[RunFixCommand] ");
	private static FixCommand[] fixArr = new FixCommand[] {
		new FixCommand(Pattern.quote("ERROR: unable to download video data: HTTP Error 403: Forbidden"), YoutubeClipboardAutoDownloader.getYtdlpPath() + "yt-dlp", "--rm-cache-dir")
	};
	
	public static Stream<Boolean> runFixCommand(String err) {
		List<FixCommand> list = Arrays.stream(fixArr).filter(f -> f.error.matcher(err).matches()).toList();

		if(list.isEmpty()) {
			log.info("no available fix for : " + err);
			return Stream.of(Boolean.FALSE);
		}
		return list.stream().map(FixCommand::runFixCommand);
	}
	
	
	static class FixCommand {
		
		private final Pattern error;
		private final String[] command;
		
		FixCommand(String error, String... command) {
			this.error = Pattern.compile(error);
			this.command = command;
		}
		
		public boolean runFixCommand() {
			log.newLine();
			log.info("Found known error : \"" + error + "\"");
			log.info("Trying to fix error automatically by executing \"" + Arrays.stream(command).collect(Collectors.joining(" ")) + "\"");
			
			// start process
			try {
				int ret = ProcessExecutor.runNow(log, null, command);
				log.info("Executing ended with exit code : " + ret);
				return ret == 0;
			} catch (Exception e) {
				SwingDialogs.error("Error!", "Error when fixing yt-dlp problem!\n%e%", e, false);
				return false;
			}
		}
	}
}
