@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "PROJECT_ROOT=%%~fI"
set "POWERSHELL_EXE=powershell.exe"

where %POWERSHELL_EXE% >nul 2>nul
if errorlevel 1 (
    echo PowerShell nao encontrado no PATH.
    exit /b 1
)

%POWERSHELL_EXE% -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%pjb-api-clean-test-errors.ps1" -ProjectRoot "%PROJECT_ROOT%" %*
exit /b %ERRORLEVEL%
