package uk.ac.ebi.pride.archive.px;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.archive.px.reader.ReadMessage;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.xml.PxMarshaller;
import uk.ac.ebi.pride.archive.px.xml.PxUnmarshaller;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

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
     * @param submission the summary containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @param datasetPathFragment the public path fragment
     * @return a File that is the updated PX XML.
     * @throws SubmissionFileException
     * @throws IOException
     */
    public static File updateReferencesPxXml(File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion, Submission submission) throws IOException, SubmissionFileException {
        final String CURRENT_VERSION = "1.4.0";
        Assert.isTrue(submission.getProjectMetaData().hasPubmedIds() || submission.getProjectMetaData().hasDois(),
            "Submission Summary should have PubMed IDs or DOIs listed!");
        Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());
        File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");

        ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);

        int revisionNumber = preUpdateSteps(pxFile, outputDirectory, pxAccession);
        MessageWriter messageWriter = Util.getSchemaStrategy(pxSchemaVersion);
        Assert.isTrue(messageWriter != null, "No implementation found for " + pxSchemaVersion);

        // make new PX XML if dealing with old schema version in current PX XML
        if (!proteomeXchangeDataset.getFormatVersion().equalsIgnoreCase(CURRENT_VERSION)) {
            proteomeXchangeDataset = createNewPXXML(messageWriter, pxFile, submission, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
        }
        // set new publication
        proteomeXchangeDataset.getPublicationList().getPublication().clear();
        StringBuilder sb = new StringBuilder("");
        String reference;
        Set<String> pubmedIds = submission.getProjectMetaData().getPubmedIds();
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
        if (submission.getProjectMetaData().hasDois()) {
          for (String doi : submission.getProjectMetaData().getDois()) {
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

        changeRevisionNumber( proteomeXchangeDataset,  pxAccession, Integer.toString(revisionNumber + 1 )); // increase the revision number when updating PX XML

        updatePXXML(pxFile, proteomeXchangeDataset, pxSchemaVersion);
        return pxFile;
    }

    /**
   * Method to update a PX XML file with a newly generated version,
   * e.g. with up-to-date FTP links, project tags, etc, according to the latest schema.
   *
   * @param submissionSummary the summary containing the PX submission summary information.
   * @param outputDirectory the path to the PX XML output directory.
   * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
   * @param datasetPathFragment the public path fragment

   * @return a File that is the updated PX XML.
   * @throws SubmissionFileException
   * @throws IOException
   */

  public static File updateMetadataPxXml(Submission submissionSummary, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws Exception {
    return  updateMetadataPxXml(submissionSummary, outputDirectory, pxAccession, datasetPathFragment, true, pxSchemaVersion);
  }

  /**
   * Method to update a PX XML file with a newly generated version,
   * e.g. with up-to-date FTP links, project tags, etc, according to the latest schema.
   * @param submission the summary containing the PX submission summary information.
   * @param outputDirectory the path to the PX XML output directory.
   * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
   * @param datasetPathFragment the public path fragment
   * @param changeLogEntry include a change log entry or not.
   * @return
   * @throws SubmissionFileException
   * @throws IOException
   */
  public static File updateMetadataPxXml(Submission submission, File outputDirectory, String pxAccession, String datasetPathFragment, boolean changeLogEntry, String pxSchemaVersion) throws Exception {
     Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());
    File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
    //Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");
      try {
          int revisionNumber = preUpdateSteps(pxFile, outputDirectory, pxAccession);
          MessageWriter messageWriter = Util.getSchemaStrategy(pxSchemaVersion);
          Assert.isTrue(messageWriter != null, "No implementation found for " + pxSchemaVersion);
          ProteomeXchangeDataset proteomeXchangeDataset = createNewPXXML(messageWriter, pxFile, submission, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
          if (changeLogEntry) {
            messageWriter.addChangeLogEntry(proteomeXchangeDataset, "Updated project metadata.");
          }
          changeRevisionNumber( proteomeXchangeDataset,  pxAccession, Integer.toString(revisionNumber + 1 )); // increase the revision number when updating PX XML
          updatePXXML(pxFile, proteomeXchangeDataset, pxSchemaVersion);
      } catch (Exception e) {
         throw new Exception("Failed to update project metadata : " + e.getMessage());
      }
      return pxFile;
  }

    /**
     * Before changing the PX XML file,
     *  First, check for any empty XML file. If the file is empty, recover it from the previous backup
     *  Secondly, find the revision number
     *  Finally, take a backup of the current XML file before we do any change
     * @param pxFile Active PX XML file (non-backup file with <accession>.xml filename)
     * @param outputDirectory generated folder
     * @return PX XML revision number
     * @throws IOException
     */
    private static int preUpdateSteps(File pxFile, File outputDirectory, String pxAccession) throws IOException {

        boolean isPXXMLExists = pxFile.isFile() && pxFile.exists()&& pxFile.length()>1;

        // 1) if PX file is not exists, try to take from the backup
        if(!isPXXMLExists) {
            revertbackupPxXml(pxFile, outputDirectory);
            isPXXMLExists = pxFile.isFile() && pxFile.exists()&& pxFile.length()>1;
        }

        // 2) find the revision number before backup xml file
        int revisionNumber = getRevisionNumber(pxFile, pxAccession);

        // 3) after recover, check again and backup
        if(isPXXMLExists) {
            logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
            backupPxXml(pxFile, outputDirectory);
        }

        return revisionNumber;
    }

    /**
     * Create new PXXML File
     * @param messageWriter the appropriate messageWriter should be passed, based on the PX version
     * @param pxFile The PX XML file
     * @param submission the summary containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @param datasetPathFragment the public path fragment(year/month/accession)
     * @param pxSchemaVersion latest PX schema version
     * @return ProteomeXchangeDataset Object
     * @throws SubmissionFileException
     * @throws IOException
     */
  private static ProteomeXchangeDataset createNewPXXML(MessageWriter messageWriter, File pxFile, Submission submission, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException {
      pxFile = messageWriter.createIntialPxXml(submission, outputDirectory, pxAccession, datasetPathFragment, pxSchemaVersion);
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
     * Recover PXXML file (if empty) from the latest backup
     * @param pxFile active PX XML file with <accession>.xml file name
     * @param outputDirectory "generated" folder in the dataset
     * @throws IOException
     */
    private static void revertbackupPxXml(File pxFile, File outputDirectory) throws IOException{
        File[] files = outputDirectory.listFiles();
        List<String> filenames = new ArrayList<>();
        if (files != null) {
            for (File file:files) {
                filenames.add(file.getName());
            }
            Collections.sort(filenames);

            for (int i=filenames.size()-1; i>=0; i--){
                System.out.println(filenames.get(i));
                File pxCurrentFile = new File(outputDirectory.getAbsolutePath() + "/" + filenames.get(i));
                boolean isPXXMLExists = pxCurrentFile.isFile() && pxCurrentFile.exists()&& pxCurrentFile.length()>1;
                if(isPXXMLExists){
                    Files.move(pxCurrentFile.toPath(), pxFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    break;
                }
            }
        }
    }

    /**
     * First try to get the revision number from ProteomeXchange.
     * There may be cases where dataset is public in PRIDE, but still it is not retrievable in PX.
     * In such cases, as the second attempt, it will read the revision number from the existing
     * PXXML file.
     *
     * @param pxFile active PX XML file with <accession>.xml file name
     * @param pxAccession Project Accession
     * @return Revision version
     */
    private static int getRevisionNumber(File pxFile, String pxAccession) {
        int currentRevisionNo = 0;

            int revNo = getRevisionNoFromProteomeCentral(pxAccession);
            if(revNo != -1) { // which means an error occured from ProteomeCentral
                currentRevisionNo = revNo;
            }
//            else{
//                currentRevisionNo = readRevisionNoFromSubmissionPX(pxFile, pxAccession);
//            }
        return currentRevisionNo;
    }


    /**
     * Get the revision number from the ProteomXchange Record.
     * We take it from the HTML because they do not provide revision version in their output JSON (2020-08)
     * @param pxAccession Project Accession
     * @return Revision version
     */
    private static int getRevisionNoFromProteomeCentral(String pxAccession){
        int currentRevisionNo = -1;
        try {
            // JSON output does not give revision information
            String proteomeExchangeUrl= Constants.PROTEOME_EXCHANGE_URL + "GetDataset?ID=" + pxAccession;
            RestTemplate restTemplate = new RestTemplate();
            String html = restTemplate.getForObject(proteomeExchangeUrl, String.class);

            Document doc = Jsoup.parse(html);

            // <td class="dataset-currentrev"><a href="GetDataset?ID=PXD017848-1&test=no"><span class='current'>&#9205;</span> 1</a></td>
            // dataset-currentrev class element -> anchor tag -> second element
            currentRevisionNo = Integer.parseInt(doc.select(".dataset-currentrev>a[href]").get(0).childNode(1).toString().trim());
        } catch (RestClientException | NumberFormatException e) {
            logger.warn("Current revision number cannot retrieve from ProteomeXchange!");
        }
        return currentRevisionNo;
    }

    /**
     * Reads the current PX XML file and find the revision version
     * @param pxFile current PX XML file
     * @param pxAccession Project accession
     * @return revision number
     * @throws IOException
     */
    private static int readRevisionNoFromSubmissionPX(File pxFile, String pxAccession) {
        int currentRevisionNo = 1;
        boolean isAccessionRecordFound = false;
        ProteomeXchangeDataset proteomeXchangeDataset = new PxUnmarshaller().unmarshall(pxFile);
        DatasetIdentifierList datasetIdentifierList = proteomeXchangeDataset.getDatasetIdentifierList();
        List<DatasetIdentifier> datasetIdentifiers = datasetIdentifierList.getDatasetIdentifier();
        for (DatasetIdentifier datasetIdentifier : datasetIdentifiers) {
            List<CvParam> cvParams = datasetIdentifier.getCvParam();
            for (CvParam cvParam : cvParams) {
                if (cvParam.getAccession().equals("MS:1001919") && cvParam.getValue().equals(pxAccession)) { // ProteomeXchange accession number
                    isAccessionRecordFound = true;
                }
                if (cvParam.getAccession().equals("MS:1001921")) { // ProteomeXchange accession number version number
                    currentRevisionNo = Integer.parseInt(cvParam.getValue());
                    break;
                }
            }
            if(isAccessionRecordFound) break;
        }
        return currentRevisionNo;
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
