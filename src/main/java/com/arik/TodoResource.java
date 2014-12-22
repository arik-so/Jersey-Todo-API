package com.arik;

import com.arik.models.TodoItem;
import com.arik.models.TodoItemState;
import com.arik.search.JestException;
import com.arik.search.SearchlyConnector;
import com.arik.twilio.PhoneNumberNormalizer;
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
import java.util.List;

/**
 * Controller for handling actions related to the To-do model
 */
@Path("/") // we operate right at the root path
public class TodoResource {

    private static final String QUERY_PRESET_PATH = "todo-query-preset.json";

    /**
     * Function to be called when no further parameters are provided
     *
     * @return JSON string representing the list of all to-do items
     */
    @GET
    @Produces("application/json")
    public String listTodoItems() {

        JSONArray json = new JSONArray();
        List<TodoItem> allTodoItems = null;

        try {
            allTodoItems = TodoItem.fetchAllTodoItems();
        } catch (UnknownHostException e) {
            RestAPIExceptionHandler.handleExternalServiceException(e);
        }

        for (TodoItem currentItem : allTodoItems) {

            json.add(currentItem.toJSONObject(false));

        }

        return json.toString();

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
    public String getTodoItem(@PathParam("id") final String identifier) {

        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            RestAPIExceptionHandler.handleExternalServiceException(e);
        }

        // the item with that ID does no exist
        if (todoItem == null) {
            RestAPIExceptionHandler.handleException(Response.Status.NOT_FOUND, "Invalid item ID");
        }

        return todoItem.toJSONObject(false).toString();

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
    public String createTodoItem(@FormParam("title") final String title, @FormParam("body") final String body) {

        if (title == null || title.isEmpty()) {
            RestAPIExceptionHandler.handleException(Response.Status.BAD_REQUEST, "The title must not be empty");
        }

        TodoItem todoItem;

        try {

            todoItem = TodoItem.create();

            todoItem.setTitle(title);
            todoItem.setBody(body);
            todoItem.setDone(false);

            todoItem.save();

        } catch (JestException | UnknownHostException e) {

            RestAPIExceptionHandler.handleExternalServiceException(e);

            // this will never be called because the handler throws an error
            return null;

        }

        return todoItem.toJSONObject(true).toString();

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
    @Produces("text/json")
    public String subscribeToChangesOfTodoItem(@PathParam("id") final String identifier, @PathParam("phone") final String phoneNumber) {

        // let's check if the item exists
        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            RestAPIExceptionHandler.handleExternalServiceException(e);
        }

        // the item with that ID does no exist
        if (todoItem == null) {
            RestAPIExceptionHandler.handleException(Response.Status.NOT_FOUND, "Invalid item ID");
        }

        // next, let's normalize the phone number representation
        final String normalizedPhoneNumber = PhoneNumberNormalizer.normalizePhoneNumber(phoneNumber);
        final String successMessage = "You have subscribed to the changes of task \"" + todoItem.getTitle() + "\".";

        // we do not need to check the phone number and send SMS if the item has already been subscribed to
        if (!todoItem.getSubscribers().contains(normalizedPhoneNumber)) {

            try {

                // first of all, let's send a test SMS
                // if it fails, we will be taken to the catch clause immediately and the object will no be modified
                TwilioConnector.sendSMS(normalizedPhoneNumber, successMessage);

                todoItem.addSubscriber(normalizedPhoneNumber);
                todoItem.save();

            } catch (TwilioRestException | JestException | UnknownHostException e) {
                RestAPIExceptionHandler.handleExternalServiceException(e);
            }

        }

        final JSONObject successJSON = new JSONObject();
        successJSON.put("status", Response.Status.OK.getStatusCode());
        successJSON.put("message", successMessage);
        return successJSON.toString();

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
    public String updateTodoItem(@PathParam("id") final String identifier, @FormParam("modification_token") final String modificationToken, @FormParam("title") final String title, @FormParam("body") final String body, @FormParam("done") final String isDoneString) {

        final TodoItem todoItem;

        try {

            todoItem = TodoItem.fetchTodoItemByID(identifier);

            // the item with that ID does no exist
            if (todoItem == null) {
                RestAPIExceptionHandler.handleException(Response.Status.NOT_FOUND, "Invalid item ID");
            }

            // alas, Jersey does not support HTTP Basic Authentication, which I would have used otherwise
            if (!todoItem.getModificationToken().equals(modificationToken)) {
                RestAPIExceptionHandler.handleException(Response.Status.UNAUTHORIZED, "Invalid modification token");
            }

            if (title != null && !title.isEmpty()) {
                todoItem.setTitle(title);
            }

            if (body != null) {
                todoItem.setBody(body);
            }


            final TodoItemState.DoneState doneState = TodoItemState.DoneState.fromString(isDoneString);

            if (doneState.isModifier()) {
                todoItem.setDone(doneState.isDone());
            }

            // we need to ensure persistence before we notify via Twilio
            todoItem.save();

            // after persistence is guaranteed, we notify the Twilio subscribers about the change
            notifySubscribers(todoItem, doneState);

        } catch (UnknownHostException | JestException e) {

            RestAPIExceptionHandler.handleExternalServiceException(e);

            return null;

        }

        final JSONObject json = todoItem.toJSONObject(false);
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
    @Produces("application/json")
    public Response removeTodoItem(@PathParam("id") final String identifier, @QueryParam("modification_token") final String modificationToken) {

        TodoItem todoItem = null;
        try {
            todoItem = TodoItem.fetchTodoItemByID(identifier);
        } catch (UnknownHostException e) {
            RestAPIExceptionHandler.handleExternalServiceException(e);
        }

        // the item with that ID does no exist
        if (todoItem == null) {
            RestAPIExceptionHandler.handleException(Response.Status.NOT_FOUND, "Invalid item ID");
        }

        if (!todoItem.getModificationToken().equals(modificationToken)) {
            RestAPIExceptionHandler.handleException(Response.Status.UNAUTHORIZED, "Invalid modification token");
        }

        try {
            todoItem.remove();
        } catch (UnknownHostException e) {
            RestAPIExceptionHandler.handleExternalServiceException(e);
        } catch (JestException e) {

            // even if the index has failed to be removed, the item no longer exists
            
            // stderr directs the output to Heroku's logger
            e.printStackTrace();

        }

        return Response.status(Response.Status.NO_CONTENT).build();

    }

    /**
     * Search existing to-do items
     *
     * @param queryString The query string (includes support for wildcards)
     * @return JSON string representing the matches in decreasing order of relevance
     */
    @GET
    @Path("/search/{query}")
    @Produces("application/json")
    public String searchTodoItems(@PathParam("query") final String queryString) {

        String elasticSearchQuery = null;

        // this clause should always succeed. There are no external factors able to contribute to failure, so if it
        // fails, it's due to erroneous configuration, and no specific messages should leave the server
        try {
            // this should never be zero
            final URL queryPresetURL = SearchlyConnector.class.getClassLoader().getResource(QUERY_PRESET_PATH);

            final String queryPreset = new String(Files.readAllBytes(Paths.get(queryPresetURL.toURI())));
            
            // we need to sanitize the input to be a properly formatted JSON string in order to prevent search injection
            final String sanitizedQueryString = JSONObject.escape(queryString);

            elasticSearchQuery = StringUtils.replace(queryPreset, "{QUERY_STRING}", sanitizedQueryString);

        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
            RestAPIExceptionHandler.handleException(Response.Status.INTERNAL_SERVER_ERROR, null);
        }

        final Search search = new Search.Builder(elasticSearchQuery).addIndex(TodoItem.JEST_INDEX).addType(TodoItem.JEST_TYPE).build();
        final JestClient client = SearchlyConnector.getJestClient();

        final SearchResult result;

        try {
            result = client.execute(search);
        } catch (Exception e) {

            RestAPIExceptionHandler.handleExternalServiceException(new JestException(e));

            // this will never be called because the handler throws an error
            return null;

        }

        final JSONArray output = new JSONArray();
        String errorMessage = result.getErrorMessage();

        // if there was an error message, i. e. a parse error, sent from Searchly, it's none of the user's business
        // we just say nothing was found
        if(errorMessage == null) {

            final JSONObject foundItemDetails = (JSONObject) JSONValue.parse(result.getJsonString());
            final JSONObject outerHits = (JSONObject) foundItemDetails.get("hits");
            final JSONArray foundItems = (JSONArray) outerHits.get("hits");


            for (Object currentFindObject : foundItems) {

                JSONObject currentFind = (JSONObject) currentFindObject;
                String currentIdentifier = (String) currentFind.get("_id");

                TodoItem currentItem = null;
                try {
                    currentItem = TodoItem.fetchTodoItemByID(currentIdentifier);
                } catch (UnknownHostException e) {
                    RestAPIExceptionHandler.handleExternalServiceException(e);
                }

                // occasionally, an item will have been removed from MongoDB but an index removal error could have occurred thereafter
                if (currentItem == null) { continue; }

                output.add(currentItem.toJSONObject(false));

            }

        }

        return output.toString();

    }

    /**
     * @param todoItem
     * @param doneState
     */
    private void notifySubscribers(final TodoItem todoItem, final TodoItemState.DoneState doneState) {

        if (doneState.isModifier()) {

            for (String phoneNumber : todoItem.getSubscribers()) {

                try {
                    TwilioConnector.sendSMS(phoneNumber, "\"" + todoItem.getTitle() + "\" task has been marked as " + doneState.getStateMessage() + ".");
                } catch (TwilioRestException e) {

                    // we suppress these errors from propagation
                    e.printStackTrace();

                }

            }

        }

    }

}
