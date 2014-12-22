package com.arik.models;

import com.arik.persistence.PersistentStorage;
import com.arik.search.JestException;
import com.arik.search.SearchlyConnector;
import com.mongodb.*;
import io.searchbox.annotations.JestId;
import io.searchbox.client.JestClient;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Model of a to-do item
 */
public class TodoItem {

    public static final String JEST_INDEX = "todo-items";
    public static final String JEST_TYPE = "todo-item";
    private static final String DB_TABLE = "todo-items";

    /**
     * The ID of the item used both by MongoDB and by Searchly
     */
    @JestId
    private String identifier;

    private String title;

    private String body;

    private boolean isDone;

    /**
     * A list of phone numbers to be notified whenever a change occurs
     * I would have preferred to use a Set, but since MongoDB returns a BasicDBList, which is incompatible with
     * a Set, we use List
     */
    private List<String> subscribers;

    /**
     * Required in order to update or delete an item
     * We want to make sure that only the creator of the item can modify or delete it
     * The token is only shown at creation
     */
    private String modificationToken;

    /**
     * A MongoDB object row that always reflects the properties of the class for more convenient DB update operations
     */
    private DBObject row;

    /**
     * In order to avoid confusion, we do not allow package-external calls to the empty constructor such that objects
     * are always created using the create()-method an automatically added to the DB
     */
    private TodoItem() {
    }

    private TodoItem(final DBObject row) {

        final ObjectId identifier = (ObjectId) row.get("_id");

        this.row = row;

        this.identifier = identifier.toString();
        this.title = (String) row.get("title");
        this.body = (String) row.get("body");
        this.isDone = (Boolean) row.get("is_done");

        this.subscribers = (List<String>) row.get("subscribers");
        if (this.subscribers == null) {
            this.subscribers = new ArrayList<>();
        }

        this.modificationToken = (String) row.get("modification_token");

    }

    /**
     * Create a new to-do item by storing it in a database and creating an index referencing it
     *
     * @return An instance of the new item
     * @throws com.arik.search.JestException Thrown if there was an issue with Searchly
     * @throws java.net.UnknownHostException Thrown if there was an issue with MongoDB
     */
    public static TodoItem create() throws JestException, UnknownHostException {

        final DB database = PersistentStorage.getDatabaseConnection();
        final DBCollection table = database.getCollection(DB_TABLE);

        final DBObject row = new TodoItemDBObject();
        table.insert(row);

        // after the object has been added to the DB, the _id field is filled with an ObjectId
        // MongoDB guarantees that the _id field is safe to cast
        final ObjectId insertionID = (ObjectId) row.get("_id");

        final TodoItem todoItem = fetchTodoItemByID(insertionID.toString());

        // create the search index
        try {

            final JestClient jestClient = SearchlyConnector.getJestClient();
            final Index index = new Index.Builder(todoItem.toElasticSearchMap()).index(JEST_INDEX).type(JEST_TYPE).id(todoItem.getID()).build();
            jestClient.execute(index);

        } catch (Exception e) {

            // if we have failed to index this item, it's unsearchable, and thus, useless
            todoItem.remove();
            throw new JestException(e);

        }

        return todoItem;

    }

    /**
     * Get an existing to-do item by its ID
     *
     * @param identifier the ID of the to-do item
     * @return An instance of the item
     * @throws UnknownHostException Thrown if there was an issue with MongoDB
     */
    public static TodoItem fetchTodoItemByID(final String identifier) throws UnknownHostException {

        final DB database = PersistentStorage.getDatabaseConnection();
        final DBCollection table = database.getCollection(DB_TABLE);

        final BasicDBObject query = new BasicDBObject();
        final ObjectId objectID = new ObjectId(identifier);

        query.put("_id", objectID);

        final DBObject row = table.findOne(query);

        if (row == null) {
            return null;
        }

        return new TodoItem(row);

    }

