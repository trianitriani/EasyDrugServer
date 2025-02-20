@echo off
echo Avvio OpenVPN...
start "" "C:\Program Files\OpenVPN\bin\openvpn-gui.exe" --connect largescale2023.ovpn
timeout /t 15
echo Avvio dei server MongoDB e Redis...

:: Apertura Terminale 1
start cmd /k "echo Connessione a 10.1.1.20... && ssh root@10.1.1.20  "mongod --replSet lsmdb --dbpath ~/data --port 27017 --bind_ip localhost,10.1.1.20 --oplogSize 200 && cd /etc/redis && redis-server ./redis.conf && redis-server ./sentinel.conf --sentinel && exec bash -i""

:: Apertura Terminale 2
start cmd /k "echo Connessione a 10.1.1.21... && ssh root@10.1.1.21 "mongod --replSet lsmdb --dbpath ~/data --port 27017 --bind_ip localhost,10.1.1.21 --oplogSize 200 && cd /etc/redis && redis-server ./redis.conf && redis-server ./sentinel.conf --sentinel && exec bash -i""

:: Apertura Terminale 3
start cmd /k "echo Connessione a 10.1.1.22... && ssh root@10.1.1.22 "mongod --replSet lsmdb --dbpath ~/data --port 27017 --bind_ip localhost,10.1.1.22 --oplogSize 200 && cd /etc/redis && redis-server ./redis.conf && redis-server ./sentinel.conf --sentinel && exec bash -i""

echo Tutto avviato!
pause