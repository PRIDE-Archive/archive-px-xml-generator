package uk.ac.ebi.pride.archive.px;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;

import java.io.File;
import java.io.IOException;

/**
 * @author Suresh Hewapathirana
 */
public class UpdateMessageOnePointFourTest {

    File submissionSummaryFile;
    File outputDirectory;
    final String pxAccession = "PXT000001";
    final String datasetPathFragment = "2013/07/PXT000001";
    final String pxSchemaVersion = "1.4.0";

    @Before
    public void setUp() throws Exception {
        submissionSummaryFile = new File("src/test/resources/submission.px");
        outputDirectory = new File("src/test/resources/pxxml");
    }

    @Test
    public void updateMetadataPxXmlTest() throws Exception {

        try {
            UpdateMessage.updateMetadataPxXml(SubmissionFileParser.parse(submissionSummaryFile), outputDirectory, pxAccession, datasetPathFragment, true, pxSchemaVersion);
        } catch (SubmissionFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}