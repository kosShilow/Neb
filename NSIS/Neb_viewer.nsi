; The name of the installer
Name "Neb viewer"

; The file to write
OutFile "Install_Neb_viewer.exe"

; Request application privileges for Windows Vista
RequestExecutionLevel user

; Build Unicode installer
Unicode True

; The default installation directory
InstallDir "$LocalAppData\Programs\Neb_viewer"

;--------------------------------

; Pages

;Page directory
Page instfiles

;--------------------------------

; The stuff to install
Section "" ;No components page, name is not important

  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  File Neb_viewer-1.0.jar
  File neb.ico
  File /r lib
  File /r jdk-15

  Var /GLOBAL JAVAW
  ;  StrCpy $JAVAW "C:\Program Files\Java\jdk-15\bin\javaw.exe"
  SetOutPath "$INSTDIR"
  StrCpy $JAVAW $INSTDIR"\jdk-15\bin\javaw.exe"
  
  CreateShortcut "$DESKTOP\Ñץולû סועט.lnk" "$JAVAW" "-jar -Dfile.encoding=UTF-8 Neb_viewer-1.0.jar" "$INSTDIR\neb.ico" 0 SW_SHOWMAXIMIZED

  ; Write the uninstall keys for Windows
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer" "DisplayName" "Neb viewer"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer" "Publisher" "Kos"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer" "DisplayIcon" "$INSTDIR\neb.ico"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer" "NoRepair" 1
  WriteUninstaller "$INSTDIR\uninstall.exe"


SectionEnd ; end the section


Section "Uninstall"

  ; Remove registry keys
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\Neb viewer"

  
  Delete "$DESKTOP\Ñץולû סועט.lnk"

  RMDir /r "$INSTDIR"

SectionEnd
