#Set-ExecutionPolicy RemoteSigned

Set-Location $PSScriptRoot
$ErrorActionPreference = "Inquire"

$ffmpeg_version='ffmpeg-4.3.1-2021-01-26-essentials_build'
$youtubedl_version='20210116'

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12



if (Test-Path -Path '.\YoutubeAudioAutoDownloader-resources') { Remove-Item -Path '.\YoutubeAudioAutoDownloader-resources' -Recurse }


Write-output "  Making Directory..."
New-Item -Force -Type Directory -Path YoutubeAudioAutoDownloader-resources


if (Test-Path -Path '.\YoutubeAudioAutoDownloader-resources\ydldate.txt') { Remove-Item -Path '.\YoutubeAudioAutoDownloader-resources\ydldate.txt' }

Set-content -Path '.\YoutubeAudioAutoDownloader-resources\ydldate.txt' -Value $youtubedl_version

Write-output "  Downloading ffmpeg..."
(new-object System.Net.WebClient).DownloadFile("https://www.gyan.dev/ffmpeg/builds/packages/$ffmpeg_version.zip", '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip')

Write-output "  Expanding Archive..."
Expand-Archive -LiteralPath '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip' -DestinationPath '.\YoutubeAudioAutoDownloader-resources'
Write-Output "  Renaming Archive..."
Rename-Item ".\YoutubeAudioAutoDownloader-resources\$ffmpeg_version" -NewName "ffmpeg"
Remove-Item '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip'

Write-output "  Downloading youtube-dl..."
(new-object System.Net.WebClient).DownloadFile("https://github.com/ytdl-org/youtube-dl/releases/download/$youtubedl_version/youtube-dl.exe", '.\YoutubeAudioAutoDownloader-resources\ffmpeg\bin\youtube-dl.exe')


Write-output "  Done!"