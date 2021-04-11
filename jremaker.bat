@echo off
jlink --output jre-11.0.10 --compress=2 --no-header-files --no-man-pages --module-path ../jmods --add-modules java.base,java.desktop

pause