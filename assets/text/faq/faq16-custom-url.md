## Using the Custom URL feature

The Custom URL feature allows you to log GPS points to a public URL.  This can be a third party API that accepts `GET` requests, or an application that you've written and are hosting on your own server.

    https://myserver.com/log?lat=%LAT&long=%LON...

If your phone goes **offline**, then the app will queue these requests until a data connection becomes available.     

You can add **authentication** by prefixing the username and password like so:

    https://username:password@myserver.com/log?lat=%LAT&long=%LON
    
The credentials are sent as Basic Authentication headers, not as part of the URL.     

If you use a self signed **SSL** certificate, be sure to [validate it first](#customsslcertificates).

If you check the 'POST' checkbox, then the querystring parameters are sent in the HTTP POST body as:

    a=b&c=d&e=f&...
    


