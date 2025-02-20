@echo off
echo Avvio OpenVPN...
start "" "C:\Program Files\OpenVPN\bin\openvpn-gui.exe" --connect largescale2023.ovpn
timeout /t 15
echo Chiusura di MongoDB...

:: Apertura Terminale 1
start cmd /c "echo Connessione a 10.1.1.20... && ssh root@10.1.1.20  "pkill mongod""

:: Apertura Terminale 2
start cmd /c "echo Connessione a 10.1.1.21... && ssh root@10.1.1.21 "pkill mongod""

:: Apertura Terminale 3
start cmd /c "echo Connessione a 10.1.1.22... && ssh root@10.1.1.22 "pkill mongod""

echo Tutto chiuso!