package com.arik.twilio;

import com.twilio.sdk.TwilioRestException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by arik-so on 12/18/14.
 */
public class TwilioConnector {

    private static final String API_ENDPOINT = "https://api.twilio.com/2010-04-01/Accounts/AC2225010f929f77a0d5e779e5818b3a07/Messages.json";
    private static final String ACCOUNT_SID = "AC2225010f929f77a0d5e779e5818b3a07";
    private static final String ACCOUNT_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    private static final String SENDER_NUMBER = "%2B16506207470";

    public static void sendSMS(String recipientPhoneNumber, String smsMessage) throws TwilioRestException {

        HttpURLConnection connection = null;
        int responseStatusCode = -1;

        String authentication = ACCOUNT_SID + ":" + ACCOUNT_AUTH_TOKEN;
        String base64Authentication = new BASE64Encoder().encode(authentication.getBytes());

        // this is a Java bug, we need to perform a workaround
        base64Authentication = base64Authentication.replaceAll("\n", "");

        String postParams = "From=" + SENDER_NUMBER;
        String response = "";


        try {
            postParams += "&To=" + URLEncoder.encode(recipientPhoneNumber, "UTF-8");
            postParams += "&Body=" + URLEncoder.encode(smsMessage, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {

            URL url = new URL(API_ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();

            // we need to authenticate the API user
            connection.setRequestProperty("Authorization", "Basic " + base64Authentication);

            // we wanna be able to send POST data
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // and we wanna read the response
            connection.setDoInput(true);

            // send the POST data
            DataOutputStream postStream = new DataOutputStream(connection.getOutputStream());
            postStream.writeBytes(postParams);
            postStream.flush();
            postStream.close();

            responseStatusCode = connection.getResponseCode();

            // and get the response
            InputStream responseStream;
            try {
                responseStream = connection.getInputStream();
            } catch (IOException e) {
                responseStream = connection.getErrorStream();
            }
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(responseStream));

            String responseLine;
            while ((responseLine = responseReader.readLine()) != null) {
                response += responseLine + '\r' + '\n';
            }
            responseReader.close();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        } finally {

            if (connection != null) {
                connection.disconnect();
            }

        }

        JSONObject responseDetails = (JSONObject) JSONValue.parse(response);
        String responseMessage = (String) responseDetails.get("message");

        if (responseStatusCode != 200) {
            throw new TwilioRestException(responseMessage, responseStatusCode);
        }


    }

}
