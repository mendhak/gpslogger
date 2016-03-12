### GPSLogger Test Harness

Creates a bunch of docker containers to test various senders such as FTP, SMTP, ownCloud, HTTPS, UDP.

`./1.setup.sh`

Starts the containers with a little access info:

```bash
--------------
FTP: 192.168.1.91
OwnCloud: 192.168.1.91
SMTP: 192.168.1.91:1025 - but no TLS!
UDP: 192.168.1.91:4001
HTTPS: http://192.168.1.91:8080 and https://192.168.1.91:8443
--------------
```


`./2.teardown.sh`

Stops containers.

`./3.cleanup.sh`

Removes folders that docker may have created.
