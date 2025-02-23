Set-Location $PSScriptRoot

$builds = ".\target\builds"
$githubRelease = ".\target\builds\githubRelease"

New-Item -Path $githubRelease -ItemType Directory -Force | Out-Null

Get-ChildItem -Path "$builds\jpackage" | ForEach-Object {
    $newName = $_.Name -replace '[\-_]\d\.\d\.\d',''
    Copy-Item -Path $_.FullName -Destination (Join-Path $githubRelease $Newname) -Force
}

Compress-Archive -DestinationPath "$githubRelease\Clipboard-dl.zip" -Path "$builds\Clipboard-dl\*" -Force

Compress-Archive -DestinationPath "$githubRelease\Clipboard-dl_launch4j_jre.zip" -Path "$builds\launch4j\*" -Force

$excludeJRE = Get-ChildItem -Path "$builds\launch4j" -Exclude "jre"
Compress-Archive -DestinationPath "$githubRelease\Clipboard-dl_launch4j.zip" -Path $excludeJRE -Force

Pause
