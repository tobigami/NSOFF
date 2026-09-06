@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo   NinjaSchool Online - Setup + Start Server
echo ============================================
echo.
echo Ghi chu: MongoDB (mongodb.bat trong config.properties) dang de "false"
echo tuc la KHONG dung MongoDB. Script nay chi lo phan MySQL/MariaDB.
echo Neu ban bat mongodb.bat=true thi phai tu cai va chay MongoDB rieng.
echo.

REM ================= Doc cau hinh tu mysql.properties =================
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=nso_test
set DB_USER=root
set DB_PASS=

for /f "usebackq tokens=1,* delims==" %%A in ("mysql.properties") do (
    if "%%A"=="nsoz.database.host" set DB_HOST=%%B
    if "%%A"=="nsoz.database.port" set DB_PORT=%%B
    if "%%A"=="nsoz.database.name" set DB_NAME=%%B
    if "%%A"=="nsoz.database.user" set DB_USER=%%B
    if "%%A"=="nsoz.database.pass" set DB_PASS=%%B
)

echo DB config: !DB_USER!@!DB_HOST!:!DB_PORT!/!DB_NAME!
echo.

if "!DB_PASS!"=="" (
    set "PASSARG="
) else (
    set "PASSARG=-p!DB_PASS!"
)

REM ================= Kiem tra cong cu can thiet =================
where java >nul 2>nul
if errorlevel 1 (
    echo [LOI] Khong tim thay "java" trong PATH. Hay cai JDK 8 tro len va them vao PATH.
    goto :fail
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo [LOI] Khong tim thay "mvn" trong PATH. Hay cai Apache Maven va them vao PATH.
    goto :fail
)

where mysql >nul 2>nul
if errorlevel 1 (
    echo [LOI] Khong tim thay "mysql" trong PATH.
    echo       Neu dung XAMPP/WAMP, them thu muc "mysql\bin" cua no vao PATH roi chay lai.
    goto :fail
)

REM ================= Tao database neu chua co =================
echo Dang kiem tra / tao database "!DB_NAME!" ...
mysql -h !DB_HOST! -P !DB_PORT! -u !DB_USER! !PASSARG! -e "CREATE DATABASE IF NOT EXISTS `!DB_NAME!` CHARACTER SET utf8mb4;"
if errorlevel 1 (
    echo [LOI] Khong ket noi duoc MySQL/MariaDB. Kiem tra lai dich vu MySQL da chay va thong tin trong mysql.properties.
    goto :fail
)

REM ================= Import schema neu database dang rong =================
set TABLE_COUNT=0
for /f %%C in ('mysql -h !DB_HOST! -P !DB_PORT! -u !DB_USER! !PASSARG! -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='!DB_NAME!';"') do set TABLE_COUNT=%%C

if "!TABLE_COUNT!"=="0" (
    echo Database rong - dang import cau truc + du lieu mau tu nso_test.sql ...
    mysql -h !DB_HOST! -P !DB_PORT! -u !DB_USER! !PASSARG! !DB_NAME! < nso_test.sql
    if errorlevel 1 (
        echo [LOI] Import nso_test.sql that bai. Xem log ben tren.
        goto :fail
    )
    echo Import xong.
) else (
    echo Database "!DB_NAME!" da co !TABLE_COUNT! bang - bo qua import de khong ghi de du lieu hien co.
    echo ^(Muon import lai tu dau: xoa het bang trong database roi chay script nay lan nua.^)
)
echo.

REM ================= Build neu chua co jar =================
if "%~1"=="build" (
    echo Duoc yeu cau build lai - dang chay "mvn clean package" ...
    call mvn clean package
    if errorlevel 1 (
        echo [LOI] Build that bai. Xem log ben tren.
        goto :fail
    )
) else if not exist target\Nso-jar-with-dependencies.jar (
    echo Chua co target\Nso-jar-with-dependencies.jar - dang build bang Maven
    echo ^(lan dau se can Internet de Maven tai thu vien^) ...
    call mvn clean package
    if errorlevel 1 (
        echo [LOI] Build that bai. Xem log ben tren.
        goto :fail
    )
) else (
    echo Da co san target\Nso-jar-with-dependencies.jar - bo qua build.
    echo ^(Chay "start-server.bat build" de ep build lai.^)
)
echo.

REM ================= Chay may chu =================
echo Dang khoi dong may chu NinjaSchool ...
echo.
java -server -Dfile.encoding=UTF-8 -Xms2G -Xmx2G -jar target\Nso-jar-with-dependencies.jar

pause
exit /b 0

:fail
pause
exit /b 1
