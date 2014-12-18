package com.arik;

import com.arik.twilio.TwilioConnector;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.core.Application;

import static org.junit.Assert.*;

/**
 * Created by arik-so on 12/18/14.
 */
public class TwilioTest extends JerseyTest{

    @Override
    protected Application configure() {
        return new ResourceConfig(); // in this instance, we are not using any particular resource
    }

    @Test
    public void testTwilioAuthToken(){

        assertNotNull(TwilioConnector.ACCOUNT_AUTH_TOKEN, "Making sure TWILIO_AUTH_TOKEN config variable is set.");

    }

}
