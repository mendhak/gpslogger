#!/bin/bash

export SERVERIP=`hostname -I | cut -f1 -d' '`
echo $SERVERIP

make-ssl-cert generate-default-snakeoil
docker-compose up -d
docker ps -a

docker exec gpslogger-ftpd-server bash -c 'echo -e "Passw0rd\nPassw0rd" > /tmp/bobp.txt'
docker exec gpslogger-ftpd-server bash -c 'pure-pw useradd bob -u ftpuser -d /home/ftpusers/bob < /tmp/bobp.txt'
docker exec gpslogger-ftpd-server pure-pw mkdb

echo "----------------------------------------------------------------------"
echo "FTP            : $SERVERIP:21, bob Passw0rd"
echo "SMTP w/ TLS    : $SERVERIP:525 noreply@COFFEE.home:docker relays to :1025"
echo "SMTP no TLS    : $SERVERIP:1025 view on http://$SERVERIP:8025"
echo "UDP            : $SERVERIP:4001"
echo "OwnCloud       : https://$SERVERIP"
echo "HTTPS          : http://$SERVERIP:8081 and https://$SERVERIP:8443"
echo "----------------------------------------------------------------------"
