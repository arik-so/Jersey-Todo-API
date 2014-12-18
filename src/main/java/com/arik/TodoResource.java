package com.arik;

import com.arik.twilio.TwilioConnector;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Created by arik-so on 12/18/14.
 */
@Path("/todo")
public class TodoResource {

    @POST
    public String createTodoItem(){
        return "Created Todo item";
    }

    @GET
    @Path("/twilinfo")
    @Produces("text/plain")
    public String getTwilioInfo(){
        return "Twilio info: "+TwilioConnector.ACCOUNT_AUTH_TOKEN;
    }

}
