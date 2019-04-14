#!/bin/bash
# $@ - all command line params
# $1 - first param
# $# - number of command line params

if [ $# != 2 ]; then
    # Type your script name below
    echo "deploy.sh <version> <username>";
    echo "Example: ./deploy.sh 1.1 username"
else
    VER=$1
    USERNAME=$2

    # cd into directory so md5 doesn't include the path
    cd dist/bundles

    #Check if file doesn't exists, remove the "!" to make it check for the file
    if [ ! -f ChessLR-${VER}.exe ]; then
    	echo "ChessLR-${VER}.exe doesn't exists"
    else
        echo "Calculated hash for ChessLR-${VER}.exe"
        shasum -a 512 ChessLR-${VER}.exe > ChessLR-${VER}.exe.sha512.txt
        md5 ChessLR-${VER}.exe > ChessLR-${VER}.exe.md5.txt

        echo Deploying Windows version to 8BitCoder.com
        scp ChessLR-${VER}.exe ${USERNAME}@8BitCoder.com:www/downloads
        scp ChessLR-${VER}.exe.md5.txt ${USERNAME}@8BitCoder.com:www/downloads
        scp ChessLR-${VER}.exe.sha512.txt ${USERNAME}@8BitCoder.com:www/downloads
    fi

    #Check if file doesn't exists, remove the "!" to make it check for the file
    if [ ! -f ChessLR-${VER}.zip ]; then
    	echo "ChessLR-${VER}.zip doesn't exists"
    else
        echo "Calculated hash for ChessLR-${VER}.zip"
        shasum -a 512 ChessLR-${VER}.zip > ChessLR-${VER}.zip.sha512.txt
        md5 ChessLR-${VER}.zip > ChessLR-${VER}.zip.md5.txt

        echo Deploying Mac version to 8BitCoder.com
        scp ChessLR-${VER}.zip ${USERNAME}@8BitCoder.com:www/downloads
        scp ChessLR-${VER}.zip.md5.txt ${USERNAME}@8BitCoder.com:www/downloads
        scp ChessLR-${VER}.zip.sha512.txt ${USERNAME}@8BitCoder.com:www/downloads
    fi

    cd ../..
fi
