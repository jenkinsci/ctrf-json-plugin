package io.jenkins.plugins.PublishCtrfJson;

public class ToolInfo {
    private String name;

    public ToolInfo() {}

    public ToolInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
