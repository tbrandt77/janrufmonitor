# !/bash/sh
cd $(dirname $(readlink -f ${0}))
java -Djava.library.path=. -cp jamapi.jar:jam.jar:jamlinux.jar:hsqldb.jar:i18n.jar:swt.jar:jffix.jar de.janrufmonitor.application.RunUI