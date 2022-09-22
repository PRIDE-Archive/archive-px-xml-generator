package uk.ac.ebi.pride.archive.px.writer;

import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.model.Submission;

import java.io.File;
import java.io.IOException;

/**
 * @author Suresh Hewapathirana
 */
public interface MessageWriter {

    public String formatversion = null;

    File createIntialPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException;
    void addChangeLogEntry(ProteomeXchangeDataset pxXML, String message);
    Publication getPublication(Long pmid);
    Publication getPublicationDoi(String doi);
    KeywordList getKeywordList(Submission submissionSummary);
    ModificationList getModificationList(Submission submissionSummary);
    InstrumentList getInstrumentList(Submission submissionSummary);
    SpeciesList getSpeciesList(Submission submissionSummary);
    File createIntialPxXml(Submission submissionSummary, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion)throws IOException;
}
