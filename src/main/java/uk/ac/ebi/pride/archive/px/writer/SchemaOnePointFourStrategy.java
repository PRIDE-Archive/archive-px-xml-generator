package uk.ac.ebi.pride.archive.px.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.pubmed.PubMedFetcher;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

/**
 * Writes out the PX XML file, which contains all the metadata for a dataset to be sent to ProteomeCentral.
 */
public class SchemaOnePointFourStrategy extends SchemaCommonStrategy {

  private static final Logger logger = LoggerFactory.getLogger(SchemaOnePointFourStrategy.class);

  private String formatVersion;

  /**
   * Default constructor.
   */
  public SchemaOnePointFourStrategy(String version) {
    this.formatVersion = version;
  }

    /**
     * Method to generate the initial PX XML document.
     * Note: this will not add a change log, since that is not needed for the first version of the PX XML.
     *       Subsequent changes to an already existing PX XML should add change log entries documenting
     *       the changes that have been done.
     *
     * @param submissionSummary the Submission object containing the PX submission summary information.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @param datasetPathFragment the path fragment that points to the dataset (pattern: /yyyy/mm/accession/).
     * @return a ProteomeXchangeDataset ready for marshaling into a PX XML file.
     */
    @Override
    protected ProteomeXchangeDataset createPxXml(Submission submissionSummary, String pxAccession, String datasetPathFragment, String pxSchemaVersion) {
        if ( !isValidPXAccession(pxAccession) ) {
            String err = "Specified PX accession is not valid! " + pxAccession;
            logger.error(err);
            throw new IllegalArgumentException(err);
        }
        if ( !isValidPathFragment(datasetPathFragment, pxAccession) ) {
            String err = "Specified dataset path fragment is not valid! " + datasetPathFragment;
            logger.error(err);
            throw new IllegalArgumentException(err);
        }
        ProteomeXchangeDataset pxXml = new ProteomeXchangeDataset();
        pxXml.setId(pxAccession);
        pxXml.setFormatVersion(this.formatVersion);
        CvList cvList = getCvList();
        pxXml.setCvList(cvList);
        // no change log, since initial PX XML generation
        DatasetSummary datasetSummary = getDatasetSummary(submissionSummary);
        pxXml.setDatasetSummary(datasetSummary);
        // add the DatasetIdentifier (add a DOI record for complete submissions)
        boolean withDOI = submissionSummary.getProjectMetaData().getSubmissionType() == SubmissionType.COMPLETE;
        DatasetIdentifierList datasetIdentifierList = getDatasetIdentifierList(pxAccession, withDOI);
        pxXml.setDatasetIdentifierList(datasetIdentifierList);
        // add dataset origin info (this is constant right now: PRIDE)
        DatasetOriginList datasetOriginList = getDatasetOriginList();
        pxXml.setDatasetOriginList(datasetOriginList);
        // add species
        SpeciesList speciesList = getSpeciesList(submissionSummary);
        pxXml.setSpeciesList(speciesList);
        // add instruments
        InstrumentList instrumentList = getInstrumentList(submissionSummary);
        pxXml.setInstrumentList(instrumentList);
        // add modifications
        ModificationList modificationList = getModificationList(submissionSummary);
        pxXml.setModificationList(modificationList);
        // extract contacts from summary file, data like title, description, hosting repo, announce date, review level, repo support level
        ContactList contactList = getContactList(submissionSummary);
        pxXml.setContactList(contactList);
        // add the publication list
        PublicationList publicationList = getPublicationList(submissionSummary);
        pxXml.setPublicationList(publicationList);
        // extract keywords from summary file as submitter keywords
        KeywordList keywordList = getKeywordList(submissionSummary);
        pxXml.setKeywordList(keywordList);
        // create the link to the full dataset (PRIDE FTP)
        FullDatasetLinkList fullDatasetLinkList = createFullDatasetLinkList(datasetPathFragment, pxAccession);
        pxXml.setFullDatasetLinkList(fullDatasetLinkList);
        // add the list of files in this dataset (optional XML element)
        DatasetFileList datasetFileList = createDatasetFileList(submissionSummary, datasetPathFragment);
        pxXml.setDatasetFileList(datasetFileList);
        // add the repository record list (optional XML element)
        RepositoryRecordList repositoryRecordList = createRepositoryRecordList(submissionSummary, pxAccession);
        pxXml.setRepositoryRecordList(repositoryRecordList);
        return  pxXml;
    }

