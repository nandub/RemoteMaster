@echo off
if not exist target mkdir target
if not exist target\classes mkdir target\classes


echo compile classes
javac -nowarn -d target\classes -sourcepath jvm -cp "c:\users\graha\documents\jp1\jni4net\lib\jni4net.j-0.8.8.0.jar"; "jvm\rmirwin10ble\IBleInterface.java" "jvm\rmirwin10ble\IBleInterface_.java" "jvm\rmirwin10ble\WCLble.java" 
IF %ERRORLEVEL% NEQ 0 goto end


echo RMIRWCLble.j4n.jar 
jar cvf RMIRWCLble.j4n.jar  -C target\classes "rmirwin10ble\IBleInterface.class"  -C target\classes "rmirwin10ble\IBleInterface_.class"  -C target\classes "rmirwin10ble\__IBleInterface.class"  -C target\classes "rmirwin10ble\WCLble.class"  > nul 
IF %ERRORLEVEL% NEQ 0 goto end


echo RMIRWCLble.j4n.dll 
csc /nologo /warn:0 /t:library /out:RMIRWCLble.j4n.dll /recurse:clr\*.cs  /reference:"C:\Users\graha\Documents\JP1\jni4net\RMIRWCLble\work\RMIRWCLble.dll" /reference:"C:\Users\graha\Documents\JP1\jni4net\lib\jni4net.n-0.8.8.0.dll"
IF %ERRORLEVEL% NEQ 0 goto end


:end
