#!/bin/bash
# $@ - all command line params
# $ 1 - first param (no space)
# $# - number of command line params

# Copies the compiled classes to the target machine, then runs the app remotely.

scp -r target/classes pi@botfly:chesslr/target
scp scripts/runchesslr.sh pi@botfly:.
