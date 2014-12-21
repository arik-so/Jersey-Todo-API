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

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by arik-so on 12/18/14.
 */
@Path("/todo")
public class TodoResource {

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
    public String subscribeToChangesOfTodoItem(@PathParam("id") String identifier, @PathParam("phone") String phoneNumber) {

        // if we have a +, it's url-decoded as ' ', which could be problematic
        if (phoneNumber.startsWith(" ")) {
            phoneNumber = "+" + phoneNumber.trim();
        }

        // in some places intl numbers start with 00, but Twilio requires a +
        if (phoneNumber.startsWith("00")) {
            phoneNumber = "+" + phoneNumber.substring(2);
        }

        String successMessage = "You have subscribed to the changes of Todo item " + identifier + ".";

        // first of all, let's send a test SMS
        try {
            TwilioConnector.sendSMS(phoneNumber, successMessage);
        } catch (TwilioRestException e) {
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity("There was an issue with Twilio: " + e.getErrorMessage()).build());
        }

        return successMessage;

    }

    @PUT
    @Path("/{id}")
    @Produces("application/json")
    public String updateTodoItem(@PathParam("id") String identifier, @FormParam("modification_token") String modificationToken, @FormParam("title") String title, @FormParam("body") String body, @FormParam("done") String isDoneString) throws UnknownHostException, TwilioRestException {

        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);

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

            if (isDoneString.equalsIgnoreCase("true") || isDoneString.equalsIgnoreCase("1")) {
                todoItem.setDone(true);
            } else if (isDoneString.equalsIgnoreCase("false") || isDoneString.equalsIgnoreCase("0")) {
                todoItem.setDone(false);
            }

        }

        try {
            todoItem.save();
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Item could not be modified: " + e.getMessage()).build());
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

        URL queryPresetURL = SearchlyHelper.class.getClassLoader().getResource("todo-query-preset.json");
        String queryPreset = new String(Files.readAllBytes(Paths.get(queryPresetURL.toURI())));
        String elasticSearchQuery = StringUtils.replace(queryPreset, "{QUERY_STRING}", queryString);

        System.out.println(elasticSearchQuery);

        Search search = new Search.Builder(elasticSearchQuery).addIndex(TodoItem.JEST_INDEX).addType(TodoItem.JEST_TYPE).build();

        JestClient client = SearchlyHelper.getJestClient();
        SearchResult result = client.execute(search);

        System.out.println("Search: "+search);
        System.out.println("Result: "+result);
        System.out.println("Error: " + result.getErrorMessage());
        System.out.println("Result path: "+result.getPathToResult());

        JSONArray json = new JSONArray();
        return result.getJsonString();

    }


}
