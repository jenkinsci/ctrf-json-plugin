package io.jenkins.plugins.PublishCtrfJson;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.util.FormValidation;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PublishCtrfJson extends Recorder implements SimpleBuildStep {

    private final String jsonFilePattern;

    @DataBoundConstructor
    public PublishCtrfJson(String jsonFilePattern) {
        this.jsonFilePattern = jsonFilePattern;
    }

    public String getJsonFilePattern() {
        return jsonFilePattern;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, EnvVars env, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        FilePath[] jsonFiles = workspace.list(this.jsonFilePattern);

        if (jsonFiles == null || jsonFiles.length == 0) {
            listener.getLogger().println("No JSON test results found matching pattern: " + this.jsonFilePattern);
            return;
        }

        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (FilePath jsonFile : jsonFiles) {
            listener.getLogger().println("Processing JSON report: " + jsonFile.getRemote());

            if (jsonFile.length() == 0) {
                listener.getLogger().println("Ignoring empty JSON file: " + jsonFile.getRemote());
                continue;
            }

            try (InputStream inputStream = jsonFile.read()) {
                JsonParser jsonParser = jsonFactory.createParser(inputStream);
                if (!containsResultsKey(jsonParser)) {
                    listener.getLogger().println("Ignoring non-CTRF JSON file: " + jsonFile.getRemote());
                    continue;
                }

                jsonParser.close();
                TestResults testResults = objectMapper.readValue(jsonFile.read(), TestResults.class);
                Results results = testResults.getResults();
                if (results == null) {
                    listener.getLogger().println("No test results found in " + jsonFile.getRemote());
                    continue;
                }

                try {
                    FilePath junitReport = convertToJUnitXMLFormatAndWrite(results, workspace, jsonFile.getName());
                    if (junitReport != null) {
                        try {
                            JUnitResultArchiver archiver = new JUnitResultArchiver(junitReport.getName());
                            archiver.perform(run, workspace, env, launcher, listener);
                        } catch (Exception e) {
                            listener.getLogger()
                                    .println("Error publishing JUnit report for " + jsonFile.getRemote() + ": "
                                            + e.getMessage());
                            e.printStackTrace(listener.getLogger());
                            run.setResult(Result.FAILURE);
                        }
                    }
                } catch (TransformerException | ParserConfigurationException e) {
                    listener.getLogger()
                            .println("Error transforming XML for " + jsonFile.getRemote() + ": " + e.getMessage());
                    e.printStackTrace(listener.getLogger());
                    run.setResult(Result.FAILURE);
                }
            } catch (IOException e) {
                listener.getLogger()
                        .println("Failed to process JSON file: " + jsonFile.getRemote() + ": " + e.getMessage());
                e.printStackTrace(listener.getLogger());
                run.setResult(Result.FAILURE);
            }
        }
    }

    private boolean containsResultsKey(JsonParser jsonParser) throws IOException {
        while (jsonParser.nextToken() != null) {
            if (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME && "results".equals(jsonParser.getCurrentName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({ "lgtm[jenkins/csrf]", "lgtm[jenkins/unsafe-classes]" })
    private FilePath convertToJUnitXMLFormatAndWrite(Results results, FilePath workspace, String jsonFileName)
            throws IOException, TransformerException, ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        // Prevent XXE
        docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        docFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        docFactory.setXIncludeAware(false);
        docFactory.setExpandEntityReferences(false);

        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("testsuite");
        doc.appendChild(rootElement);

        rootElement.setAttribute("name", "CTRF Test Suite");
        rootElement.setAttribute("tests", String.valueOf(results.getTests().size()));

        for (TestResult test : results.getTests()) {
            Element testcase = doc.createElement("testcase");
            testcase.setAttribute("classname", "ctrf");
            testcase.setAttribute("name", test.getName());
            testcase.setAttribute("time", String.valueOf(test.getDuration() / 1000.0));

            switch (test.getStatus()) {
                case "failed":
                    Element failure = doc.createElement("failure");
                    failure.setAttribute("message", "Test failed");
                    testcase.appendChild(failure);
                    break;
                case "passed":
                    Element passed = doc.createElement("passed");
                    passed.setAttribute("message", "Test passed");
                    testcase.appendChild(passed);
                    break;
                case "skipped":
                    Element skipped = doc.createElement("skipped");
                    testcase.appendChild(skipped);
                    break;
                case "pending":
                    Element pending = doc.createElement("skipped");
                    pending.setAttribute("message", "Test is pending");
                    testcase.appendChild(pending);
                    break;
                case "other":
                    Element systemOut = doc.createElement("system-out");
                    systemOut.setTextContent("Test has an 'other' status");
                    testcase.appendChild(systemOut);
                    break;
                default:
                    Element unknown = doc.createElement("system-out");
                    unknown.setTextContent("Test has an unrecognized status: " + test.getStatus());
                    testcase.appendChild(unknown);
                    break;
            }

            rootElement.appendChild(testcase);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);

        String junitFilePath = "junitResult-" + jsonFileName + ".xml";
        FilePath junitFile = new FilePath(workspace, junitFilePath);

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(junitFile.getRemote()),
                StandardCharsets.UTF_8)) {
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
        }

        return junitFile;
    }

    @Extension
    @Symbol("publishCtrfResults")
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        @SuppressWarnings({ "lgtm[jenkins/no-permission-check]", "lgtm[jenkins/csrf]" })
        public FormValidation doCheckJsonFilePattern(@QueryParameter String value) {
            if (value.isEmpty()) {
                return FormValidation.error("The JSON file path must not be empty.");
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Publish CTRF test result report";
        }
    }
}
