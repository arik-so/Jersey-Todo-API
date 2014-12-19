package com.arik;

import com.arik.models.TodoItem;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;
import java.util.ArrayList;

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

        for(TodoItem currentItem : allTodoItems){

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
        if(todoItem == null){
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        JSONObject json = todoItem.toJSONObject(false);
        return json.toJSONString();

    }

    @POST
    @Produces("application/json")
    public String createTodoItem(@FormParam("title") String title, @FormParam("body") String body) throws
            UnknownHostException {

        if(title == null || title.length() < 1){
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("The title must not be empty").build());
        }

        TodoItem todoItem = TodoItem.create();

        todoItem.setTitle(title);
        todoItem.setBody(body);
        todoItem.setDone(false);

        todoItem.save();

        JSONObject json = todoItem.toJSONObject(true);
        return json.toString();

    }

    @PUT
    @Path("/{id}")
    @Produces("application/json")
    public String updateTodoItem(@PathParam("id") String identifier, @FormParam("modification_token") String
            modificationToken, @FormParam("title") String title, @FormParam("body") String body, @FormParam
            ("done") String isDoneString) throws UnknownHostException {

        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);

        // the item with that ID does no exist
        if(todoItem == null){
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        if(!todoItem.getModificationToken().equals(modificationToken)){
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid " +
                    "modification token").build());
        }

        if(title != null){
            todoItem.setTitle(title);
        }

        if(body != null){
            todoItem.setBody(body);
        }

        if(isDoneString != null){

            if(isDoneString.equalsIgnoreCase("true") || isDoneString.equalsIgnoreCase("1")){
                todoItem.setDone(true);
            }else if(isDoneString.equalsIgnoreCase("false") || isDoneString.equalsIgnoreCase("0")){
                todoItem.setDone(false);
            }

        }

        todoItem.save();

        JSONObject json = todoItem.toJSONObject(false);
        return json.toString();

    }

    @DELETE
    @Path("/{id}")
    public void removeTodoItem(@PathParam("id") String identifier, @FormParam("modification_token") String
            modificationToken) throws UnknownHostException {

        TodoItem todoItem = TodoItem.fetchTodoItemByID(identifier);

        // the item with that ID does no exist
        if(todoItem == null){
            throw new NotFoundException(Response.status(Response.Status.NOT_FOUND).entity("Invalid item ID").build());
        }

        if(!todoItem.getModificationToken().equals(modificationToken)){
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid " +
                    "modification token").build());
        }

        todoItem.remove();

    }

    @GET
    @Path("/{query}")
    public String searchTodoItems(@PathParam("query") String queryString){

        throw new NotFoundException("We're sorry");

    }


}
