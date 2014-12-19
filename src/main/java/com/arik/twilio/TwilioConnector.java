package com.arik.twilio;

import java.util.*;
import com.twilio.sdk.*;
import com.twilio.sdk.resource.factory.*;
import com.twilio.sdk.resource.instance.*;
import com.twilio.sdk.resource.list.*;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

/**
 * Created by arik-so on 12/18/14.
 */
public class TwilioConnector {

    public static final String ACCOUNT_SID = "AC2225010f929f77a0d5e779e5818b3a07";
    public static final String ACCOUNT_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");

    public static void sendSMS(String recipientPhoneNumber, String smsMessage) throws TwilioRestException {

        TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, ACCOUNT_AUTH_TOKEN);

        // Build the parameters
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("From", "+16506207470"));
        params.add(new BasicNameValuePair("To", recipientPhoneNumber));
        params.add(new BasicNameValuePair("Body", smsMessage));

        MessageFactory messageFactory = client.getAccount().getMessageFactory();
        Message message = messageFactory.create(params);
        System.out.println(message.getSid());

    }

}
