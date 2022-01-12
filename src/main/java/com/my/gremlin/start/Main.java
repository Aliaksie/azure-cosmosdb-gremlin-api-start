package com.my.gremlin.start;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.my.gremlin.start.model.CircleRequest;
import com.my.gremlin.start.model.Edge;
import com.my.gremlin.start.model.Vertex;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.Result;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        /**
         * There typically needs to be only one Cluster instance in an application.
         */
        Cluster cluster;

        /**
         * Use the Cluster instance to construct different Client instances (e.g. one for sessionless communication
         * and one or more sessions). A sessionless Client should be thread-safe and typically no more than one is
         * needed unless there is some need to divide connection pools across multiple Client instances. In this case
         * there is just a single sessionless Client instance used for the entire App.
         */
        Client client;
        GraphTraversalSource g;
        try {
            // Attempt to create the connection objects
            cluster = Cluster.build(new File("src/main/resources/remote.yaml")).create();
            g = AnonymousTraversalSource.traversal()
                    .withRemote(DriverRemoteConnection.using(cluster));
            client = cluster.connect();
        } catch (FileNotFoundException e) {
            // Handle file errors.
            System.out.println("Couldn't find the configuration file.");
            e.printStackTrace();
            return;
        }

        var objectMapper = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var rq = new CircleRequest();
        rq.setUserId("userId_02");
        rq.setProfileId("profileId_01");
        rq.setRelationship("pet");
        //        rq.role("member");
        create(client, g, rq);

        var circles = findAllCircle(client, g, "userId_02");
        List<Map<Object, Object>> convertCircles = circles.stream()
                .map(o -> getMap(objectMapper, (Map<String, List<Map<String, String>>>) o.getObject()))
                .collect(Collectors.toList());
        System.out.println(convertCircles);

        List<Result> caregivers = circleDetails(client, g, "userId");
        List<HashMap<Object, Object>> convertCaregivers = caregivers.stream()
                .map(o -> getMap(objectMapper, (Map<String, List<Map<String, String>>>) o.getObject()))
                .collect(Collectors.toList());
        System.out.println(convertCaregivers);

        // Properly close all opened clients and the cluster
        cluster.close();
        System.exit(0);
    }

    private static HashMap<Object, Object> getMap(ObjectMapper objectMapper,
            Map<String, List<Map<String, String>>> o) {
        var map = new HashMap<>();
        map.put(objectMapper.convertValue(o.get(GremlinConstant.VERTEX), Vertex.class), objectMapper
                .convertValue(o.get(GremlinConstant.EDGE), Edge.class));
        return map;
    }

    private static List<Result> findAllCircle(Client client, GraphTraversalSource g, String nodeId) {
        var t = g.V().has(GremlinConstant.NODE_ID, nodeId)
                .hasLabel(GremlinConstant.CARE_LB)
                .outE(GremlinConstant.BELONGS_EDGE_LB).as(GremlinConstant.EDGE)
                .inV().as(GremlinConstant.VERTEX)
                .select(GremlinConstant.EDGE, GremlinConstant.VERTEX);

        return GremlinConnection.executeQuery(client, t);
    }

    private static List<Result> circleDetails(Client client, GraphTraversalSource g, String nodeId) {
        var t = g.V().has(GremlinConstant.NODE_ID, nodeId)
                .hasLabel(GremlinConstant.CIRCLE_LB)
                .inE(GremlinConstant.BELONGS_EDGE_LB).as(GremlinConstant.EDGE)
                .bothV().as(GremlinConstant.VERTEX)
                .select(GremlinConstant.EDGE, GremlinConstant.VERTEX);

        return GremlinConnection.executeQuery(client, t);
    }

    private static void create(Client client, GraphTraversalSource g, CircleRequest rq) {

        var tCircle = g.V().has(GremlinConstant.NODE_ID, rq.getProfileId()).hasLabel(GremlinConstant.CIRCLE_LB);
        var circle = GremlinConnection.executeQuery(client, tCircle);
        if (circle.isEmpty()) {
            var tCreation = g.addV(GremlinConstant.CIRCLE_LB).property(GremlinConstant.NODE_ID, rq.getProfileId());
            var results = GremlinConnection.executeQuery(client, tCreation);
            System.out.println("Circle vertex created: " + results);
        }

        if (rq.getRole() == null) {
            var tCaregiver = g.V().has(GremlinConstant.NODE_ID, rq.getUserId()).hasLabel(GremlinConstant.CARE_LB);
            var caregiver = GremlinConnection.executeQuery(client, tCaregiver);
            if (caregiver.isEmpty()) {
                var tCreation = g.addV(GremlinConstant.CARE_LB).property(GremlinConstant.NODE_ID, rq.getUserId());
                var list = GremlinConnection.executeQuery(client, tCreation);
                System.out.println("Care vertex created: " + list);
            }

            var tAddEdge = tCaregiver
                    .addE(GremlinConstant.BELONGS_EDGE_LB)
                    .property(GremlinConstant.ROLE, GremlinConstant.OWNER)
                    .property(GremlinConstant.RELATIONSHIP, rq.getRelationship())
                    .to(tCircle);
            var resultList = GremlinConnection.executeQuery(client, tAddEdge);
            System.out.println("Care vertex added to Circle: " + resultList);
        }
    }
}