  /**
   * Gets the publication list. There should always be a publication list, but it may have records to say 'no reference' or 'reference pending'
   * @param submissionSummary the submission summary.
   * @return The PublicationList is returned.
   */
  @Override
  protected PublicationList getPublicationList(Submission submissionSummary) {
    PublicationList list = new PublicationList();
    Set<String> pubmedIDs = submissionSummary.getProjectMetaData().getPubmedIds();
    Set<String> dois = submissionSummary.getProjectMetaData().getDois();
    if ((pubmedIDs==null || pubmedIDs.size()<1) && (dois==null || dois.size()<1)) {
      // no pubmed ID, so no publication, we assume it is pending
      Publication publication = new Publication();
      CvParam cvParam = new CvParam();
      cvParam.setCvRef(MS_CV);
      cvParam.setName("Dataset with its publication pending");
      cvParam.setAccession("MS:1002858");
      publication.setId("pending");
      publication.getCvParam().add(cvParam);
      list.getPublication().add(publication);
    } else { // we have already publications
      if (pubmedIDs!=null) {
        for (String pubmedID : pubmedIDs) {
          Long pmid = Long.parseLong(pubmedID);
          list.getPublication().add(getPublication(pmid));
        }
      }
      if (dois != null) {
        for (String doi : dois) {
          list.getPublication().add(getPublicationDoi(doi));
        }
      }
    }
    return  list;
  }

  /**
   * Extracts the publication from a refline.
   * @param refLine The refline to obtain the publication.
   * @return the Publication object
   */
  @Override
  public Publication getPublication(String refLine) {
    if (refLine == null) {
      throw new IllegalArgumentException("No ref line provided!");
    }
    Publication publication = new Publication();
    publication.setId("PUBLICATION"); // ToDo: this should be unique!
    publication.getCvParam().add(createCvParam("MS:1002866", refLine, "Reference", MS_CV));
    return publication;
  }

  /**
   * Gets a Publication from a PubMed ID
   * @param pmid the PubMed ID
   * @return the Publication object
   */
  @Override
  public Publication getPublication(Long pmid) {
    if (pmid == null) {
      throw new IllegalArgumentException("No PMID provided!");
    }
    Publication publication = new Publication();
    publication.setId("PMID" + pmid);
    publication.getCvParam().add(createCvParam("MS:1000879", pmid.toString(), "PubMed identifier", MS_CV));
    String refLine;
    try {
      refLine = PubMedFetcher.getPubMedSummary(Long.toString(pmid)).getRefLine();
    } catch (URISyntaxException | IOException e) {
      logger.error("Problems getting reference line from PubMed " + e.getMessage());
      refLine = "No refLine for PMID: " + pmid; // ToDo: better default value?
    }
    publication.getCvParam().add(createCvParam("MS:1002866", refLine, "Reference", MS_CV));
    return publication;
  }

  /**
   * Gets a list of allowed CVs
   * @return CvList.
   */
  @Override
  protected CvList getCvList() {
    CvList list = new CvList();
    list.getCv().add(MS_CV);
    list.getCv().add(MOD_CV);
    list.getCv().add(UNIMOD_CV);
    return list;
  }

