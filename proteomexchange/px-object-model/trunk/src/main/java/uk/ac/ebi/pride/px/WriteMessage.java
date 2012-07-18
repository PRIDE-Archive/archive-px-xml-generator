package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.data.util.MassSpecFileType;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.xml.PxMarshaller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dani
 * Date: 11/10/11
 * Time: 16:49
 * To change this template use File | Settings | File Templates.
 */
public class WriteMessage {
    private static DBController dbac;
    private static PxMarshaller marshaller;

    private static final String FORMAT_VERSION = "1.0.0";

    private static final Logger logger = LoggerFactory.getLogger(WriteMessage.class);

    //this list will store the contact emails present in the file, so we don't add them again from DB
    private static Set<String> contactEmails = new HashSet<String>();

    //main method to write a message to ProteomeXchange
    public WriteMessage(DBController dbController) {
        dbac = dbController;
    }

    /**
     * Method overloading, this method is for the first time when a PX xml is created
     */
    public File createXMLMessage(String pxAccession, File directory, File submissionFile) throws IOException, SubmissionFileException {
        return createXMLMessage(pxAccession, directory, submissionFile, null);
    }

    /*
        the pxSummaryLocation will indicate where in the filesystem is stored the summary file
        to extract some of the information
     */
    public File createXMLMessage(String pxAccession, File directory, File submissionFile, String changeLog) throws IOException, SubmissionFileException {
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

    private ProteomeXchangeDataset createProteomeXchangeDataset(String pxAccession, File submissionFile, String changeLog) throws SubmissionFileException {
        Submission submissionSummary = SubmissionFileParser.parse(submissionFile);
        //will return if submission contains only supported files
        //to extract info from database or not supported files
        //and extract info from the metadata file
        boolean submissionSupported = submissionSummary.getMetaData().isSupported();

        ProteomeXchangeDataset proteomeXchangeDataset = new ProteomeXchangeDataset();
        //extract DatasetSummary: this information will always come from Summary object
        DatasetSummary datasetSummary = getDatasetSummary(submissionSummary);
        proteomeXchangeDataset.setDatasetSummary(datasetSummary);

        //extract ContactList: this information comes from summary file
        ContactList contactList = getContactList(submissionSummary);
        proteomeXchangeDataset.setContactList(contactList);

        //extract Keyword List from file
        KeywordList keywordList = getKeywordList(submissionSummary);
        proteomeXchangeDataset.setKeywordList(keywordList);
        if (!submissionSupported) {
            populatePxSubmissionFromFile(proteomeXchangeDataset, submissionSummary, pxAccession);
        } else {
            populatePxSubmissionFromDB(proteomeXchangeDataset, pxAccession);
        }

        //and add the attributes
        proteomeXchangeDataset.setId(pxAccession);
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

    //method to retrieve keyword list from the summary file
    private static KeywordList getKeywordList(Submission submissionSummary) {
        KeywordList keywordList = new KeywordList();
        keywordList.getCvParam().add(createCvParam("MS:1001925", submissionSummary.getMetaData().getKeywords(), "submitter keyword", "MS"));
        return keywordList;
    }

    //method to populate all information in the proteomeXchange dataset from the
    //summary file
    private static void populatePxSubmissionFromFile(ProteomeXchangeDataset proteomeXchangeDataset, Submission submissionSummary, String pxAccession) {
        DatasetIdentifierList datasetIdentifierList = new DatasetIdentifierList();
        datasetIdentifierList.getDatasetIdentifier().add(getDatasetIdentifier(submissionSummary, pxAccession));
        proteomeXchangeDataset.setDatasetIdentifierList(datasetIdentifierList);
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
        instrumentList.getInstrument().addAll(getInstrument(submissionSummary));
        proteomeXchangeDataset.setInstrumentList(instrumentList);
        //add modification
        ModificationList modificationList = new ModificationList();
        modificationList.getCvParam().addAll(getModificationCvParams(submissionSummary));
        proteomeXchangeDataset.setModificationList(modificationList);
        //add pubmed information, if present
        if (submissionSummary.getMetaData().hasPubmedIds()) {
            PublicationList publicationList = new PublicationList();
            publicationList.getPublication().addAll(getPublicationParams(submissionSummary));
            proteomeXchangeDataset.setPublicationList(publicationList);
        }
        //add dataset link list, data will be in FTP only, so link will refer to files in FTP
        FullDatasetLinkList fullDatasetLinkList = createFullDatasetLinkList(submissionSummary);
        proteomeXchangeDataset.setFullDatasetLinkList(fullDatasetLinkList);
    }

    //method to extract Publication information from file
    private static List<Publication> getPublicationParams(Submission submissionSummary) {
        List<Publication> publications = new ArrayList<Publication>();

        for (String pubmedID : submissionSummary.getMetaData().getPubmedIds()) {
            Publication publication = new Publication();
            publication.setId("PMID" + pubmedID);
            publication.getCvParam().add(createCvParam("MS:1000879", pubmedID, "PubMed identifier", "MS"));
            publications.add(publication);
        }
        return publications;
    }

    //method to extract modifications from summary file
    private static List<CvParam> getModificationCvParams(Submission submissionSummary) {
        List<CvParam> cvParams = new ArrayList<CvParam>();
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getMetaData().getModifications()) {
            cvParams.add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), cvParam.getCvLabel()));
        }
        return cvParams;
    }

    //method to extract instrument information from summary file
    private static List<Instrument> getInstrument(Submission submissionSummary) {
        List<Instrument> instruments = new ArrayList<Instrument>();
        int instrumentNum = 1;
        //convert CvParam into px CvParam
        for (List<uk.ac.ebi.pride.data.model.CvParam> cvParams : submissionSummary.getMetaData().getInstruments()) {
            Instrument instrument = new Instrument();
            instrument.setId("Instrument_" + instrumentNum);
            instrumentNum++;
            for (uk.ac.ebi.pride.data.model.CvParam cvParam : cvParams) {
                instrument.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), cvParam.getCvLabel()));
            }
            instruments.add(instrument);
        }
        return instruments;
    }

    //method to get Species information from summary file
    private static Species getSpecies(Submission submissionSummary) {
        Species species = new Species();
        //need to create 2 cvParam: one with the NEWT code and one with the name
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getMetaData().getSpecies()) {
            species.getCvParam().add(createCvParam("MS:1001469", cvParam.getName(), "taxonomy: scientific name", "PSI-MS"));
            species.getCvParam().add(createCvParam("MS:1001467", cvParam.getAccession(), "taxonomy: NCBI TaxID", "PSI-MS"));
        }
        return species;
    }

    //method to add Dataset identifier information from file
    //at the moment, let's not worry about PxAccessions, they refer to previous submissions
    private static DatasetIdentifier getDatasetIdentifier(Submission submissionSummary, String pxAccession) {
        //add px number as CvParam
        DatasetIdentifier px = new DatasetIdentifier();
        px.getCvParam().add(createCvParam("MS:1001919", pxAccession, "ProteomeXchange accession number", "MS"));
//        //add DOI from file if present
//        if (submissionSummary.getMetaData().hasPxAccessions()){
//            for (String accession : submissionSummary.getMetaData().getPxAccessions()) {
//                px.getCvParam().add(createCvParam("MS:1001922", accession, "Digital Object Identifier (DOI)", "MS"));
//            }
//        }
        return px;
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
    private static void populatePxSubmissionFromDB(ProteomeXchangeDataset proteomeXchangeDataset, String pxAccession) {
        //get all experiments in the project
        List<Long> experimentIDs = dbac.getExperimentIds(pxAccession);
        if (experimentIDs.isEmpty()) {
            logger.error("Project contains no experiments");
            System.exit(0);
        }
        DatasetIdentifierList datasetIdentifierList = dbac.getDatasetIdentifierList(experimentIDs);
        proteomeXchangeDataset.setDatasetIdentifierList(datasetIdentifierList);
        DatasetOriginList datasetOriginList = new DatasetOriginList();
        datasetOriginList.setDatasetOrigin(getDatasetOrigin());
        proteomeXchangeDataset.setDatasetOriginList(datasetOriginList);
        //extract Species information for all experiments
        SpeciesList speciesList = dbac.getSpecies(experimentIDs);
        proteomeXchangeDataset.setSpeciesList(speciesList);
        //extract instrument information
        InstrumentList instrumentList = dbac.getInstrumentList(experimentIDs);
        proteomeXchangeDataset.setInstrumentList(instrumentList);
        //extract modification list
        ModificationList modificationList = dbac.getModificationList(experimentIDs);
        proteomeXchangeDataset.setModificationList(modificationList);
        //extract contact list that are not present already in the file
        ContactList newContactList = dbac.getContactList(experimentIDs, contactEmails);
        ContactList contactList = proteomeXchangeDataset.getContactList();
        //add contacts from DB
        contactList.getContact().addAll(newContactList.getContact());
        proteomeXchangeDataset.setContactList(contactList);
        //extract publicationList
        PublicationList publicationList = dbac.getPublicationList(experimentIDs);
        proteomeXchangeDataset.setPublicationList(publicationList);
//        KeywordList keywordList = dbac.getKeywordList(experimentIDs);
//        proteomeXchangeDataset.setKeywordList(keywordList);
        FullDatasetLinkList datasetLinkList = dbac.getFullDataSetLinkList(experimentIDs);
        if (!datasetLinkList.getFullDatasetLink().isEmpty()) {
            proteomeXchangeDataset.setFullDatasetLinkList(datasetLinkList);
        }
        //TODO: no DatasetFileList, where are the raw files stored?
        // create RepositoryRecordList with all experiments in project
        RepositoryRecordList repositoryRecordList = new RepositoryRecordList();
        for (long experimentID : experimentIDs) {
            //get new record
            RepositoryRecord repositoryRecord = dbac.getRepositoryRecord(experimentID);
            //we now need to add the additional elements to the record: source, publication, instrument, sample, modification
            //TODO; need to deal with source file, where are they ??
            //TODO: an experiment can have more than 1 publication or more than 1 instrument ??
            Ref publicationRef = dbac.getRef("publication", experimentID);
            repositoryRecord.getPublicationRef().add(publicationRef);
            Ref instrumentRef = dbac.getRef("instrument", experimentID);
            repositoryRecord.getInstrumentRef().add(instrumentRef);
            SampleList sampleList = dbac.getSampleList(experimentID);
            repositoryRecord.getSampleList().add(sampleList);
            //and the modificationList
            List<Long> expId = new ArrayList<Long>();  //need to create a list with 1 element for the method
            expId.add(experimentID);
            modificationList = dbac.getModificationList(expId);
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

    //helper method to return DatasetLink
    private static FullDatasetLinkList createFullDatasetLinkList(Submission submissionSummary) {
        FullDatasetLinkList fullDatasetLinkList = new FullDatasetLinkList();

        //for each of the result files, add it to the DatasetLinkList
        for (DataFile dataFile : submissionSummary.getDataFiles()) {
            if (dataFile.getFileType().equals(MassSpecFileType.RESULT)) {
                FullDatasetLink fullDatasetLink = new FullDatasetLink();
                CvParam datasetLinkParam = createCvParam("PRIDE:0000411", dataFile.getFile().getAbsolutePath(), "Dataset FTP location", "PRIDE");
                fullDatasetLink.setCvParam(datasetLinkParam);
                fullDatasetLinkList.getFullDatasetLink().add(fullDatasetLink);
            }
        }
        return fullDatasetLinkList;
    }

    //this information will come from the summary file
    private static DatasetSummary getDatasetSummary(Submission submissionSummary) {
        DatasetSummary datasetSummary = new DatasetSummary();
        datasetSummary.setTitle(submissionSummary.getMetaData().getTitle());
        datasetSummary.setDescription(submissionSummary.getMetaData().getDescription());
        datasetSummary.setAnnounceDate(Calendar.getInstance());
        datasetSummary.setHostingRepository(HostingRepositoryType.PRIDE);
        //add Review level, depending wether has a pubmed or not
        ReviewLevelType reviewLevelType = addReviewLevel(submissionSummary);
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
        if (submissionSummary.getMetaData().isSupported()) {
            repositorySupport = createCvParam("PRIDE:0000416", null, "Supported dataset by repository", "PRIDE");
        } else {
            repositorySupport = createCvParam("PRIDE:0000417", null, "Unsupported dataset by repository", "PRIDE");
        }
        repositorySupportType.setCvParam(repositorySupport);
        return repositorySupportType;
    }

    //helper method to retrieve reviewLeveltype, either peered or non-peered at the moment
    private static ReviewLevelType addReviewLevel(Submission submissionSummary) {
        ReviewLevelType reviewLevelType = new ReviewLevelType();

        CvParam reviewLevel;
        if (submissionSummary.getMetaData().hasPubmedIds()) {
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
        contact.setId(submissionSummary.getContact().getName().replace(' ', '_'));
        contact.getCvParam().add(createCvParam("MS:1000586", submissionSummary.getContact().getName(), "contact name", "MS"));
        contact.getCvParam().add(createCvParam("MS:1000589", submissionSummary.getContact().getEmail(), "contact email", "MS"));
        contactEmails.add(submissionSummary.getContact().getEmail());
        contact.getCvParam().add(createCvParam("MS:1000590", submissionSummary.getContact().getAffiliation(), "contact affiliation", "MS"));
        contactList.getContact().add(contact);
        return contactList;
    }

}
