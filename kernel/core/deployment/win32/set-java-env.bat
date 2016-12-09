@echo off
setlocal ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

set JRE_HOME=
set JDK_HOME=

:settings

:getjrelocation
 rem Resolve location of Java runtime environment
 set KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Runtime Environment
 set Cmd=reg query "%KeyName%" /s
 for /f "tokens=2*" %%i in ('%Cmd% ^| find "JavaHome"') do set JRE_HOME=%%j

:getjdklocation
 rem Resolve location of Java JDK environment
 set KeyName=HKEY_LOCAL_MACHINE\SOFTWARE\JavaSoft\Java Development Kit
 set Cmd=reg query "%KeyName%" /s
 for /f "tokens=2*" %%i in ('%Cmd% ^| find "JavaHome"') do set JDK_HOME=%%j

:setsysenv
 rem Check if we located either of the Java environments
 if "%JRE_HOME%%JDK_HOME%"=="" goto errornojava

 rem If a Java runtime environment is located then set both JAVA_HOME and JAVA_JRE system environment variables
 if not "%JRE_HOME%"=="" (
 echo Java Runtime environment found setting JAVA_HOME and JAVA_JRE to "%JRE_HOME%"
 setx JAVA_HOME "%JRE_HOME%"
 setx JAVA_JRE "%JRE_HOME%"
 setx PATH "%JRE_HOME%\bin;%PATH%"
 )

 rem If a Java JDK environment is located then set JAVA_HOME system environment variables
 if not "%JDK_HOME%"=="" (
 echo Java JDK environment found setting JAVA_HOME "%JDK_HOME%"
 setx JAVA_HOME "%JDK_HOME%"
 setx PATH "%JDK_HOME%\bin;%PATH%"
 )

 goto end
 
:errornojava
 echo Failed to locate any installed java environments event tried locating a JDK, please install a Java Runtime Evnironment or JDK
 goto end
 
:end
endlocal