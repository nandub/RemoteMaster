@echo off
rem NOTE: The three files wcl*.dll need to be already present in the work directory
rem before running this command file.
rem
rem The next line copies the output dll from a C# build to the work directory
copy \Users\graha\source\repos\RMIRWCLble\RMIRWCLble\bin\Debug\RMIRWCLble.dll work
copy ..\lib\*.* work
..\bin\proxygen.exe work\RMIRWCLble.dll -wd work
cd work
call build.cmd
cd ..
rem The next three lines copy files to the RemoteMaster\rmirwin10ble that have not already
rem    been copied there from generateProxies of RMIRwin10BLE
copy work\jvm\rmirwin10ble\WCLble.java \Users\graha\workspace\RemoteMaster\rmirwin10ble
copy work\RMIRWCLble*.dll \Users\graha\workspace\RemoteMaster\rmirwin10ble
rem Now copy the wcl*.dll files to this same folder
copy work\wcl*.dll \Users\graha\workspace\RemoteMaster\rmirwin10ble
