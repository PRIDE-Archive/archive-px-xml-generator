package uk.ac.ebi.pride.archive.px.writer;

import org.springframework.util.Assert;
import uk.ac.ebi.pride.archive.px.Util;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;

import java.io.File;
import java.io.IOException;

/**
 * This Class is the Context class of the Strategy design pattern. Based on the
 * PX_VERSION(strategy), the appropriate implementation will be called.
 *
 * @author Suresh Hewapathirana
 */
public class WriteMessageContext {

  public static File executeWriteMessage(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException {
    MessageWriter messageWriter = Util.getSchemaStrategy(pxAccession);
    Assert.isTrue(messageWriter != null, "No strategy found for the version "+ pxSchemaVersion);
    return messageWriter.createIntialPxXml(
        submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
  }
}
