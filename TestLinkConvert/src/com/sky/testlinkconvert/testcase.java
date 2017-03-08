package com.sky.testlinkconvert;

/**
 * Created by Administrator on 2017-03-08.
 */
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class testcase {
    public int getInternalid() {
        return internalid;
    }
    @XmlAttribute
    public void setInternalid(int internalid) {
        this.internalid = internalid;
    }

    public String getName() {
        return name;
    }
    @XmlAttribute
    public void setName(String name) {
        this.name = name;
    }

    public String getNode_order() {
        return node_order;
    }
    @XmlElement
    public void setNode_order(String node_order) {
        this.node_order = node_order;
    }

    public String getExternalid() {
        return externalid;
    }
    @XmlElement
    public void setExternalid(String externalid) {
        this.externalid = externalid;
    }

    public String getSummary() {
        return summary;
    }
    @XmlElement
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSteps() {
        return steps;
    }
    @XmlElement
    public void setSteps(String steps) {
        this.steps = steps;
    }

    public String getExpectedresults() {
        return expectedresults;
    }
    @XmlElement
    public void setExpectedresults(String expectedresults) {
        this.expectedresults = expectedresults;
    }

    int internalid;
    String name;
    String node_order;
    String externalid;
    String summary;
    String steps;
    String expectedresults;
}
