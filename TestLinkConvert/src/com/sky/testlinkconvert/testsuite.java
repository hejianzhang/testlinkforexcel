package com.sky.testlinkconvert;

/**
 * Created by Administrator on 2017-03-08.
 */
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
@XmlRootElement
public class testsuite {
    String name;
    String node_order;
    int details;

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

    public int getDetails() {
        return details;
    }
    @XmlElement
    public void setDetails(int details) {
        this.details = details;
    }



    public List<com.sky.testlinkconvert.testsuite> getTestsuite() {
        return testsuite;
    }
    @XmlElement
    public void setTestsuite(List<com.sky.testlinkconvert.testsuite> testsuite) {
        this.testsuite = testsuite;
    }

    public List<com.sky.testlinkconvert.testcase> getTestcase() {
        return testcase;
    }
    @XmlElement
    public void setTestcase(List<com.sky.testlinkconvert.testcase> testcase) {
        this.testcase = testcase;
    }


    List<testsuite> testsuite;
    List<testcase>  testcase;



}