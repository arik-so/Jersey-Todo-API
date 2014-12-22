package com.arik;

import com.arik.search.JestException;
import com.twilio.sdk.TwilioRestException;
import org.json.simple.JSONObject;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;

/**
 * Handles exceptions that occur pretty often due to reliance on external services such as Twilio, MongoDB, or Searchly
 */
public class RestAPIExceptionHandler {

    // no need for singletons in this particular instance
    public static void handleExternalServiceException(final Exception externalServiceException) {

        externalServiceException.printStackTrace();
        
        String errorMessage;

        if (externalServiceException instanceof UnknownHostException) {
            errorMessage = "There was an issue with MongoDB: ";
        } else if (externalServiceException instanceof TwilioRestException) {
            errorMessage = "There was an issue with Twilio: ";
        } else if (externalServiceException instanceof JestException) {
            errorMessage = "There was an issue with Searchly: ";
        } else {
            return;
        }

        errorMessage += externalServiceException.getMessage();
        handleException(Response.Status.INTERNAL_SERVER_ERROR, errorMessage);

    }
    
    public static void handleException(final Response.Status status, final String errorMessage){

        JSONObject jsonError = new JSONObject();
        jsonError.put("status", status.getStatusCode());
        
        if(errorMessage != null && !errorMessage.isEmpty()) {
            jsonError.put("message", errorMessage);
        }else{
            jsonError.put("message", status.getReasonPhrase());
        }
        
        throw new WebApplicationException(Response.status(status).entity(jsonError.toString()).build());
        
    }

}
