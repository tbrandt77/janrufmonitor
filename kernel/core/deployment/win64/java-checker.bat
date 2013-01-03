del java-check-result.txt
echo Java Checker for jAnrufmonitor 1.0 >> java-check-result.txt
echo ---------------------------------- >> java-check-result.txt
echo aktuelles Verzeichnis: >> java-check-result.txt
cd >> java-check-result.txt
dir >> java-check-result.txt
echo JAVA_HOME=%JAVA_HOME% >> java-check-result.txt
echo PATH=%PATH% >> java-check-result.txt
echo "java -version output:" >> java-check-result.txt
"%JAVA_HOME%\bin\java" -version 2>> java-check-result.txt
