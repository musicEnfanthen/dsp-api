#!/usr/bin/env bash

set -e

# Variables

GREEN='\033[0;32m'
PURPLE='\033[0;35m';
RED='\033[0;31m'
NO_COLOUR='\033[0m'
DELIMITER="*********\n* "

if [ "$(uname)" == "Darwin" ]; then
    # Do something under Mac OS X platform
    MACHINE="Mac"
    TSLOADFILE="graphdb-free-init-knora-test.sh"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # Do something under GNU/Linux platform
    MACHINE="Linux"
    TSLOADFILE="graphdb-free-init-knora-test.sh"
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW32_NT" ]; then
    # Do something under 32 bits Windows NT platform
    MACHINE="MinGW 32bit"
    TSLOADFILE="win-graphdb-free-init-knora-test.sh"
elif [ "$(expr substr $(uname -s) 1 10)" == "MINGW64_NT" ]; then
    # Do something under 64 bits Windows NT platform
    MACHINE="MinGW 64bit"
    TSLOADFILE="win-graphdb-free-init-knora-test.sh"
fi

# Start script

printf "${PURPLE}${DELIMITER}Executing script on ${MACHINE}${NO_COLOUR}\n\n"

printf "${PURPLE}${DELIMITER}Loading ontologies into triplestore${NO_COLOUR}\n\n"

# load file corresponding to system
source $TSLOADFILE

printf "${PURPLE}${DELIMITER}Ontologies loaded into triplestore${NO_COLOUR}\n\n"

printf "${PURPLE}${DELIMITER}Change to folder: webapi${NO_COLOUR}\n\n"

cd ..
pwd

printf "${PURPLE}${DELIMITER}Start sbt${NO_COLOUR}\n\n"

sbt
