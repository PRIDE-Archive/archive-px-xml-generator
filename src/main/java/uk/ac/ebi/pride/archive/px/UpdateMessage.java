package uk.ac.ebi.pride.archive.px;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.archive.px.reader.ReadMessage;
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
import java.util.List;
import java.util.Set;

/**
 * Class to update existing PX XML, to use new references or other meta-data.
 *
 * @author Tobias Ternent
 */
public class UpdateMessage {
  private static final Logger logger = LoggerFactory.getLogger(UpdateMessage.class);

  static Cv MS_CV;

  static {
    MS_CV = new Cv();
    MS_CV.setFullName("PSI-MS");
    MS_CV.setId("MS");
    MS_CV.setUri("https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo");
  }

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

    int revisionNo = getRevisionNumverFromPX(pxAccession);

    logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
    backupPxXml(pxFile, outputDirectory);

    MessageWriter messageWriter = Util.getSchemaStrategy(pxSchemaVersion);
    // make new PX XML if dealing with old schema version in current PX XML
    if (!proteomeXchangeDataset.getFormatVersion().equalsIgnoreCase(CURRENT_VERSION)) {
        proteomeXchangeDataset = createNewPXXML(messageWriter, pxFile, submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
    }
    // set new publication
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
    // change the log for the publication
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

    changeRevisionNumber( proteomeXchangeDataset,  pxAccession, Integer.toString(revisionNo + 1 )); // increase the revision number when updating PX XML

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

  public static File updateMetadataPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws Exception {
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
  public static File updateMetadataPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, boolean changeLogEntry, String pxSchemaVersion) throws Exception {
    Assert.isTrue(submissionSummaryFile.isFile() && submissionSummaryFile.exists(), "Summary file should already exist! In: " + submissionSummaryFile.getAbsolutePath());
    Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());
    File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
    Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");
      try {
          int revisionNo = getRevisionNumverFromPX(pxAccession);

          logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
          backupPxXml(pxFile, outputDirectory);
          MessageWriter messageWriter = Util.getSchemaStrategy(pxSchemaVersion);
          ProteomeXchangeDataset proteomeXchangeDataset = createNewPXXML(messageWriter, pxFile, submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
          if (changeLogEntry) {
            messageWriter.addChangeLogEntry(proteomeXchangeDataset, "Updated project metadata.");
          }
          changeRevisionNumber( proteomeXchangeDataset,  pxAccession, Integer.toString(revisionNo + 1 )); // increase the revision number when updating PX XML
          updatePXXML(pxFile, proteomeXchangeDataset, pxSchemaVersion);
      } catch (Exception e) {
         throw new Exception("Failed to update project metadata : " + e.getMessage());
      }
      return pxFile;
  }

    /**
     * Create new PXXML File
     * @param messageWriter the appropriate messageWriter should be passed, based on the PX version
     * @param pxFile The PX XML file
     * @param submissionSummaryFile the summary file containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @param datasetPathFragment the public path fragment(year/month/accession)
     * @param pxSchemaVersion latest PX schema version
     * @return ProteomeXchangeDataset Object
     * @throws SubmissionFileException
     * @throws IOException
     */
  private static ProteomeXchangeDataset createNewPXXML(MessageWriter messageWriter, File pxFile, File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException {
      pxFile = messageWriter.createIntialPxXml(submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
      if (pxFile != null && pxFile.length() > 0) {
          logger.info("Generated PX XML message file " + pxFile.getAbsolutePath());
      } else {
          final String MSG = "Failed to create PX XML message file at " + outputDirectory.getAbsolutePath();
          logger.error(MSG);
          throw new SubmissionFileException(MSG);
      }
      ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
      return proteomeXchangeDataset;
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

    /**
     * Get the revision number from the ProteomXchange Record.
     * We take it from the HTML because they do not provide revision version in their output JSON (2020-08)
     * @param pxAccession Project Accession
     * @return Revision version
     */
    private static int getRevisionNumverFromPX(String pxAccession){
        // JSON output does not give revision information
        String proteomeExchangeUrl= Constants.PROTEOME_EXCHANGE_URL + pxAccession;
        RestTemplate restTemplate = new RestTemplate();
        String html = restTemplate.getForObject(proteomeExchangeUrl, String.class);

        Document doc = Jsoup.parse(html);

        // <td class="dataset-currentrev"><a href="GetDataset?ID=PXD017848-1&test=no"><span class='current'>&#9205;</span> 1</a></td>
        // dataset-currentrev class element -> anchor tag -> second element
        int currentRevision = Integer.parseInt(doc.select(".dataset-currentrev>a[href]").get(0).childNode(1).toString().trim());
        return currentRevision;
    }

  /**
   * Increment the revision number if the entry already exists, if not this method will add a new entry to the PX XML
   * @param proteomeXchangeDataset
   * @param pxAccession
   * @param revision
   */
    private static void changeRevisionNumber(ProteomeXchangeDataset proteomeXchangeDataset, String pxAccession, String revision){
        boolean isAccessionRecordFound = false;
        boolean isRevisionRecordExists = false;
        int index = 0;
        List<DatasetIdentifier> datasetIdentifiers = proteomeXchangeDataset.getDatasetIdentifierList().getDatasetIdentifier();
        for (DatasetIdentifier datasetIdentifier : datasetIdentifiers) {
            List<CvParam> cvParams = datasetIdentifier.getCvParam();
            for (CvParam cvParam : cvParams) {
                if (cvParam.getAccession().equals("MS:1001919") && cvParam.getValue().equals(pxAccession)) { // ProteomeXchange accession number
                    isAccessionRecordFound = true;
                    index++;
                }else if (cvParam.getAccession().equals("MS:1001921")) { // ProteomeXchange accession number version number
                    isRevisionRecordExists = true;
                }
            }
            if(isAccessionRecordFound){
                if(isRevisionRecordExists){
                  // edit record
                  datasetIdentifier.getCvParam().get(index).setValue(revision);
                }else{
                  // add new record
                  cvParams.add(createCvParam("MS:1001921", revision, "ProteomeXchange accession number version number", MS_CV));
                }
                break;
            }
        }
    }

    /**
     * Method to create a CV Param.
     * @param accession the term's accession number
     * @param value the term's value
     * @param name the term's name
     * @param cvRef the term's ontology
     * @return
     */
    static CvParam createCvParam(String accession, String value, String name, Cv cvRef) {
        CvParam cvParam = new CvParam();
        cvParam.setAccession(accession.trim());
        cvParam.setValue(value == null ? null : value.trim());
        cvParam.setName(name.trim());
        cvParam.setCvRef(cvRef);
        return cvParam;
    }
}
