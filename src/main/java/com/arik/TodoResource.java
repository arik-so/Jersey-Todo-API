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
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Controller for handling actions related to the To-do model
 */
@Path("/") // we operate right at the root path
public class TodoResource {

    private static final String QUERY_PRESET_PATH = "todo-query-preset.json";

    /**
     * Handles exceptions that occur pretty often due to reliance on external services such as Twilio, MongoDB, or Searchly
     *
     * @param e The exception to be handled.
     */
    private static void handleAmbiguousException(Exception e) {

        e.printStackTrace();

        if (e instanceof UnknownHostException) {
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was an issue with MongoDB: " + e.getMessage()).build());
        } else if (e instanceof TwilioRestException) {
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was an issue with Twilio: " + e.getMessage()).build());
        }

    }

    /**
     * Function to be called when no further parameters are provided
     *
     * @return JSON string representing the list of all to-do items
     */
    @GET
    @Produces("application/json")
    public String listTodoItems() {

        JSONArray json = new JSONArray();
        ArrayList<TodoItem> allTodoItems = null;
        try {
            allTodoItems = TodoItem.fetchAllTodoItems();
        } catch (UnknownHostException e) {
            handleAmbiguousException(e);
        }

        for (TodoItem currentItem : allTodoItems) {

            json.add(currentItem.toJSONObject(false));

        }

        return json.toJSONString();

    }

    /**
     * Show one particular to-do item
     *
     * @param identifier The ID of the item
     * @return JSON string representing the tiem
     */
    @GET
    @Path("/{id}")
    @Produces("application/json")
    public String getTodoItem(@PathParam("id") String identifier) {

        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            handleAmbiguousException(e);
        }

        // the item with that ID does no exist
        if (todoItem == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        JSONObject json = todoItem.toJSONObject(false);
        return json.toJSONString();

    }

    /**
     * Create a new to-do item
     *
     * @param title The title of the new item
     * @param body  Its body or description text
     * @return JSON string representing the new item, including the modification token necessary to modify or remove it
     */
    @POST
    @Produces("application/json")
    public String createTodoItem(@FormParam("title") String title, @FormParam("body") String body) {

        if (title == null || title.length() < 1) {
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("The title must not be empty").build());
        }


        TodoItem todoItem;

        try {

            todoItem = TodoItem.create();

            todoItem.setTitle(title);
            todoItem.setBody(body);
            todoItem.setDone(false);

            todoItem.save();

        } catch (Exception e) {

            e.printStackTrace();
            handleAmbiguousException(e);

            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was an issue with Searchly: " + e.getMessage()).build());

        }

