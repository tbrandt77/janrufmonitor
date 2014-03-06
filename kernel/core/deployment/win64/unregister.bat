@echo off
@echo Running on Java:
java -version
@echo ...
@echo Unregistering jAnrufmonitor at Update-Server…
java -Djava.library.path=. -cp jam.jar;jamapi.jar de.janrufmonitor.application.Unregister >> logs/unregister.log
@echo ...finished !
