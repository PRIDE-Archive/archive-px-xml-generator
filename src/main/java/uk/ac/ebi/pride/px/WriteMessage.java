package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.model.DatasetSummary;
import uk.ac.ebi.pride.px.model.InstrumentList;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.px.model.SpeciesList;
import uk.ac.ebi.pride.px.xml.PxMarshaller;

import java.io.FileWriter;
import java.io.IOException;

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
        if (args.length == 0) {
            logger.error("There should be one argument: experiment_accession");
            System.exit(1);
        }
        String accession = args[0];
        //we will use accesion_number.xml as the file name
        try {
            FileWriter fw = new FileWriter(accession + ".xml");
            //first, create the DBController and connect to the database
            dbac = new DBController();
            marshaller = new PxMarshaller();
            //TODO: what should I do with DatasetIdentifierList and DatasetOriginList ??
            ProteomeXchangeDataset proteomeXchangeDataset = new ProteomeXchangeDataset();
            //extract DatasetSummary
            DatasetSummary datasetSummary = dbac.getDatasetSummary(accession);
            proteomeXchangeDataset.setDatasetSummary(datasetSummary);
            //extract Species information
            SpeciesList speciesList = dbac.getSpecies(accession);
            proteomeXchangeDataset.setSpeciesList(speciesList);
            //extract instrument information
            InstrumentList instrumentList = dbac.getInstrumentList(accession);
            proteomeXchangeDataset.setInstrumentList(instrumentList);
            //and marshal it
            marshaller.marshall(proteomeXchangeDataset, fw);
        } catch (IOException e) {
            logger.error(e.getMessage(), e); //To change body of catch statement use File | Settings | File Templates.

        }
    }
}
