@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0repair_maven_plugin_resolution.ps1"
endlocal
