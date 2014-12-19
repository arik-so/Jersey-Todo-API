package com.arik.models;

import com.arik.persistency.PersistentStorage;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.net.UnknownHostException;
import java.security.SecureRandom;

/**
 * Created by arik-so on 12/19/14.
 */
public class TodoItem {

    private static SecureRandom secureRandom = new SecureRandom();

    /**
     *
     */
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
        DBCollection table = database.getCollection("tests");

        String modificationToken = new BigInteger(128, secureRandom).toString(32);

        BasicDBObject row = new BasicDBObject();
        row.append("title", null);
        row.append("body", null);
        row.append("is_done", false);
        row.append("modification_token", modificationToken);

        table.insert(row);
        ObjectId insertionID = (ObjectId)row.get("_id");

        return getTodoItemByID(insertionID.toString());

    }

    public static TodoItem getTodoItemByID(String identifier) throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection("tests");

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

    public void save() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection("tests");

        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.update(query, this.row);

    }

    public void remove() throws UnknownHostException {

        DB database = PersistentStorage.getDatabaseConnection();
        DBCollection table = database.getCollection("tests");

        BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.remove(query);

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
