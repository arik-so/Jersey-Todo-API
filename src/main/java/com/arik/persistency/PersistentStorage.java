package com.arik.persistency;

import com.mongodb.*;

import java.net.UnknownHostException;

/**
 * A class that deals with the database stuff in order to make the rest implementation-agnostic
 */
public class PersistentStorage {

    private static com.mongodb.DB databaseConnection;

    /**
     * A method to establish (if necessary) a connection to the database.
     * @return An instance of a writable MongoDB database wrapper object
     * @throws UnknownHostException Thrown if no MongoDB server is available
     */
    public synchronized static com.mongodb.DB getDatabaseConnection() throws UnknownHostException { // DB is so short and
    // ambiguous

        if(databaseConnection != null){
            return databaseConnection;
        }

        MongoClient mongoClient;
        String database;

        final String mongoSoupHost = System.getenv("MONGOSOUP_URL");

        // if the environment tells us the mongosoup DB url, we use that one. Otherwise, we revert/default to local
        if(mongoSoupHost != null && mongoSoupHost.length() > 0){

            MongoClientURI mongoURI = new MongoClientURI(mongoSoupHost);
            mongoClient = new MongoClient(mongoURI);

            // the mongosoup instance only permits one database, which is given in the environment config
            database = mongoURI.getDatabase();

        }else{

            // just the default local configuration and an appropriate database name
            mongoClient = new MongoClient("localhost", 27017);
            database = "todo-api-db"; // our default database

        }

        databaseConnection = mongoClient.getDB(database);
        return databaseConnection;

    }

}
