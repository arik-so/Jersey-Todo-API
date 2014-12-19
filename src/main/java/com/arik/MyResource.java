package com.arik;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Root resource (exposed at "/" path)
 */
@Path("/")
public class MyResource {

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Path("myresource")
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        return "Hello, Heroku!";
    }

    @GET
    @Path("arikresource")
    @Produces("text/html")
    public String helloWorld(){
        return "Arik test";
    }

    @GET
    @Path("/users/{username}")
    public String getUser(@PathParam("username") String username, @QueryParam("search") String search){
        return "the user it got: "+username+", but searched for: "+search;
    }

}
