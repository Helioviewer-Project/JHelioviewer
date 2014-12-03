#!/bin/bash

 # make sure we are in the correct dir when we double-click a .command file
dir=${0%/*}
if [ -d "$dir" ]; then
  cd "$dir"
fi

APP_NAME=$(pwd)"/build/macApp/JHelioviewer.app"
DEVELOPER_CERTIFICATE="Developer ID Application: Stefan Meier (7VU87NT5AU)"
BUILD_DIRECTORY=$(pwd)"/build/"
GRANDLE="gradle-2.1/bin/gradle"

clear
printf "Build app with gradle \n"
printf "**************************************\n"
"$GRANDLE" -q -p "../../" build

printf "\n \n \n Start codesigning \n"
printf "**************************************\n"
codesign -f --deep -s "$DEVELOPER_CERTIFICATE" "$APP_NAME"

printf "\n \n \n test codesigning \n"
printf "**************************************\n"
spctl -a -t exec -vv "$APP_NAME"

printf "\n \n \n finished codesigning \n"
printf "**************************************\n"

printf "\n \n \n clear build directory \n"
printf "**************************************\n"

rm -r "$BUILD_DIRECTORY"classes/ "$BUILD_DIRECTORY"distributions/ "$BUILD_DIRECTORY"tmp "$BUILD_DIRECTORY"dependency-cache "$BUILD_DIRECTORY"libs "$BUILD_DIRECTORY"resources
printf "Application is ready to pack in a DMG"
