package io.jenkins.plugins.PublishCtrfJson;

import java.util.List;

public class Results {
    private ToolInfo tool;
    private TestSummary summary;
    private List<TestResult> tests;

    // Constructors
    public Results() {}

    public Results(ToolInfo tool, TestSummary summary, List<TestResult> tests) {
        this.tool = tool;
        this.summary = summary;
        this.tests = tests;
    }

    // Getters and setters
    public ToolInfo getTool() {
        return tool;
    }

    public void setTool(ToolInfo tool) {
        this.tool = tool;
    }

    public TestSummary getSummary() {
        return summary;
    }

    public void setSummary(TestSummary summary) {
        this.summary = summary;
    }

    public List<TestResult> getTests() {
        return tests;
    }

    public void setTests(List<TestResult> tests) {
        this.tests = tests;
    }
}
