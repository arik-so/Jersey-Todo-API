package com.arik.search;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

/**
 * A class used as an interface for communication with Searchly using Jest
 */
public class SearchlyConnector {

    private static JestClient jestClient;

    /**
     * Get a static singleton client to communicate with Searchly
     *
     * @return An instance of JestClient to use for communication with Searchly
     */
    public static synchronized JestClient getJestClient() {

        if (jestClient != null) {
            return jestClient;
        }

        String connectionURL = System.getenv("SEARCHBOX_URL");

        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig.Builder(connectionURL).multiThreaded(true).build());

        jestClient = factory.getObject();
        return jestClient;

    }

}
