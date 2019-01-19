package com.mendhak.gpslogger.loggers.customurl;

import android.support.v4.util.Pair;

import com.mendhak.gpslogger.common.Strings;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CustomUrlRequest implements Serializable {


    private HashMap<String, String> httpHeaders = new HashMap<String, String>();
    private String logURL;
    private String httpMethod;
    private String httpBody;
    private String rawHeaders;



    public CustomUrlRequest(String logUrl, String httpMethod) {
        this(logUrl, httpMethod, "", "");
    }

    public CustomUrlRequest(String logUrl)  {
        this(logUrl, "GET");
    }

    public CustomUrlRequest(String logURL, String httpMethod, String httpBody, String rawHeaders){
        this.logURL = logURL;
        this.httpMethod = httpMethod.toUpperCase();
        this.httpBody = httpBody;
        this.rawHeaders = rawHeaders;


        Pair<String, String> creds = getBasicAuthCredentialsFromUrl(this.logURL);
        addAuthorizationHeader(creds);
        removeCredentialsFromUrl(creds);

        //HttpHeaders.putAll(getHeadersFromTextBlock(RawHeaders))
        this.httpHeaders.putAll(getHeadersFromTextBlock(rawHeaders));


    }

    private Map<String,String> getHeadersFromTextBlock(String rawHeaders) {

        HashMap<String, String> map = new HashMap<>();
        String[] lines = rawHeaders.split("\\r?\\n");
        for (String line : lines){
            if(!Strings.isNullOrEmpty(line) && line.contains(":")){
                String[] lineParts = line.split(":");
                if(lineParts.length == 2){
                    String lineKey = line.split(":")[0].trim();
                    String lineValue = line.split(":")[1].trim();

                    if(!Strings.isNullOrEmpty(lineKey) && !Strings.isNullOrEmpty(lineValue)){
                        map.put(lineKey, lineValue);
                    }
                }

            }
        }

        return map;

    }

    private void addAuthorizationHeader(Pair<String, String> creds) {

        if(!Strings.isNullOrEmpty(creds.first) && !Strings.isNullOrEmpty(creds.second)){
            String credential = okhttp3.Credentials.basic(creds.first, creds.second);
            this.httpHeaders.put("Authorization", credential);
        }

    }

    private void removeCredentialsFromUrl(Pair<String, String> creds) {
        this.logURL = this.logURL.replace(creds.first + ":" + creds.second + "@","");
    }

    private Pair<String, String> getBasicAuthCredentialsFromUrl(String logURL) {
        Pair<String, String> result  = new Pair<>("","");

        Pattern r = Pattern.compile("(\\w+):(\\w+)@.+");
        Matcher m = r.matcher(logURL);
        while(m.find()){
            result = new Pair<>(m.group(1), m.group(2));
        }

        return result;

    }



    String getHttpMethod(){
        return this.httpMethod;
    }

    String getLogURL(){
        return this.logURL;
    }

    HashMap<String, String> getHttpHeaders(){
        return this.httpHeaders;
    }

    String getHttpBody(){
        return this.httpBody;
    }
}
