package uk.ac.ebi.pride.archive.px.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.data.model.Submission;

/**
 * Writes out the PX XML file, which contains all the metadata for a dataset to be sent to ProteomeCentral.
 */
public class SchemaOnePointThreeStrategy extends SchemaCommonStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SchemaOnePointThreeStrategy.class);

    private String formatVersion;

    /**
     * Default constructor.
     */
    public SchemaOnePointThreeStrategy(String version) {
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
        pxXml.setId(this.formatVersion);
        pxXml.setFormatVersion(pxSchemaVersion);
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

}