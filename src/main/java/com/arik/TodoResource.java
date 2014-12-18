package com.arik;

import com.arik.persistency.PersistentStorage;
import com.arik.twilio.TwilioConnector;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created by arik-so on 12/18/14.
 */
@Path("/todo")
public class TodoResource {

    @POST
    @Produces("text/plain")
    public String createTodoItem(){
        return "Created Todo item";
    }

    @GET
    @Path("/twilinfo")
    @Produces("text/plain")
    public String getTwilioInfo(){
        return "Twilio info: "+TwilioConnector.ACCOUNT_AUTH_TOKEN;
    }

    @GET
    @Path("/dbtest")
    @Produces("application/json")
    public String testDB() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();

        DBCollection table = database.getCollection("tests");

        BasicDBObject row = new BasicDBObject();
        row.append("name", "Test Entry");
        row.append("timestamp", new Date());

        table.insert(row);

        DBObject foundRow = table.findOne();
        return foundRow.toString();

    }

}
