@echo off

REM This wrapper allows you to run IrpTransmogrifier with the Remotemaster distribution.
REM For more information and support, see
REM http://www.hifi-remote.com/forums/viewtopic.php?t=101943 ,
REM https://github.com/bengtmartensson/IrpTransmogrifier , and
REM http://www.harctoolbox.org/IrpTransmogrifier.html

REM This file is for use with Windows/DOS.

REM The command line name to use to invoke java; change if desired.
REM Must be Java 8 or higher.

REM The command line name to use to invoke java.exe, change if desired.
set JAVA=java

REM Where the files are located, change if desired
set APPLICATIONHOME=%~dp0

REM Configfile to use
set CONFIGFILE=%APPLICATIONHOME%\IrpProtocols.xml

REM Normally no need to change after this line

set MAINCLASS=org.harctoolbox.irp.IrpTransmogrifier
set JAR=%APPLICATIONHOME%\RemoteMaster.jar

"%JAVA%" -cp "%JAR%" "%MAINCLASS%" --configfile "%CONFIGFILE%" %*
