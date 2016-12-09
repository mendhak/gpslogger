## Custom SSL Certificates

![SSLValidation](images/21sslvalidation.gif)

If you use **self signed certificates** or **custom CA certificates** in Custom URL, OwnCloud, OpenGTS, FTP or SMTP then you will need to get GPSLogger to recognize and store your custom certificates.    

It's easy.  Just go into a setting screen where you have specified a custom SSL URL or server, and click `Validate SSL Certificate`.  You will be prompted with the certificate's details, you can then choose to accept; the certificate will be stored in the local keystore.  
 
This validation is required as it's a security best practice. It helps protect your information between your device and the server, it prevents attackers from listening in.  The Google Play Store is also asking developers to be stricter with SSL based verifications.
 
In the case of OwnCloud, OpenGTS and Custom URL, the certificate must match the domain you're connecting to.   In other words, if your certificate is issued to `example.com` then the URL you are connecting to must match `example.com`. More specifically this means that the certificate *Common Name* or *Subject Alternative Name* must match the host of the URL you are connecting to. 