package io.jenkins.plugins.PublishCtrfJson;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class PublishCtrfJsonTest {

    @Mock
    private Run<?, ?> run;

    @Mock
    private FilePath workspace;

    @Mock
    private TaskListener listener;

    @Mock
    private FilePath jsonFile;

    @InjectMocks
    private PublishCtrfJson publisher;

    private AutoCloseable closeable;

    @Mock
    private PrintStream printStream;

    @BeforeEach
    void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        publisher = new PublishCtrfJson("**/*.json");
        when(workspace.list(anyString())).thenReturn(new FilePath[0]);
        when(listener.getLogger()).thenReturn(printStream);
    }

    @Test
    void testProcessValidJsonFile() throws Exception {
        when(workspace.list("**/*.json")).thenReturn(new FilePath[] {jsonFile});
        when(jsonFile.read())
                .thenReturn(new ByteArrayInputStream(
                        "{ \"results\": [{ \"name\": \"Test1\", \"status\": \"passed\", \"duration\": \"3534\" }] }"
                                .getBytes()));
        publisher.perform(run, workspace, null, null, listener);
        verify(listener.getLogger()).println("Processing JSON report: " + jsonFile.getRemote());
        verify(run, never()).setResult(Result.FAILURE);
        verifyNoMoreInteractions(run);
    }

    @Test
    void testNoJsonFilesFound() throws Exception {
        when(workspace.list("**/*.json")).thenReturn(new FilePath[0]);
        publisher.perform(run, workspace, null, null, listener);
        verify(printStream).println("No JSON test results found matching pattern: **/*.json");
        verify(run, never()).setResult(Result.FAILURE);
        verifyNoMoreInteractions(run);
    }

    @Test
    void testIgnoringNonCTRFJsonFile() throws Exception {
        when(workspace.list("**/*.json")).thenReturn(new FilePath[] {jsonFile});
        when(jsonFile.getRemote()).thenReturn("nonCTRF.json");
        when(jsonFile.length()).thenReturn(100L);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("{}".getBytes());
        when(jsonFile.read()).thenReturn(byteArrayInputStream);
        publisher.perform(run, workspace, null, null, listener);
        verify(listener.getLogger()).println("Ignoring non-CTRF JSON file: nonCTRF.json");
        verify(run, never()).setResult(Result.FAILURE);
        verifyNoMoreInteractions(run);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }
}
