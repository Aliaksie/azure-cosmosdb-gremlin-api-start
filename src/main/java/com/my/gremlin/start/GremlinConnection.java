package com.my.gremlin.start;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.exception.ResponseException;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.translator.GroovyTranslator;

public class GremlinConnection {

    public static List<Result> executeQuery(final Client client, Traversal<?, ?> t) {
        try {
            String query = GroovyTranslator.of("g").translate(t.asAdmin().getBytecode());
            var results = client.submit(query);
            // todo: error handlers, throw not_found
            handleResponseHeaders(results.statusAttributes().get());

            return results.all().get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ResponseException) {
                var responseException = (ResponseException) e.getCause();
                // todo: error handlers, throw not_found
            }
            throw new RuntimeException("Error during work with CosmosDB");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during work with CosmosDB");
        }
    }

    private static void handleResponseHeaders(Map<String, Object> headers) {
        var code = (Integer) headers.get(GremlinConstant.X_MS_CODE);
        if (code == 200 || code == 404) {
            return;
        }

        throw new RuntimeException("Error during work with CosmosDB");
    }
}
