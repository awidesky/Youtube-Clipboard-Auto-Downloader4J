
Set-Location $PSScriptRoot

New-Item -Type Directory -Path YoutubeAudioAutoDownloader-resources

(new-object System.Net.WebClient).DownloadFile('https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip', '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip')
[System.IO.Compression.ZipFile]::ExtractToDirectory(".\YoutubeAudioAutoDownloader-resources\ffmpeg.zip", ".\YoutubeAudioAutoDownloader-resources\ffmpeg")
Remove-Item ".\YoutubeAudioAutoDownloader-resources\ffmpeg.zip"

(new-object System.Net.WebClient).DownloadFile('https://github.com/ytdl-org/youtube-dl/releases/download/2021.01.16/youtube-dl.exe', '.\YoutubeAudioAutoDownloader-resources\ffmpeg\bin\youtube-dl.exe')
