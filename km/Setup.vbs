Set WshShell = WScript.CreateObject("WScript.Shell")
Set objFS = WScript.CreateObject("Scripting.FileSystemObject")

sCurrDir = objFS.GetParentFolderName(Wscript.ScriptFullName) 
WScript.Echo "Installation folder is " & sCurrDir

Sub makeShortcut( lnkFile, path, args, desc, icon, dir )
    Set shortcut = WshShell.CreateShortcut( lnkFile )
    shortcut.TargetPath = path
    shortcut.Arguments = args
    shortcut.Description = desc
    If Not IsNull(icon) then
        shortcut.IconLocation = icon
    End If
    shortcut.WindowStyle ="1"
    shortcut.WorkingDirectory = dir
    shortcut.save
End Sub

Sub associate( name, desc, icon, command, ext )
    call WshShell.RegWrite( "HKEY_CLASSES_ROOT\" & name & "\", desc, "REG_SZ" )
    call WshShell.RegWrite( "HKEY_CLASSES_ROOT\" & name & "\DefaultIcon\", icon, "REG_SZ" )
    call WshShell.RegWrite( "HKEY_CLASSES_ROOT\" & name & "\Shell\open\command\", command, "REG_SZ" )
    call WshShell.RegWrite( "HKEY_CLASSES_ROOT\" & ext & "\", name, "REG_SZ" )
End Sub

sRMFolder = WshShell.SpecialFolders("Programs") & "\Remote Master"
if Not objFS.FolderExists( sRMFolder ) Then
   objFS.CreateFolder( sRMFolder )
End If

sJavaw = WshShell.RegRead("HKLM\SOFTWARE\JavaSoft\Java Runtime Environment\1.6\JavaHome") & "\bin\javaw.exe"

if objFs.FileExists( sJavaw ) Then 
	sRunRMIR = """" & sJavaw & """ -jar """ & sCurrDir & "\RemoteMaster.jar"" -h """ & sCurrDir & """"
	sRunRM = sRunRM & " -rm" 

	call makeShortcut( sRMFolder & "\Remote Master.LNK", sJavaw,                               "-jar RemoteMaster.jar -rm",     "RemoteMaster", sCurrDir & "\RM.ICO",   sCurrDir )
	call makeShortcut( sRMFolder & "\RMIR.LNK",          sJavaw,                               "-jar RemoteMaster.jar", "RMIR",         sCurrDir & "\RMIR.ICO", sCurrDir )
	call makeShortcut( sRMFolder & "\Read Me.LNK",       sCurrDir & "\Readme.html",            "",                          "Read Me",      Null,                   sCurrDir )
	call makeShortcut( sRMFolder & "\Tutorial.LNK",      sCurrDir & "\tutorial\tutorial.html", "",                          "Tutorial",     Null,                   sCurrDir & "\tutorial" )

	call associate( "RMDeviceUpgrade", "Remote Master Device Upgrade",       sCurrDir & "\RM.ico",   sRunRM & " ""%1""", ".rmdu" )
	call associate( "RMRemoteConfig",  "Remote Master Remote Configuration", sCurrDir & "\RMIR.ico", sRunRMIR & " ""%1""", ".rmir" )
	WScript.Echo "Program shortcuts created successfully."
Else
	WScript.Echo "Program shortcuts were not created because javaw.exe was not found."
End If