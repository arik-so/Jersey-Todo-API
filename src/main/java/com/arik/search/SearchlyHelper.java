package com.arik.search;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

/**
 * Created by arik-so on 12/20/14.
 */
public class SearchlyHelper {

    private static JestClient jestClient;

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
