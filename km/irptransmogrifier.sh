#!/bin/sh

# This wrapper allows you to run IrpTransmogrifier with the Remotemaster distribution.
# For more information and support, see
# http://www.hifi-remote.com/forums/viewtopic.php?t=101943 ,
# https://github.com/bengtmartensson/IrpTransmogrifier , and
# http://www.harctoolbox.org/IrpTransmogrifier.html

# This file is for use with Linux and similar systems.

# It is recommended to make a symbolic link to this file
# from /usr/local/bin, i.e.
#
# ln -s <this-file> /usr/local/bin/irptransmogrifier

# The command line name to use to invoke java; change if desired.
# Must be Java 8 or higher.
JAVA=java

# Where the files are located; change if desired
RMHOME="$(dirname -- "$(readlink -f -- "${0}")" )"

# Use configfile contained in the jar.
#CONFIGFILE=${RMHOME}/IrpProtocols.xml

# Normally no need to change anything after thins line

JAR=${RMHOME}/RemoteMaster.jar
MAINCLASS=org.harctoolbox.irp.IrpTransmogrifier

exec "${JAVA}" -cp "${JAR}" "${MAINCLASS}" "$@"
