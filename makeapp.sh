#!/bin/bash
# $@ - all command line params
# $1 - first param
# $# - number of command line params

if [ $# != 1 ]; then
    echo "makeapp <version>";
    echo "Example: ./makeapp B.1"
else

VER=$1

# Deal with icon image
#cd images
echo `pwd`
# . makeicons.sh icon.png chesslr
#cd ..
cp images/chesslr-macOS_128.png ChessLR.iconset/icon_128x128.png
iconutil --convert icns ChessLR.iconset

# Create the JAR file
cp images/chesslr-macOS_128.png src/main/java/img/icon.png
mvn package
#cp -v target/chesslr-${VER}.jar ~/Public/chesslr-${VER}.jar

#Check if file doesn't exists, remove the "!" to make it check for the file
if [ ! -f target/chesslr-${VER}.jar ]; then
	echo "chesslr jar doesn't exist"
	exit
fi

# Make the Mac application bundle
javapackager -deploy \
    -title "ChessLR" \
    -name "ChessLR" \
    -appclass com.axorion.chessboard.ChessLR \
    -native image \
    -Bicon=ChessLR.icns \
    -outdir dist \
    -outfile ChessLR.app \
    -srcfiles target/chesslr-${VER}.jar

cp -v target/chesslr-${VER}.jar   ~/Public/chesslr
cp -v makeexe.bat               ~/Public/chesslr
cp -v images/icon.ico           ~/Public/chesslr

# check if exe already exists and remove if so
#Check if file doesn't exists, remove the "!" to make it check for the file
if [ -f  ~/Public/chesslr/dist/bundles/ChessLR-${VER}.exe ]; then
	rm ~/Public/chesslr/dist/bundles/ChessLR-${VER}.exe
fi

#Check if file doesn't exists, remove the "!" to make it check for the file
if [ ! -d dist/bundles/ChessLR.app ]; then
	echo "App File doesn't exists"
else
    cd dist/bundles
    zip -r ChessLR-${VER}.zip ChessLR.app
    cd ../..
fi

echo Run:  makeexe.bat on Windows to create Windows version.
echo Then: cp ~/Public/chesslr/dist/bundles/ChessLR-${VER}.exe dist/bundles/

# echo If you wanted your icon updated, run makeicons.sh icon.png chesslr from the images directory
fi
