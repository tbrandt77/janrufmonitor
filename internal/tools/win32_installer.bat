@echo off
cd ghostinstaller
attrib -r /S
cd ..
cd win32-installer
call ..\ghostinstaller\GIBuild.exe win32-installer.gpr -nowait
cd..
cd ghostinstaller
attrib +r /S
cd ..