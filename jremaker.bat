@echo on

rmdir /s /q .\jre

where jlink > jreLoc.txt
set /p location=<jreLoc.txt
del jreLoc.txt

"%location%" --output jre --compress=2 --no-header-files --no-man-pages --module-path ../jmods --add-modules java.base,java.desktop,java.datatransfer

@pause