        JSONObject json = todoItem.toJSONObject(true);
        return json.toString();

    }

    /**
     * Subscribe to the done status changes of a to-do item
     *
     * @param identifier  The ID of the item
     * @param phoneNumber Your phone number
     * @return A success message
     */
    @GET
    @Path("/{id}/subscribe/{phone: ([+]|%2[bB])?[0-9]+}") // it starts with a +, a %2b (case-insensitive), or a number
    @Produces("text/plain")
    public String subscribeToChangesOfTodoItem(@PathParam("id") String identifier, @PathParam("phone") String phoneNumber) {

        // let's check if the item exists
        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            handleAmbiguousException(e);
        }

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

        String successMessage = "You have subscribed to the changes of task \"" + todoItem.getTitle() + "\".";

        // first of all, let's send a test SMS

        try {

            TwilioConnector.sendSMS(phoneNumber, successMessage);
            todoItem.addSubscriber(phoneNumber);
            todoItem.save();

        } catch (Exception e) {

            handleAmbiguousException(e);

            // if it was not handled, the issue was most probably with Searchly, which, alas, does not specify concrete error classes
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was an issue with Searchly: " + e.getMessage()).build());

        }

        return successMessage;

    }

    /**
     * Modify an existing to-do item
     *
     * @param identifier        The ID of the item
     * @param modificationToken The modification token necessary to modify or remove it
     * @param title             Its new title (null if it is to remain unchanged)
     * @param body              Its new body (null if it is to remain unchanged)
     * @param isDoneString      true, false, 1 or 0 (case-insensitive, null if it is to remain unchanged)
     * @return JSON string representing the item in its new form
     */
    @PUT
    @Path("/{id}")
    @Produces("application/json")
    public String updateTodoItem(@PathParam("id") String identifier, @FormParam("modification_token") String modificationToken, @FormParam("title") String title, @FormParam("body") String body, @FormParam("done") String isDoneString) {

        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            handleAmbiguousException(e);
        }
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

            } else {
                notifySubscribers = false;
            }

        }

        try {
            todoItem.save();
        } catch (Exception e) {

            handleAmbiguousException(e);
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was an issue with Searchly: " + e.getMessage()).build());

        }

        if (notifySubscribers) {

            for (String phoneNumber : todoItem.getSubscribers()) {

                try {
                    TwilioConnector.sendSMS(phoneNumber, "\"" + todoItem.getTitle() + "\" task has been marked as " + doneStatusModifier);
                } catch (TwilioRestException e) {

                    // we suppress these errors from propagation
                    e.printStackTrace();

                }

            }

        }

        JSONObject json = todoItem.toJSONObject(false);
        return json.toString();

    }

    /**
     * Remove an existing to-do item
     *
     * @param identifier        The ID of the item
     * @param modificationToken The modification token necessary to modify or remove it
     * @return A success message
     */
    @DELETE
    @Path("/{id}")
    public String removeTodoItem(@PathParam("id") String identifier, @QueryParam("modification_token") String modificationToken) {

        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            handleAmbiguousException(e);
        }

        // the item with that ID does no exist
        if (todoItem == null) {
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        if (!todoItem.getModificationToken().equals(modificationToken)) {
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid modification token").build());
        }

        try {
            todoItem.remove();
        } catch (UnknownHostException e) {
            handleAmbiguousException(e);
        }

        return "Task \"" + todoItem.getTitle() + "\" has been removed.";

    }

    /**
     * Search existing to-do items
     *
     * @param queryString The query string (includes support for wildcards)
     * @return JSON string representing the matches in decreasing order of relevance
     */
    @GET
    @Path("/search/{query}")
    public String searchTodoItems(@PathParam("query") String queryString) {

        String elasticSearchQuery = null;

        // this clause should always succeed. There are no external factors able to contribute to failure, so if it
        // fails, it's due to erroneous configuration, and no specific messages should leave the server
        try {
            // this should never be zero
            URL queryPresetURL = SearchlyHelper.class.getClassLoader().getResource(QUERY_PRESET_PATH);

            String queryPreset = new String(Files.readAllBytes(Paths.get(queryPresetURL.toURI())));
            elasticSearchQuery = StringUtils.replace(queryPreset, "{QUERY_STRING}", queryString);

        } catch (IOException e) {
            e.printStackTrace();
            throw new InternalServerErrorException();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new InternalServerErrorException();
        }

        Search search = new Search.Builder(elasticSearchQuery).addIndex(TodoItem.JEST_INDEX).addType(TodoItem.JEST_TYPE).build();

        JestClient client = SearchlyHelper.getJestClient();
        SearchResult result = null;
        try {
            result = client.execute(search);
        } catch (Exception e) {

            e.printStackTrace();
            throw new InternalServerErrorException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("There was an issue with Searchly: " + e.getMessage()).build());

        }

        JSONObject foundItemDetails = (JSONObject) JSONValue.parse(result.getJsonString());
        JSONObject outerHits = (JSONObject) foundItemDetails.get("hits");
        JSONArray foundItems = (JSONArray) outerHits.get("hits");

        JSONArray output = new JSONArray();

        for (Object currentFindObject : foundItems) {

            JSONObject currentFind = (JSONObject) currentFindObject;
            String currentIdentifier = (String) currentFind.get("_id");

            TodoItem currentItem = null;
            try {
                currentItem = TodoItem.fetchTodoItemByID(currentIdentifier);
            } catch (UnknownHostException e) {
                handleAmbiguousException(e);
            }

            // occasionally, an item will have been removed from MongoDB but an index removal error could have occurred thereafter
            if (currentItem == null) { continue; }

            output.add(currentItem.toJSONObject(false));

        }

        return output.toJSONString();

    }

}
