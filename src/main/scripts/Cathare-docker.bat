@echo on

rem assume:
rem  1. WSL2 is installed on your Windows: "wsl.exe --install", then reboot, then "wsl.exe --set-defaut-version 2"
rem  2. Debian distro is installed in WSL2: "wsl.exe --install -d Debian"
rem  3. if behind proxy, within Debian: "export https_proxy="http://login:passwd@proxy-ip:port", and "docker pull irsn/cathare2:v25_3_mod931" to pre-install Cathare.
rem Then, following run should work:
wsl -u root -e bash -c "CWD=$(dirname `readlink -f %0`); docker run -v ${PWD}:/workdir --mount type=bind,source=${CWD}/Cathare.sh,target=/opt/Cathare.sh,readonly irsn/cathare2:v25_3_mod931 /bin/sh /opt/Cathare.sh %1"

for /F "TOKENS=1,2,*" %%a in ('tasklist /FI "IMAGENAME eq wsl.exe"') do set PID_WSL=%%b
echo %PID_WSL% > PID

:loop
tasklist | find " %PID_WSL% " >nul
if not errorlevel 1 (
    timeout /t 1 >nul
    goto :loop
)

del /f PID
