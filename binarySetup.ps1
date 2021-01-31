#Set-ExecutionPolicy RemoteSigned

Set-Location $PSScriptRoot

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

Write-output "Making Directory..."
New-Item -Force -Type Directory -Path YoutubeAudioAutoDownloader-resources

Write-output "Downloading ffmpeg..."
(new-object System.Net.WebClient).DownloadFile('https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip', '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip')

Write-output "Expanding Archive..."
Expand-Archive -LiteralPath '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip' -DestinationPath '.\YoutubeAudioAutoDownloader-resources'
Rename-Item '.\YoutubeAudioAutoDownloader-resources\ffmpeg-4.3.1-2021-01-26-essentials_build' -NewName 'ffmpeg'
Remove-Item '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip'

Write-output "ownloading youtube-dl..."
(new-object System.Net.WebClient).DownloadFile('https://github.com/ytdl-org/youtube-dl/releases/download/2021.01.16/youtube-dl.exe', '.\YoutubeAudioAutoDownloader-resources\ffmpeg\bin\youtube-dl.exe')


Write-output "Done!"