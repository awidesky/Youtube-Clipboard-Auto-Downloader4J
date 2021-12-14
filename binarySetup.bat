@ECHO OFF

powershell -Noprofile -Executionpolicy Unrestricted .\binarySetup.ps1

echo.

set /p YN="Want to make jre for build Windows standalone application (Y/N)? "

if /i "%YN%" == "y" goto jre
if /i "%YN%" == "n" goto end

:jre
echo.
jremaker.bat
goto end

:end
pause
