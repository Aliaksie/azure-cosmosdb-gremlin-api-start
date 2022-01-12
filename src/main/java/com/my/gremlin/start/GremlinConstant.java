package com.my.gremlin.start;

/**
 * Constant for Gremlin API.
 */
public class GremlinConstant {

    // Gremlin server response headers
    public static final String X_MS_CODE = "x-ms-status-code";

    // vertex labels
    public static final String CARE_LB = "giver";
    public static final String CIRCLE_LB = "circle";

    // vertex property
    public static final String NODE_ID = "nodeId";

    // edge labels
    public static final String BELONGS_EDGE_LB = "belongs";

    // edge property
    public static final String OWNER = "owner";
    public static final String MEMBER = "member";
    public static final String ROLE = "role";
    public static final String RELATIONSHIP = "relationship";

    // for select
    public static final String VERTEX = "v";
    public static final String EDGE = "e";

    private GremlinConstant() {
    }
}
