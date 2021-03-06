#!/bin/bash
# $@ - all command line params
# $ 1 - first param (no space)
# $# - number of command line params

# Exports the display so you can run remotely, kills any existing instances, turns off screen saver, runs the app.
#
# Should be run on the remote machine. You can use a command like the following
# on your local machine to run it on the remote machine.
#
#     ssh pi@botfly ./runchesslr.sh
#
# Since you need to run as root to allow access to i2c ports, make java run as root.
# sudo chmod 4755 /etc/alternatives/java

export DISPLAY=:0
killall java
cd chesslr
xset s off
java $1 -classpath ../lib/'*':target/classes com.axorion.chesslr.ChessLR

