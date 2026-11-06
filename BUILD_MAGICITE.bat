@echo off
setlocal
cd /d "%~dp0"
title Memoria Magicite - Forge 1.20.1 Build
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0BUILD_MAGICITE.ps1"
echo.
echo If the build failed, send BUILD_LOG.txt or a screenshot of the error.
echo DO NOT copy uncompiled project files into your Minecraft mods folder.
pause
