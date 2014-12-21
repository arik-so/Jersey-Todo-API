package com.arik;

import com.arik.models.TodoItem;
import com.arik.search.SearchlyHelper;
import com.arik.twilio.TwilioConnector;
import com.twilio.sdk.TwilioRestException;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Created by arik-so on 12/18/14.
 */
@Path("/") // we operate right at the root path
public class TodoResource {

    private static final String QUERY_PRESET_PATH = "todo-query-preset.json";
    
    @GET
    @Produces("application/json")
    public String listTodoItems() throws UnknownHostException {

        JSONArray json = new JSONArray();
        ArrayList<TodoItem> allTodoItems = TodoItem.fetchAllTodoItems();

        for (TodoItem currentItem : allTodoItems) {

            json.add(currentItem.toJSONObject(false));

        }

        return json.toJSONString();

    }

    @GET
    @Path("/{id}")
    @Produces("application/json")
    public String getTodoItem(@PathParam("id") String identifier) throws UnknownHostException {

        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);

        // the item with that ID does no exist
        if (todoItem == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        JSONObject json = todoItem.toJSONObject(false);
        return json.toJSONString();

    }

    @POST
    @Produces("application/json")
    public String createTodoItem(@FormParam("title") String title, @FormParam("body") String body) throws Exception {

        if (title == null || title.length() < 1) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("The title must not be empty").build());
        }


        TodoItem todoItem = TodoItem.create();

        todoItem.setTitle(title);
        todoItem.setBody(body);
        todoItem.setDone(false);

        try {
            todoItem.save();
        } catch (Exception e) {
            todoItem.remove();
        }

        JSONObject json = todoItem.toJSONObject(true);
        return json.toString();

    }

    @GET
    @Path("/{id}/subscribe/{phone: ([+]|%2[bB])?[0-9]+}") // it starts with a +, a %2b (case-insensitive), or a number
    @Produces("text/plain")
    public String subscribeToChangesOfTodoItem(@PathParam("id") String identifier, @PathParam("phone") String phoneNumber) throws UnknownHostException {

        // let's check if the item exists
        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);

        // the item with that ID does no exist
        if (todoItem == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }
        
        
        // next, let's normalize the phone number representation
        
        // if we have a +, it's url-decoded as ' ', which could be problematic
        if (phoneNumber.startsWith(" ")) {
            phoneNumber = "+" + phoneNumber.trim();
        }

        // in some places intl numbers start with 00, but Twilio requires a +
        if (phoneNumber.startsWith("00")) {
            phoneNumber = "+" + phoneNumber.substring(2);
        }

        String successMessage = "You have subscribed to the changes of Todo item " + identifier + ".";

        System.out.println("Would be success: " + successMessage);

        // first of all, let's send a test SMS

        try {

            TwilioConnector.sendSMS(phoneNumber, successMessage);
            todoItem.addSubscriber(phoneNumber);
            todoItem.save();

        } catch (TwilioRestException e) {

            e.printStackTrace();

            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity("There was an issue with Twilio: " + e.getErrorMessage()).build());
        } catch (Exception e) {
            
            /*
            Only occurs if Jest has not managed to reindex the item. We do not care about indexation at this instance,
            however, because none of the indexed properties have been modified
             */
            e.printStackTrace();
            
        }

        return successMessage;

    }

    @PUT
    @Path("/{id}")
    @Produces("application/json")
    public String updateTodoItem(@PathParam("id") String identifier, @FormParam("modification_token") String modificationToken, @FormParam("title") String title, @FormParam("body") String body, @FormParam("done") String isDoneString) throws UnknownHostException, TwilioRestException {

        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);
        boolean notifySubscribers = false;
        String doneStatusModifier = null;

        // the item with that ID does no exist
        if (todoItem == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        if (!todoItem.getModificationToken().equals(modificationToken)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid " + "modification token").build());
        }

        if (title != null) {

            if (title.length() < 1) {
                throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("The title must not be empty").build());
            }

            todoItem.setTitle(title);
        }

        if (body != null) {
            todoItem.setBody(body);
        }

        if (isDoneString != null) {

            notifySubscribers = true;
            
            if (isDoneString.equalsIgnoreCase("true") || isDoneString.equalsIgnoreCase("1")) {
                
                todoItem.setDone(true);
                doneStatusModifier = "done.";
                
            } else if (isDoneString.equalsIgnoreCase("false") || isDoneString.equalsIgnoreCase("0")) {
                
                todoItem.setDone(false);
                doneStatusModifier = "not done.";
                
            }else{
                notifySubscribers = false;
            }

        }

        try {
            todoItem.save();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Item could not be modified: " + e.getMessage()).build());
        }
        
        if(notifySubscribers){

            for(String phoneNumber : todoItem.getSubscribers()){

                try {
                    TwilioConnector.sendSMS(phoneNumber, "\""+todoItem.getTitle()+"\" task has been marked as " + doneStatusModifier);
                } catch (TwilioRestException e) {
                    e.printStackTrace();
                }

            }

        }

        // TwilioConnector.sendSMS();

        JSONObject json = todoItem.toJSONObject(false);
        return json.toString();

    }

    @DELETE
    @Path("/{id}")
    public void removeTodoItem(@PathParam("id") String identifier, @QueryParam("modification_token") String modificationToken) throws UnknownHostException {

        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);

        // the item with that ID does no exist
        if (todoItem == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        if (!todoItem.getModificationToken().equals(modificationToken)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid modification token").build());
        }

        todoItem.remove();

    }

    @GET
    @Path("/search/{query}")
    public String searchTodoItems(@PathParam("query") String queryString) throws Exception {

        // this should never be zero
        URL queryPresetURL = SearchlyHelper.class.getClassLoader().getResource(QUERY_PRESET_PATH);
        
        String queryPreset = new String(Files.readAllBytes(Paths.get(queryPresetURL.toURI())));
        String elasticSearchQuery = StringUtils.replace(queryPreset, "{QUERY_STRING}", queryString);

        Search search = new Search.Builder(elasticSearchQuery).addIndex(TodoItem.JEST_INDEX).addType(TodoItem.JEST_TYPE).build();

        JestClient client = SearchlyHelper.getJestClient();
        SearchResult result = client.execute(search);

        System.out.println("Search: " + elasticSearchQuery);
        System.out.println("Search error: " + result.getErrorMessage());
        System.out.println("Result: " + result.getJsonString());
        
        
        JSONObject foundItemDetails = (JSONObject) JSONValue.parse(result.getJsonString());
        JSONObject outerHits = (JSONObject) foundItemDetails.get("hits");
        JSONArray foundItems = (JSONArray) outerHits.get("hits");
        
        JSONArray output = new JSONArray();
        
        for(Object currentFindObject : foundItems){
            
            JSONObject currentFind = (JSONObject) currentFindObject;
            String currentIdentifier = (String) currentFind.get("_id");
            
            TodoItem currentItem = TodoItem.fetchTodoItemByID(currentIdentifier);
            if(currentItem == null){ continue; }
            
            output.add(currentItem.toJSONObject(false));
            
        }
        
        return output.toJSONString();

    }


}
