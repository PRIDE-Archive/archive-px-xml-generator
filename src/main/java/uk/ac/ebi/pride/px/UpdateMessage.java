package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.px.Reader.ReadMessage;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.px.xml.PxMarshaller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
     * Note: this will  add a change log, since that is  needed after the first version of the PX XML.
     *
     * @param submissionSummaryFile the summary file containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @return a File that is the updated PX XML.
     */
    public static File updateReferencesPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession) throws SubmissionFileException, IOException {
        // the submission summary file has to exist, with PMIDs
        Assert.isTrue(submissionSummaryFile.isFile() && submissionSummaryFile.exists(), "Summary file should already exist! In: " + submissionSummaryFile.getAbsolutePath());
        Submission submissionSummary = SubmissionFileParser.parse(submissionSummaryFile);
        Assert.isTrue(submissionSummary.getProjectMetaData().hasPubmedIds(), "Summary file should have PubMed IDs listed!");

        // the output directory has to exist
        Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());

        // the PX XML file has to exist
        File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
        Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");

        ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
        proteomeXchangeDataset.getPublicationList().getPublication().clear();
        StringBuilder sb = new StringBuilder("");
        String reference;
        Set<String> references = submissionSummary.getProjectMetaData().getPubmedIds();
        Iterator<String> it = references.iterator();
        while (it.hasNext()) {
            reference = it.next();
            proteomeXchangeDataset.getPublicationList().getPublication().add(WriteMessage.getPublication(Long.parseLong(reference.trim())));
            sb.append(reference);
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        WriteMessage.addChangeLogEntry(proteomeXchangeDataset, "Updated publication reference for PubMed record(s): " + sb.toString() + ".");


        logger.debug("Updating PX XML file: " + pxFile.getAbsolutePath());

        FileWriter fw = null;
        try {
            fw = new FileWriter(pxFile);
            new PxMarshaller().marshall(proteomeXchangeDataset, fw);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
        logger.info("PX XML file updated: " + pxFile.getAbsolutePath());
        return pxFile;
    }

    /**
     * Method to update a PX XML file with new meta-data.
     * Note: this will  add a change log, since that is  needed after the first version of the PX XML.
     * Meta-data that will be updated includes:
     *  Title, description, modification list, species list, instrument list, and keyword list.
     *
     * @param submissionSummaryFile the summary file containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @return a File that is the updated PX XML.
     */
    // ToDo : to be used for further updates of the PX XML?
    @SuppressWarnings("unused")
    public File updateMetadataPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession) throws SubmissionFileException, IOException {
        // the submission summary file has to exist, with PMIDs
        Assert.isTrue(submissionSummaryFile.isFile() && submissionSummaryFile.exists(), "Summary file should already exist! In: " + submissionSummaryFile.getAbsolutePath());
        Submission submissionSummary = SubmissionFileParser.parse(submissionSummaryFile);
        Assert.isTrue(submissionSummary.getProjectMetaData().hasPubmedIds(), "Summary file should have PubMed IDs listed!");

        // the output directory has to exist
        Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());

        // the PX XML file has to exist
        File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
        Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");
        ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);

        proteomeXchangeDataset.getDatasetSummary().setTitle(submissionSummary.getProjectMetaData().getProjectTitle());
        proteomeXchangeDataset.getDatasetSummary().setDescription(submissionSummary.getProjectMetaData().getProjectDescription());

        proteomeXchangeDataset.setModificationList(WriteMessage.getModificationList(submissionSummary));
        proteomeXchangeDataset.setSpeciesList(WriteMessage.getSpeciesList(submissionSummary));
        proteomeXchangeDataset.setInstrumentList(WriteMessage.getInstrumentList(submissionSummary));
        proteomeXchangeDataset.setKeywordList(WriteMessage.getKeywordList(submissionSummary));
        // no protocols in px xml
        // no tissue in px xml

        WriteMessage.addChangeLogEntry(proteomeXchangeDataset, "Updated project meta-data.");

        logger.debug("Updating PX XML file: " + pxFile.getAbsolutePath());
        FileWriter fw = null;
        try {
            fw = new FileWriter(pxFile);
            new PxMarshaller().marshall(proteomeXchangeDataset, fw);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
        logger.info("PX XML file updated: " + pxFile.getAbsolutePath());
        return pxFile;
    }


}
