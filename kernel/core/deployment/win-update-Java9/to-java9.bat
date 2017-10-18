@echo off
del jam.exe
del jam.ini
del loader.ini
ren jam.exe.java9 jam.exe
ren to-java9.bat to-java9.bat.done
echo Migration to Java9 done.