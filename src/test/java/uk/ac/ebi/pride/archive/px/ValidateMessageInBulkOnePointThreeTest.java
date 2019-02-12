package uk.ac.ebi.pride.archive.px;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This test class validates a test PX XML file, which should not return any errors.
 *
 * @author Tobias Ternent
 */

public class ValidateMessageInBulkOnePointThreeTest {
    Logger logger = Logger.getLogger(ValidateMessageInBulkOnePointThreeTest.class.getName());

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    final String SCHEMA_VERSION = "1.4.0";
    XMLParams params = new XMLParams();

    /**
     * Sets up unit test
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set parameters for the Px web service for testing/validating
        params.setTest("yes");
        params.setMethod("validateXML");
        params.setVerbose("yes");
        params.setPxPartner("TestRepo");
        params.setAuthentication("goon400"); // replace with the password
    }

    /**
     * Test validation on public datasets.
     */
    @Test
    public void testValidationInBulk() throws InterruptedException {
        Map<String, String> errorMessages = new HashMap<>();

        try {
            List<File> submissionSummaryFiles = Files.walk(Paths.get("src/test/resources/temp_px"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .sorted()
                    .collect(Collectors.toList());
            for (File submissionSummaryFile : submissionSummaryFiles) {
                String filename = submissionSummaryFile.getName();
                String[] dataPathFragmentList = filename.split("_");
                String pxAccession = dataPathFragmentList[2];
                String dataPathFragment = String.join("/", Arrays.copyOf(dataPathFragmentList, dataPathFragmentList.length-1));
                String response = validate(submissionSummaryFile, pxAccession, dataPathFragment);
                if(response.toUpperCase().contains("ERROR:")){
                    errorMessages.put(pxAccession, response);
                }
                Thread.sleep(2000);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        errorMessages.entrySet().forEach(System.out::println);
    }

    /**
     * Test validation on private datasets.
     */
    @Test
    public void testPrivateValidationInBulk() throws InterruptedException {

        FileHandler fileHandler = null;
        try {
            fileHandler = new FileHandler("app.log", false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.addHandler(fileHandler);

        try {
            List<File> submissionSummaryFiles = Files.walk(Paths.get("src/test/resources/private"))
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .sorted()
                    .collect(Collectors.toList());
            for (File submissionSummaryFile : submissionSummaryFiles.subList(2000, 4305)) { // 4305
                String filename = submissionSummaryFile.getName();
                String[] dataPathFragmentList = filename.split("_submission.px");
                String pxAccession = dataPathFragmentList[0];
                System.out.println(pxAccession);
                if (!pxAccession.equals("PXD009694")) {
                  String dataPathFragment = "2019/01/" + pxAccession;
                  String response = validate(submissionSummaryFile, pxAccession, dataPathFragment);
                  if (response.toUpperCase().contains("ERROR:")) {
                    logger.info(pxAccession + ":" + response);
                    logger.info("------------------------------------------");
                  }
                }
                Thread.sleep(1000);
            }
            logger.info("DONE");
        } catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }

    private String validate(File submissionFile, String pxAccession, String datasetPathFragment){

        String response;
        File directory ;
        MessageWriter messageWriter = Util.getSchemaStrategy(SCHEMA_VERSION);
        File file = null;
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            directory = temporaryFolder.newFolder(pxAccession + "_"+ timestamp.getTime());
            file = messageWriter.createIntialPxXml(submissionFile, directory, pxAccession, datasetPathFragment, SCHEMA_VERSION);
            response = PostMessage.postFile(file, params, SCHEMA_VERSION);
            return  response;
        } catch (SubmissionFileException | IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Tears down tests.
     * @throws IOException
     */
    @After
    public void tearDown() throws IOException {
    }
}
