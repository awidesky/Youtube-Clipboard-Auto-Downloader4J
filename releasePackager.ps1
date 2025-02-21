$builds = ".\target\builds"

Get-ChildItem -Path "$builds\jpackage" | Rename-Item -NewName {($_.Basename -replace "[\-_]\d\.\d\.\d","") + $_.extension}

Compress-Archive -DestinationPath "$builds\Clipboard-dl.zip" -Path "$builds\Clipboard-dl\*"

Compress-Archive -DestinationPath "$builds\Clipboard-dl_launch4j_jre.zip" -Path "$builds\launch4j\*"

Remove-Item -Path $builds\launch4j\jre -Force -Recurse

Compress-Archive -DestinationPath "$builds\Clipboard-dl_launch4j.zip" -Path "$builds\launch4j\*"