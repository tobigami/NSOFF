@echo off
rem Doi dia chi may chu cua client PC (ban Unity pb189).
rem File nay chi la vo boc -- viec that nam trong doi-may-chu-pc.ps1 ben canh.
rem
rem -ExecutionPolicy Bypass la bat buoc: Windows mac dinh chan chay file .ps1, bam dup
rem thang vao .ps1 chi mo Notepad chu khong chay.
rem chcp 65001 de cua so lenh khong bop meo chu co dau.
setlocal
cd /d "%~dp0"
chcp 65001 >nul

rem === Dia chi may chu ===
rem De trong thi script hoi tung buoc, tu go dia chi vao.
rem Dien san vao day TRUOC KHI gui cho ban be thi ho chi viec bam dup, khong phai go gi ca.
set "MAYCHU="
set "CONG=14444"

if defined MAYCHU (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0doi-may-chu-pc.ps1" -May "%MAYCHU%" -Cong "%CONG%" -KhongHoi
) else (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0doi-may-chu-pc.ps1" %*
)

echo.
pause
