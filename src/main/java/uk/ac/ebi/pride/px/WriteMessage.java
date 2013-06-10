package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.*;
import uk.ac.ebi.pride.prider.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.model.Contact;
import uk.ac.ebi.pride.px.model.CvParam;
import uk.ac.ebi.pride.px.xml.PxMarshaller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Dani Rios
 * @author Jose A. Dianes (PRIDE-R updates and refactoring)
 * @version $Id$
 *
 */
public class WriteMessage {
    private static DBController dbac;
    private static PxMarshaller marshaller;

    private static final String FORMAT_VERSION = "1.0.0";
    private static final String DOI_PREFFIX = "10.6019";
    private static final String NCBI_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";
    private static final String FTP = "ftp://ftp.pride.ebi.ac.uk";
    private static final String PRIVATE_DIR = "/nfs/pride/private/data";

    private static final Logger logger = LoggerFactory.getLogger(WriteMessage.class);


    //this list will store the contact emails present in the file, so we don't add them again from DB
    private static Set<String> contactEmails = new HashSet<String>();

    /**
     *
     * @param dbController
     */
    public WriteMessage(DBController dbController) {
        dbac = dbController;
    }

    /**
     * * Method overloading, this method is for the first time when a PX xml is created
     *
     * @param projectAccession
     * @param directory
     * @param submissionFile
     * @return
     * @throws IOException
     * @throws SubmissionFileException
     * @throws SQLException
     */
    public File createXMLMessage(String projectAccession, File directory, File submissionFile) throws IOException, SubmissionFileException, SQLException {
        return createXMLMessage(projectAccession, directory, submissionFile, null);
    }

    /**
     * the pxSummaryLocation will indicate where in the filesystem is stored the summary file to extract some of
     * the information
     *
     * @param pxAccession
     * @param directory
     * @param submissionFile
     * @param changeLog
     * @return
     * @throws IOException
     * @throws SubmissionFileException
     * @throws SQLException
     */
    public File createXMLMessage(String pxAccession, File directory, File submissionFile, String changeLog) throws IOException, SubmissionFileException, SQLException {
        //first, extract submission file object
        if (!submissionFile.isFile() || !submissionFile.exists()) {
            throw new IllegalArgumentException("No submission file in " + submissionFile.getAbsolutePath());
        }

        File file = new File(directory.getAbsolutePath() + File.separator + pxAccession + ".xml");
        FileWriter fw = new FileWriter(file);

        marshaller = new PxMarshaller();
        ProteomeXchangeDataset proteomeXchangeDataset = createProteomeXchangeDataset(pxAccession, submissionFile, changeLog);
        //and marshal it
        marshaller.marshall(proteomeXchangeDataset, fw);

        return file;
    }

