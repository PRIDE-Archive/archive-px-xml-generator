package uk.ac.ebi.pride.archive.px;

import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;

import java.io.File;

import static org.junit.Assert.assertTrue;

/**
 * This test class attempts to post a test PX XML to Proteome Central. Doesn't actually log in
 * but should get a reply anyway about the attempt.
 *
 * @author Tobias Ternent
 */
public class PostMessageOnePointThreeTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * Tests posting PX XML.
     * @throws Exception Problems posting to ProteomeCentral in test omode.
     */
    @Test
    public void testPost() throws Exception{

         File directory;
         File submissionFile;
         String response;
         final String SCHEMA_VERSION = "1.3.0";

        // Create an XML file
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        MessageWriter messageWriter = Util.getSchemaStrategy(SCHEMA_VERSION);
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001", SCHEMA_VERSION);

        // Set parameters for the Px web service for testing/validating
        XMLParams params = new XMLParams();
        params.setTest("yes");
        params.setMethod("validateXML");
        params.setVerbose("yes");
        params.setPxPartner("TestRepo");
        params.setAuthentication("*******"); // replace with the password
        response = PostMessage.postFile(file, params, SCHEMA_VERSION);

        assertTrue(StringUtils.isNotEmpty(response) && !(response.toLowerCase().contains("internal server error")));
        System.out.println(response);
        assertTrue(response.startsWith("result=SUCCESS"));
        assertTrue(!response.contains("message=ERROR"));
    }
}
