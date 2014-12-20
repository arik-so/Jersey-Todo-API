package com.arik.models;

import com.arik.persistency.PersistentStorage;
import com.arik.search.SearchlyHelper;
import com.mongodb.*;
import io.searchbox.annotations.JestId;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;

/**
 * Created by arik-so on 12/19/14.
 */
public class TodoItem {

    private static final String DB_TABLE = "tests";
    private static final String JEST_INDEX = "todo-items";
    private static final String JEST_TYPE = "todo-item";

    private static SecureRandom secureRandom = new SecureRandom();

    @JestId
    private String identifier;

    private String title;

    private String body;

    private boolean isDone;

    /**
     * Necessary in order to update or delete an item
     */
    private String modificationToken;

    private DBObject row;

    /**
     * In order to avoid confusion, we do not allow external calls to the constructor
     */
    private TodoItem(){}

    public static TodoItem create() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        String modificationToken = new BigInteger(128, secureRandom).toString(32);

        BasicDBObject row = new BasicDBObject();
        row.append("title", null);
        row.append("body", null);
        row.append("is_done", false);
        row.append("modification_token", modificationToken);

        table.insert(row);
        ObjectId insertionID = (ObjectId)row.get("_id");

        TodoItem todoItem = fetchTodoItemByID(insertionID.toString());

        // create the search index
        JestClient jestClient = SearchlyHelper.getJestClient();
        try {

            Index index = new Index.Builder(todoItem).index(JEST_INDEX).type(JEST_TYPE).build();
            jestClient.execute(index);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return todoItem;

    }

    public static TodoItem fetchTodoItemByID(String identifier) throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        BasicDBObject query = new BasicDBObject();
        ObjectId objectID;

        try {
            objectID = new ObjectId(identifier);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        query.put("_id", objectID);

        DBObject row;

        try {
            row = table.findOne(query);
        } catch (MongoException e) {
            e.printStackTrace();
            return null;
        }

        if(row == null){
            return null;
        }

        TodoItem todoItem = new TodoItem();
        todoItem.row = row;

        todoItem.identifier = identifier;
        todoItem.title = (String) row.get("title");
        todoItem.body = (String) row.get("body");
        todoItem.isDone = (Boolean) row.get("is_done");
        todoItem.modificationToken = (String) row.get("modification_token");

        return todoItem;

    }

    public static ArrayList<TodoItem> fetchAllTodoItems() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        ArrayList<TodoItem> allItems = new ArrayList<>();

        DBCursor cursor = table.find();
        try {
            while(cursor.hasNext()) {
                String currentIdentifier = cursor.next().get("_id").toString();
                allItems.add(fetchTodoItemByID(currentIdentifier));
            }
        } finally {
            cursor.close();
        }

        return allItems;

    }

    public void save() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.update(query, this.row);


        // update the search index
        JestClient jestClient = SearchlyHelper.getJestClient();
        try {

            Update update = new Update.Builder(this).index(JEST_INDEX).type(JEST_TYPE).id(this.getID()).build();
            jestClient.execute(update);

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void remove() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.remove(query);


        // remove the search index
        JestClient jestClient = SearchlyHelper.getJestClient();
        try {

            Delete delete = new Delete.Builder(this.getID()).index(JEST_INDEX).type(JEST_TYPE).build();
            jestClient.execute(delete);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public JSONObject toJSONObject(boolean includeModificationToken){

        JSONObject json = new JSONObject();
        json.put("id", this.getID());
        json.put("title", this.getTitle());
        json.put("body", this.getBody());
        json.put("done", this.isDone());

        if(includeModificationToken){
            json.put("modification_token", this.getModificationToken());
        }

        return json;

    }

    public String getID() {
        return identifier;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isDone() {
        return isDone;
    }

    public String getModificationToken() {
        return modificationToken;
    }

    public void setTitle(String title) {
        this.title = title;
        this.row.put("title", title);
    }

    public void setBody(String body) {
        this.body = body;
        this.row.put("body", body);
    }

    public void setDone(boolean isDone) {
        this.isDone = isDone;
        this.row.put("is_done", isDone);
    }
}
