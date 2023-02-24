@echo off

if exist .\jre rmdir /s /q .\jre

where jlink > jreLoc.txt
set /p location=<jreLoc.txt
del jreLoc.txt

@echo on
"%location%" --output jre --compress=2 --no-header-files --no-man-pages --module-path ../jmods --add-modules java.base,java.desktop

@pause