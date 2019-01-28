package uk.ac.ebi.pride.archive.px;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * This test class validates a test PX XML file, which should not return any errors.
 *
 * @author Tobias Ternent
 */
public class ValidateMessageTest {

    @Rule
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
        submissionFile = new File("src/test/resources/PXD010568_submission.px");
        WriteMessage messageWriter = new WriteMessage();
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXD010568", "2019/01/PXD010568");
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