    private ProteomeXchangeDataset createProteomeXchangeDataset(String projectAccession, File submissionFile, String changeLog) throws SubmissionFileException, SQLException {
        Submission submissionSummary = SubmissionFileParser.parse(submissionFile);

        ProteomeXchangeDataset proteomeXchangeDataset = new ProteomeXchangeDataset();
        //extract DatasetSummary: this information will always come from Summary object
        DatasetSummary datasetSummary = getDatasetSummary(submissionSummary, projectAccession);
        proteomeXchangeDataset.setDatasetSummary(datasetSummary);

        //extract ContactList: this information comes from summary file
        ContactList contactList = getContactList(submissionSummary);
        proteomeXchangeDataset.setContactList(contactList);

        //extract Keyword List from file
        KeywordList keywordList = getKeywordList(submissionSummary);
        proteomeXchangeDataset.setKeywordList(keywordList);

        //add FTP DatasetLink

        FullDatasetLinkList fullDatasetLinkList = createFTPDatasetLink(submissionSummary, projectAccession);

        //add DatasetIdentifier
        DatasetIdentifierList datasetIdentifierList = getDatasetIdentifierList(submissionSummary, projectAccession);
        proteomeXchangeDataset.setDatasetIdentifierList(datasetIdentifierList);
        proteomeXchangeDataset.setFullDatasetLinkList(fullDatasetLinkList);

        //extract publication information from DB
        PublicationList publicationList = dbac.getPublicationList(projectAccession);
        proteomeXchangeDataset.setPublicationList(publicationList);

        // populate dataset
        //will return if submission contains only supported files
        //to extract info from database or not supported files
        //and extract info from the metadata file
        SubmissionType type = submissionSummary.getProjectMetaData().getSubmissionType();
        if (type != SubmissionType.COMPLETE) {
            populatePxSubmissionFromFile(proteomeXchangeDataset, submissionSummary, projectAccession);
            //not relevant now, maybe in the future will be added PrideInspector URL
        } else {
            //if it is supported, need to add prideInspectorURL to datasetLink
            populatePxSubmissionFromDB(proteomeXchangeDataset, projectAccession);

        }
        //and add the attributes
        proteomeXchangeDataset.setId(projectAccession);
        //TODO: format version will always be hardcoded ??
        proteomeXchangeDataset.setFormatVersion(FORMAT_VERSION);

        // add change log if there is any
        if (changeLog != null) {
            ChangeLogType changeLogType = new ChangeLogType();
            ChangeLogEntryType changeLogEntryType = new ChangeLogEntryType();
            changeLogEntryType.setValue(changeLog);
            changeLogEntryType.setDate(Calendar.getInstance());
            changeLogType.getChangeLogEntry().add(changeLogEntryType);
            proteomeXchangeDataset.setChangeLog(changeLogType);
        }

        return proteomeXchangeDataset;
    }

    /**
     * method to retrieve keyword list from the summary file
     *
     * @param submissionSummary
     * @return
     */
    private static KeywordList getKeywordList(Submission submissionSummary) {
        KeywordList keywordList = new KeywordList();
        keywordList.getCvParam().add(createCvParam("MS:1001925", submissionSummary.getProjectMetaData().getKeywords(), "submitter keyword", "MS"));
        return keywordList;
    }

