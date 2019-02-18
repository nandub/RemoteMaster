@echo off
rem The next line copies the output dll from a C# build to the work directory
copy \Users\graha\source\repos\RMIRWin10BLE\RMIRWin10BLE\bin\Debug\RMIRWin10BLE.dll work
copy ..\lib\*.* work
..\bin\proxygen.exe work\RMIRWin10BLE.dll -wd work
cd work
call build.cmd
cd ..
rem The next three lines copy output files from proxy generation to the RemoteMaster\rmirwin10ble
rem    folder of the Eclipse IDE, ready for building RMIR
copy work\jvm\rmirwin10ble\*.* \Users\graha\workspace\RemoteMaster\rmirwin10ble
copy work\RMIRWin10BLE.j4n.* \Users\graha\workspace\RemoteMaster\rmirwin10ble
copy work\RMIRWin10BLE.dll \Users\graha\workspace\RemoteMaster\rmirwin10ble

