# Youtube-Clipboard-Auto-Downloader (a.k.a. Clipboard-dl)

This program listens to user's clipboard and if you copied Youtube video link, it will download the video or audio automatically!
Just copy link with `ctrl + v` of `command + c` and you'll get audio/video file in any format, in directory where you chose!

## COMMAND LINE USAGE
```
usage : java -jar Clipboard-dl_2.0.0.jar [options]

Options :
	--help : show this help info.
	--version : show version info.
	--logbyTask : Logs from a task is gathered till the task is done/terminated.
	              Useful when you don't want to see dirty log file when multiple tasks running.
	--logTime : Log with TimeStamps
	--logOnConsole : Write log in command line console, not in a log file.
	--verbose : Print verbose logs(like GUI Windows or extra debug info, etc.)
	--ytdlpArgs=<options...> : Add additional yt-dlp options(listed at https://github.com/yt-dlp/yt-dlp#usage-and-options)
	                           that will be appended at the end(but before the url) of yt-dlp execution.
	                           If your options contains space, wrap them with ""
	                           If you need multiple options, wrap them with ""


exit codes :
	  0 : Program exited successfully as user intended
	100 : Unable to locate project root library("YoutubeClipboardAutoDownloader-resources" folder)
	200 : Failed to find ffmpeg installation
	300 : Failed to find yt-dlp installation
	 -1 : Invalid command line argument(s)
	 -2 : Task execution from GUI event dispatch thread has failed
	-100 : Unknown Error(e.g. unhandled Exception/JVM Error)
```
