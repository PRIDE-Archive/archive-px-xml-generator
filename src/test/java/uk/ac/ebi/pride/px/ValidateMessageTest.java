package uk.ac.ebi.pride.px;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: tobias
 * Date: 20/02/14
 * Time: 16:16
 * To change this template use File | Settings | File Templates.
 */
public class ValidateMessageTest {

    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public File directory;
    public File submissionFile;
    public String errorOutput;

    @Before
    public void setUp() throws Exception {
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage();
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001");
        errorOutput = ValidateMessage.validateMessage(file);
    }

    @Test
    public void testValidation(){
        assertTrue(errorOutput.length()<1);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(directory);
    }
}
