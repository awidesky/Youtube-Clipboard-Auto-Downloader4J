#Set-ExecutionPolicy RemoteSigned

Set-Location $PSScriptRoot
$ErrorActionPreference = "Inquire"

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


Write-output "  Done!"