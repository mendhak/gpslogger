package com.mendhak.gpslogger.senders.gdocs;

import android.accounts.*;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Utilities;
import com.mendhak.gpslogger.senders.IFileSender;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;


public class GDocsHelper implements IActionListener, IFileSender
{
    Context ctx;
    IActionListener callback;


    public GDocsHelper(Context applicationContext, IActionListener callback)
    {

        this.ctx = applicationContext;
        this.callback = callback;
    }

    /** OAuth 2 scope to use */
    //https://docs.google.com/feeds/ gives full access to the user's documents
    private static final String SCOPE = "oauth2:https://docs.google.com/feeds/";


    /**
     * Returns the Google API CLIENT ID to use in API calls
     * @param applicationContext
     * @return
     */
    private static String GetClientID(Context applicationContext)
    {
        int RClientId = applicationContext.getResources().getIdentifier(
                "gdocs_clientid", "string", applicationContext.getPackageName());


        return applicationContext.getString(RClientId);
    }

    /**
     * Returns the Google API CLIENT SECRET to use in API calls
     * @param applicationContext
     * @return
     */
    private static String GetClientSecret(Context applicationContext)
    {

        int RClientSecret = applicationContext.getResources().getIdentifier(
                "gdocs_clientsecret", "string", applicationContext.getPackageName());

        return applicationContext.getString(RClientSecret);
    }

