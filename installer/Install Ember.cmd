@echo off
title Ember phone installer
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1"
if errorlevel 1 (
  echo.
  echo Installation did not finish. Read the message above, then try again.
  pause
)
