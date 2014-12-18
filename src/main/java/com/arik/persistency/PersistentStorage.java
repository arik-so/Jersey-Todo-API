package com.arik.persistency;

import com.mongodb.*;

import java.net.UnknownHostException;

/**
 * Created by arik-so on 12/18/14.
 */
public class PersistentStorage {

    public static com.mongodb.DB getDatabaseConnection() throws UnknownHostException { // DB is so short and ambiguous

        MongoClient mongoClient;
        String database = "todo-api-db";

        final String mongoSoupHost = System.getenv("MONGOSOUP_URL");
        if(mongoSoupHost != null && mongoSoupHost.length() > 0){
            MongoClientURI mongoURI = new MongoClientURI(mongoSoupHost);
            mongoClient = new MongoClient(mongoURI);
            database = mongoURI.getDatabase();
        }else{
            mongoClient = new MongoClient("localhost", 27017);
        }

        return mongoClient.getDB(database);

    }

}
