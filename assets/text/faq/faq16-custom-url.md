## Using the Custom URL feature

The Custom URL feature allows you to log GPS points to a public URL.  This can be a third party API that accepts `GET` requests, or an application that you've written and are hosting on your own server, [a webhook](https://www.ilovefreesoftware.com/21/featured/free-webhook-creator-websites-to-create-test-webhooks-online.html), a third party API, an [AWS Lambda](https://docs.aws.amazon.com/lambda/latest/dg/lambda-urls.html), anything like that.  

    https://myserver.com/log?lat=%LAT&long=%LON...

Tap the Parameters to see a list of available parameters that you can use, including `%TIME`, `%SAT`, `%ACC`, etc.  Adding the `%ALL` string will simply get substituted by all available parameters with values, if available.

You can add your HTTP body, HTTP header, HTTP method and basic authentication credentials in the Custom URL screen.

If your phone goes **offline**, then the app will queue these requests until a data connection becomes available. This behavior can be changed with the 'Discard offline locations' toggle.

If you use a self signed **SSL** certificate, be sure to [validate it first](#customsslcertificates).

The 'Log to custom URL' toggle will send an HTTP request each time the app acquires a new point.  

The 'Allow auto sending' toggle will also enable CSV logging out of necessity, and this feature will then make many requests to the Custom URL, one for each line in the recorded CSV.  It does this at the auto-send interval which is by default 60 minutes.  If you are writing these points to a database for example, it is your responsibility to 'deduplicate' when receiving these requests, for that reason it's a good idea to send a timestamp along.  

The 'Discard offline locations' toggle will disable queueing the log requests while the phone is offline. It will always only keep the request with most recent location. This prevents sending many requests when the phone becomes online after a long period of time. You can enable this toggle if you are interested only in the most recent location. The auto sending feature is not affected by this toggle.



