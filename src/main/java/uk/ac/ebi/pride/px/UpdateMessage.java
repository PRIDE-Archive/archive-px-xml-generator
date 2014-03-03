package uk.ac.ebi.pride.px;

import org.apache.commons.io.FilenameUtils;
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
import java.nio.file.Files;
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
     * Note: this will add a change log, since that is needed after the first version of the PX XML.
     * Will also backup the PX XML before updating.
     *
     * @param submissionSummaryFile the summary file containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @return a File that is the updated PX XML.
     */
    public static File updateReferencesPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment) throws SubmissionFileException, IOException {
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
        logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
        backupPxXml(pxFile, outputDirectory);

        // make new PX XML if dealing with old schema version in current PX XML
        if (!proteomeXchangeDataset.getFormatVersion().equalsIgnoreCase(WriteMessage.FORMAT_VERSION)) {
            WriteMessage writeMessage = new WriteMessage();
            pxFile = writeMessage.createIntialPxXml(submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment);
            if (pxFile != null) {
                logger.info("Generated PX XML message file " + pxFile.getAbsolutePath());
            } else {
                final String MSG = "Failed to create PX XML message file at " + outputDirectory.getAbsolutePath();
                logger.error(MSG);
                throw new SubmissionFileException(MSG);
            }
            proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
        }

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

        logger.debug("Updating new reference for PX XML file: " + pxFile.getAbsolutePath());

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
     * Method to update a PX XML file with new meta-data, intended only for *public* projects.
     * Note: this will add a change log, since that is needed after the first version of the PX XML.
     * Will also backup the PX XML before updating.
     * Meta-data that will be updated includes:
     *  Title, description, modification list, species list, instrument list, and keyword list.
     *
     * @param submissionSummaryFile the summary file containing the PX submission summary information.
     * @param outputDirectory the path to the PX XML output directory.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @return a File that is the updated PX XML.
     */
    public static File updateMetadataPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment) throws SubmissionFileException, IOException {
        // the submission summary file has to exist
        Assert.isTrue(submissionSummaryFile.isFile() && submissionSummaryFile.exists(), "Summary file should already exist! In: " + submissionSummaryFile.getAbsolutePath());
        Submission submissionSummary = SubmissionFileParser.parse(submissionSummaryFile);

        // the output directory has to exist
        Assert.isTrue(outputDirectory.exists() && outputDirectory.isDirectory(), "PX XML output directory should already exist! In: " + outputDirectory.getAbsolutePath());

        // the PX XML file has to exist
        File pxFile = new File(outputDirectory.getAbsolutePath() + File.separator + pxAccession + ".xml");
        Assert.isTrue(pxFile.isFile() && pxFile.exists(), "PX XML file should already exist!");
        ProteomeXchangeDataset proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
        logger.debug("Backing up current PX XML file: " + pxFile.getAbsolutePath());
        backupPxXml(pxFile, outputDirectory);

        // make new PX XML if dealing with old schema version in current PX XML
        if (!proteomeXchangeDataset.getFormatVersion().equalsIgnoreCase(WriteMessage.FORMAT_VERSION)) {
            WriteMessage writeMessage = new WriteMessage();
            pxFile = writeMessage.createIntialPxXml(submissionSummaryFile, outputDirectory, pxAccession, datasetPathFragment);
            if (pxFile != null) {
                logger.info("Generated PX XML message file " + pxFile.getAbsolutePath());
            } else {
                final String MSG = "Failed to create PX XML message file at " + outputDirectory.getAbsolutePath();
                logger.error(MSG);
                throw new SubmissionFileException(MSG);
            }
            proteomeXchangeDataset = ReadMessage.readPxXml(pxFile);
        }

        proteomeXchangeDataset.getDatasetSummary().setTitle(submissionSummary.getProjectMetaData().getProjectTitle());
        proteomeXchangeDataset.getDatasetSummary().setDescription(submissionSummary.getProjectMetaData().getProjectDescription());

        proteomeXchangeDataset.setModificationList(WriteMessage.getModificationList(submissionSummary));
        proteomeXchangeDataset.setSpeciesList(WriteMessage.getSpeciesList(submissionSummary));
        proteomeXchangeDataset.setInstrumentList(WriteMessage.getInstrumentList(submissionSummary));
        proteomeXchangeDataset.setKeywordList(WriteMessage.getKeywordList(submissionSummary));
        // no protocols in px xml
        // no tissue in px xml
        // no project tags in px xml

        WriteMessage.addChangeLogEntry(proteomeXchangeDataset, "Updated project metadata.");

        logger.debug("Updating metadata for PX XML file: " + pxFile.getAbsolutePath());
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

    private static void backupPxXml(File pxFile, File outputDirectory) throws IOException{
        String baseName = FilenameUtils.getBaseName(pxFile.getName());
        String ext = FilenameUtils.getExtension(pxFile.getName());
        final String VERSION_SEP = "_";
        final String EXT_SEP = ".";
        int nextVersionNumber = 1;
        File backupPx =  new File(outputDirectory.getAbsolutePath() + File.separator + baseName + VERSION_SEP + nextVersionNumber + EXT_SEP + ext);
        while (backupPx.exists()) {
            baseName = FilenameUtils.getBaseName(backupPx.getName());
            String[] splittedSting = baseName.split(VERSION_SEP);
            nextVersionNumber = (Integer.parseInt(splittedSting[1])) + 1;
            backupPx =  new File(outputDirectory.getAbsolutePath() + File.separator + splittedSting[0] + VERSION_SEP + nextVersionNumber + EXT_SEP + ext);
        }

        Files.copy(pxFile.toPath(), backupPx.toPath());
    }

}
