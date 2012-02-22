package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.xml.PxMarshaller;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private static final Logger logger = LoggerFactory.getLogger(WriteMessage.class);

    //main method to write a message to ProteomeXChange
    //needs one argument: experiment_accession
    public static void main(String args[]) {
//        if (args.length == 0) {
//            logger.error("There should be one argument: project_name");
//            System.exit(1);
//        }
//        String accession = args[0];
//        //we will use accesion_number.xml as the file name

        String pxAccession = "PXD000018";

        try {
            FileWriter fw = new FileWriter(pxAccession + ".xml");
            //first, create the DBController and connect to the database
            dbac = new DBController();
            marshaller = new PxMarshaller();
            ProteomeXchangeDataset proteomeXchangeDataset = new ProteomeXchangeDataset();
            //get all experiments in the project
            List<Long> experimentIDs = dbac.getExperimentIds(pxAccession);
            if (experimentIDs.isEmpty()) {
                logger.error("Project contains no experiments");
                System.exit(0);
            }
            //extract DatasetSummary: should be same for all experiments, using first expermient one only
            DatasetSummary datasetSummary = dbac.getDatasetSummary(experimentIDs.get(0));
            proteomeXchangeDataset.setDatasetSummary(datasetSummary);

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
            //extract contact list
            ContactList contactList = dbac.getContactList(experimentIDs);
            proteomeXchangeDataset.setContactList(contactList);
            //extract publicationList
            PublicationList publicationList = dbac.getPublicationList(experimentIDs);
            proteomeXchangeDataset.setPublicationList(publicationList);
            //TODO: test keyword list, none present in DB at the moment
            KeywordList keywordList = dbac.getKeywordList(experimentIDs);
            proteomeXchangeDataset.setKeywordList(keywordList);
            FullDatasetLinkList datasetLinkList = dbac.getFullDataSetLinkList(experimentIDs);
            proteomeXchangeDataset.setFullDatasetLinkList(datasetLinkList);
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
                //TODO: get Sample List
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
            //and marshal it
            marshaller.marshall(proteomeXchangeDataset, fw);
        } catch (IOException e) {
            logger.error(e.getMessage(), e); //To change body of catch statement use File | Settings | File Templates.

        }
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
}
