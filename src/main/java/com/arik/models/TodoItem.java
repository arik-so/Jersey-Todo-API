package com.arik.models;

import com.arik.persistency.PersistentStorage;
import com.arik.search.SearchlyHelper;
import com.mongodb.*;
import io.searchbox.annotations.JestId;
import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by arik-so on 12/19/14.
 */
public class TodoItem {

    public static final String JEST_INDEX = "todo-items";
    public static final String JEST_TYPE = "todo-item";
    private static final String DB_TABLE = "tests";
    private static SecureRandom secureRandom = new SecureRandom();

    @JestId
    private String identifier;

    private String title;

    private String body;

    private boolean isDone;

    /**
     * A list of phone numbers to be notified whenever a change occurs
     */
    private ArrayList<String> subscribers;

    /**
     * Necessary in order to update or delete an item
     */
    private String modificationToken;

    private DBObject row;

    /**
     * In order to avoid confusion, we do not allow external calls to the constructor
     */
    private TodoItem() {
    }

    public static TodoItem create() throws Exception {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        String modificationToken = new BigInteger(128, secureRandom).toString(32);

        BasicDBObject row = new BasicDBObject();
        row.append("title", null);
        row.append("body", null);
        row.append("is_done", false);
        row.append("subscribers", new ArrayList<String>());
        row.append("modification_token", modificationToken);

        table.insert(row);
        ObjectId insertionID = (ObjectId) row.get("_id");

        TodoItem todoItem = fetchTodoItemByID(insertionID.toString());

        // create the search index
        try {

            JestClient jestClient = SearchlyHelper.getJestClient();
            Index index = new Index.Builder(todoItem.toElasticSearchMap()).index(JEST_INDEX).type(JEST_TYPE).id(todoItem.getID()).build();
            jestClient.execute(index);

        } catch (Exception e) {

            // if we have failed to index this item, it's unsearchable, and thus, useless
            todoItem.remove();
            throw e;

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

        if (row == null) {
            return null;
        }

        TodoItem todoItem = new TodoItem();
        todoItem.row = row;

        todoItem.identifier = identifier;
        todoItem.title = (String) row.get("title");
        todoItem.body = (String) row.get("body");
        todoItem.isDone = (Boolean) row.get("is_done");
        
        todoItem.subscribers = (ArrayList<String>) row.get("subscribers");
        if(todoItem.subscribers == null){
            todoItem.subscribers = new ArrayList<String>();
        }
        
        todoItem.modificationToken = (String) row.get("modification_token");

        return todoItem;

    }

    public static ArrayList<TodoItem> fetchAllTodoItems() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        ArrayList<TodoItem> allItems = new ArrayList<>();

        DBCursor cursor = table.find();

        try {
            while (cursor.hasNext()) {
                String currentIdentifier = cursor.next().get("_id").toString();
                allItems.add(fetchTodoItemByID(currentIdentifier));
            }
        } finally {
            cursor.close();
        }

        return allItems;

    }

    public void save() throws Exception {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.update(query, this.row);

        // update the search index
        JestClient jestClient = SearchlyHelper.getJestClient();
        Index update = new Index.Builder(this.toElasticSearchMap()).index(JEST_INDEX).type(JEST_TYPE).id(this.getID()).build();

        jestClient.execute(update);

    }

    public void remove() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection(DB_TABLE);

        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.remove(query);


        // remove the search index
        try {
            JestClient jestClient = SearchlyHelper.getJestClient();
            Delete delete = new Delete.Builder(this.getID()).index(JEST_INDEX).type(JEST_TYPE).build();

            jestClient.execute(delete);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public JSONObject toJSONObject(boolean includeModificationToken) {

        JSONObject json = new JSONObject();
        json.put("id", this.getID());
        json.put("title", this.getTitle());
        json.put("body", this.getBody());
        json.put("done", this.isDone());

        if (includeModificationToken) {
            json.put("modification_token", this.getModificationToken());
        }

        return json;

    }

    private Map<String, String> toElasticSearchMap() {

        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("title", this.getTitle());
        map.put("body", this.getBody());

        return map;

    }

    public String getID() {
        return this.identifier;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.row.put("title", title);
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        this.row.put("body", body);
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean isDone) {
        this.isDone = isDone;
        this.row.put("is_done", isDone);
    }

    public String getModificationToken() {
        return this.modificationToken;
    }

    public ArrayList<String> getSubscribers() {
        return this.subscribers;
    }

    public void addSubscriber(String phoneNumber){
        
        if(!this.subscribers.contains(phoneNumber)){
            this.subscribers.add(phoneNumber);
            this.row.put("subscribers", this.subscribers);
        }
        
    }
    
    public void removeSubscriber(String phoneNumber){
        
        if(this.subscribers.contains(phoneNumber)){
            this.subscribers.remove(phoneNumber);
            this.row.put("subscribers", this.subscribers);
        }
        
    }
    
}
