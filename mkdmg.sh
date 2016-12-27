#!/usr/bin/env bash -x

SRC="$1"
DEST="$2"
VOLUME="$3"
DMG_PATH="${DEST}${VOLUME}.dmg"

echo Source: ${SRC}
echo Destination: ${DEST}
echo Volume: ${VOLUME}
echo DMG-Path: ${DMG_PATH}

TEMP="TEMPORARY"

## cd $BASE

/usr/bin/hdiutil create -srcfolder "${SRC}" "${DMG_PATH}"
/usr/bin/hdiutil internet-enable -yes "${DMG_PATH}"
