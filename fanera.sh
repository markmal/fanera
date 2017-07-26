#!/bin/bash

JAVA=$(which java)
#JAVA=~/opt/jdk1.8.0_131/bin/java
$JAVA -version 2>&1| grep -i '64-Bit' >/dev/null 2>&1
if [ $? -eq 0 ]; then ARCH=amd64; else ARCH=i386; fi

CP=
for J in lib/*.jar; do CP=$CP:$J; done 

LD_LIBRARY_PATH=lib/$ARCH $JAVA -jar fanera.jar $*
