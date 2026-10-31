@echo off
title Launch Skim Emulator
echo ========================================================
echo   Launching Android Emulator (Medium Phone API 36.1)
echo ========================================================
start "" "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_36.1
echo Waiting for device to appear...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" wait-for-device
echo Emulator connected! Waiting for system boot...
:wait_boot
for /f "tokens=*" %%i in ('"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell getprop sys.boot_completed 2^>nul') do set boot=%%i
if not "%boot%"=="1" (
    timeout /t 2 /nobreak >nul
    goto wait_boot
)
echo Boot complete! Unlocking screen...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell input keyevent 82
echo Installing latest build from C:\Users\Amit\Kazatune...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" install -r "C:\Users\Amit\Kazatune\app\build\outputs\apk\debug\app-debug.apk"
echo Launching Skim...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell am start -n com.kazatune.debug/com.kazatune.MainActivity
echo ========================================================
echo   Skim is now running in the emulator window!
echo ========================================================
timeout /t 5

