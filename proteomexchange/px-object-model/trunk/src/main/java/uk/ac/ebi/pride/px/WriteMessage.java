package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.*;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.model.Contact;
import uk.ac.ebi.pride.px.model.CvParam;
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
    //name of the file containing the summary of the submission
    private static final String SUBMISSION_SUMMARY_FILE = "submission.px";

    private static final Logger logger = LoggerFactory.getLogger(WriteMessage.class);

    //this list will store the contact emails present in the file, so we don't add them again from DB
    private static Set<String> contactEmails = new HashSet<String>();

    //main method to write a message to ProteomeXChange
    public WriteMessage(DBController dbController){
        dbac = dbController;
    }

    //the pxSummaryLocation will indicate where in the filesystem is stored the summary file
    //to extract some of the information
    public File createXMLMessage(String pxAccession, File directory, File pxSummaryLocation) throws SubmissionFileException {
        //first, extract submission file object
        File submissionFile = new File(pxSummaryLocation + File.separator + SUBMISSION_SUMMARY_FILE);
        Submission submissionSummary = SubmissionFileParser.parse(submissionFile);
        //will return if submission contains only supported files
        //to extract info from database or not supported files
        //and extract info from the metadata file
        boolean submissionSupported = submissionSummary.getMetaData().isSupported();
        File file = new File(directory.getAbsolutePath() + File.separator + pxAccession + ".xml");
        try {

            FileWriter fw = new FileWriter(file);
            marshaller = new PxMarshaller();
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
            if (!submissionSupported){
                populatePxSubmissionFromFile(proteomeXchangeDataset, submissionSummary, pxAccession);
            }
            else{
                populatePxSubmissionFromDB(proteomeXchangeDataset, pxAccession);
            }
            //and add the attributes
            proteomeXchangeDataset.setId(pxAccession);
            //TODO: format version will always be hardcoded ??
            proteomeXchangeDataset.setFormatVersion(FORMAT_VERSION);
            //and marshal it
            marshaller.marshall(proteomeXchangeDataset, fw);
        } catch (IOException e) {
            logger.error(e.getMessage(), e); //To change body of catch statement use File | Settings | File Templates.

        }
        return file;
    }

    //method to retrieve keyword list from the summary file
    private static KeywordList getKeywordList(Submission submissionSummary){
        KeywordList keywordList = new KeywordList();
        keywordList.getCvParam().add(createCvParam("MS:1001925", submissionSummary.getMetaData().getKeywords(), "submitter keyword", "MS"));
        return keywordList;
    }

    //method to populate all information in the proteomeXchange dataset from the
    //summary file
    private static void populatePxSubmissionFromFile(ProteomeXchangeDataset proteomeXchangeDataset, Submission submissionSummary, String pxAccession){
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
        if (submissionSummary.getMetaData().hasPubmedIds()){
            PublicationList publicationList = new PublicationList();
            publicationList.getPublication().addAll(getPublicationParams(submissionSummary));
            proteomeXchangeDataset.setPublicationList(publicationList);
        }
        //TODO:ask Flo about FullDatasetLinkList: data not in Pride, what to have
    }

    //method to extract Publication information from file
    private static List<Publication> getPublicationParams(Submission submissionSummary){
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
    private static List<CvParam> getModificationCvParams(Submission submissionSummary){
        List<CvParam> cvParams = new ArrayList<CvParam>();
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getMetaData().getModifications()) {
            cvParams.add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), cvParam.getCvLabel()));
        }
        return cvParams;
    }

    //method to extract instrument information from summary file
    private static List<Instrument> getInstrument(Submission submissionSummary){
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
    private static Species getSpecies(Submission submissionSummary){
        Species species = new Species();
        //need to create 2 cvParam: one with the NEWT code and one with the name
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : submissionSummary.getMetaData().getSpecies()) {
            species.getCvParam().add(createCvParam("MS:1001469", cvParam.getAccession(), "taxonomy: scientific name", "PSI-MS"));
            species.getCvParam().add(createCvParam("MS:1001467", cvParam.getValue(), "taxonomy: NCBI TaxID", "PSI-MS"));
        }
        return species;
    }

    //method to add Dataset identifier information from file
    //at the moment, let's not worry about PxAccessions, they refer to previous submissions
    private static DatasetIdentifier getDatasetIdentifier(Submission submissionSummary, String pxAccession){
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

    private static CvParam createCvParam(String accession, String value, String name, String cvRef){

        CvParam cvParam = new CvParam();
        cvParam.setAccession(accession);
        cvParam.setValue(value);
        cvParam.setName(name);
        cvParam.setCvRef(cvRef);

        return cvParam;
    }

    //method will get all information for a specific submission from the database and populate
    //the ProteomeXchangeDataset object with it
    private static void populatePxSubmissionFromDB(ProteomeXchangeDataset proteomeXchangeDataset, String pxAccession){
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
        ContactList contactList = dbac.getContactList(experimentIDs, contactEmails);
        proteomeXchangeDataset.setContactList(contactList);
        //extract publicationList
        PublicationList publicationList = dbac.getPublicationList(experimentIDs);
        proteomeXchangeDataset.setPublicationList(publicationList);
//        KeywordList keywordList = dbac.getKeywordList(experimentIDs);
//        proteomeXchangeDataset.setKeywordList(keywordList);
        FullDatasetLinkList datasetLinkList = dbac.getFullDataSetLinkList(experimentIDs);
        if (!datasetLinkList.getFullDatasetLink().isEmpty()){
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
    private static DatasetOrigin getDatasetOrigin(){
        DatasetOrigin datasetOrigin = new DatasetOrigin();
        CvParam cvParam = new CvParam();
        cvParam.setAccession("PRIDE:0000402");
        cvParam.setName("Original data");
        cvParam.setCvRef("PRIDE");
        datasetOrigin.getCvParam().add(cvParam);
        return datasetOrigin;
    }

    //TODO: Florian : ReviewLevel and RepositorySupport, no idea what to put there
    //this information will come from the summary file
    private static DatasetSummary getDatasetSummary(Submission submissionSummary){
        DatasetSummary datasetSummary = new DatasetSummary();
        datasetSummary.setTitle(submissionSummary.getMetaData().getTitle());
        datasetSummary.setDescription(submissionSummary.getMetaData().getDescription());
        datasetSummary.setAnnounceDate(Calendar.getInstance());
        datasetSummary.setHostingRepository(HostingRepositoryType.PRIDE);
//        datasetSummary.setReviewLevel();
//        datasetSummary.setRepositorySupport();
        return datasetSummary;
    }

    //private method to extract the contact list from the summary file
    private static ContactList getContactList(Submission submissionSummary){
        ContactList contactList = new ContactList();
        Contact contact = new Contact();
        contact.setId(submissionSummary.getContact().getName().replace(' ','_'));
        contact.getCvParam().add(createCvParam("MS:1000586", submissionSummary.getContact().getName(), "contact name", "MS"));
        contact.getCvParam().add(createCvParam("MS:1000589", submissionSummary.getContact().getEmail(), "contact email", "MS"));
        contactEmails.add(submissionSummary.getContact().getEmail());
        contact.getCvParam().add(createCvParam("MS:1000590", submissionSummary.getContact().getAffiliation(), "contact affiliation", "MS"));
        contactList.getContact().add(contact);
        return contactList;
    }

}