    /**
     * Gets the stored authToken, which may be expired
     */
    public static String GetAuthToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDOCS_AUTH_TOKEN","");
    }

    /**
     * Gets the stored account name
     */
    public static String GetAccountName(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        return prefs.getString("GDOCS_ACCOUNT_NAME","");
        
    }

    /**
     * Saves the authToken and account name into shared preferences
     */
    public static void SaveAuthToken(Context applicationContext,AccountManagerFuture<Bundle> bundleAccountManagerFuture)
    {
        try
        {
            String authToken = bundleAccountManagerFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN);
            String accountName =bundleAccountManagerFuture.getResult().getString(AccountManager.KEY_ACCOUNT_NAME); 
            
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            SharedPreferences.Editor editor = prefs.edit();

            Utilities.LogDebug("Saving GDocs authToken: " + authToken);
            editor.putString("GDOCS_AUTH_TOKEN", authToken);
            editor.putString("GDOCS_ACCOUNT_NAME", accountName);
            editor.commit();
        }
        catch (Exception e)
        {

            Utilities.LogError("GDocsHelper.SaveAuthToken", e);
        }

    }

    /**
     * Removes the authToken and account name from storage
     * @param applicationContext
     */
    public static void ClearAuthToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("GDOCS_AUTH_TOKEN");
        editor.remove("GDOCS_ACCOUNT_NAME");
        editor.commit();
    }


    /**
     * Returns whether the app is authorized to perform Google API operations
     * @param applicationContext
     * @return
     */
    public static boolean IsLinked(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        String gdocsAuthToken = prefs.getString("GDOCS_AUTH_TOKEN","");
        String gdocsAccount = prefs.getString("GDOCS_ACCOUNT_NAME","");
        return gdocsAuthToken.length() > 0 && gdocsAccount.length() > 0;
    }


    /**
     * Gets an instance of an AccountManager to use for authorizing the app
     * @param applicationContext
     * @return
     */
    public static AccountManager GetAccountManager(Context applicationContext)
    {
        return AccountManager.get(applicationContext);
    }

    /**
     * This version show the user an access request screen for the user to authorize the app
     * @param accountManager
     * @param account
     * @param ota
     * @param activity
     */
    public static void GetAuthTokenFromAccountManager(AccountManager accountManager, Account account,
                                                      AccountManagerCallback<Bundle> ota, Activity activity)
    {
        Bundle bundle = new Bundle();
        accountManager.getAuthToken(account,
                SCOPE,
                bundle,
                activity,
                ota,
                null);
    }

    /**
     * This version puts a message in the notification area asking for authorization
     * @param accountManager
     * @param account
     * @param ota
     */
    public static void GetAuthTokenFromAccountManager(AccountManager accountManager, Account account,
                                                      AccountManagerCallback<Bundle> ota)
    {
        accountManager.getAuthToken(
                account,                     // Account retrieved using getAccountsByType()
                SCOPE,            // Auth scope
                true,
                ota,          // Callback called when a token is successfully acquired
                null);    // Callback called if an error occurs    
    }

    /**
     * Invalidates the authToken and requests a new one and saves the new authToken
     * @param applicationContext
     * @param accountManager
     */
    private void ResetAuthToken(final Context applicationContext, final AccountManager accountManager, 
                                final Thread threadToStart)
    {

        //To completely revoke access, adb -e shell 'sqlite3 /data/system/accounts.db "delete from grants;"'
        //Invalidate token, get new token, invalidate it again, then get it again.
        //As weird as that sounds, the first time you get a token it will be expired.
        
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account = null;

        for(Account acc : accounts)
        {
            if(acc.name.equalsIgnoreCase(GetAccountName(applicationContext)))
            {
                account = acc;
            }
        }

        final Account finalAccount = account;

        //Invalidate the token
        accountManager.invalidateAuthToken("com.google", GetAuthToken(applicationContext));


        //Request a token (AccountManager will return a cached and probably expired token)
        GetAuthTokenFromAccountManager(accountManager,account,new AccountManagerCallback<Bundle>()
        {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture)
            {
                //Save the (stale) token
                SaveAuthToken(applicationContext,bundleAccountManagerFuture);

                //Invalidate it again
                accountManager.invalidateAuthToken("com.google", GetAuthToken(applicationContext));

                //Request a token again
                GetAuthTokenFromAccountManager(accountManager, finalAccount,new AccountManagerCallback<Bundle>()
                {
                    @Override
                    public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture)
                    {
                        //and finally save it
                        SaveAuthToken(applicationContext,bundleAccountManagerFuture);
                        threadToStart.start();
                    }
                });
            }
        });
    }
    
    public static Account[] GetAccounts(AccountManager accountManager)
    {
        return accountManager.getAccountsByType("com.google");
    }

    public void UploadTestFile()
    {

        if(!IsLinked(ctx))
        {
            callback.OnFailure();
            return;
        }

        try
        {
            AccountManager accountManager = GetAccountManager(ctx);


            Thread t = new Thread(new GDocsUploadHandler(
                    new ByteArrayInputStream("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<a>This is a test upload</a>".getBytes()),
                    "test.xml", GDocsHelper.this));

            ResetAuthToken(ctx, accountManager, t);
        }
        catch(Exception e)
        {
            callback.OnFailure();
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

    @Override
    public void UploadFile(List<File> files)
    {
        //If there's a zip file, upload just that
        //Else upload everything in files.

        File zipFile = null;


        for(File f : files)
        {
            if(f.getName().contains(".zip"))
            {
                zipFile = f;
                break;
            }
        }

        if(zipFile != null)
        {
            UploadFile(zipFile.getName());
        }
        else
        {
            for(File f : files)
            {
                UploadFile(f.getName());
            }
        }
    }


    public void UploadFile(final String fileName)
    {

        if(!IsLinked(ctx))
        {
            callback.OnFailure();
            return;
        }

        try
        {
            AccountManager accountManager = GetAccountManager(ctx);

            File gpsDir = new File(Environment.getExternalStorageDirectory(), "GPSLogger");
            File gpxFile = new File(gpsDir, fileName);
            FileInputStream fis = new FileInputStream(gpxFile);

            Thread t = new Thread(new GDocsUploadHandler(fis, fileName, GDocsHelper.this));

            ResetAuthToken(ctx, accountManager,t);
        }
        catch(Exception e)
        {
            callback.OnFailure();
            Utilities.LogError("GDocsHelper.UploadFile", e);
        }
    }

    @Override
    public boolean accept(File dir, String name)
    {
        return name.toLowerCase().endsWith(".zip")
                || name.toLowerCase().endsWith(".gpx")
                || name.toLowerCase().endsWith(".kml");
    }

    private class GDocsUploadHandler implements Runnable
    {

        String fileName;
        InputStream inputStream;
        IActionListener callback;

        GDocsUploadHandler(InputStream inputStream, String fileName, 
                           IActionListener callback)
        {

            this.inputStream = inputStream;
            this.fileName = fileName;
            this.callback = callback;
        }


        @Override
        public void run()
        {
               
            try
            {

                if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO)
                {
                    //Due to a pre-froyo bug
                    //http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                    System.setProperty("http.keepAlive", "false");
                }

                String gpsLoggerFolderFeed = SearchForGpsLoggerFolder();

                if(Utilities.IsNullOrEmpty(gpsLoggerFolderFeed))
                {
                    //Couldn't find anything, need to create it.
                    gpsLoggerFolderFeed = CreateFolder();
                }

                FileAccessLocations fileSearch = SearchForFile(gpsLoggerFolderFeed, fileName);

                if(Utilities.IsNullOrEmpty(fileSearch.UpdateUrl))
                {
                    //The file doesn't exist, you must create it.
                    CreateFile(fileSearch, fileName, Utilities.GetByteArrayFromInputStream(inputStream));
                }
                else
                {
                    //The file exists, update its contents instead
                    UpdateFile(fileSearch, fileName, Utilities.GetByteArrayFromInputStream(inputStream));
                }
                
                callback.OnComplete();

            }
            catch (Exception e)
            {
                Utilities.LogError("GDocsUploadHandler.run", e);
                callback.OnFailure();
            }
        }


        private void UpdateFile(FileAccessLocations accessLocations, String fileName, byte[] fileContents)
        {

            String resumableFileUploadUrl = UploadFileContentsToResumableUrl(accessLocations.UpdateUrl+"?convert=false",
                    fileName, fileContents,true);

            UploadFileContentsToResumableUrl(resumableFileUploadUrl, fileName, fileContents, true);

        }

        private void CreateFile(FileAccessLocations accessLocations, String fileName, byte[] fileContents)
        {
            String createFileAtomXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<entry xmlns=\"http://www.w3.org/2005/Atom\" xmlns:docs=\"http://schemas.google.com/docs/2007\">\n" +
                    "  <category scheme=\"http://schemas.google.com/g/2005#kind\"\n" +
                    "      term=\"http://schemas.google.com/docs/2007#document\"/>\n" +
                    "  <title>" + fileName + "</title>\n" +
                    "</entry>";

            String resumableFileUploadUrl = UploadFileContentsToResumableUrl(accessLocations.CreateUrl + "?convert=false",
                    fileName, createFileAtomXml.getBytes(), false);

            UploadFileContentsToResumableUrl(resumableFileUploadUrl, fileName, fileContents, false);

        }

        private String UploadFileContentsToResumableUrl(String resumableFileUploadUrl,
                                                        String fileName,
                                                        byte[] fileContents,
                                                        boolean isUpdate)
        {
            //This method gets used 4 times - to get the resumable location for create/edit, and to do the actual uploads.

            String newLocation = "";
            HttpURLConnection conn = null;

            try
            {

                URL url = new URL(resumableFileUploadUrl);
                conn = (HttpURLConnection) url.openConnection();
                AddCommonHeaders(conn);

                conn.setRequestProperty("X-Upload-Content-Length", String.valueOf(fileContents.length)); //back to 0
                
                conn.setRequestProperty("X-Upload-Content-Type", Utilities.GetMimeTypeFromFileName(fileName));
                conn.setRequestProperty("Content-Type", Utilities.GetMimeTypeFromFileName(fileName));
                conn.setRequestProperty("Content-Length", String.valueOf(fileContents.length));
                conn.setRequestProperty("Slug", fileName);


                if(isUpdate)
                {
                    conn.setRequestProperty("If-Match", "*");
                    conn.setRequestMethod("PUT");
                }
                else
                {
                    conn.setRequestMethod("POST");
                }

                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(
                        conn.getOutputStream());
                //wr.writeBytes(fileContents);
                wr.write(fileContents);
                wr.flush();
                wr.close();

                //Make the request
                conn.getResponseCode();
                newLocation = conn.getHeaderField("location");
            }
            catch (Exception e)
            {
                 Utilities.LogError("GDocsUploadHandler.UploadFileContentsToResumableUrl", e);
            }
            finally
            {
                if(conn != null)
                {
                    conn.disconnect();
                }
            }

            return newLocation;

        }

        private FileAccessLocations SearchForFile(String gpsLoggerFolderFeed, String fileName)
        {

            FileAccessLocations fal = new FileAccessLocations();
            HttpURLConnection conn = null;
            String searchUrl = gpsLoggerFolderFeed + "?title=" + fileName;

            try
            {

                URL url = new URL(searchUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                AddCommonHeaders(conn);

                Document doc = Utilities.GetDocumentFromInputStream(conn.getInputStream());
                fal.CreateUrl = GetFileUploadUrl(doc);
                fal.UpdateUrl = GetFileEditUrl(doc);
            }
            catch (Exception e)
            {
                Utilities.LogError("GDocsUploadHandler.SearchForFile", e);
            }
            finally
            {
                if(conn != null)
                {
                    conn.disconnect();
                }
            }

            return fal;

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


        private String CreateFolder()
        {

            String folderFeedUrl = "";
            HttpURLConnection conn = null;

            String createFolderUrl = "https://docs.google.com/feeds/default/private/full";

            String createXml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<entry xmlns=\"http://www.w3.org/2005/Atom\">\n" +
                    "  <category scheme=\"http://schemas.google.com/g/2005#kind\"\n" +
                    "      term=\"http://schemas.google.com/docs/2007#folder\"/>\n" +
                    "  <title>GPSLogger For Android</title>\n" +
                    "</entry>";

            try
            {

                URL url = new URL(createFolderUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                AddCommonHeaders(conn);
                conn.setRequestProperty("Content-Type", "application/atom+xml");

                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(
                        conn.getOutputStream());
                wr.writeBytes(createXml);
                wr.flush();
                wr.close();

                folderFeedUrl = GetFolderFeedUrlFromInputStream(conn.getInputStream());

            }
            catch (Exception e)
            {

               Utilities.LogError("GDocsUploadHandler.CreateFolder", e);
            }
            finally
            {
                if(conn != null)
                {
                    conn.disconnect();
                }

            }

            return folderFeedUrl;
        }



        private String GetFolderFeedUrlFromInputStream(InputStream inputStream)
        {
            String folderFeedUrl = "";

            Document createFolderDoc = Utilities.GetDocumentFromInputStream(inputStream);

            Node newFolderContentNode = createFolderDoc.getElementsByTagName("content").item(0);

            if (newFolderContentNode == null)
            {
                Utilities.LogInfo("Could not get collection info from response");
            }
            else
            {
                folderFeedUrl = createFolderDoc.getElementsByTagName("content").item(0)
                        .getAttributes().getNamedItem("src").getNodeValue();
            }

            return folderFeedUrl;
        }


        private String SearchForGpsLoggerFolder()
        {

            String folderFeedUrl = "";

            String searchUrl = "https://docs.google.com/feeds/default/private/full?title=GPSLogger+For+Android&showfolders=true";
            HttpURLConnection conn = null;

            try
            {

                URL url = new URL(searchUrl);

                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                AddCommonHeaders(conn);

                folderFeedUrl = GetFolderFeedUrlFromInputStream(conn.getInputStream());
            }
            catch (Exception e)
            {
                Utilities.LogError("GDocsUploadHandler.SearchForGpsLoggerFolder", e);
            }
            finally
            {
                if(conn != null)
                {
                    conn.disconnect();
                }
            }

            return folderFeedUrl;
        }


        /**
         * Adds headers commonly  used when talking to Google APIs
         * @param conn
         */
        private void AddCommonHeaders(HttpURLConnection conn)
        {
            conn.addRequestProperty("client_id", GDocsHelper.GetClientID(ctx));
            conn.addRequestProperty("client_secret", GDocsHelper.GetClientSecret(ctx));
            conn.setRequestProperty("GData-Version", "3.0");
            conn.setRequestProperty("User-Agent", "GPSLogger for Android");
            conn.setRequestProperty("Authorization", "OAuth " + GDocsHelper.GetAuthToken(ctx));
        }

        private class FileAccessLocations
        {
            public String CreateUrl;
            public String UpdateUrl;
        }

    }



}
