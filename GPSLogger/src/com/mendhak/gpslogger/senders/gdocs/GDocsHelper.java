package com.mendhak.gpslogger.senders.gdocs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessProtectedResource;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;
import com.google.api.client.googleapis.json.JsonCParser;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;


public class GDocsHelper implements IActionListener
{
    Context ctx;
    IActionListener callback;
    
    
    public GDocsHelper(Context applicationContext, IActionListener callback)
    {

        this.ctx = applicationContext;
        this.callback = callback;
    }

   
    /** Value of the "Client ID" shown under "Client ID for installed applications". */
    //private static final String CLIENT_ID = "";

    /** Value of the "Client secret" shown under "Client ID for installed applications". */
    //private static final String CLIENT_SECRET = "";

    /** OAuth 2 scope to use */
    //https://docs.google.com/feeds/ gives full access to the user's documents
    private static final String SCOPE = "https://docs.google.com/feeds/";

    /** OAuth 2 redirect uri */
    private static final String REDIRECT_URI = "http://localhost";


    public static void SaveAccessToken(AccessTokenResponse accessTokenResponse, Context applicationContext)
    {
        //Store in preferences, we'll use it later.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("GDOCS_ACCESS_TOKEN",accessTokenResponse.accessToken );
        editor.putLong("GDOCS_EXPIRES_IN", accessTokenResponse.expiresIn);
        editor.putString("GDOCS_REFRESH_TOKEN", accessTokenResponse.refreshToken);
        editor.putString("GDOCS_SCOPE", accessTokenResponse.scope);
        editor.commit();
    }

