package uk.ac.ebi.pride.archive.px;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * This test class validates a test PX XML file, which should not return any errors.
 *
 * @author Tobias Ternent
 */
public class ValidateMessageOnePointFourTest {

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
        final String SCHEMA_VERSION = "1.4.0";
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        MessageWriter messageWriter = Util.getSchemaStrategy(SCHEMA_VERSION);
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXD010568", "2019/01/PXD010568", SCHEMA_VERSION);
        errorOutput = ValidateMessage.validateMessage(file, SCHEMA_VERSION);
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
