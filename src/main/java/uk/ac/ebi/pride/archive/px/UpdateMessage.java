package uk.ac.ebi.pride.archive.px;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.archive.px.reader.ReadMessage;
import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.xml.PxMarshaller;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Set;

/**
 * Class to update existing PX XML, to use new references or other meta-data.
 *
 * @author Tobias Ternent
 */
public class UpdateMessage {
  private static final Logger logger = LoggerFactory.getLogger(UpdateMessage.class);

  /**
   * Method to update a PX XML file with new references, intended only for *public* projects.
   * Note: this will add a change log, since that is needed after the first version of the PX XML.
   * Will also backup the PX XML before updating.
   *
   * @param submissionSummaryFile the summary file containing the PX submission summary information.
   * @param outputDirectory the path to the PX XML output directory.
   * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
   * @param datasetPathFragment the public path fragment
   * @return a File that is the updated PX XML.
   * @throws SubmissionFileException
   * @throws IOException
   */
  public static File updateReferencesPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException {
    final String CURRENT_VERSION = "1.4.0";
    Assert.isTrue(submissionSummaryFile.isFile() && submissionSummaryFile.exists(), "Summary file should already exist! In: " + submissionSummaryFile.getAbsolutePath());
    Submission submissionSummary = SubmissionFileParser.parse(submissionSummaryFile);
    Assert.isTrue(submissionSummary.getProjectMetaData().hasPubmedIds() || submissionSummary.getProjectMetaData().hasDois(),
        "Summary file should have PubMed IDs or DOIs listed!");
    Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());
    File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
    Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");

    ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
    logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
    backupPxXml(pxFile, outputDirectory);

    MessageWriter messageWriter = Util.getSchemaStrategy(pxSchemaVersion);
    // make new PX XML if dealing with old schema version in current PX XML
    if (!proteomeXchangeDataset.getFormatVersion().equalsIgnoreCase(CURRENT_VERSION)) {

      pxFile = messageWriter.createIntialPxXml(submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment,pxSchemaVersion);
      if (pxFile != null) {
        logger.info("Generated PX XML message file " + pxFile.getAbsolutePath());
      } else {
        final String MSG = "Failed to create PX XML message file at " + outputDirectory.getAbsolutePath();
        logger.error(MSG);
        throw new SubmissionFileException(MSG);
      }
      proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
    }
    proteomeXchangeDataset.getPublicationList().getPublication().clear();
    StringBuilder sb = new StringBuilder("");
    String reference;
    Set<String> pubmedIds = submissionSummary.getProjectMetaData().getPubmedIds();
    Iterator<String> it = pubmedIds.iterator();
    while (it.hasNext()) {
      reference = it.next();
      proteomeXchangeDataset.getPublicationList().getPublication().add(messageWriter.getPublication(Long.parseLong(reference.trim())));
      sb.append(reference);
      if (it.hasNext()) {
        sb.append(", ");
      }
    }
    if (sb.length()>0) {
      messageWriter.addChangeLogEntry(proteomeXchangeDataset, "Updated publication reference for PubMed record(s): " + sb.toString() + ".");
    }
    sb.delete(0, sb.length());
    if (submissionSummary.getProjectMetaData().hasDois()) {
      for (String doi : submissionSummary.getProjectMetaData().getDois()) {
        proteomeXchangeDataset.getPublicationList().getPublication().add(messageWriter.getPublicationDoi(doi));
        sb.append(doi);
        if (it.hasNext()) {
          sb.append(", ");
        }
      }
      if (sb.length()>0) {
        messageWriter.addChangeLogEntry(proteomeXchangeDataset, "Updated publication reference for DOI(s): " + sb.toString() + ".");
      }
    }
    updatePXXML(pxFile, proteomeXchangeDataset, pxSchemaVersion);
    return pxFile;
  }

  /**
   * Method to update a PX XML file with a newly generated version,
   * e.g. with up-to-date FTP links, project tags, etc, according to the latest schema.
   *
   * @param submissionSummaryFile the summary file containing the PX submission summary information.
   * @param outputDirectory the path to the PX XML output directory.
   * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
   * @param datasetPathFragment the public path fragment

   * @return a File that is the updated PX XML.
   * @throws SubmissionFileException
   * @throws IOException
   */

  public static File updateMetadataPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException {
    return  updateMetadataPxXml(submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment, true, pxSchemaVersion);
  }

  /**
   * Method to update a PX XML file with a newly generated version,
   * e.g. with up-to-date FTP links, project tags, etc, according to the latest schema.
   * @param submissionSummaryFile the summary file containing the PX submission summary information.
   * @param outputDirectory the path to the PX XML output directory.
   * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
   * @param datasetPathFragment the public path fragment
   * @param changeLogEntry include a change log entry or not.
   * @return
   * @throws SubmissionFileException
   * @throws IOException
   */
  public static File updateMetadataPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, boolean changeLogEntry, String pxSchemaVersion) throws SubmissionFileException, IOException {
    Assert.isTrue(submissionSummaryFile.isFile() && submissionSummaryFile.exists(), "Summary file should already exist! In: " + submissionSummaryFile.getAbsolutePath());
    Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());
    File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
    Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");

    logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
    backupPxXml(pxFile, outputDirectory);
    MessageWriter messageWriter = Util.getSchemaStrategy(pxSchemaVersion);
    pxFile = messageWriter.createIntialPxXml(submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
    if (pxFile != null) {
      logger.info("Generated PX XML message file " + pxFile.getAbsolutePath());
    } else {
      final String MSG = "Failed to create PX XML message file at " + outputDirectory.getAbsolutePath();
      logger.error(MSG);
      throw new SubmissionFileException(MSG);
    }
    ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
    if (changeLogEntry) {
      messageWriter.addChangeLogEntry(proteomeXchangeDataset, "Updated project metadata.");
    }

    updatePXXML(pxFile, proteomeXchangeDataset, pxSchemaVersion);
    return pxFile;
  }

  private static void updatePXXML(File pxFile, ProteomeXchangeDataset proteomeXchangeDataset, String pxSchemaVersion){
    logger.debug("Updating metadata for PX XML file: " + pxFile.getAbsolutePath());
    FileWriter fw = null;

    try {
      fw = new FileWriter(pxFile);
      new PxMarshaller().marshall(proteomeXchangeDataset, fw, pxSchemaVersion);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (fw != null) {
        try {
          fw.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    logger.info("PX XML file updated: " + pxFile.getAbsolutePath());
  }

  /**
   * Backs up the current PX XML to a target directory, using a suffix _number.
   * @param pxFile The PX XML file to backup
   * @param outputDirectory Target directory where to backup to.
   * @throws IOException
   */
  private static void backupPxXml(File pxFile, File outputDirectory) throws IOException{
    String baseName = FilenameUtils.getBaseName(pxFile.getName());
    String ext = FilenameUtils.getExtension(pxFile.getName());
    final String VERSION_SEP = "_";
    final String EXT_SEP = ".";
    int nextVersionNumber = 1;
    File backupPx =  new File(outputDirectory.getAbsolutePath() + File.separator + baseName + VERSION_SEP + nextVersionNumber + EXT_SEP + ext);
    while (backupPx.exists()) {
      baseName = FilenameUtils.getBaseName(backupPx.getName());
      String[] split = baseName.split(VERSION_SEP);
      nextVersionNumber = (Integer.parseInt(split[1])) + 1;
      backupPx =  new File(outputDirectory.getAbsolutePath() + File.separator + split[0] + VERSION_SEP + nextVersionNumber + EXT_SEP + ext);
    }
    Files.copy(pxFile.toPath(), backupPx.toPath());
  }
}
