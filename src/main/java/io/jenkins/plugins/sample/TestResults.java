package io.jenkins.plugins.sample;

public class TestResults {
    private Results results;

    // Constructors
    public TestResults() {}

    public TestResults(Results results) {
        this.results = results;
    }

    // Getter and setter
    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }
}
