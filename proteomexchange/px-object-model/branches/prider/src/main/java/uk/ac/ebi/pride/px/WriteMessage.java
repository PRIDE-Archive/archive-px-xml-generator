package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.pubmed.PubMedFetcher;
import uk.ac.ebi.pride.pubmed.model.PubMedSummary;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.xml.PxMarshaller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dani Rios
 * @author Jose A. Dianes (PRIDE-R updates and refactoring)
 * @version $Id$
 *
 */
public class WriteMessage {

    private static final Logger logger = LoggerFactory.getLogger(WriteMessage.class);
    private static final String FORMAT_VERSION = "1.0.0";
    private static final String DOI_PREFFIX = "10.6019";
    private static final String NCBI_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final String FTP = "ftp://ftp.pride.ebi.ac.uk";
    private static final String PRIDE_REPO_PROJECT_BASE_URL = "http://www.ebi.ac.uk/pride/repo/projects/";
    // ToDo: check PXST summary file definition with regards to PARTIAL/COMPLETE differences
    // ToDo (general): extract CV params to global util package?
    // ToDo (general): perhaps change to non-static implementation and keep certain data in the instance (px accession, datasetPathFragment, counters...)


    public WriteMessage() {

    }

    @Deprecated
    public File createXMLMessage(String pxAccession, File directory, File submissionFile) throws IOException, SubmissionFileException {
        return createXMLMessage(pxAccession, directory, submissionFile, Calendar.getInstance().getTime());
    }

    @Deprecated
    public File createXMLMessage(String pxAccession, File directory, File submissionFile, Date publicationDate) throws IOException, SubmissionFileException {
        return createXMLMessage(pxAccession, directory, submissionFile, publicationDate, null);
    }

    /**
     * the pxSummaryLocation will indicate where in the filesystem is stored the summary file to extract some of
     * the information
     *
     * @param pxAccession the PX accession number of the submission.
     * @param directory the output directory, where to generate the PX XML file.
     * @param submissionFile the submission summary file of the submission.
     * @param changeLog a change log message.
     * @return The File reference to the PX XML file generated.
     * @throws IOException
     * @throws SubmissionFileException
     */
    @Deprecated
    public File createXMLMessage(String pxAccession, File directory, File submissionFile, Date publicationDate, String changeLog) throws IOException, SubmissionFileException {
        //first, extract submission file object
        if (!submissionFile.isFile() || !submissionFile.exists()) {
            throw new IllegalArgumentException("No submission file in " + submissionFile.getAbsolutePath());
        }

        String fileName = directory.getAbsolutePath() + File.separator + pxAccession + ".xml";
        File file = new File(fileName);
        FileWriter fw = new FileWriter(file);

        ProteomeXchangeDataset proteomeXchangeDataset = createProteomeXchangeDataset(pxAccession, submissionFile, publicationDate, changeLog);

        //and marshal it
        new PxMarshaller().marshall(proteomeXchangeDataset, fw);

        return file;
    }


