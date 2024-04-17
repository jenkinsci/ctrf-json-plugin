package io.jenkins.plugins.PublishCtrfJson;

public class TestResult {
    private String name;
    private String status;
    private long duration;

    public TestResult() {}

    public TestResult(String name, String status, long duration) {
        this.name = name;
        this.status = status;
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
