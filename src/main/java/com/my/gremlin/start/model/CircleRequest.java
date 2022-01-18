package com.my.gremlin.start.model;

import java.util.Arrays;
import java.util.List;

public class CircleRequest {

    private String memberId;

    private String pk;

    private String linkedUserId;

    private List<CircleType> labels; // could be null, only [HEALTH, CARE]

    private CircleType type; // could be null

    private String relationship;

    public CircleType getType() {
        return type;
    }

    public void setType(CircleType type) {
        this.type = type;
    }

    public List<CircleType> getLabels() {
        return labels;
    }

    public void setLabels(List<CircleType> labels) {
        this.labels = labels;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getLinkedUserId() {
        return linkedUserId;
    }

    public void setLinkedUserId(String linkedUserId) {
        this.linkedUserId = linkedUserId;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPk() {
        return pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    public enum CircleType {
        HEALTH, CARE, ADVISOR, COACH, SECONDARY, PRIMARY
    }
}