  /**
   * Method to extract modifications from summary file
   * @param submissionSummary the submission summary object of the project
   * @return ModificationList.
   */
  @Override
  // Note: this will primarily look at project level, and only look at result file level if no annotation was found
  public ModificationList getModificationList(Submission submissionSummary) {
    ModificationList list = new ModificationList();
    // ToDo: take into account that modifications on project level are not mandatory for complete submissions
    // the modification annotation is mandatory in the submission summary file AND the PX XML
    // however, in the summary file modifications can be annotated at project level or for each result file (in case of complete submissions)
    Set<uk.ac.ebi.pride.data.model.CvParam> modificationSet = submissionSummary.getProjectMetaData().getModifications();
    if (modificationSet == null) {
      modificationSet = new HashSet<>();
    }
    if (modificationSet.isEmpty()) {
      // maybe we are dealing with a complete submission and the modifications have not been gathered at project level
      // so we are looking at the per result file sample data level
      for (DataFile dataFile : submissionSummary.getDataFiles()) {
        if (dataFile.getSampleMetaData() != null) {
          Set<uk.ac.ebi.pride.data.model.CvParam> mods = dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.MODIFICATION);
          if (mods != null) {
            modificationSet.addAll(mods);
          }
        }
      }
    }
    // we should have modifications by now, we will continue with a warning if the modification is not available
    if(modificationSet.isEmpty()){
      logger.warn("Modification annotation is mandatory in submission.px, however it was not found!");
      list.getCvParam().add(createCvParam("MS:1002864", "", "No PTMs are included in the dataset", MS_CV));
    }else{
      for (uk.ac.ebi.pride.data.model.CvParam cvParam : modificationSet) {
        // check if we have PSI-MOD or UNIMOD ontology terms
        if (cvParam.getCvLabel().equalsIgnoreCase("psi-mod") || cvParam.getCvLabel().equalsIgnoreCase("mod")) {
          list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), MOD_CV));
        } else if (cvParam.getCvLabel().equalsIgnoreCase("unimod")) {
          list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), UNIMOD_CV));
        } else if (cvParam.getCvLabel().equalsIgnoreCase("ms") && cvParam.getAccession().equalsIgnoreCase("MS:1001460")) {
          list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), MS_CV));
        } else if (modificationSet.size()==1 && cvParam.getCvLabel().equalsIgnoreCase("pride") && cvParam.getAccession().equalsIgnoreCase("PRIDE:0000398")) {
          list.getCvParam().add(createCvParam("MS:1002864", cvParam.getValue(), cvParam.getName(), MS_CV)); // transformed to PSI-MS CV Param
        }  else if (modificationSet.size() > 1 && cvParam.getCvLabel().equalsIgnoreCase("pride") && cvParam.getAccession().equalsIgnoreCase("PRIDE:0000398")) {
          continue; // skip "No PTMs reported in the dataset" if there are other modifications listed
        } else {
          // That should never happen, since the validation pipeline should have checked this before.
          String msg = "Found unknown modification CV: " + cvParam.getCvLabel();
          logger.error(msg);
          throw new IllegalStateException(msg);
        }
      }
    }
    return list;
  }

  /**
   * Convert a uk.ac.ebi.pride.data.model.CvParam to a uk.ac.ebi.pride.archive.px.model.CvParam.
   * @param cvParam The CV Parameter to convert
   * @return The converted CV Parameter
   */
  private CvParam convertCvParam(uk.ac.ebi.pride.data.model.CvParam cvParam) {
    if (cvParam.getCvLabel().trim().equalsIgnoreCase(MS_CV.getId()) || cvParam.getCvLabel().trim().equalsIgnoreCase("PSI-MS")) {
      return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), MS_CV);
    } else if (cvParam.getCvLabel().trim().equalsIgnoreCase(UNIMOD_CV.getId())) {
      return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), UNIMOD_CV);
    } else if (cvParam.getCvLabel().trim().equalsIgnoreCase(MOD_CV.getId())) {
      return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), MOD_CV);
    } else {
      throw new IllegalArgumentException("Not a valid CV :" + cvParam.getCvLabel() + "! PX XML only supports the following CVs: MS, MOD, UNIMOD.");
    }
  }

  /**
   * Creates a list of files for the dataset
   * @param submissionSummary the submission summary object of the project
   * @param datasetPathFragment the path fragment
   * @return DatasetFileList.
   */
  @Override
  protected DatasetFileList createDatasetFileList(Submission submissionSummary, String datasetPathFragment) {
    DatasetFileList list = new DatasetFileList();
    // create a link to the public FTP location for each file of the dataset
    for (DataFile dataFile : submissionSummary.getDataFiles()) {
      DatasetFile df = new DatasetFile();
      CvParam extraUrlLink = null;
      df.setId("FILE_"+dataFile.getFileId()); // ID to uniquely identify the DatasetFile
      String fileName = dataFile.getFile().getName();
      df.setName(fileName);
      String fileUri = FTP + "/" + datasetPathFragment + "/" + fileName;
      CvParam fileParam;
      Set<String> allowedAltDomains = new HashSet<>();
      allowedAltDomains.add(FRED_LAVANDER_LAB_SWE);
      switch (dataFile.getFileType()) {
        case RAW    : fileParam = createCvParam("MS:1002846", fileUri, "Associated raw file URI", MS_CV);
          if (dataFile.getUrl()!=null && dataFile.getUrl().toString().trim().length()>0) {
            try {
              URI uri = new URI(dataFile.getUrl().toString().trim());
              String domain = uri.getHost();
              if (allowedAltDomains.contains(domain)) {
                extraUrlLink =  createCvParam("MS:1002859", dataFile.getUrl().toString().trim(), "Additional associated raw file URI", MS_CV);
              } else {
                logger.error("Alternative URL's domain not allowed: " + domain);                                        }
            } catch (URISyntaxException urise) {
              logger.error("Error checking alternative URL: " +  dataFile.getUrl().toString().trim());
              logger.error(urise.toString());
            }
          }
          break;
        case RESULT : fileParam = createCvParam("MS:1002848", fileUri, "Result file URI", MS_CV);
          break;
        case SEARCH : fileParam = createCvParam("MS:1002849", fileUri, "Search engine output file URI", MS_CV);
          break;
        case PEAK   : fileParam = createCvParam("MS:1002850", fileUri, "Peak list file URI", MS_CV);
          break;
        case GEL   : fileParam = createCvParam("MS:1002860", fileUri, "Gel image file URI", MS_CV);
          break;
        case OTHER  : fileParam = createCvParam("MS:1002851", fileUri, "Other type file URI", MS_CV);
          break;
        default     : fileParam = createCvParam("MS:1002845", fileUri, "Associated file URI", MS_CV);
          break;
      }
      df.getCvParam().add(fileParam);
      if (extraUrlLink!=null) {
        df.getCvParam().add(extraUrlLink);
      }
      // ToDo (imminently): extra filetype support for 'fasta' and 'spectrum library'
      // ToDo (future): calculate and add checksum for file
      list.getDatasetFile().add(df);
    }
    return list;
  }

  /**
   * The DatasetOriginList, at the moment, it is hardcoded, all are new submissions who's origin is in the PRIDE PX repository
   * @return DatasetOriginList.
   */
  @Override
  protected DatasetOriginList getDatasetOriginList() {
    DatasetOriginList list = new DatasetOriginList();
    CvParam cvParam = new CvParam();
    cvParam.setAccession("MS:1002868");
    cvParam.setName("Original data");
    cvParam.setCvRef(MS_CV);
    DatasetOrigin prideOrigin = new DatasetOrigin();
    prideOrigin.getCvParam().add(cvParam);
    list.setDatasetOrigin(prideOrigin);
    return list;
  }

  /**
   * Helper method to return full DatasetLink with FTP location of the dataset
   * @param datasetPathFragment the path fragment
   * @param pxAccession the PX accession number
   * @return
   */
  @Override
  protected FullDatasetLinkList createFullDatasetLinkList(String datasetPathFragment, String pxAccession)  {
    FullDatasetLinkList fullDatasetLinkList = new FullDatasetLinkList();
    FullDatasetLink prideFtpLink = new FullDatasetLink();
    CvParam ftpParam = createCvParam("MS:1002852", FTP + "/" + datasetPathFragment, "Dataset FTP location", MS_CV);
    prideFtpLink.setCvParam(ftpParam);
    FullDatasetLink prideRepoLink = new FullDatasetLink();
    CvParam repoParam = createCvParam("MS:1001930", PRIDE_REPO_PROJECT_BASE_URL + pxAccession, "PRIDE project URI", MS_CV);
    prideRepoLink.setCvParam(repoParam);
    fullDatasetLinkList.getFullDatasetLink().add(prideFtpLink);
    fullDatasetLinkList.getFullDatasetLink().add(prideRepoLink);
    return fullDatasetLinkList;
  }

  /**
   *  Helper method to create RepositorySupportType for either complete or partial submissions (other types are currently not supported and will return null).
   * @param type the submission type
   * @return RepositorySupportType
   */
  @Override
  protected RepositorySupportType createRepositorySupport(SubmissionType type) {
    RepositorySupportType repositorySupport = new RepositorySupportType();
    CvParam cvparam;
    if (type == SubmissionType.COMPLETE) {
      cvparam = createCvParam("MS:1002856", null, "Supported dataset by repository", MS_CV);
    } else if (type == SubmissionType.PARTIAL) {
      cvparam = createCvParam("MS:1002857", null, "Unsupported dataset by repository", MS_CV);
    } else {
      logger.error("Encoutered unexpected submission type: " + type.name());
      return null;
    }
    repositorySupport.setCvParam(cvparam);
    return repositorySupport;
  }

  /**
   * Helper method to create a ReviewLevelType, either peer-reviewed or non-peer-reviewed
   * @param peerReviewed peer reviewed dataet, or not.
   * @return ReviewLevelType.
   */
  @Override
  protected ReviewLevelType createReviewLevel(boolean peerReviewed) {
    ReviewLevelType reviewLevel = new ReviewLevelType();
    CvParam cvparam ;
    if (peerReviewed) {
      cvparam = createCvParam("MS:1002854", null, "Peer-reviewed dataset", MS_CV);
    } else {
      cvparam = createCvParam("MS:1002855", null, "Non peer-reviewed dataset", MS_CV);
    }
    reviewLevel.setCvParam(cvparam);
    return reviewLevel;
  }

}
