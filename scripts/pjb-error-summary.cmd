@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
set "POWERSHELL_EXE=powershell.exe"

where %POWERSHELL_EXE% >nul 2>nul
if errorlevel 1 (
    echo PowerShell nao encontrado no PATH.
    exit /b 1
)

%POWERSHELL_EXE% -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%pjb-error-summary.ps1" %*
exit /b %ERRORLEVEL%
