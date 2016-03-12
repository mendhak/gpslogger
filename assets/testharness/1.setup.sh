#!/bin/bash

export SERVERIP=`hostname -I | cut -f1 -d' '`
echo $SERVERIP

make-ssl-cert generate-default-snakeoil
docker-compose up -d
docker ps -a

docker exec gpslogger-ftpd-server bash -c 'echo -e "Passw0rd\nPassw0rd" > /tmp/bobp.txt'
docker exec gpslogger-ftpd-server bash -c 'pure-pw useradd bob -u ftpuser -d /home/ftpusers/bob < /tmp/bobp.txt'
docker exec gpslogger-ftpd-server pure-pw mkdb

echo "--------------"
echo "FTP: $SERVERIP, bob Passw0rd"
echo "OwnCloud: $SERVERIP"
echo "SMTP: $SERVERIP:1025 - but no TLS!"
echo "UDP: $SERVERIP:4001"
echo "HTTPS: http://$SERVERIP:8080 and https://$SERVERIP:8443"
echo "--------------"
