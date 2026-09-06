@echo off
rem Mo NSO ban LOCAL bang MicroEmulator va ghi lai moi thu vao log.txt.
rem Ban nay noi vao 127.0.0.1:14444 -- tuc la may chu chay tren CHINH may nay.
rem Phai bat may chu truoc (work\server\NSO_KEM\start-server.bat) roi moi chay file nay.
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem === Tim Java ===
set "JAVA=jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=..\Micro_AngelChip\jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=..\jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

rem === Tu chon ban MOI NHAT trong thu muc ===
set "GAME="
for /f "delims=" %%f in ('dir /b /o-n NSO-*.jar 2^>nul') do if not defined GAME set "GAME=%%f"
if not defined GAME (
  echo.
  echo   KHONG TIM THAY file NSO-*.jar nao trong thu muc nay.
  echo.
  pause
  exit /b 1
)

echo ================================================== > log.txt
echo Game: %GAME% >> log.txt
echo May chu: 127.0.0.1:14444 ^(local^) >> log.txt
"%JAVA%" -version >> log.txt 2>&1
echo ================================================== >> log.txt

echo.
echo   Dang mo %GAME% ...
echo   May chu: 127.0.0.1:14444 (phai bat server truoc thi moi vao duoc)
echo.
echo   CUA SO DEN NAY PHAI DE NGUYEN, dong la game tat theo.
echo   Khong thay chu gi chay la binh thuong, moi thu ghi vao log.txt.
echo.
echo   ** KHI GAME BI TREO: bam vao cua so den nay roi nhan Ctrl+Break
echo      (ban phim khong co phim Break thi nhan Ctrl+Pause, hoac
echo      Ctrl+Fn+B). Man hinh khong doi gi ca, nhung log.txt se ghi
echo      lai vi tri game dang ket.
echo.

"%JAVA%" -cp "lib\*" org.microemu.app.Main "%GAME%" >> log.txt 2>&1

rem === Java qua cu thi bao thang ra, khoi phai doan ===
findstr /c:"UnsupportedClassVersionError" log.txt >nul 2>&1
if not errorlevel 1 (
  echo.
  echo   ***********************************************************
  echo   JAVA TREN MAY NAY QUA CU so voi ban game nay.
  echo   Can Java 8 tro len.
  echo   ***********************************************************
)

echo.
echo   Game da dong. Log nam trong log.txt.
pause
