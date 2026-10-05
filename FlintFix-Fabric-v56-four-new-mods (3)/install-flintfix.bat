@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0install-flintfix.ps1" %*
set "FLINTFIX_EXIT=%ERRORLEVEL%"
echo.
if not "%FLINTFIX_EXIT%"=="0" echo FlintFix install failed. See the error above.
pause
exit /b %FLINTFIX_EXIT%