    /**
     * Get a list of all to-do items
     *
     * @return A List containing every to-do item
     * @throws UnknownHostException Thrown if there is an issue with MongoDB
     */
    public static List<TodoItem> fetchAllTodoItems() throws UnknownHostException {

        final DB database = PersistentStorage.getDatabaseConnection();
        final DBCollection table = database.getCollection(DB_TABLE);

        final List<TodoItem> allItems = new ArrayList<>();

        // using try with automatic resource management
        // finally is not required because cursor.close() is called automatically
        try (DBCursor cursor = table.find()) {

            String currentIdentifier;
            while (cursor.hasNext()) {
                currentIdentifier = cursor.next().get("_id").toString();
                allItems.add(fetchTodoItemByID(currentIdentifier));
            }

        }

        return allItems;

    }

    /**
     * Save a modified object to the database
     *
     * @throws UnknownHostException Thrown if there is an issue with MongoDB
     * @throws JestException        Thrown if there is an issue with Searchly
     */
    public void save() throws UnknownHostException, JestException {

        final DB database = PersistentStorage.getDatabaseConnection();
        final DBCollection table = database.getCollection(DB_TABLE);

        final BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.update(query, this.row);

        // update the search index
        final JestClient jestClient = SearchlyConnector.getJestClient();
        final Index update = new Index.Builder(this.toElasticSearchMap()).index(JEST_INDEX).type(JEST_TYPE).id(this.getID()).build();

        try {
            jestClient.execute(update);
        } catch (Exception e) {
            throw new JestException(e);
        }

    }

    /**
     * Remove an object from the database
     *
     * @throws UnknownHostException
     * @throws JestException
     */
    public void remove() throws UnknownHostException, JestException {

        final DB database = PersistentStorage.getDatabaseConnection();
        final DBCollection table = database.getCollection(DB_TABLE);

        final BasicDBObject query = new BasicDBObject();
        query.append("_id", new ObjectId(identifier));

        table.remove(query);


        // remove the search index
        final JestClient jestClient = SearchlyConnector.getJestClient();
        final Delete delete = new Delete.Builder(this.getID()).index(JEST_INDEX).type(JEST_TYPE).build();

        try {
            jestClient.execute(delete);
        } catch (Exception e) {
            throw new JestException(e);
        }

    }

    /**
     * Get a JSON object reflecting the relevant values of the object
     *
     * @param includeModificationToken Whether or not the modification token should be included in the JSON object
     * @return The JSON object containing the values
     */
    public JSONObject toJSONObject(final boolean includeModificationToken) {

        // alas, JSONObject does not support Generics and therefore produces unchecked call
        final JSONObject json = new JSONObject();
        json.put("id", this.getID());
        json.put("title", this.getTitle());
        json.put("body", this.getBody());
        json.put("done", this.isDone());

        if (includeModificationToken) {
            json.put("modification_token", this.getModificationToken());
        }

        return json;

    }

    public String getID() {
        return this.identifier;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
        this.row.put("title", title);
    }

    public String getBody() {
        return body;
    }

    public void setBody(final String body) {
        this.body = body;
        this.row.put("body", body);
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(final boolean isDone) {
        this.isDone = isDone;
        this.row.put("is_done", isDone);
    }

    public String getModificationToken() {
        return this.modificationToken;
    }

    public List<String> getSubscribers() {
        return this.subscribers;
    }

    /**
     * Add a phone number to the subscribers list
     *
     * @param phoneNumber The phone number to be added
     */
    public void addSubscriber(final String phoneNumber) {

        // this check would not have been necessary had the container been a Set
        if (!this.subscribers.contains(phoneNumber)) {
            this.subscribers.add(phoneNumber);
            this.row.put("subscribers", this.subscribers);
        }

    }

    /**
     * Internal function for the indexation on Searchly
     *
     * @return A map used for the indexation on Searchly
     */
    private Map<String, String> toElasticSearchMap() {

        final LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("title", this.getTitle());
        map.put("body", this.getBody());

        return map;

    }

}
