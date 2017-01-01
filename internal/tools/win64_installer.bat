@echo off
cd ghostinstaller
attrib -r /S
cd ..
cd win64-installer
call ..\ghostinstaller\GIBuild.exe win64-installer.gpr -nowait
cd..
cd ghostinstaller
attrib +r /S
cd ..