    //method to populate all information in the proteomeXchange dataset from the
    //summary file
    private static void populatePxSubmissionFromFile(ProteomeXchangeDataset proteomeXchangeDataset, Submission submissionSummary, String projectAccession) {
        //add DataSet info, it is constant right now
        DatasetOriginList datasetOriginList = new DatasetOriginList();
        datasetOriginList.setDatasetOrigin(getDatasetOrigin());
        proteomeXchangeDataset.setDatasetOriginList(datasetOriginList);
        //add species from file
        SpeciesList speciesList = new SpeciesList();
        speciesList.setSpecies(getSpecies(submissionSummary));
        proteomeXchangeDataset.setSpeciesList(speciesList);
        //add instrument from file
        InstrumentList instrumentList = new InstrumentList();
        instrumentList.getInstrument().addAll(getInstruments(submissionSummary));
        proteomeXchangeDataset.setInstrumentList(instrumentList);
        //add modification
        ModificationList modificationList = new ModificationList();
        modificationList.getCvParam().addAll(getModificationCvParams(submissionSummary));
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

//    //method to extract Publication information from file
//    private static List<Publication> getPublicationParams(Submission submissionSummary) {
//        List<Publication> publications = new ArrayList<Publication>();
//        PubMedFetcher pubMedFetcher = new PubMedFetcher(NCBI_URL);
//
//        for (String pubmedID : submissionSummary.getMetaData().getPubmedIds()) {
//            Publication publication = new Publication();
//            //add pubmedID
//            publication.setId("PMID" + pubmedID);
//            publication.getCvParam().add(createCvParam("MS:1000879", pubmedID, "PubMed identifier", "MS"));
//            //and the reference
//            //get reference line using external library
//            PubMedSummary pubMedSummary = null;
//            try {
//                pubMedSummary = pubMedFetcher.getPubMedSummary(pubmedID);
//            } catch (IOException e) {
//                logger.error("Problems getting reference line from pubMed " + e.getMessage());
//            }
//            String reference_line = pubMedSummary.getReference();
//            publication.getCvParam().add(createCvParam("PRIDE:0000400", reference_line, "Reference", "PRIDE"));
//            publications.add(publication);
//        }
//        return publications;
//    }

    //method to extract modifications from summary file
    private static List<CvParam> getModificationCvParams(Submission submissionSummary) {
        List<CvParam> cvParams = new ArrayList<CvParam>();
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getProjectMetaData().getModifications()) {
            cvParams.add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), cvParam.getCvLabel()));
        }
        return cvParams;
    }

    //method to extract instrument information from summary file
    private static List<Instrument> getInstruments(Submission submissionSummary) {
        List<Instrument> instruments = new ArrayList<Instrument>();
        int instrumentNum = 1;
        //convert CvParam into px CvParam
        Set<uk.ac.ebi.pride.data.model.CvParam> auxInstruments = submissionSummary.getProjectMetaData().getInstruments();
        for (uk.ac.ebi.pride.data.model.CvParam auxInstrument : auxInstruments) {
            Instrument instrument = new Instrument();
            instrument.setId("Instrument_" + instrumentNum);
            instrumentNum++;
            instrument.getCvParam().add(createCvParam(auxInstrument.getAccession(), auxInstrument.getValue(), auxInstrument.getName(), auxInstrument.getCvLabel()));
            instruments.add(instrument);
        }
        return instruments;
    }

    //method to get Species information from summary file
    private static Species getSpecies(Submission submissionSummary) {
        Species species = new Species();
        //need to create 2 cvParam: one with the NEWT code and one with the name
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getProjectMetaData().getSpecies()) {
            species.getCvParam().add(createCvParam("MS:1001469", cvParam.getName(), "taxonomy: scientific name", "PSI-MS"));
            species.getCvParam().add(createCvParam("MS:1001467", cvParam.getAccession(), "taxonomy: NCBI TaxID", "PSI-MS"));
        }
        return species;
    }

    //method to add Dataset identifier information from file
    //at the moment, let's not worry about PxAccessions, they refer to previous submissions
    private static DatasetIdentifierList getDatasetIdentifierList(Submission submissionSummary, String projectAccession) {
        DatasetIdentifierList datasetIdentifierList = new DatasetIdentifierList();
        //add px number as CvParam
        DatasetIdentifier px = new DatasetIdentifier();
        px.getCvParam().add(createCvParam("MS:1001919", projectAccession, "ProteomeXchange accession number", "MS"));
        datasetIdentifierList.getDatasetIdentifier().add(px);
        //add DOI from if is supported
        SubmissionType type = submissionSummary.getProjectMetaData().getSubmissionType();
        if (type != SubmissionType.COMPLETE) {
            DatasetIdentifier DOI = new DatasetIdentifier();
            //add DOI value
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

    //method will get all information for a specific submission from the database and populate
    //the ProteomeXchangeDataset object with it
    private static void populatePxSubmissionFromDB(ProteomeXchangeDataset proteomeXchangeDataset, String projectAccession) throws SubmissionFileException {
        //get all experiments in the project
        List<Long> assayIds = dbac.getAssayIds(projectAccession);
        if (assayIds.isEmpty()) {
            logger.error("Project contains no experiments");
            System.exit(0);
        }
//        DatasetIdentifier datasetIdentifier = dbac.getDatasetIdentifier(experimentIDs);
//        DatasetIdentifierList datasetIdentifierList = proteomeXchangeDataset.getDatasetIdentifierList();
//        proteomeXchangeDataset.setDatasetIdentifierList(datasetIdentifierList);
        DatasetOriginList datasetOriginList = new DatasetOriginList();
        datasetOriginList.setDatasetOrigin(getDatasetOrigin());
        proteomeXchangeDataset.setDatasetOriginList(datasetOriginList);
        //extract Species information for all experiments
        SpeciesList speciesList = dbac.getSpecies(projectAccession);
        proteomeXchangeDataset.setSpeciesList(speciesList);
        //extract instrument information
        InstrumentList instrumentList = dbac.getInstrumentList(assayIds);
        proteomeXchangeDataset.setInstrumentList(instrumentList);
        //extract modification list
        ModificationList modificationList = dbac.getModificationList(projectAccession);
        proteomeXchangeDataset.setModificationList(modificationList);
        //extract contact list that are not present already in the file
        ContactList newContactList = dbac.getContactList(projectAccession, contactEmails);
        ContactList contactList = proteomeXchangeDataset.getContactList();
        //add contacts from DB
        contactList.getContact().addAll(newContactList.getContact());
        proteomeXchangeDataset.setContactList(contactList);
//        //extract publicationList
//        PublicationList publicationList = dbac.getPublicationList(experimentIDs);
//        proteomeXchangeDataset.setPublicationList(publicationList);
//        KeywordList keywordList = dbac.getKeywordList(experimentIDs);
//        proteomeXchangeDataset.setKeywordList(keywordList)
        //generatate prideInspectorURL
//        FullDatasetLink fullDatasetLink = dbac.generatePrideInspectorURL(experimentIDs);
//        //and add it to list of links
//        fullDatasetLinkList.getFullDatasetLink().add(fullDatasetLink);
//        FullDatasetLinkList datasetLinkList = dbac.getFullDataSetLinkList(experimentIDs);
//        if (!datasetLinkList.getFullDatasetLink().isEmpty()) {
//            //add links to PRIDE experiments to dataset list
//            FullDatasetLinkList fullDatasetLinkList = proteomeXchangeDataset.getFullDatasetLinkList();
//            fullDatasetLinkList.getFullDatasetLink().addAll(datasetLinkList.getFullDatasetLink());
//        }
        //TODO: no DatasetFileList, where are the raw files stored?
        // create RepositoryRecordList with all experiments in project
        RepositoryRecordList repositoryRecordList = new RepositoryRecordList();
        for (long assayId : assayIds) {
            //get new record
            RepositoryRecord repositoryRecord = dbac.getRepositoryRecord(assayId);
            //we now need to add the additional elements to the record: source, publication, instrument, sample, modification
            //TODO; need to deal with source file, where are they ??
            //TODO: an experiment can have more than 1 publication or more than 1 instrument ??
            //add publication ref, all projects will have 1, either no publication or the proper pubmedID
            Ref publicationRef = dbac.getPublicationRef(projectAccession);
            repositoryRecord.getPublicationRef().add(publicationRef);
            Ref instrumentRef = dbac.getInstrumentRef(assayId);
            repositoryRecord.getInstrumentRef().add(instrumentRef);
            SampleList sampleList = dbac.getSampleList(assayId);
            repositoryRecord.getSampleList().add(sampleList);
            //and the modificationList
            modificationList = dbac.getAssayModificationList(assayId);
            repositoryRecord.setModificationList(modificationList);
            repositoryRecordList.getRepositoryRecord().add(repositoryRecord);

        }
        proteomeXchangeDataset.setRepositoryRecordList(repositoryRecordList);
    }

    //TODO: DatasetOriginList, at the moment, it is hardcoded, all are new submissions
    //might change in future
    private static DatasetOrigin getDatasetOrigin() {
        DatasetOrigin datasetOrigin = new DatasetOrigin();
        CvParam cvParam = new CvParam();
        cvParam.setAccession("PRIDE:0000402");
        cvParam.setName("Original data");
        cvParam.setCvRef("PRIDE");
        datasetOrigin.getCvParam().add(cvParam);
        return datasetOrigin;
    }

    //helper method to return DatasetLink with FTP location of files
    private static FullDatasetLinkList createFTPDatasetLink(Submission submissionSummary, String projectAccession) throws SubmissionFileException {
        //get first file, all should point to same location
        FullDatasetLinkList fullDatasetLinkList = new FullDatasetLinkList();
        Date publicationDate = dbac.getPublicationDate(projectAccession);
        if (publicationDate == null) {
            String err = "No publication date in db for project " + projectAccession;
            logger.error(err);
            throw new SubmissionFileException(err);
        }
        //extract year/month path from the date was published
        String dataPath = getYearMonthSubmission(publicationDate);
        //for each of the result files, add it to the DatasetLinkList
//        for (DataFile dataFile : submissionSummary.getDataFiles()) {
//            if (dataFile.getFileType().equals(MassSpecFileType.RESULT)) {
        FullDatasetLink fullDatasetLink = new FullDatasetLink();
        CvParam datasetLinkParam = createCvParam("PRIDE:0000411", FTP + "/" + dataPath + "/" + projectAccession, "Dataset FTP location", "PRIDE");
        fullDatasetLink.setCvParam(datasetLinkParam);
        fullDatasetLinkList.getFullDatasetLink().add(fullDatasetLink);
//            }
//        }
        return fullDatasetLinkList;
    }

    //helper method to return the year and month the XML is being generated to be included in the path
    private static String getYearMonthSubmission(Date publicationDate) {

        Calendar calendar = Calendar.getInstance();

        calendar.setTime(publicationDate);

        int month = calendar.get(Calendar.MONTH) + 1;

        String path = calendar.get(Calendar.YEAR) + File.separator + (month < 10 ? "0" : "") + month;

        return path;
    }

    //this information will come from the summary file
    private static DatasetSummary getDatasetSummary(Submission submissionSummary, String projectAccession) throws SQLException {
        DatasetSummary datasetSummary = new DatasetSummary();
        datasetSummary.setTitle(submissionSummary.getProjectMetaData().getTitle());
        datasetSummary.setDescription(submissionSummary.getProjectMetaData().getProjectDescription());
        datasetSummary.setAnnounceDate(Calendar.getInstance());
        datasetSummary.setHostingRepository(HostingRepositoryType.PRIDE);
        //add Review level, depending wether has a pubmed or not
        ReviewLevelType reviewLevelType = addReviewLevel(submissionSummary, projectAccession);
        datasetSummary.setReviewLevel(reviewLevelType);
        //add Repository Support level, depending if files are supported or not
        RepositorySupportType repositorySupportType = addRepositorySupport(submissionSummary);
        datasetSummary.setRepositorySupport(repositorySupportType);
        return datasetSummary;
    }

    //helper method to retrieve Repository support, either submission is supported or non supported at the moment
    private static RepositorySupportType addRepositorySupport(Submission submissionSummary) {
        RepositorySupportType repositorySupportType = new RepositorySupportType();
        CvParam repositorySupport;

        SubmissionType type = submissionSummary.getProjectMetaData().getSubmissionType();
        if (type != SubmissionType.COMPLETE) {
            repositorySupport = createCvParam("PRIDE:0000416", null, "Supported dataset by repository", "PRIDE");
        } else {
            repositorySupport = createCvParam("PRIDE:0000417", null, "Unsupported dataset by repository", "PRIDE");
        }
        repositorySupportType.setCvParam(repositorySupport);
        return repositorySupportType;
    }

    //helper method to retrieve reviewLeveltype, either peered or non-peered at the moment
    private static ReviewLevelType addReviewLevel(Submission submissionSummary, String projectAccession) throws SQLException {
        ReviewLevelType reviewLevelType = new ReviewLevelType();

        CvParam reviewLevel;
        //i
        if (dbac.getPubmedID(projectAccession) != null){
//        if (submissionSummary.getMetaData().hasPubmedIds()) {
            reviewLevel = createCvParam("PRIDE:0000414", null, "Peer-reviewed dataset", "PRIDE");
        } else {
            reviewLevel = createCvParam("PRIDE:0000415", null, "Non peer-reviewed dataset", "PRIDE");
        }
        reviewLevelType.setCvParam(reviewLevel);
        return reviewLevelType;
    }

    //private method to extract the contact list from the summary file
    private static ContactList getContactList(Submission submissionSummary) {
        ContactList contactList = new ContactList();
        Contact contact = new Contact();
        uk.ac.ebi.pride.data.model.Contact aux = submissionSummary.getProjectMetaData().getContact();
        contact.setId(aux.getName().replace(' ', '_'));
        contact.getCvParam().add(createCvParam("MS:1000586", aux.getName(), "contact name", "MS"));
        contact.getCvParam().add(createCvParam("MS:1000589", aux.getEmail(), "contact email", "MS"));
        contactEmails.add(aux.getEmail());
        contact.getCvParam().add(createCvParam("MS:1000590", aux.getAffiliation(), "contact affiliation", "MS"));
        contactList.getContact().add(contact);
        return contactList;
    }

}