    public static void ClearAccessToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("GDOCS_ACCESS_TOKEN");
        editor.remove("GDOCS_EXPIRES_IN");
        editor.remove("GDOCS_REFRESH_TOKEN");
        editor.remove("GDOCS_SCOPE");
        editor.commit();
    }

    public static AccessTokenResponse GetAccessToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        AccessTokenResponse atr = new AccessTokenResponse();

        atr.accessToken = prefs.getString("GDOCS_ACCESS_TOKEN","");
        atr.expiresIn = prefs.getLong("GDOCS_EXPIRES_IN",0);
        atr.refreshToken = prefs.getString("GDOCS_REFRESH_TOKEN","");
        atr.scope =  prefs.getString("GDOCS_SCOPE","");

        if(atr.accessToken.length() == 0 || atr.refreshToken.length() == 0)
        {
            return null;
        }
        else
        {
            return atr;
        }

    }
    
    public static boolean IsLinked(Context applicationContext)
    {
        return (GetAccessToken(applicationContext) != null);
    }


    public static String GetAuthorizationRequestUrl(Context applicationContext)
    {
        return  new GoogleAuthorizationRequestUrl(GetClientID(applicationContext), REDIRECT_URI, SCOPE).build();
    }

    public static boolean IsSuccessfulRedirectUrl(String url)
    {
        return url.startsWith(REDIRECT_URI);
    }
    
    

    private static String GetClientID(Context applicationContext)
    {
        int RClientId = applicationContext.getResources().getIdentifier(
                    "gdocs_clientid", "string", applicationContext.getPackageName());
                            
                    
        return applicationContext.getString(RClientId);
    }
    
    private static String GetClientSecret(Context applicationContext)
    {

        int RClientSecret = applicationContext.getResources().getIdentifier(
                    "gdocs_clientsecret", "string", applicationContext.getPackageName());

        return applicationContext.getString(RClientSecret);
    }

    public static AccessTokenResponse GetAccessTokenResponse(String code, Context applicationContext)
    {
        try
        {

           return new GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant(
                    new NetHttpTransport(),
                    new JacksonFactory(),
                    GetClientID(applicationContext),
                    GetClientSecret(applicationContext),
                    code,
                    REDIRECT_URI).execute();
        }
        catch (Exception e)
        {
            Utilities.LogError("GDocsHelper.GetAccessTokenResponse", e);
        }

        return null;
    }

    public static void SaveAccessTokenFromUrl(String url, Context applicationContext)
    {
        String code = extractCodeFromUrl(url);
        AccessTokenResponse accessTokenResponse =  GDocsHelper.GetAccessTokenResponse(code, applicationContext);
        GDocsHelper.SaveAccessToken(accessTokenResponse, applicationContext);
    }

    private static String extractCodeFromUrl(String url)
    {
        return url.substring(REDIRECT_URI.length() + 7, url.length());
    }


    private static GoogleAccessProtectedResource GetAccessProtectedResource(String clientId, String clientSecret,
                                                                     String accessToken, String refreshToken)
    {

        final JsonFactory jsonFactory = new JacksonFactory();
        HttpTransport transport = new NetHttpTransport();

        return new GoogleAccessProtectedResource(
                accessToken,
                transport,
                jsonFactory,
                clientId,
                clientSecret,
                refreshToken);
    }


    public void UploadTestFile()
    {

        //Create an AccessTokenResponse from the stored data
        AccessTokenResponse accessTokenResponse = GetAccessToken(ctx);

        if(accessTokenResponse == null)
        {
            callback.OnFailure();
            return;
        }


        try
        {

            GoogleAccessProtectedResource accessProtectedResource = GetAccessProtectedResource(
                    GetClientID(ctx), GetClientSecret(ctx),
                    accessTokenResponse.accessToken,  accessTokenResponse.refreshToken);


           
            Thread t = new Thread(new GDocsUploadHandler(
                    new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>This is a test upload</a>".getBytes()), 
                    "test.xml" ,this, accessProtectedResource));
            t.start();

        }
        catch(Exception e) 
        {
            Utilities.LogError("GDocsHelper.UploadTestFile", e);
        }

    }


    @Override
    public void OnComplete()
    {
         callback.OnComplete();
    }

    @Override
    public void OnFailure()
    {
          callback.OnFailure();
    }
    
    
    private class GDocsUploadHandler implements Runnable
    {
        
        String fileName;
        InputStream inputStream;
        IActionListener callback;
        GoogleAccessProtectedResource accessProtectedResource;
        
        GDocsUploadHandler(InputStream inputStream, String fileName, IActionListener callback, GoogleAccessProtectedResource accessProtectedResource)
        {
            
            this.inputStream = inputStream;
            this.fileName = fileName;
            this.callback = callback;
            this.accessProtectedResource = accessProtectedResource;
        }
        

        @Override
        public void run()
        {
            try
            {
                //Slow, but always refresh token.  If it errors, then we're not authorized.
                accessProtectedResource.refreshToken();

                String gpsLoggerFolderFeed;


                //Search for the 'GPSLogger For Android' folder.
                HttpRequest searchForCollectionRequest = GetSearchForFolderRequest(accessProtectedResource);
                HttpResponse searchForCollectionResponse = searchForCollectionRequest.execute();

                gpsLoggerFolderFeed = GetFolderFeedUrl(searchForCollectionResponse);

                if (IsNullOrEmpty(gpsLoggerFolderFeed))
                {
                    //Not found, create the folder
                    HttpRequest createFolderRequest = GetCreateFolderRequest(accessProtectedResource);
                    HttpResponse createFolderResponse = createFolderRequest.execute();
                    gpsLoggerFolderFeed = GetFolderFeedUrl(createFolderResponse);
                }

                //Now that you have the collection feed url, search for the file test.xml
                HttpRequest searchForFileRequest = GetSearchForFileRequest(gpsLoggerFolderFeed, fileName,
                        accessProtectedResource);
                HttpResponse searchForFileResponse = searchForFileRequest.execute();

                Document fileSearchNode = GetDocumentFromHttpResponse(searchForFileResponse);

                String fileEditUrl = GetFileEditUrl(fileSearchNode);

                if (IsNullOrEmpty(fileEditUrl))
                {
                    //The file doesn't exist, you must create it.

                    //Start a 'create file' session
                    String fileUploadUrl = GetFileUploadUrl(fileSearchNode);

                    String createFileAtomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:docs=\"http://schemas.google.com/docs/2007\">\n" +
                            "  <!-- Replace the following line appropriately to create another type of resource. -->\n" +
                            "  <category scheme=\"http://schemas.google.com/g/2005#kind\"\n" +
                            "      term=\"http://schemas.google.com/docs/2007#document\"/>\n" +
                            "  <title>" + fileName + "</title>\n" +
                            "</entry>";

                    HttpRequest createFileSessionRequest = GetCreateFileRequest(fileName, createFileAtomXml,
                            fileUploadUrl + "?convert=false", accessProtectedResource);

                    HttpResponse createFileSessionResponse = createFileSessionRequest.execute();

                    //Get the actual file upload location
                    String newUploadLocation = createFileSessionResponse.headers.location;

                    //Write to actual file upload location
                    String fileContents =  inputStreamToString(inputStream);

                    HttpRequest createFileRequest = GetCreateFileRequest(fileName, fileContents, newUploadLocation,
                            accessProtectedResource);

                    createFileRequest.execute();

                }
                else
                {
                    //The file exists.  Update contents.

                    String fileContents = inputStreamToString(inputStream);

                    HttpRequest updateFileSessionRequest = GetUpdateFileRequest(fileName, fileContents,
                            fileEditUrl + "?convert=false", accessProtectedResource);

                    HttpResponse updateFileSessionResponse = updateFileSessionRequest.execute();
                    String newUpdateLocation = updateFileSessionResponse.headers.location;


                    HttpRequest updateFileRequest = GetUpdateFileRequest(fileName, fileContents,
                            newUpdateLocation, accessProtectedResource);

                    updateFileRequest.execute();

                }

                callback.OnComplete();
            }
            catch (Exception e)
            {
                Utilities.LogError("GDocsUploadHandler", e);
                callback.OnFailure();
            }
        }


        private String GetFileUploadUrl(Document fileSearchNode)
        {
            String fileUploadUrl = "";

            NodeList linkNodes = fileSearchNode.getElementsByTagName("link");

            for (int i = 0; i < linkNodes.getLength(); i++)
            {
                String rel = linkNodes.item(i).getAttributes().getNamedItem("rel").getNodeValue();

                if (rel.equalsIgnoreCase("http://schemas.google.com/g/2005#resumable-create-media"))
                {
                    fileUploadUrl = linkNodes.item(i).getAttributes().getNamedItem("href").getNodeValue();
                }
            }

            return fileUploadUrl;
        }


        private String GetFileEditUrl(Document fileSearchNode)
        {
            String fileEditUrl = "";

            NodeList linkNodes = fileSearchNode.getElementsByTagName("link");

            for (int i = 0; i < linkNodes.getLength(); i++)
            {
                String rel = linkNodes.item(i).getAttributes().getNamedItem("rel").getNodeValue();

                if (rel.equalsIgnoreCase("http://schemas.google.com/g/2005#resumable-edit-media"))
                {
                    fileEditUrl = linkNodes.item(i).getAttributes().getNamedItem("href").getNodeValue();
                }
            }

            return fileEditUrl;
        }

        private Document GetDocumentFromHttpResponse(HttpResponse searchForFileResponse)
        {
            Document doc;

            try
            {
                DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
                xmlFactory.setNamespaceAware(true);
                DocumentBuilder builder;
                builder = xmlFactory.newDocumentBuilder();
                doc = builder.parse(searchForFileResponse.getContent());
            }
            catch (Exception e)
            {
                doc = null;
            }

            return doc;

        }

        private HttpRequest GetSearchForFileRequest(String gpsLoggerFolderFeed, String fileTitle,
                                                    final GoogleAccessProtectedResource accessProtectedResource) throws IOException
        {


            final JsonFactory jsonFactory = new JacksonFactory();
            HttpTransport transport = new NetHttpTransport();


            HttpRequestFactory httpBasicFactory = transport.createRequestFactory(new HttpRequestInitializer()
            {

                @Override
                public void initialize(HttpRequest request)
                {
                    JsonCParser parser = new JsonCParser();
                    parser.jsonFactory = jsonFactory;
                    request.addParser(parser);
                    GoogleHeaders headers = new GoogleHeaders();
                    headers.setApplicationName("GPSLogger for Android");
                    headers.set("Authorization", "Bearer " + accessProtectedResource.getAccessToken());
                    headers.gdataVersion = "3";
                    request.headers = headers;
                }
            });

            //https://docs.google.com/feeds/default/private/full/folder:...../contents?title=test.xml
            return httpBasicFactory.buildGetRequest(new GenericUrl(gpsLoggerFolderFeed + "?title=" + fileTitle));
        }

        private HttpRequest GetSearchForFolderRequest(final GoogleAccessProtectedResource accessProtectedResource) throws IOException
        {


            final JsonFactory jsonFactory = new JacksonFactory();
            HttpTransport transport = new NetHttpTransport();

            HttpRequestFactory httpBasicFactory = transport.createRequestFactory(new HttpRequestInitializer()
            {

                @Override
                public void initialize(HttpRequest request)
                {
                    JsonCParser parser = new JsonCParser();
                    parser.jsonFactory = jsonFactory;
                    request.addParser(parser);
                    GoogleHeaders headers = new GoogleHeaders();
                    headers.setApplicationName("GPSLogger for Android");
                    headers.set("Authorization", "Bearer " + accessProtectedResource.getAccessToken());
                    headers.gdataVersion = "3";
                    request.headers = headers;
                }
            });

            return httpBasicFactory.buildGetRequest(
                    new GenericUrl("https://docs.google.com/feeds/default/private/full?title=GPSLogger+For+Android&showfolders=true"));
        }


        private String GetFolderFeedUrl(HttpResponse createFolderResponse)
        {
            String gpsLoggerFolderFeed = "";

            try
            {
                Document createFolderDoc = GetDocumentFromHttpResponse(createFolderResponse);

                Node newFolderContentNode = createFolderDoc.getElementsByTagName("content").item(0);

                if (newFolderContentNode == null)
                {
                    System.out.println("Failed to create a collection");
                }
                else
                {
                    //<content type="application/atom+xml;type=feed" src=".../contents"/>
                    gpsLoggerFolderFeed = createFolderDoc.getElementsByTagName("content").item(0)
                            .getAttributes().getNamedItem("src").getNodeValue();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            return gpsLoggerFolderFeed;
        }


        private HttpRequest GetUpdateFileRequest(final String fileName, final String payload, String updateUrl,
                                                 final GoogleAccessProtectedResource accessProtectedResource) throws IOException
        {

            final JsonFactory jsonFactory = new JacksonFactory();
            HttpTransport transport = new NetHttpTransport();


            HttpRequestFactory updateFactory = transport.createRequestFactory( new HttpRequestInitializer()
            {
                @Override
                public void initialize(HttpRequest request) throws IOException
                {
                    // set the parser
                    JsonCParser parser = new JsonCParser();
                    parser.jsonFactory = jsonFactory;
                    request.addParser(parser);
                    // set up the Google headers
                    GoogleHeaders headers = new GoogleHeaders();
                    headers.setApplicationName("GPSLogger for Android");
                    headers.set("Authorization", "Bearer " + accessProtectedResource.getAccessToken());
                    headers.set("X-Upload-Content-Length", payload.length());
                    headers.set("X-Upload-Content-Type", "text/xml");
                    headers.set("Content-Type", "text/xml");
                    headers.set("Content-Length", String.valueOf(payload.length()));
                    headers.setSlugFromFileName(fileName);
                    headers.set("If-Match", "*");
                    headers.gdataVersion = "3";
                    headers.putAll(request.headers);
                    request.headers = headers;
                }
            });

            return updateFactory.buildPutRequest(new GenericUrl(updateUrl), new HttpContent()
            {
                @Override
                public long getLength() throws IOException
                {
                    return payload.length();
                }

                @Override
                public String getEncoding()
                {
                    return null;
                }

                @Override
                public String getType()
                {
                    return "text/xml";
                }

                @Override
                public void writeTo(OutputStream outputStream) throws IOException
                {
                    outputStream.write(payload.getBytes());
                }

                @Override
                public boolean retrySupported()
                {
                    return false;
                }
            });

        }


        private HttpRequest GetCreateFileRequest(final String fileName, final String payload, String uploadUrl,
                                                 final GoogleAccessProtectedResource accessProtectedResource
        ) throws IOException
        {


            final JsonFactory jsonFactory = new JacksonFactory();
            HttpTransport transport = new NetHttpTransport();


            HttpRequestFactory uploadFactory = transport.createRequestFactory(new HttpRequestInitializer()
            {

                @Override
                public void initialize(HttpRequest request)
                {
                    // set the parser
                    JsonCParser parser = new JsonCParser();
                    parser.jsonFactory = jsonFactory;
                    request.addParser(parser);
                    // set up the Google headers
                    GoogleHeaders headers = new GoogleHeaders();
                    headers.setApplicationName("GPSLogger for Android");
                    headers.set("Authorization", "Bearer " + accessProtectedResource.getAccessToken());
                    headers.set("X-Upload-Content-Length", "0");
                    headers.set("X-Upload-Content-Type", "text/xml");
                    headers.set("Content-Type", "text/xml");
                    headers.set("Content-Length", String.valueOf(payload.length()));
                    headers.setSlugFromFileName(fileName);
                    headers.gdataVersion = "3";
                    headers.putAll(request.headers);
                    request.headers = headers;
                }
            });

            return uploadFactory.buildPostRequest(new GenericUrl(uploadUrl), new HttpContent()
            {
                @Override
                public long getLength() throws IOException
                {
                    return payload.length();
                }

                @Override
                public String getEncoding()
                {
                    return null;
                }

                @Override
                public String getType()
                {
                    return "text/xml";
                }

                @Override
                public void writeTo(OutputStream outputStream) throws IOException
                {
                    outputStream.write(payload.getBytes());
                }

                @Override
                public boolean retrySupported()
                {
                    return false;
                }
            });

        }


        private HttpRequest GetCreateFolderRequest(final GoogleAccessProtectedResource accessProtectedResource) throws IOException
        {


            final JsonFactory jsonFactory = new JacksonFactory();
            HttpTransport transport = new NetHttpTransport();


            HttpRequestFactory httpBasicFactory = transport.createRequestFactory(new HttpRequestInitializer()
            {

                @Override
                public void initialize(HttpRequest request)
                {
                    JsonCParser parser = new JsonCParser();
                    parser.jsonFactory = jsonFactory;
                    request.addParser(parser);
                    GoogleHeaders headers = new GoogleHeaders();
                    headers.setApplicationName("GPSLogger for Android");
                    headers.set("Authorization", "Bearer " + accessProtectedResource.getAccessToken());
                    headers.gdataVersion = "3";
                    request.headers = headers;
                }
            });

            return httpBasicFactory.buildPostRequest(
                    new GenericUrl("https://docs.google.com/feeds/default/private/full"), new HttpContent()
            {

                String createXml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                        "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                        "  <category scheme=\"http://schemas.google.com/g/2005#kind\"\n" +
                        "      term=\"http://schemas.google.com/docs/2007#folder\"/>\n" +
                        "  <title>GPSLogger For Android</title>\n" +
                        "</entry>";

                @Override
                public long getLength() throws IOException
                {
                    return createXml.length();
                }

                @Override
                public String getEncoding()
                {
                    return null;
                }

                @Override
                public String getType()
                {
                    return "application/atom+xml";
                }

                @Override
                public void writeTo(OutputStream outputStream) throws IOException
                {
                    outputStream.write(createXml.getBytes());
                }

                @Override
                public boolean retrySupported()
                {
                    return false;
                }
            });
        }

        private String inputStreamToString(InputStream is)
        {
            String line;
            StringBuilder total = new StringBuilder();

            // Wrap a BufferedReader around the InputStream
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));

            // Read response until the end
            try
            {
                while ((line = rd.readLine()) != null)
                {
                    total.append(line);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    is.close();
                }
                catch(Exception e)
                {
                    Utilities.LogWarning("inputStreamToString - could not close stream");
                }
            }

            // Return full string
            return total.toString();
        }
        

        private boolean IsNullOrEmpty(String gpsLoggerFolderFeed)
        {
            return gpsLoggerFolderFeed == null || gpsLoggerFolderFeed.length() == 0;
        }
        
    }
    
}
