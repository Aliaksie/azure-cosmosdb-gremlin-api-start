package com.my.gremlin.start.model;

import java.util.Map;

/**
 * Represents the graph edge.
 */
public class Edge {

    private String id;

    private String label;

    private String type;

    private String inVLabel;

    private String outVLabel;

    private String inV;

    private String outV;

    private Map<String, String> properties;

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

    public String getInVLabel() {
        return inVLabel;
    }

    public void setInVLabel(String inVLabel) {
        this.inVLabel = inVLabel;
    }

    public String getOutVLabel() {
        return outVLabel;
    }

    public void setOutVLabel(String outVLabel) {
        this.outVLabel = outVLabel;
    }

    public String getInV() {
        return inV;
    }

    public void setInV(String inV) {
        this.inV = inV;
    }

    public String getOutV() {
        return outV;
    }

    public void setOutV(String outV) {
        this.outV = outV;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
}
