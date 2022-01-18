package com.my.gremlin.start;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Property;

public class Main {
    private static Client client;
    private static GraphTraversalSource g;
    private static Cluster cluster;

    private static void initClient() {
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
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        initClient();

        //GremlinConnection.executeQuery(client, g.V().has(GremlinConstant.TYPE,"XYZ1").fold().coalesce(__.<org.apache.tinkerpop.gremlin.structure.Vertex>unfold(), __.addV().property("code","XYZ1").property("type","XYZ1")));

        var objectMapper = new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            // init def groups for ACL goal and etc.
            initGroup();

            var membersRq = prepareListOfMembers();

            membersRq.forEach(Main::addMember);
            //create(client, g, rq);

            var circles = findAllCareCircle("member_id_02");
            List<Map<Object, Object>> convertCircles = circles.stream()
                    .map(o -> getMap(objectMapper, (Map<String, List<Map<String, String>>>) o.getObject()))
                    .collect(Collectors.toList());
            System.out.println(convertCircles);

            List<Result> caregivers = circleDetails("member_id_01");
            List<HashMap<Object, Object>> convertCaregivers = caregivers.stream()
                    .map(o -> getMap(objectMapper, (Map<String, List<Map<String, String>>>) o.getObject()))
                    .collect(Collectors.toList());
            System.out.println(convertCaregivers);
        } catch (Exception e) {
            // Properly close all opened clients and the cluster
            System.err.println(e);
            cluster.close();
            System.exit(1);
        }

