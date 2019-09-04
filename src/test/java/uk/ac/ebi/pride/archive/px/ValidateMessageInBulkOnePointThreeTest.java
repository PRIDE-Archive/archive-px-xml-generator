package uk.ac.ebi.pride.archive.px;

import java.io.IOException;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This test class validates a test PX XML file, which should not return any errors.
 *
 * @author Suresh Hewapathirana
 */
public class ValidateMessageInBulkOnePointThreeTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  final String SCHEMA_VERSION = "1.3.0";
  XMLParams params = new XMLParams();

  /**
   * Sets up unit test
   *
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

  /** Test validation on public datasets. */
  @Test
  @Ignore
  public void testValidationInBulk() throws InterruptedException {
    Map<String, String> errorMessages = new HashMap<>();

    try {
      List<File> submissionSummaryFiles =
          Files.walk(Paths.get("src/test/resources/temp_px"))
              .filter(Files::isRegularFile)
              .map(Path::toFile)
              .sorted()
              .collect(Collectors.toList());
      for (File submissionSummaryFile : submissionSummaryFiles) {
        String filename = submissionSummaryFile.getName();
        String[] dataPathFragmentList = filename.split("_");
        String pxAccession = dataPathFragmentList[2];
        String dataPathFragment =
            String.join("/", Arrays.copyOf(dataPathFragmentList, dataPathFragmentList.length - 1));
        String response = validate(submissionSummaryFile, pxAccession, dataPathFragment);
        if (response.toUpperCase().contains("ERROR:")) {
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
   * You will be able to find a script in https://github.com/PRIDE-Archive/pride-curation-scripts
   * to easily download submission.px files from File system and then use this method to validate all at once.
   * Save data in a folder called private under resources.
   * NOTE: change the output filepath
   * */
  @Test
  @Ignore
  public void testPrivateValidationInBulk() throws InterruptedException {

    Map<String, String> errorList = new HashMap<>();
    int count = 0;
    final String outfile = "/Users/hewapathirana/Desktop/pxXML.xlsx";

    try {
      List<File> submissionSummaryFiles =
          Files.walk(Paths.get("src/test/resources/private"))
              .filter(Files::isRegularFile)
              .map(Path::toFile)
              .sorted()
              .collect(Collectors.toList());
      for (File submissionSummaryFile : submissionSummaryFiles) {
        String filename = submissionSummaryFile.getName();
        String[] dataPathFragmentList = filename.split("_submission.px");
        String pxAccession = dataPathFragmentList[0];
        System.out.println(pxAccession);
        if (!pxAccession.contains("-RESUB")) {
          if (!pxAccession.equals("PXD009694")) { // gives an error
            String dataPathFragment = "2019/01/" + pxAccession; // accession with  randam data path fragment
            try {
              String response = validate(submissionSummaryFile, pxAccession, dataPathFragment);
              if (response.toUpperCase().contains("ERROR:")) {
                System.out.println(count + " --> " + pxAccession + " : " + response);
                errorList.put(pxAccession, response);
              }
            } catch (Exception e) {
              System.out.println(e.getMessage());
            }
          }
        }
        count++;
      }
      if (errorList.size() > 0) {
        Util.saveInExcel(errorList, "Errors", outfile);
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  private String validate(File submissionFile, String pxAccession, String datasetPathFragment) {

    String response;
    File directory;
    MessageWriter messageWriter = Util.getSchemaStrategy(SCHEMA_VERSION);
    File file;
    try {
      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      directory = temporaryFolder.newFolder(pxAccession + "_" + timestamp.getTime());
      file =
          messageWriter.createIntialPxXml(
              submissionFile, directory, pxAccession, datasetPathFragment, SCHEMA_VERSION);
      response = PostMessage.postFile(file, params, SCHEMA_VERSION);
      return response;
    } catch (SubmissionFileException | IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Tears down tests.
   *
   * @throws IOException
   */
  @After
  public void tearDown() throws IOException {}
}
