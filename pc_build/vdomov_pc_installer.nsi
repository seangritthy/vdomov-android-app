; NSIS Script for VDOmov Windows PC 1-Click Installer
!define APP_NAME "VDOmov"
!define APP_VERSION "1.0.6"
!define APP_PUBLISHER "VDOmov"
!define APP_URL "https://www.vdomov.com"

Name "${APP_NAME}"
OutFile "../vdomov-pc.exe"
InstallDir "$LOCALAPPDATA\VDOmov"
RequestExecutionLevel user
ShowInstDetails hide
AutoCloseWindow true

Section "MainSection" SEC01
    SetOutPath "$INSTDIR"

    ; Create Windows batch launcher for standalone App mode
    FileOpen $0 "$INSTDIR\VDOmov.bat" w
    FileWrite $0 "@echo off$\r$\n"
    FileWrite $0 "start msedge --app=${APP_URL} --user-data-dir=%LOCALAPPDATA%\VDOmov\Profile --title=${APP_NAME}$\r$\n"
    FileClose $0

    ; Create VBScript launcher (hides console window)
    FileOpen $0 "$INSTDIR\VDOmov.vbs" w
    FileWrite $0 'Set WshShell = CreateObject("WScript.Shell")$\r$\n'
    FileWrite $0 'WshShell.Run chr(34) & "$INSTDIR\VDOmov.bat" & chr(34), 0$\r$\n'
    FileWrite $0 'Set WshShell = Nothing$\r$\n'
    FileClose $0

    ; Create Desktop Shortcut
    CreateShortCut "$DESKTOP\VDOmov.lnk" "$INSTDIR\VDOmov.vbs" "" "$SYSDIR\shell32.dll" 14

    ; Create Start Menu Shortcuts
    CreateDirectory "$SMPROGRAMS\VDOmov"
    CreateShortCut "$SMPROGRAMS\VDOmov\VDOmov.lnk" "$INSTDIR\VDOmov.vbs" "" "$SYSDIR\shell32.dll" 14
    CreateShortCut "$SMPROGRAMS\VDOmov\Uninstall VDOmov.lnk" "$INSTDIR\Uninstall.exe"

    ; Write Uninstaller
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    ; Auto-launch VDOmov on 1-click install completion
    Exec '"$INSTDIR\VDOmov.vbs"'
SectionEnd

Section "Uninstall"
    Delete "$DESKTOP\VDOmov.lnk"
    Delete "$SMPROGRAMS\VDOmov\VDOmov.lnk"
    Delete "$SMPROGRAMS\VDOmov\Uninstall VDOmov.lnk"
    RMDir "$SMPROGRAMS\VDOmov"
    RMDir /r "$INSTDIR"
SectionEnd