    public File createIntialPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment) throws SubmissionFileException, IOException {
        // the submission summary file has to exist
        if (!submissionSummaryFile.isFile() || !submissionSummaryFile.exists()) {
            throw new IllegalArgumentException("No submission file in " + submissionSummaryFile.getAbsolutePath());
        }
        // we need to be able to parse the submission summary file (we throw on the exception if there is any)
        Submission submissionSummary = SubmissionFileParser.parse(submissionSummaryFile);

        // the output directory has to exist (or we need to be able to create it)
        // ToDo: perhaps expect the file to write to as argument instead of creating it
        if (!outputDirectory.isDirectory()) {
            // the output directory may not exist yet
            logger.info("PX XML output directory does not exist, attempt to create: " + outputDirectory);
            boolean success = outputDirectory.mkdirs();
            if (!success) {
                logger.error("Could not create output directory: " + outputDirectory);
            }
        }


        File file = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            logger.debug("Creating PX XML file: " + file.getAbsolutePath());

            ProteomeXchangeDataset proteomeXchangeDataset = createPxXml(submissionSummary, pxAccession, datasetPathFragment);

            // write out the ProteomeXchangeDataset object to the specified file
            new PxMarshaller().marshall(proteomeXchangeDataset, fw);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
        logger.info("PX XML file generated: " + file.getAbsolutePath());
        return file;
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
    private ProteomeXchangeDataset createPxXml(Submission submissionSummary, String pxAccession, String datasetPathFragment) {
        // some parameter checks...
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
        // we assume that the submission summary is OK, it would be too much to check it properly here


        // now get on with the real business
        ProteomeXchangeDataset pxXml = new ProteomeXchangeDataset();
        //and add the attributes
        pxXml.setId(pxAccession);
        pxXml.setFormatVersion(FORMAT_VERSION);

        // ToDo: move to version 1.1.0
        // ToDo (for version 1.1.0): add CV list!

        // no change log, since initial PX XML generation

        // extract DatasetSummary (
        DatasetSummary datasetSummary = getDatasetSummary(submissionSummary);
        pxXml.setDatasetSummary(datasetSummary);

        // add the DatasetIdentifier (add a DOI record for complete submissions)
        // ToDo: we assume a DOI was registered if the submission type is COMPLETE. Perhaps we should check (e.g. DOI provided as parameter)?
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

        // ToDo: RepositoryRecordList omitted due to missing information
        // add the repository record list (optional XML element)
//        RepositoryRecordList repositoryRecordList = createRepositoryRecordList(submissionSummary, pxAccession);
//        pxXml.setRepositoryRecordList(repositoryRecordList);

        return  pxXml;
    }

    private ProteomeXchangeDataset replacePrimaryReference(ProteomeXchangeDataset pxXml, Long pmid) {
        Assert.notNull(pxXml, "The PX XML object cannot be null!");
        Assert.notNull(pmid, "The PMID for the publication cannot be null!");
        // we remove the old entry (which we assume to be 'publication pending')
        pxXml.getPublicationList().getPublication().clear();
        // and add a new record
        pxXml.getPublicationList().getPublication().add(getPublication(pmid));
        // add a change log entry
        addChangeLogEntry(pxXml, "Replaced publication reference for PubMed record: " + pmid);

        return pxXml;
    }
    private ProteomeXchangeDataset replacePrimaryReference(ProteomeXchangeDataset pxXml, String refLine) {
        Assert.notNull(pxXml, "The PX XML object cannot be null!");
        Assert.notNull(refLine, "The ref line for the publication cannot be null!");
        // we remove any old entry (which we assume to be out-dated)
        pxXml.getPublicationList().getPublication().clear();
        // and add the new record
        pxXml.getPublicationList().getPublication().add(getPublication(refLine));
        // add a change log entry
        addChangeLogEntry(pxXml, "Replaced publication reference with ref line: " + refLine);

        return pxXml;
    }

    @Deprecated
    private ProteomeXchangeDataset createProteomeXchangeDataset(String projectAccession, File submissionFile, Date publicationDate, String changeLog) throws SubmissionFileException {
        Submission submissionSummary = SubmissionFileParser.parse(submissionFile);

        ProteomeXchangeDataset proteomeXchangeDataset = new ProteomeXchangeDataset();

        //extract DatasetSummary: this information will always come from Summary object
        DatasetSummary datasetSummary = getDatasetSummary(submissionSummary);
        proteomeXchangeDataset.setDatasetSummary(datasetSummary);

        // extract contacts from summary file
        ContactList contactList = getContactList(submissionSummary);
        proteomeXchangeDataset.setContactList(contactList);

        //extract Keyword List from file
        KeywordList keywordList = getKeywordList(submissionSummary);
        proteomeXchangeDataset.setKeywordList(keywordList);

        //add FTP DatasetLink


//        FullDatasetLinkList fullDatasetLinkList = createFTPDatasetLink(publicationDate, projectAccession);
//        proteomeXchangeDataset.setFullDatasetLinkList(fullDatasetLinkList);

        //add DatasetIdentifier
//        DatasetIdentifierList datasetIdentifierList = getDatasetIdentifierList(submissionSummary, projectAccession);
//        proteomeXchangeDataset.setDatasetIdentifierList(datasetIdentifierList);

        //the dataset will in most cases not yet carry a publication reference
        PublicationList publicationList = getPublicationList(submissionSummary);
        if ( publicationList != null ) {
            proteomeXchangeDataset.setPublicationList(publicationList);
        }

        // populate dataset
        //will return if submission contains only supported files
        //to extract info from database or not supported files
        //and extract info from the metadata file
        SubmissionType type = submissionSummary.getProjectMetaData().getSubmissionType();

        // ToDo: check what is the difference between the two cases!
        if (type != SubmissionType.COMPLETE) {
            populatePxSubmissionFromFile(proteomeXchangeDataset, submissionSummary, projectAccession);
            //not relevant now, maybe in the future will be added PrideInspector URL
        } else {
            //if it is supported, need to add prideInspectorURL to datasetLink
            // ToDo: do we need to generate the PX XML from the DB?? This should be possible only from the PX submission summary file!
            // ToDo: investigate and change (if possible)!
//            populatePxSubmissionFromDB(proteomeXchangeDataset, projectAccession);

        }
        //and add the attributes
        proteomeXchangeDataset.setId(projectAccession);
        proteomeXchangeDataset.setFormatVersion(FORMAT_VERSION);

        // add change log if there is any
        // ToDo: change this? (the initial PX XML will not have a change log, only subsequent updates will have, and those should probably handled by a separate 'update' method
        if (changeLog != null) {
            addChangeLogEntry(proteomeXchangeDataset, changeLog);
        }

        return proteomeXchangeDataset;
    }

    private static boolean isValidPXAccession(String pxAccession) {
        Pattern p = Pattern.compile("PX[D|T]\\d{6}");
        Matcher m = p.matcher(pxAccession);
        if (m.matches()) {
            logger.debug("PX identifier valid: " + pxAccession);
            return true;
        } else {
            logger.info("PX identifier not valid: " + pxAccession);
            return false;
        }
    }

    private static boolean isValidPathFragment(String datasetPathFragment, String pxAccession) {
        Pattern p = Pattern.compile("/201./[0,1][0-9]/"+pxAccession+"/");
        Matcher m = p.matcher(datasetPathFragment);
        if (!m.matches()) {
            logger.info("The dataset path fragment '" + datasetPathFragment + "' is not valid for PX accession: " + pxAccession );
            return false;
        }

        return true;
    }

    private static void addChangeLogEntry(ProteomeXchangeDataset pxXML, String message) {
        // create a new change log entry for the provided message
        ChangeLogEntryType entry = new ChangeLogEntryType();
        entry.setValue(message);
        entry.setDate(Calendar.getInstance());

        // if there is not already a change log, then create a new one
        ChangeLogType changeLog = pxXML.getChangeLog();
        if (changeLog == null) {
            changeLog = new ChangeLogType();
            pxXML.setChangeLog(changeLog);
        }

        // finally add the new log entry to the change log
        changeLog.getChangeLogEntry().add(entry);
    }

    // there shoud always be a publication list, but it may have records to say 'no reference' or 'reference pending'
    private static PublicationList getPublicationList(Submission submissionSummary) {
        PublicationList list = new PublicationList();

        Set<String> pubmedIDs = submissionSummary.getProjectMetaData().getPubmedIds();

        if ( pubmedIDs == null || pubmedIDs.size() < 1 ) { // no publications
            // no pubmed ID, so no publication, we assume it is pending
            Publication publication = new Publication();
            CvParam cvParam = new CvParam();
            cvParam.setCvRef("PRIDE");
            cvParam.setName("Dataset with its publication pending");
            cvParam.setAccession("PRIDE:0000432");
            publication.setId("pending");
            publication.getCvParam().add(cvParam);
            list.getPublication().add(publication);
        } else { // we have already publications
            if (pubmedIDs.size() > 1) {
                // ToDo: why can there be more than one pubmed IDs and how should we treat them? (in PX there should be one reference per dataset)
                logger.error("More than one PMID found on dataset! This will cause problems...");
            }
            for (String pubmedID : pubmedIDs) {
                Long pmid = Long.parseLong(pubmedID);
                list.getPublication().add(getPublication(pmid));
            }
        }

        return  list;
    }

    private static Publication getPublication(String refLine) {
        if (refLine == null) {
            throw new IllegalArgumentException("No ref line provided!");
        }

        Publication publication = new Publication();
        publication.setId("PUBLICATION"); // ToDo: this should be unique!
        publication.getCvParam().add(createCvParam("PRIDE:0000400", refLine, "Reference", "PRIDE"));
        return publication;
    }
    private static Publication getPublication(Long pmid) {
        if (pmid == null) {
            throw new IllegalArgumentException("No PMID provided!");
        }

        Publication publication = new Publication();

        // add the PMID
        publication.setId("PMID" + pmid);
        publication.getCvParam().add(createCvParam("MS:1000879", pmid.toString(), "PubMed identifier", "MS"));

        // try to get the ref line using an external service
        String refLine;
            try {
                PubMedFetcher pubMedFetcher = new PubMedFetcher(NCBI_URL);
                PubMedSummary pubMedSummary = pubMedFetcher.getPubMedSummary(pmid.toString());
                refLine = pubMedSummary.getReference();
            } catch (IOException e) {
                logger.error("Problems getting reference line from PubMed " + e.getMessage());
                refLine = ""; // ToDo: specify a better default value?
            }
        // ToDo: is there no MS term for this? Is this the cv param we are supposed to use?
        publication.getCvParam().add(createCvParam("PRIDE:0000400", refLine, "Reference", "PRIDE"));
        return publication;
    }

    /**
     * method to retrieve keyword list from the summary file
     *
     * @param submissionSummary  the object representing the PX submission summary file content.
     * @return a KeywordList with all the keywords mentioned in the submission summary file.
     */
    private static KeywordList getKeywordList(Submission submissionSummary) {
        KeywordList keywordList = new KeywordList();
        keywordList.getCvParam().add(createCvParam("MS:1001925", submissionSummary.getProjectMetaData().getKeywords(), "submitter keyword", "MS"));
        return keywordList;
    }

    // method to populate all information in the proteomeXchange dataset from the
    // summary file
    @Deprecated
    private static void populatePxSubmissionFromFile(ProteomeXchangeDataset proteomeXchangeDataset, Submission submissionSummary, String projectAccession) {
        //add Dataset origin info (this is constant right now: PRIDE)
        DatasetOriginList datasetOriginList = getDatasetOriginList();
        proteomeXchangeDataset.setDatasetOriginList(datasetOriginList);

        //add species from file
        SpeciesList speciesList = getSpeciesList(submissionSummary);
        proteomeXchangeDataset.setSpeciesList(speciesList);

        //add instrument from file
        InstrumentList instrumentList = getInstrumentList(submissionSummary);
        proteomeXchangeDataset.setInstrumentList(instrumentList);

        //add modification
        ModificationList modificationList = getModificationList(submissionSummary);
        proteomeXchangeDataset.setModificationList(modificationList);

//        //add pubmed information, if present
//        PublicationList publicationList = new PublicationList();
//        if (submissionSummary.getMetaData().hasPubmedIds()) {
//            publicationList.getPublication().addAll(getPublicationParams(submissionSummary));
//        }
//        //if there is no publication, add the special no publication param
//        else {
//            CvParam cvParam = new CvParam();
//            cvParam.setCvRef("PRIDE");
//            cvParam.setName("Dataset with no associated published manuscript");
//            cvParam.setAccession("PRIDE:0000412");
//            Publication publication = new Publication();
//            publication.setId("PUB1");
//            publication.getCvParam().add(cvParam);
//            publicationList.getPublication().add(publication);
//        }
//        proteomeXchangeDataset.setPublicationList(publicationList);
    }

    // method to extract modifications from summary file
    private static ModificationList getModificationList(Submission submissionSummary) {
        ModificationList list = new ModificationList();

        // ToDo: take new PX summary format into account (sample meta-data section)? mods currently not required!
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getProjectMetaData().getModifications()) {
            list.getCvParam().add(convertCvParam(cvParam));
        }

        return list;
    }

    // method to extract instrument information from summary file
    private static InstrumentList getInstrumentList(Submission submissionSummary) {
        InstrumentList list = new InstrumentList();

        int instrumentNum = 1; // artificial counter to give each instrument a unique id
        // ToDo: take new PX summary format into account (sample meta-data section)?
        for (uk.ac.ebi.pride.data.model.CvParam auxInstrument : submissionSummary.getProjectMetaData().getInstruments()) {
            Instrument instrument = new Instrument();
            instrument.setId("Instrument_" + instrumentNum++);
            instrument.getCvParam().add(convertCvParam(auxInstrument));
            list.getInstrument().add(instrument);
        }

        return list;
    }

    // method to get Species information from summary file
    private static SpeciesList getSpeciesList(Submission submissionSummary) {
        SpeciesList list = new SpeciesList();

        // ToDo: take new PX summary format into account (sample meta-data section)?
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getProjectMetaData().getSpecies()) {
            Species species = new Species();
            // PX guidelines state that each species has to be represented with two CV parameters: one for the name and one for the taxonomy ID
            species.getCvParam().add(createCvParam("MS:1001469", cvParam.getName(), "taxonomy: scientific name", "MS"));
            species.getCvParam().add(createCvParam("MS:1001467", cvParam.getAccession(), "taxonomy: NCBI TaxID", "MS"));
            // ToDo: WARNING! this will change! The PX XML will allow multiple species, so this species will have to be added to a list as soon as the model is updated! Now we'll overwrite in case of multiple species!
            list.setSpecies(species);
        }

        return list;
    }

    // method to add Dataset identifier information
    // ToDo: take submissions into account that refer to previous datasets/submissions
    private static DatasetIdentifierList getDatasetIdentifierList(String projectAccession, boolean withDOI) {
        DatasetIdentifierList datasetIdentifierList = new DatasetIdentifierList();

        // add the PX accession
        DatasetIdentifier px = new DatasetIdentifier();
        px.getCvParam().add(createCvParam("MS:1001919", projectAccession, "ProteomeXchange accession number", "MS"));
        datasetIdentifierList.getDatasetIdentifier().add(px);

        // add a corresponding DOI record if requested
        if (withDOI) {
            DatasetIdentifier DOI = new DatasetIdentifier();
            DOI.getCvParam().add(createCvParam("MS:1001922", DOI_PREFFIX + "/" + projectAccession, "Digital Object Identifier (DOI)", "MS"));
            datasetIdentifierList.getDatasetIdentifier().add(DOI);
        }

        return datasetIdentifierList;
    }

    private static CvParam createCvParam(String accession, String value, String name, String cvRef) {

        CvParam cvParam = new CvParam();
        cvParam.setAccession(accession);
        cvParam.setValue(value);
        cvParam.setName(name);
        cvParam.setCvRef(cvRef);

        return cvParam;
    }
    private static CvParam convertCvParam(uk.ac.ebi.pride.data.model.CvParam cvParam) {
        return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), cvParam.getCvLabel());
    }

    private static DatasetFileList createDatasetFileList(Submission submissionSummary, String datasetPathFragment) {
        DatasetFileList list = new DatasetFileList();
        // create a link to the public FTP location for each file of the dataset
        int cnt = 1;
        for (DataFile dataFile : submissionSummary.getDataFiles()) {
            DatasetFile df = new DatasetFile();
            df.setId("FILE_"+cnt++); // artificial ID to uniquely identify the DatasetFile
            String fileName = dataFile.getFile().getName();
            df.setName(fileName);
            String fileUri = FTP + datasetPathFragment + fileName;
            CvParam param = createCvParam("PRIDE:0000403", fileUri, "Associated file URI", "MS");
            df.getCvParam().add(param);
            // ToDo (future): calculate and add checksum for file
            list.getDatasetFile().add(df);
        }

        return list;
    }

    private RepositoryRecordList createRepositoryRecordList(Submission submissionSummary, String pxAccession) {
        RepositoryRecordList list = new RepositoryRecordList();

        // create a PRIDE repository link for the whole project
        RepositoryRecord record = new RepositoryRecord();
        record.setRepositoryID(HostingRepositoryType.PRIDE);
        record.setUri(PRIDE_REPO_PROJECT_BASE_URL + pxAccession);
        record.setLabel("PRIDE project");
        record.setName(submissionSummary.getProjectMetaData().getTitle());
        record.setRecordID(pxAccession);

        // ToDo: instrument ref is mandatory, but on project level we may have more than one...

        // ToDo: there is no sample information in the current PX summary file, take the new format into account!

        // ToDo: add list of modifications

        list.getRepositoryRecord().add(record);


        // ToDo (future): create a PRIDE repository link for each assay of the project?

        return list;
    }

    // the DatasetOriginList, at the moment, it is hardcoded, all are new submissions who's origin is in the PRIDE PX repository
    private static DatasetOriginList getDatasetOriginList() {
        DatasetOriginList list = new DatasetOriginList();

        CvParam cvParam = new CvParam();
        cvParam.setAccession("PRIDE:0000402");
        cvParam.setName("Original data");
        cvParam.setCvRef("PRIDE");
        DatasetOrigin prideOrigin = new DatasetOrigin();
        prideOrigin.getCvParam().add(cvParam);
        list.setDatasetOrigin(prideOrigin);

        return list;
    }

    // helper method to return full DatasetLink with FTP location of the dataset
    private static FullDatasetLinkList createFullDatasetLinkList(String datasetPathFragment, String pxAccession)  {
        FullDatasetLinkList fullDatasetLinkList = new FullDatasetLinkList();
        FullDatasetLink prideFtpLink = new FullDatasetLink();
        CvParam ftpParam = createCvParam("PRIDE:0000411", FTP + datasetPathFragment, "Dataset FTP location", "PRIDE");
        prideFtpLink.setCvParam(ftpParam);

        FullDatasetLink prideRepoLink = new FullDatasetLink();
        CvParam repoParam = createCvParam("MS:1001930", PRIDE_REPO_PROJECT_BASE_URL + pxAccession, "PRIDE project URI", "MS");
        prideRepoLink.setCvParam(repoParam);

        fullDatasetLinkList.getFullDatasetLink().add(prideFtpLink);
        fullDatasetLinkList.getFullDatasetLink().add(prideRepoLink);
        return fullDatasetLinkList;
    }

    //this information will come from the summary file
    private static DatasetSummary getDatasetSummary(Submission submissionSummary) {

        DatasetSummary datasetSummary = new DatasetSummary();
        datasetSummary.setTitle(submissionSummary.getProjectMetaData().getTitle());
        datasetSummary.setDescription(submissionSummary.getProjectMetaData().getProjectDescription());
        datasetSummary.setAnnounceDate(Calendar.getInstance());
        datasetSummary.setHostingRepository(HostingRepositoryType.PRIDE);

        // we assume a peer reviewed case be default!
        ReviewLevelType reviewLevelType = createReviewLevel(true);
        datasetSummary.setReviewLevel(reviewLevelType);

        // add Repository Support level, depending if files are supported or not
        RepositorySupportType repositorySupportType = createRepositorySupport(submissionSummary.getProjectMetaData().getSubmissionType());
        datasetSummary.setRepositorySupport(repositorySupportType);

        return datasetSummary;
    }

    // helper method to create RepositorySupportType for either complete or partial submissions
    // (other types are currently not supported and will return null)
    private static RepositorySupportType createRepositorySupport(SubmissionType type) {
        RepositorySupportType repositorySupport = new RepositorySupportType();
        CvParam cvparam;

        if (type == SubmissionType.COMPLETE) {
            cvparam = createCvParam("PRIDE:0000416", null, "Supported dataset by repository", "PRIDE");
        } else if (type == SubmissionType.PARTIAL) {
            cvparam = createCvParam("PRIDE:0000417", null, "Unsupported dataset by repository", "PRIDE");
        } else {
            logger.error("Encoutered unexpected submission type: " + type.name());
            return null;
        }
        repositorySupport.setCvParam(cvparam);

        return repositorySupport;
    }

    //helper method to create a ReviewLevelType, either peer-reviewed or non-peer-reviewed
    private static ReviewLevelType createReviewLevel(boolean peerReviewed) {
        ReviewLevelType reviewLevel = new ReviewLevelType();

        CvParam cvparam ;
        if (peerReviewed) {
            cvparam = createCvParam("PRIDE:0000414", null, "Peer-reviewed dataset", "PRIDE");
        } else {
            cvparam = createCvParam("PRIDE:0000415", null, "Non peer-reviewed dataset", "PRIDE");
        }
        reviewLevel.setCvParam(cvparam);

        return reviewLevel;
    }

    //private method to extract the contact list from the summary file
    private ContactList getContactList(Submission submissionSummary) {
        ContactList list = new ContactList();

        //handle the primary contact
        uk.ac.ebi.pride.data.model.Contact aux = submissionSummary.getProjectMetaData().getContact();
        Contact submitter = new Contact();
        submitter.setId("project_contact"); // assign a unique ID to this contact
        submitter.getCvParam().add(createCvParam("MS:1000586", aux.getName(), "contact name", "MS"));
        submitter.getCvParam().add(createCvParam("MS:1000589", aux.getEmail(), "contact email", "MS"));
        submitter.getCvParam().add(createCvParam("MS:1000590", aux.getAffiliation(), "contact affiliation", "MS"));
        list.getContact().add(submitter);

        // ToDo: this will have to be updated to include the 'Lab Head' once the new version of the PX XML is used

        return list;
    }

}
