package com.my.gremlin.start.model;

import com.my.gremlin.start.GremlinConstant;

import java.util.List;
import java.util.Map;

/**
 * Represents the graph vertex.
 */
public class Vertex {

    private String id;

    private String label;

    private String type;

    private Map<String, List<Map<String, String>>> properties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, List<Map<String, String>>> getProperties() {
        return properties;
    }

    public void setProperties(
            Map<String, List<Map<String, String>>> properties) {
        this.properties = properties;
    }

    public String getNodeId() {
        return properties.get(GremlinConstant.NODE_ID).get(0).get("value");
    }
}