        cluster.close();
        System.exit(0);
    }

    /**
     * 1.1) Patient: member_id_01; Type = [CARE, HEALTH]
     * 1.1) Patient: member_id_02; Type = [CARE];
     * <p>
     * 2.1) Coach: id = coach_id_01 ; linkedUserId = member_id_01, labels = [HEALTH], type = Coach
     * <p>
     * 3.1) Care: id = care_id_01  ; linkedUserId = member_id_01, type = [CARE], type = Primary
     * 3.2) Care: id = care_id_02  ; linkedUserId = member_id_01, type = [CARE], type = Secondary
     * 3.3) Care: id = care_id_03  ; linkedUserId =, type = , type =
     * 3.4) Patient: member_id_02;  linkedUserId = member_id_01, Type = [CARE];
     * 3.5) Care: id = care_id_04  ; linkedUserId = member_id_02, type = [CARE], type = Primary
     * 3.6) Care: id = care_id_05  ; linkedUserId = member_id_02, type = [CARE], type = Secondary
     */
    private static List<CircleRequest> prepareListOfMembers() {
        // 1.1
        var rq1 = new CircleRequest();
        rq1.setMemberId("member_id_01");
        rq1.setLinkedUserId("member_id_01");
        rq1.setLabels(List.of(CircleRequest.CircleType.CARE, CircleRequest.CircleType.HEALTH));
        rq1.setPk("default");

        // 1.2
        var rq12 = new CircleRequest();
        rq12.setMemberId("member_id_02");
        rq12.setLinkedUserId("member_id_02");
        rq12.setLabels(List.of(CircleRequest.CircleType.CARE));
        rq12.setPk("default");

        // 2.1
        var rq2 = new CircleRequest();
        rq2.setMemberId("coach_id_01");
        rq2.setLinkedUserId("member_id_01");
        rq2.setLabels(List.of(CircleRequest.CircleType.HEALTH));
        rq2.setType(CircleRequest.CircleType.COACH);
        rq2.setPk("default");

        // 3.1
        var rq3 = new CircleRequest();
        rq3.setLinkedUserId("member_id_01");
        rq3.setMemberId("care_id_01");
        rq3.setLabels(List.of(CircleRequest.CircleType.CARE));
        rq3.setType(CircleRequest.CircleType.PRIMARY);
        rq3.setRelationship("relative");
        rq3.setPk("default");

        // 3.2
        var rq4 = new CircleRequest();
        rq4.setMemberId("care_id_02");
        rq4.setLinkedUserId("member_id_01");
        rq4.setLabels(List.of(CircleRequest.CircleType.CARE));
        rq4.setType(CircleRequest.CircleType.SECONDARY);
        rq4.setRelationship("system");
        rq4.setPk("default");

        // 3.3
        var rq5 = new CircleRequest();
        rq5.setMemberId("care_id_03");
        rq5.setPk("default");

        // 3.4
        var rq6 = new CircleRequest();
        rq6.setMemberId("member_id_02");
        rq6.setLinkedUserId("member_id_01");
        rq6.setLabels(List.of(CircleRequest.CircleType.CARE));
        rq6.setType(CircleRequest.CircleType.PRIMARY);
        rq6.setRelationship("friend");
        rq6.setPk("default");

        // 3.1
        var rq7 = new CircleRequest();
        rq7.setMemberId("care_id_04");
        rq7.setLinkedUserId("member_id_02");
        rq7.setLabels(List.of(CircleRequest.CircleType.CARE));
        rq7.setType(CircleRequest.CircleType.PRIMARY);
        rq7.setRelationship("relative");
        rq7.setPk("default");

        // 3.2
        var rq8 = new CircleRequest();
        rq8.setMemberId("care_id_05");
        rq8.setLinkedUserId("member_id_02");
        rq8.setLabels(List.of(CircleRequest.CircleType.CARE));
        rq8.setType(CircleRequest.CircleType.SECONDARY);
        rq8.setRelationship("system");
        rq8.setPk("default");

        return List.of(rq1, rq12, rq2, rq3, rq4, rq5, rq6, rq7, rq8);
    }

    private static HashMap<Object, Object> getMap(ObjectMapper objectMapper,
                                                  Map<String, List<Map<String, String>>> o) {
        var map = new HashMap<>();
        map.put(objectMapper.convertValue(o.get(GremlinConstant.VERTEX), Vertex.class), objectMapper
                .convertValue(o.get(GremlinConstant.EDGE), Edge.class));
        return map;
    }

    private static List<Result> findAllCareCircle(String nodeId) {
        var t = g.V().has(GremlinConstant.MEMBER_ID, nodeId)
                .hasLabel(GremlinConstant.MEMBER_LB)
                .outE(GremlinConstant.CARE_LB).as(GremlinConstant.EDGE)
                .inV().as(GremlinConstant.VERTEX)
                .select(GremlinConstant.EDGE, GremlinConstant.VERTEX);

        return GremlinConnection.executeQuery(client, t);
    }

    private static List<Result> circleDetails(String nodeId) {
        var t = g.V().has(GremlinConstant.MEMBER_ID, nodeId)
                .hasLabel(GremlinConstant.MEMBER_LB)
                .outE(GremlinConstant.OWNER_LB).as(GremlinConstant.EDGE)
                .inV().as(GremlinConstant.VERTEX)
                .select(GremlinConstant.EDGE, GremlinConstant.VERTEX);

        return GremlinConnection.executeQuery(client, t);
    }

    @SuppressWarnings("unchecked")
    public static void initGroup() {
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

        Traversal<?, ?> traversal = g.V().has(GremlinConstant.TYPE, GremlinConstant.ROOT_HEALTH).as(GremlinConstant.ROOT_HEALTH)
                .map(__.V().has(GremlinConstant.TYPE, GremlinConstant.ADVISOR)).as(GremlinConstant.ADVISOR)
                .map(__.V().has(GremlinConstant.TYPE, GremlinConstant.COACH)).as(GremlinConstant.COACH)
                .addE(GremlinConstant.PARENT_LB).from(GremlinConstant.ADVISOR).to(GremlinConstant.ROOT_HEALTH)
                .addE(GremlinConstant.PARENT_LB).from(GremlinConstant.COACH).to(GremlinConstant.ROOT_HEALTH)
                .map(__.V().has(GremlinConstant.TYPE, GremlinConstant.ROOT_CARE)).as(GremlinConstant.ROOT_CARE)
                .map(__.V().has(GremlinConstant.TYPE, GremlinConstant.PRIMARY)).as(GremlinConstant.PRIMARY)
                .map(__.V().has(GremlinConstant.TYPE, GremlinConstant.SECONDARY)).as(GremlinConstant.SECONDARY)
                .addE(GremlinConstant.PARENT_LB).from(GremlinConstant.PRIMARY).to(GremlinConstant.ROOT_CARE)
                .addE(GremlinConstant.PARENT_LB).from(GremlinConstant.SECONDARY).to(GremlinConstant.ROOT_CARE);

        GremlinConnection.executeQuery(client, traversal);
    }

    private static String getGroupType(GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Vertex> traversal) {
        return Optional.ofNullable(traversal)
                .<GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, ? extends Property<String>>>map(it -> it.properties(GremlinConstant.TYPE))
                .flatMap(Traversal::tryNext)
                .map(Property::value)
                .orElseThrow(IllegalArgumentException::new);
    }


    /*
     * todo: nodeId?
     *  g.V().has(GremlinConstant.NODE_ID, rq.getMemberId())
                            .outE(GremlinConstant.OWNER_LB).as(GremlinConstant.EDGE)
                            .inV().as(GremlinConstant.VERTEX)
                            .select(GremlinConstant.VERTEX)
     */
    @SuppressWarnings("unchecked")
    public static void addMember(CircleRequest rq) {
        var tMember = g.V().has(GremlinConstant.MEMBER_ID, rq.getMemberId()).hasLabel(GremlinConstant.MEMBER_LB).fold()
                .coalesce(__.unfold(), __.addV(GremlinConstant.MEMBER_LB)
                        .property(GremlinConstant.NODE_ID, UUID.randomUUID().toString()) // ?
                        .property(GremlinConstant.PK, rq.getPk())
                        .property(GremlinConstant.MEMBER_ID, rq.getMemberId())); // ?

        var rs = GremlinConnection.executeQuery(client, tMember);

        // memberId = linkedUserId (OWNER) and type == null;  ... more like care without THAN nothing more
        if (rq.getLabels() == null) {
            return;
        }

        // memberId = linkedUserId (OWNER) and type != null;  ... more like recipient THAN add and create group base of type[]
        if (rq.getMemberId().equals(rq.getLinkedUserId())) {
            rq.getLabels().forEach(o -> {
                var tGroup = g.V().has(GremlinConstant.MEMBER_ID, rq.getMemberId())
                        .has(GremlinConstant.TYPE, o.toString().toLowerCase())
                        .hasLabel(GremlinConstant.GROUP_LB)
                        .fold()
                        .coalesce(__.unfold(), __.addV(GremlinConstant.GROUP_LB)
                                .property(GremlinConstant.NODE_ID, UUID.randomUUID().toString())
                                .property(GremlinConstant.MEMBER_ID, rq.getMemberId()) // ?
                                .property(GremlinConstant.PK, rq.getPk())
                                .property(GremlinConstant.TYPE, o.toString().toLowerCase()));
                GremlinConnection.executeQuery(client, tGroup);

                var tAddEdge = g.V().has(GremlinConstant.MEMBER_ID, rq.getMemberId())
                        .hasLabel(GremlinConstant.MEMBER_LB)
                        .addE(GremlinConstant.OWNER_LB)
                        .property(GremlinConstant.PK, rq.getPk())
                        .to(tGroup);
                GremlinConnection.executeQuery(client, tAddEdge);

                // todo: !
                if (o == CircleRequest.CircleType.CARE) {
                    addCareGroupDf(tGroup);
                }
                if (o == CircleRequest.CircleType.HEALTH) {
                    addHealthGroupDf(tGroup);
                }
            });
        } else {
            // memberId != linkedUserId and type == CARE; ... more like care with recipient THAN add care to recipient group
            if (rq.getLabels().get(0) == CircleRequest.CircleType.CARE) {
                var tCareGroup = g.V().has(GremlinConstant.MEMBER_ID, rq.getLinkedUserId())
                        .has(GremlinConstant.TYPE, GremlinConstant.CARE_LB).hasLabel(GremlinConstant.GROUP_LB);
                rs = GremlinConnection.executeQuery(client, tCareGroup);
                if (rs.isEmpty()) {
                    System.out.println("WARN:Recipient doesn't has care group");
                    return;
                }

                var tAddEdge = tMember
                        .addE(GremlinConstant.CARE_LB)
                        .property(GremlinConstant.TYPE, rq.getType().toString())
                        .property(GremlinConstant.PK, rq.getPk())
                        .property(GremlinConstant.RELATIONSHIP, rq.getRelationship())
                        .to(tCareGroup);
                GremlinConnection.executeQuery(client, tAddEdge);
            }

            // memberId != linkedUserId and type == HEALTH; ... more like health with recipient THAN health care to recipient group
            if (rq.getLabels().get(0) == CircleRequest.CircleType.HEALTH) {
                var tHealthGroup = g.V().has(GremlinConstant.MEMBER_ID, rq.getLinkedUserId())
                        .has(GremlinConstant.TYPE, GremlinConstant.HEALTH_LB).hasLabel(GremlinConstant.GROUP_LB);
                rs = GremlinConnection.executeQuery(client, tHealthGroup);
                if (rs.isEmpty()) {
                    System.out.println("WARN:Recipient doesn't has health group");
                    return;
                }

                var tAddEdge = tMember
                        .addE(GremlinConstant.HEALTH_LB)
                        .property(GremlinConstant.TYPE, rq.getType().toString())
                        .property(GremlinConstant.PK, rq.getPk())
                        .to(tHealthGroup);
                GremlinConnection.executeQuery(client, tAddEdge);
            }
        }
    }

    private static void addCareGroupDf(GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Vertex> tGroup) {
        var tAddPrEdge = tGroup.as("g")
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.PRIMARY).hasLabel(GremlinConstant.GROUP_LB))
                .select("g")
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.SECONDARY).hasLabel(GremlinConstant.GROUP_LB));

        GremlinConnection.executeQuery(client, tAddPrEdge);
    }

    private static void addHealthGroupDf(GraphTraversal<org.apache.tinkerpop.gremlin.structure.Vertex, org.apache.tinkerpop.gremlin.structure.Vertex> tGroup) {
        var tAddAdEdge = tGroup.as("g")
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.ADVISOR).hasLabel(GremlinConstant.GROUP_LB))
                .select("g")
                .addE(GremlinConstant.PARENT_LB)
                .property(GremlinConstant.PK, "default")
                .to(g.V().has(GremlinConstant.TYPE, GremlinConstant.COACH).hasLabel(GremlinConstant.GROUP_LB));
        GremlinConnection.executeQuery(client, tAddAdEdge);
    }

    // legacy debt
    private static void create(CircleRequest rq) {
        var tCircle = g.V().has(GremlinConstant.MEMBER_ID, rq.getMemberId()).hasLabel(GremlinConstant.GROUP_LB);
        var circle = GremlinConnection.executeQuery(client, tCircle);
        if (circle.isEmpty()) {
            var tCreation = g.addV(GremlinConstant.GROUP_LB)
                    .property(GremlinConstant.MEMBER_ID, rq.getMemberId())
                    .property(GremlinConstant.PK, rq.getPk());
            var results = GremlinConnection.executeQuery(client, tCreation);
            System.out.println("Circle vertex created: " + results);
        }

        if (rq.getPk() == null) {
            var tCaregiver = g.V().has(GremlinConstant.MEMBER_ID, rq.getLinkedUserId()).hasLabel(GremlinConstant.MEMBER_LB);
            var caregiver = GremlinConnection.executeQuery(client, tCaregiver);
            if (caregiver.isEmpty()) {
                var tCreation = g.addV(GremlinConstant.MEMBER_LB)
                        .property(GremlinConstant.MEMBER_ID, rq.getLinkedUserId())
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

    private void initGroups() {
        /**g.addV(GremlinConstant).property("code","YVR").as("yvr").
         addV("airport").property("code","YYC").as("yyc").
         addV("airport").property("code","SEA").as("sea").
         addV("airport").property("code","SFO").as("sfo").
         addV("airport").property("code","LAX").as("lax").
         addE("route").from("yvr").to("yyc").
         addE("route").from("yyc").to("yvr").
         addE("route").from("yvr").to("lax").
         addE("route").from("lax").to("yyc").
         addE("route").from("sfo").to("lax").
         addE("route").from("lax").to("sfo").
         addE("route").from("yyc").to("sfo").
         addE("route").from("yyc").to("sea").
         addE("route").from("sea").to("sfo").
         addE("route").from("sfo").to("sea").
         addE("route").from("sea").to("yvr").
         addE("route").from("yvr").to("sea").
         addE("route").from("sea").to("yyc") **/
    }
}
