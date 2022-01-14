package com.my.gremlin.start;

/**
 * Constant for Gremlin API.
 */
public class GremlinConstant {

    // Gremlin server response headers
    public static final String X_MS_CODE = "x-ms-status-code";

    // common
    // property
    public static final String PK = "pk";
    public static final String TYPE = "type";
    // type value
    public static final String PRIMARY = "primary";
    public static final String SECONDARY = "secondary";
    public static final String COACH = "coach";
    public static final String ADVISOR = "advisor";

    // vertex
    // property
    public static final String NODE_ID = "nodeId"; // ?
    public static final String MEMBER_ID = "memberId";
    public static final String COUNT_ID = "count";
    public static final String CONTENT_REF = "content_ref";
    public static final String AUTHOR = "author";
    public static final String AUTHORS = "authors";
    // type value
    public static final String MANUAL = "manual";
    public static final String SYSTEM = "system";
    public static final String ROOT_HEALTH = "root_health";
    public static final String ROOT_CARE = "root_care";

    // edge property
    public static final String RELATIONSHIP = "relationship";
    // type value
    public static final String PRIVATE = "private";
    public static final String PUBLIC  = "public";

    // vertex labels
    public static final String MEMBER_LB = "member";
    public static final String USER_LB = "user";
    public static final String GROUP_LB = "group";
    public static final String POST_LB = "post";
    public static final String COMMENT_LB = "comment";
    public static final String LIKE_LB = "like";

    // edge labels
    public static final String OWNER_LB = "owner";
    public static final String BELONGS_EDGE_LB = "belongs";
    public static final String CARE_LB = "care";
    public static final String HEALTH_LB = "health";

    // for select
    public static final String VERTEX = "v";
    public static final String EDGE = "e";

    private GremlinConstant() {
    }
}
