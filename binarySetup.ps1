#Set-ExecutionPolicy RemoteSigned

Set-Location $PSScriptRoot
<<<<<<< HEAD

$ffmpeg_version='ffmpeg-4.3.1-2021-01-26-essentials_build'
$youtubedl_version='2021.01.16'

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

Write-output "Making Directory..."
New-Item -Force -Type Directory -Path YoutubeAudioAutoDownloader-resources

Write-output "Downloading ffmpeg..."
(new-object System.Net.WebClient).DownloadFile("https://www.gyan.dev/ffmpeg/builds/packages/$ffmpeg_version.zip", '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip')

Write-output "Expanding Archive..."
Expand-Archive -LiteralPath '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip' -DestinationPath '.\YoutubeAudioAutoDownloader-resources'
Rename-Item ".\YoutubeAudioAutoDownloader-resources\$ffmpeg_version" -NewName "ffmpeg"
Remove-Item '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip'

Write-output "Downloading youtube-dl..."
(new-object System.Net.WebClient).DownloadFile("https://github.com/ytdl-org/youtube-dl/releases/download/$youtubedl_version/youtube-dl.exe", '.\YoutubeAudioAutoDownloader-resources\ffmpeg\bin\youtube-dl.exe')


Write-output "Done!"

pause
=======
$ErrorActionPreference = "Inquire"
$youtubedl_version=""

if (!(Test-Path -Path '.\YoutubeAudioAutoDownloader-resources')) {
    Write-output "  Making Directory..."
    New-Item -Force -Type Directory -Path YoutubeAudioAutoDownloader-resources
} elseif (Test-Path -Path '.\YoutubeAudioAutoDownloader-resources\ffmpeg') {
    Remove-Item -Path '.\YoutubeAudioAutoDownloader-resources\ffmpeg' -Recurse
}


Write-output "  Downloading ffmpeg..."

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri 'https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip' -OutFile '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip'

Write-output "  Expanding Archive..."
Expand-Archive -LiteralPath '.\YoutubeAudioAutoDownloader-resources\ffmpeg.zip' -DestinationPath '.\YoutubeAudioAutoDownloader-resources'

Write-Output "  Renaming Archive..."
$dirs = Get-ChildItem '.\YoutubeAudioAutoDownloader-resources' -filter "ffmpeg*" -Directory

foreach($d in $dirs) {
    Rename-Item "$PSScriptRoot\YoutubeAudioAutoDownloader-resources\$d" -NewName "ffmpeg" -Force
}
Remove-Item -Path ".\YoutubeAudioAutoDownloader-resources\ffmpeg.zip"

Write-output "  Downloading youtube-dl..."
Invoke-WebRequest -Uri "https://youtube-dl.org/downloads/latest/youtube-dl.exe" -OutFile  '.\YoutubeAudioAutoDownloader-resources\ffmpeg\bin\youtube-dl.exe'


if (Test-Path -Path '.\YoutubeAudioAutoDownloader-resources\ydlver.txt') {
    #$youtubedl_version = Get-Content ".\YoutubeAudioAutoDownloader-resources\ydlver.txt" -Encoding UTF8 -Raw
    Remove-Item -Path '.\YoutubeAudioAutoDownloader-resources\ydlver.txt'
}

$youtubedl_version = (.\YoutubeAudioAutoDownloader-resources\ffmpeg\bin\youtube-dl.exe --version) -join "`n"

Set-content  -NoNewline -Path '.\YoutubeAudioAutoDownloader-resources\ydlver.txt' -Value "$youtubedl_version" -Encoding UTF8

Write-output "  youtube-dl version : $youtubedl_version"



Write-output "  Done!"
>>>>>>> dev
