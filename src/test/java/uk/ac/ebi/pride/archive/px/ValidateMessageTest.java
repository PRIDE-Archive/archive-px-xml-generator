package uk.ac.ebi.pride.archive.px;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class validates a test PX XML file, which should not return any errors.
 *
 * @author Tobias Ternent
 */
public class ValidateMessageTest {

    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public File directory;
    public File submissionFile;
    public String errorOutput;

    /**
     * Sets up unit test
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage();
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001");
        errorOutput = ValidateMessage.validateMessage(file);
    }

    /**
     * Test validation.
     */
    @Test
    public void testValidation(){
        assertTrue(errorOutput.length()<1);
    }

    /**
     * Tears down tests.
     * @throws IOException
     */
    @After
    public void tearDown() throws IOException {
    }
}
