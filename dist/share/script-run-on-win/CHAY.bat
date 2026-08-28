@echo off
rem Mo NSO bang MicroEmulator va ghi lai moi thu vao log.txt.
rem Chay xong thi tu gui log ve may chu qua Tailscale, khoi phai di tim file.
setlocal enabledelayedexpansion
cd /d "%~dp0"

rem === Ten cua ban, de nguoi sua game biet log nay cua ai ===
rem Sua dong duoi day thanh ten nhan vat cua ban (chi chu va so, khong dau).
set "TEN=ban-be"

rem Dia chi may chu tren Tailscale. Doi o day neu may chu doi dia chi.
set "MAYCHU=100.98.117.102"
set "CONGLOG=8090"

rem === Tim Java ===
set "JAVA=jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=..\Micro_AngelChip\jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=..\jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

rem === Tu chon ban MOI NHAT trong thu muc ===
rem Truoc day ten ban ghi cung trong file nay, nen ai tai ban moi ve de canh ban cu
rem thi van chay ban cu ma khong hay biet.
set "GAME="
for /f "delims=" %%f in ('dir /b /o-n NSO-*.jar 2^>nul') do if not defined GAME set "GAME=%%f"
if not defined GAME (
  echo.
  echo   KHONG TIM THAY file NSO-*.jar nao trong thu muc nay.
  echo   Tai ban moi nhat ve va de canh file CHAY.bat.
  echo.
  pause
  exit /b 1
)

echo ================================================== > log.txt
echo Ten: %TEN% >> log.txt
echo Java: %JAVA% >> log.txt
echo Game: %GAME% >> log.txt
"%JAVA%" -version >> log.txt 2>&1
echo ================================================== >> log.txt

echo.
echo   Dang mo %GAME% ...
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
  echo   Bao nguoi dung server dung lai ban game cho phu hop.
  echo   ***********************************************************
)

rem === Gui log ve may chu ===
rem Trong log.txt khong co mat khau, chi la nhung dong ma game in ra.
rem Khong muon gui thi xoa ca doan nay di, game van chay binh thuong.
echo.
echo   Dang gui log ve may chu ...
where curl.exe >nul 2>&1
if not errorlevel 1 (
  curl.exe -s -m 20 --data-binary "@log.txt" "http://%MAYCHU%:%CONGLOG%/%TEN%"
) else (
  powershell -NoProfile -Command "try { Invoke-RestMethod -Uri 'http://%MAYCHU%:%CONGLOG%/%TEN%' -Method Post -InFile 'log.txt' -TimeoutSec 20 } catch { Write-Host 'khong gui duoc:' $_.Exception.Message }"
)

echo.
echo   Game da dong. Log nam trong log.txt (va da gui ve may chu neu mang thong).
pause
