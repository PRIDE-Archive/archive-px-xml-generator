package uk.ac.ebi.pride.archive.px;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * This test class attempts to post a test PX XML to Proteome Central. Doesn't actually log in
 * but should get a reply anyway about the attempt.
 *
 * @author Tobias Ternent
 */
public class PostMessageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public File directory;
    public File submissionFile;
    public String response, responseNoEmail;

    /**
     * Setup unit test.
     * @throws Exception
     */
    @Before
    public void setUp() {
        //n/a
    }

    /**
     * Tests posting PX XML.
     */
    @Test
    public void testPost() throws Exception{
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage();
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001");
        XMLParams params = new XMLParams();
        params.setTest("yes");
        params.setMethod("submitDataset");
        params.setVerbose("no");
        params.setPxPartner("");
        params.setAuthentication("");
        response = PostMessage.postMessage(file, params);

        params = new XMLParams();
        params.setTest("yes");
        params.setMethod("submitDataset");
        params.setVerbose("no");
        params.setPxPartner("");
        params.setAuthentication("");
        params.setNoEmailBroadcast("true");
        responseNoEmail = PostMessage.postMessage(file, params);

        assertTrue(response != null && !(response.toLowerCase().contains("internal server error")));
        assertTrue(response != null && !(response.toLowerCase().contains("internal server error")));
    }

    /**
     * Tears down tests.
     * @throws IOException
     */
    @After
    public void tearDown() throws IOException {
    }

}
