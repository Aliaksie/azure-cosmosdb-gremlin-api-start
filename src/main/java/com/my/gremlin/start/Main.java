package com.my.gremlin.start;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
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
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

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


        // init def groups for ACL goal and etc.
        initGroup(client, g);

        var rq = new CircleRequest();
        rq.setLinkedUserId("userId_02");
        rq.setMemberId("profileId_01");
        rq.setRelationship("pet");
        rq.setPk("default");
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
                .hasLabel(GremlinConstant.MEMBER_LB)
                .outE(GremlinConstant.BELONGS_EDGE_LB).as(GremlinConstant.EDGE)
                .inV().as(GremlinConstant.VERTEX)
                .select(GremlinConstant.EDGE, GremlinConstant.VERTEX);

        return GremlinConnection.executeQuery(client, t);
    }

    private static List<Result> circleDetails(Client client, GraphTraversalSource g, String nodeId) {
        var t = g.V().has(GremlinConstant.NODE_ID, nodeId)
                .hasLabel(GremlinConstant.GROUP_LB)
                .inE(GremlinConstant.BELONGS_EDGE_LB).as(GremlinConstant.EDGE)
                .bothV().as(GremlinConstant.VERTEX)
                .select(GremlinConstant.EDGE, GremlinConstant.VERTEX);

        return GremlinConnection.executeQuery(client, t);
    }

    @SuppressWarnings("unchecked")
    public static void initGroup(Client client, GraphTraversalSource g) {
        var groups = List.of(GremlinConstant.ROOT_HEALTH, GremlinConstant.ROOT_CARE,
                GremlinConstant.ADVISOR, GremlinConstant.COACH, GremlinConstant.PRIMARY, GremlinConstant.SECONDARY);
        // todo: improve ?
        groups.forEach(o -> {
            GremlinConnection.executeQuery(client, g.V().has(GremlinConstant.TYPE, o).hasLabel(GremlinConstant.GROUP_LB)
                    .fold()
                    .coalesce(__.unfold(), __.addV(GremlinConstant.GROUP_LB)
                            .property(GremlinConstant.NODE_ID, UUID.randomUUID().toString())
                            .property(GremlinConstant.PK, "default")
                            .property(GremlinConstant.TYPE, o)));
        });
    }


    /*
     * todo: nodeId?
     *  g.V().has(GremlinConstant.NODE_ID, rq.getMemberId())
                            .outE(GremlinConstant.OWNER_LB).as(GremlinConstant.EDGE)
                            .inV().as(GremlinConstant.VERTEX)
                            .select(GremlinConstant.VERTEX)
     */
    @SuppressWarnings("unchecked")
    public static void addMember(Client client, GraphTraversalSource g, CircleRequest rq) {
        var tMember = g.V().has(GremlinConstant.NODE_ID, rq.getMemberId()).hasLabel(GremlinConstant.MEMBER_LB).fold()
                .coalesce(__.unfold(), __.addV(GremlinConstant.MEMBER_LB)
                        .property(GremlinConstant.NODE_ID, rq.getMemberId()) // ?
                        .property(GremlinConstant.PK, rq.getPk())
                        .property(GremlinConstant.MEMBER_ID, rq.getMemberId())); // ?

        var rs = GremlinConnection.executeQuery(client, tMember);

        // memberId = linkedUserId (OWNER) and type == null;  ... more like care without THAN nothing more
        if (rq.getType() != null) {
            // memberId = linkedUserId (OWNER) and type != null;  ... more like recipient THAN add and create group base of type[]
            if (rq.getMemberId().equals(rq.getLinkedUserId())) {
                Arrays.stream(rq.getType()).forEach(o -> {
                    var tGroup = g.V().has(GremlinConstant.NODE_ID, rq.getMemberId())
                            .has(GremlinConstant.TYPE, o.toString().toLowerCase())
                            .hasLabel(GremlinConstant.GROUP_LB)
                            .fold()
                            .coalesce(__.unfold(), __.addV(GremlinConstant.GROUP_LB)
                                    .property(GremlinConstant.NODE_ID, rq.getMemberId()) // ?
                                    .property(GremlinConstant.PK, rq.getPk())
                                    .property(GremlinConstant.TYPE, o.toString().toLowerCase()));
                    GremlinConnection.executeQuery(client, tGroup);
                    var tAddEdge = tMember
                            .addE(GremlinConstant.OWNER_LB)
                            .property(GremlinConstant.PK, rq.getPk())
                            .to(tGroup);
                    GremlinConnection.executeQuery(client, tAddEdge);

                    // todo: !
                    if (o == CircleRequest.CircleType.CARE) {
                        addCareGroupDf(client, g, tGroup);
                    }
                    if (o == CircleRequest.CircleType.HEALTH) {
                        addHealthGroupDf(client, g, tGroup);
                    }
                });
            } else {
                // memberId != linkedUserId and type == CARE; ... more like care with recipient THAN add care to recipient group
                if (rq.getType()[0] == CircleRequest.CircleType.CARE) {
                    var tCareGroup = g.V().has(GremlinConstant.NODE_ID, rq.getLinkedUserId())
                            .has(GremlinConstant.TYPE, GremlinConstant.CARE_LB).hasLabel(GremlinConstant.GROUP_LB);
                    rs = GremlinConnection.executeQuery(client, tCareGroup);
                    if(rs.isEmpty()) {
                        System.out.println("WARN:Recipient doesn't has care group");
                    }

                    var tAddEdge = tMember
                            .addE(GremlinConstant.CARE_LB)
                            .property(GremlinConstant.PK, rq.getPk())
                            .property(GremlinConstant.RELATIONSHIP, rq.getRelationship())
                            .to(tCareGroup);
                    GremlinConnection.executeQuery(client, tAddEdge);
                }

                // memberId != linkedUserId and type == HEALTH; ... more like health with recipient THAN health care to recipient group
                if (rq.getType()[0] == CircleRequest.CircleType.HEALTH) {
                    var tHealthGroup = g.V().has(GremlinConstant.NODE_ID, rq.getLinkedUserId())
                            .has(GremlinConstant.TYPE, GremlinConstant.HEALTH_LB).hasLabel(GremlinConstant.GROUP_LB);
                    rs = GremlinConnection.executeQuery(client, tHealthGroup);
                    if(rs.isEmpty()) {
                        System.out.println("WARN:Recipient doesn't has health group");
                    }

                    var tAddEdge = tMember
                            .addE(GremlinConstant.HEALTH_LB)
                            .property(GremlinConstant.PK, rq.getPk())
                            .to(tHealthGroup);
                    GremlinConnection.executeQuery(client, tAddEdge);
                }
            }
        }
    }

    private static void addCareGroupDf(Client client, GraphTraversalSource g, GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex,org.apache.tinkerpop.gremlin.structure.Vertex> tGroup) {
        var tAddPrEdge = tGroup
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.PRIMARY).hasLabel(GremlinConstant.GROUP_LB));
        GremlinConnection.executeQuery(client, tAddPrEdge);

        var tAddScEdge = tGroup
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.SECONDARY).hasLabel(GremlinConstant.GROUP_LB));
        GremlinConnection.executeQuery(client, tAddPrEdge);
    }

    private static void addHealthGroupDf(Client client, GraphTraversalSource g, GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex,org.apache.tinkerpop.gremlin.structure.Vertex> tGroup) {
        var tAddPrEdge = tGroup
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.ADVISOR).hasLabel(GremlinConstant.GROUP_LB));
        GremlinConnection.executeQuery(client, tAddPrEdge);

        var tAddScEdge = tGroup
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.COACH).hasLabel(GremlinConstant.GROUP_LB));
        GremlinConnection.executeQuery(client, tAddPrEdge);
    }

    private static void create(Client client, GraphTraversalSource g, CircleRequest rq) {

        var tCircle = g.V().has(GremlinConstant.NODE_ID, rq.getMemberId()).hasLabel(GremlinConstant.GROUP_LB);
        var circle = GremlinConnection.executeQuery(client, tCircle);
        if (circle.isEmpty()) {
            var tCreation = g.addV(GremlinConstant.GROUP_LB)
                    .property(GremlinConstant.NODE_ID, rq.getMemberId())
                    .property(GremlinConstant.PK, rq.getPk());
            var results = GremlinConnection.executeQuery(client, tCreation);
            System.out.println("Circle vertex created: " + results);
        }

        if (rq.getPk() == null) {
            var tCaregiver = g.V().has(GremlinConstant.NODE_ID, rq.getLinkedUserId()).hasLabel(GremlinConstant.MEMBER_LB);
            var caregiver = GremlinConnection.executeQuery(client, tCaregiver);
            if (caregiver.isEmpty()) {
                var tCreation = g.addV(GremlinConstant.MEMBER_LB)
                        .property(GremlinConstant.NODE_ID, rq.getLinkedUserId())
                        .property(GremlinConstant.PK, rq.getPk());
                var list = GremlinConnection.executeQuery(client, tCreation);
                System.out.println("Care vertex created: " + list);
            }

            var tAddEdge = tCaregiver
                    .addE(GremlinConstant.BELONGS_EDGE_LB)
                    .property(GremlinConstant.RELATIONSHIP, rq.getRelationship())
                    .property(GremlinConstant.PK, rq.getPk())
                    .to(tCircle);
            var resultList = GremlinConnection.executeQuery(client, tAddEdge);
            System.out.println("Care vertex added to Circle: " + resultList);
        }
    }
}
