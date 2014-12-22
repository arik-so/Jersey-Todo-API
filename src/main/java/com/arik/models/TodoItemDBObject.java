package com.arik.models;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * * 
 */
public class TodoItemDBObject extends BasicDBObject {

    private static SecureRandom secureRandom = new SecureRandom();
    
    public TodoItemDBObject(){

        // we want the modification token to be fairly random
        final String modificationToken = new BigInteger(128, secureRandom).toString(32);
        
        this.append("title", null);
        this.append("body", null);
        this.append("is_done", false);
        this.append("subscribers", new ArrayList<String>());
        this.append("modification_token", modificationToken);
        
    }
    
}
