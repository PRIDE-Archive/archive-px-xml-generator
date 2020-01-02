package uk.ac.ebi.pride.archive.px.writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import uk.ac.ebi.pride.archive.dataprovider.project.SubmissionType;
import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.archive.px.xml.PxMarshaller;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;
import uk.ac.ebi.pride.data.model.DataFile;
import uk.ac.ebi.pride.data.model.SampleMetaData;
import uk.ac.ebi.pride.data.model.Submission;
import uk.ac.ebi.pride.pubmed.PubMedFetcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Suresh Hewapathirana
 */
public abstract class SchemaCommonStrategy implements MessageWriter {

    private static final Logger logger = LoggerFactory.getLogger(MessageWriter.class);

    static final String DOI_PREFFIX = "10.6019";
    static final String FTP = "ftp://ftp.pride.ebi.ac.uk/pride/data/archive";
    static final String PRIDE_REPO_PROJECT_BASE_URL = "http://www.ebi.ac.uk/pride/archive/projects/";
    // CV Parameters
    public static final String MS_1001925 = "MS:1001925";
    public static final String MS_1001926 = "MS:1001926";
    public static final String MS_1002340 = "MS:1002340";
    // Other constants
    static final String SUBMITTER_KEYWORD = "submitter keyword";
    static final String BIOLOGICAL = "Biological";
    static final String BIOMEDICAL = "Biomedical";
    static final String CARDIOVASCULAR = "Cardiovascular";
    static final String HIGHLIGHTED = "Highlighted";
    static final String TECHNICAL = "Technical";
    static final String METAPROTEOMICS = "Metaproteomics";
    static final String CURATOR_KEYWORD = "curator keyword";
    static final String PROTEOME_XCHANGE_PROJECT_TAG = "ProteomeXchange project tag";
    static final String FRED_LAVANDER_LAB_SWE = "webdav.swegrid.se";
    // all allowed CVs
     static Cv MS_CV;
     static Cv PRIDE_CV;
     static Cv MOD_CV;
     static Cv UNIMOD_CV;

    static {
        MS_CV = new Cv();
        MS_CV.setFullName("PSI-MS");
        MS_CV.setId("MS");
        MS_CV.setUri("https://raw.githubusercontent.com/HUPO-PSI/psi-ms-CV/master/psi-ms.obo");

        PRIDE_CV = new Cv();
        PRIDE_CV.setFullName("PRIDE");
        PRIDE_CV.setId("PRIDE");
        PRIDE_CV.setUri("https://raw.githubusercontent.com/PRIDE-Utilities/pride-ontology/master/pride_cv.obo");

        MOD_CV = new Cv();
        MOD_CV.setFullName("PSI-MOD");
        MOD_CV.setId("MOD");
        MOD_CV.setUri("https://raw.githubusercontent.com/MICommunity/psidev/master/psi/mod/data/PSI-MOD.obo");

        UNIMOD_CV = new Cv();
        UNIMOD_CV.setFullName("UNIMOD");
        UNIMOD_CV.setId("UNIMOD");
        UNIMOD_CV.setUri("http://www.unimod.org/obo/unimod.obo");
    }
    // ToDo fix JavaDoc everywhere
    // ToDo (version upgrade): adapt to new submission summary file specification, take into account mandatory fields
    // ToDo (general): perhaps change to non-static implementation and keep certain data in the instance (px accession, datasetPathFragment, counters...)?
    // ToDo (general): extract CV params to global util package?
    // ToDo (general): check PXST summary file definition with regards to PARTIAL/COMPLETE differences

    /**
     * Default constructor.
     */
    public SchemaCommonStrategy() {
    }

    /**
     * Crates the first version of a PX XML file.
     * @param submissionSummaryFile the Submission object containing the PX submission summary information.
     * @param outputDirectory where the output PX XML will be written to.
     * @param pxAccession the PX project accession assigned to the dataset for which we are generating the PX XML.
     * @param datasetPathFragment the path fragment that points to the dataset (pattern: /yyyy/mm/accession/).
     * @return The generated PX XML file
     * @throws SubmissionFileException
     * @throws IOException
     */
    public File createIntialPxXml(File submissionSummaryFile, File outputDirectory, String pxAccession, String datasetPathFragment, String pxSchemaVersion) throws SubmissionFileException, IOException {
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
            ProteomeXchangeDataset proteomeXchangeDataset = createPxXml(submissionSummary, pxAccession, datasetPathFragment, pxSchemaVersion);
            new PxMarshaller().marshall(proteomeXchangeDataset, fw, pxSchemaVersion); // write out the ProteomeXchangeDataset object to the specified file
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
    protected abstract ProteomeXchangeDataset createPxXml(Submission submissionSummary, String pxAccession, String datasetPathFragment, String pxSchemaVersion);

    /**
     * This method clears the publication list of the PX XML and adds a record for the provided PubMed ID.
     * Note: the initial PX XML is generally generated without knowledge of a publication and therefore
     *       will carry a default annotation. That is the reason, why this method clears the publication
     *       list before adding a new reference.
     *
     * @param pxXml the object representing the PX XML.
     * @param pmid the PubMed ID of the publication to be added.
     * @return the updated object reflecting the updated PX XML.
     */
    @SuppressWarnings("unused")
    private ProteomeXchangeDataset replacePrimaryReference(ProteomeXchangeDataset pxXml, Long pmid) {
        Assert.notNull(pxXml, "The PX XML object cannot be null!");
        Assert.notNull(pmid, "The PMID for the publication cannot be null!");
        pxXml.getPublicationList().getPublication().clear(); // remove the old entry (assumed to be 'publication pending')
        pxXml.getPublicationList().getPublication().add(getPublication(pmid)); // and add a new record
        addChangeLogEntry(pxXml, "Replaced publication reference for PubMed record: " + pmid);
        return pxXml;
    }

    /**
     * This method clears the publication list of the PX XML and adds a record for the provided PubMed ID.
     * Note: the initial PX XML is generally generated without knowledge of a publication and therefore
     *       will carry a default annotation. That is the reason, why this method clears the publication
     *       list before adding a new reference.
     *
     * @param pxXml the object representing the PX XML.
     * @param refLine the reference line of the publication to be added (in case no PubMed ID can be provided).
     * @return the updated object reflecting the updated PX XML.
     */
    @SuppressWarnings("unused")
    private ProteomeXchangeDataset replacePrimaryReference(ProteomeXchangeDataset pxXml, String refLine) {
        Assert.notNull(pxXml, "The PX XML object cannot be null!");
        Assert.notNull(refLine, "The ref line for the publication cannot be null!");
        pxXml.getPublicationList().getPublication().clear();  // we remove any old entry (which we assume to be out-dated)
        // ToDo: getPublication uses a static ID, if multiple publication are added this will have to me made unique!
        pxXml.getPublicationList().getPublication().add(getPublication(refLine));
        addChangeLogEntry(pxXml, "Replaced publication reference with ref line: " + refLine);
        return pxXml;
    }

    /**
     * Check if the PX accession number is valid according to the regex: PX[D|T]\d{6}
     * @param pxAccession The PX accession number to check.
     * @return
     */
     boolean isValidPXAccession(String pxAccession) {
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

    /**
     * Check if the public path fragment is valid.
     * @param datasetPathFragment the path fragment to check.
     * @param pxAccession the PX accession number.
     * @return
     */
     boolean isValidPathFragment(String datasetPathFragment, String pxAccession) {
        Pattern p = Pattern.compile("20../[0,1][0-9]/"+pxAccession);
        Matcher m = p.matcher(datasetPathFragment);
        if (!m.matches()) {
            logger.info("The dataset path fragment '" + datasetPathFragment + "' is not valid for PX accession: " + pxAccession );
            return false;
        }
        return true;
    }

    /**
     * Adds a change log entry field to the PX XML.
     * @param pxXML the PX XML file to update.
     * @param message the update message to be included.
     */
    public void addChangeLogEntry(ProteomeXchangeDataset pxXML, String message) {
        ChangeLogEntryType entry = new ChangeLogEntryType();
        entry.setValue(message);
        entry.setDate(Calendar.getInstance());
        ChangeLogType changeLog = pxXML.getChangeLog();
        if (changeLog == null) {
            changeLog = new ChangeLogType();
            pxXML.setChangeLog(changeLog);
        }
        changeLog.getChangeLogEntry().add(entry);
    }

    /**
     * Gets the publication list. There should always be a publication list, but it may have records to say 'no reference' or 'reference pending'
     * @param submissionSummary the submission summary.
     * @return The PublicationList is returned.
     */
    protected PublicationList getPublicationList(Submission submissionSummary) {
        PublicationList list = new PublicationList();
        Set<String> pubmedIDs = submissionSummary.getProjectMetaData().getPubmedIds();
        Set<String> dois = submissionSummary.getProjectMetaData().getDois();
        if ((pubmedIDs==null || pubmedIDs.size()<1) && (dois==null || dois.size()<1)) {
            // no pubmed ID, so no publication, we assume it is pending
            Publication publication = new Publication();
            CvParam cvParam = new CvParam();
            cvParam.setCvRef(PRIDE_CV);
            cvParam.setName("Dataset with its publication pending");
            cvParam.setAccession("PRIDE:0000432");
            publication.setId("pending");
            publication.getCvParam().add(cvParam);
            list.getPublication().add(publication);
        } else { // we have already publications
            if (pubmedIDs!=null) {
                for (String pubmedID : pubmedIDs) {
                    Long pmid = Long.parseLong(pubmedID);
                    list.getPublication().add(getPublication(pmid));
                }
            }
            if (dois != null) {
                for (String doi : dois) {
                    list.getPublication().add(getPublicationDoi(doi));
                }
            }
        }
        return  list;
    }

    /**
     * Extracts the publication from a refline.
     * @param refLine The refline to obtain the publication.
     * @return the Publication object
     */
    protected Publication getPublication(String refLine) {
        if (refLine == null) {
            throw new IllegalArgumentException("No ref line provided!");
        }
        Publication publication = new Publication();
        publication.setId("PUBLICATION"); // ToDo: this should be unique!
        publication.getCvParam().add(createCvParam("PRIDE:0000400", refLine, "Reference", PRIDE_CV));
        return publication;
    }

    /**
     * Gets a Publication from a PubMed ID
     * @param pmid the PubMed ID
     * @return the Publication object
     */
    public Publication getPublication(Long pmid) {
        if (pmid == null) {
            throw new IllegalArgumentException("No PMID provided!");
        }
        Publication publication = new Publication();
        publication.setId("PMID" + pmid);
        publication.getCvParam().add(createCvParam("MS:1000879", pmid.toString(), "PubMed identifier", MS_CV));
        String refLine;
        try {
            refLine = PubMedFetcher.getPubMedSummary(Long.toString(pmid)).getRefLine();
        } catch (URISyntaxException | IOException e) {
            logger.error("Problems getting reference line from PubMed " + e.getMessage());
            refLine = "No refLine for PMID: " + pmid; // ToDo: better default value?
        }
        publication.getCvParam().add(createCvParam("PRIDE:0000400", refLine, "Reference", PRIDE_CV));
        return publication;
    }

    /**
     * Gets a Publication from a DOI
     * @param doi the DOI
     * @return the Publication object
     */
    public Publication getPublicationDoi(String doi) {
        if (doi==null || doi.isEmpty()) {
            throw new IllegalArgumentException("No DOI provided!");
        }
        Publication publication = new Publication();
        // add the doi
        publication.setId("DOI-" + doi.replaceAll("[^A-Za-z0-9]", "_"));
        publication.getCvParam().add(createCvParam("MS:1001922", doi, "Digital Object Identifier (DOI)", MS_CV));
        return publication;
    }

    /**
     * Gets a list of allowed CVs
     * @return CvList.
     */
    protected CvList getCvList() {
        CvList list = new CvList();
        list.getCv().add(MS_CV);
        list.getCv().add(PRIDE_CV);
        list.getCv().add(MOD_CV);
        list.getCv().add(UNIMOD_CV);
        return list;
    }

    /**
     * Method to retrieve keyword list from the summary file.
     * Now also supports project tags, e.g. parent projects or curator keywords.
     *
     * @param submissionSummary  the object representing the PX submission summary file content.
     * @return a KeywordList with all the keywords mentioned in the submission summary file.
     */
    public KeywordList getKeywordList(Submission submissionSummary) {
        KeywordList keywordList = new KeywordList();
        keywordList.getCvParam().add(createCvParam(MS_1001925, submissionSummary.getProjectMetaData().getKeywords(), SUBMITTER_KEYWORD, MS_CV));
        Set<String> projectTags = submissionSummary.getProjectMetaData().getProjectTags();
        if (projectTags!=null && projectTags.size()>0) {
            HashSet<String> allPossibleCuratorTags = new HashSet<>(Arrays.asList(BIOLOGICAL, BIOMEDICAL, CARDIOVASCULAR, HIGHLIGHTED, TECHNICAL, METAPROTEOMICS));
            for (String tag : projectTags) {
                if (allPossibleCuratorTags.contains(tag)) {
                    keywordList.getCvParam().add(createCvParam(MS_1001926, tag, CURATOR_KEYWORD, MS_CV));
                }   else {
                    keywordList.getCvParam().add(createCvParam(MS_1002340, tag, PROTEOME_XCHANGE_PROJECT_TAG, MS_CV));
                }
            }
        }
        return keywordList;
    }

    /**
     * Method to extract modifications from summary file
     * @param submissionSummary the submission summary object of the project
     * @return ModificationList.
     */
    // Note: this will primarily look at project level, and only look at result file level if no annotation was found
    public ModificationList getModificationList(Submission submissionSummary) {
        ModificationList list = new ModificationList();
        // ToDo: take into account that modifications on project level are not mandatory for complete submissions
        // the modification annotation is mandatory in the submission summary file AND the PX XML
        // however, in the summary file modifications can be annotated at project level or for each result file (in case of complete submissions)
        Set<uk.ac.ebi.pride.data.model.CvParam> modificationSet = submissionSummary.getProjectMetaData().getModifications();
        if (modificationSet == null) {
            modificationSet = new HashSet<>();
        }
        if (modificationSet.isEmpty()) {
            // maybe we are dealing with a complete submission and the modifications have not been gathered at project level
            // so we are looking at the per result file sample data level
            for (DataFile dataFile : submissionSummary.getDataFiles()) {
                if (dataFile.getSampleMetaData() != null) {
                    Set<uk.ac.ebi.pride.data.model.CvParam> mods = dataFile.getSampleMetaData().getMetaData(SampleMetaData.Type.MODIFICATION);
                    if (mods != null) {
                        modificationSet.addAll(mods);
                    }
                }
            }
        }
        // we should have modifications by now, since they are mandatory, we break if we have not found any
        Assert.isTrue(!modificationSet.isEmpty(), "Modification annotation is mandatory submissions!");
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : modificationSet) {
            // check if we have PSI-MOD or UNIMOD ontology terms
            if (cvParam.getCvLabel().equalsIgnoreCase("psi-mod") || cvParam.getCvLabel().equalsIgnoreCase("mod")) {
                list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), MOD_CV));
            } else if (cvParam.getCvLabel().equalsIgnoreCase("unimod")) {
                list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), UNIMOD_CV));
            } else if (cvParam.getCvLabel().equalsIgnoreCase("ms") && cvParam.getAccession().equalsIgnoreCase("MS:1001460")) {
                list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), MS_CV));
            } else if (modificationSet.size()==1 && cvParam.getCvLabel().equalsIgnoreCase("pride") && cvParam.getAccession().equalsIgnoreCase("PRIDE:0000398")) {
                list.getCvParam().add(createCvParam(cvParam.getAccession(), cvParam.getValue(), cvParam.getName(), PRIDE_CV));
            } else {
                // That should never happen, since the validation pipeline should have checked this before.
                String msg = "Found unknown modification CV: " + cvParam.getCvLabel();
                logger.error(msg);
                throw new IllegalStateException(msg);
            }
        }
        return list;
    }

    /**
     * Method to extract instrument information from summary file
     * @param submissionSummary the submission summary object of the project
     * @return InstrumentList.
     */
    public InstrumentList getInstrumentList(Submission submissionSummary) {
        InstrumentList list = new InstrumentList();
        // the instrument annotation is mandatory in the submission summary file AND the PX XML
        int instrumentNum = 1; // artificial counter to give each instrument a unique id
        Set<uk.ac.ebi.pride.data.model.CvParam> instrumentSet = submissionSummary.getProjectMetaData().getInstruments();
        Assert.notNull(instrumentSet, "Instrument annotation is mandatory in the submission summary file!");
        for (uk.ac.ebi.pride.data.model.CvParam auxInstrument : instrumentSet) {
            Instrument instrument = new Instrument();
            instrument.setId("Instrument_" + instrumentNum++);
            instrument.getCvParam().add(convertCvParam(auxInstrument));
            list.getInstrument().add(instrument);
        }
        return list;
    }

    /**
     * Mmethod to get Species information from summary file
     * @param submissionSummary the submission summary object of the project
     * @return SpeciesList.
     */
    public SpeciesList getSpeciesList(Submission submissionSummary) {
        SpeciesList list = new SpeciesList();
        // the species annotation is mandatory in the submission summary file AND the PX XML
        Set<uk.ac.ebi.pride.data.model.CvParam> speciesSet = submissionSummary.getProjectMetaData().getSpecies();
        Assert.notNull(speciesSet, "Species annotation is mandatory in the submission summary file!");
        for (uk.ac.ebi.pride.data.model.CvParam cvParam : speciesSet) {
            Species species = new Species();
            // PX guidelines state that each species has to be represented with two MS CV parameters: one for the name and one for the taxonomy ID
            species.getCvParam().add(createCvParam("MS:1001469", cvParam.getName(), "taxonomy: scientific name", MS_CV));
            species.getCvParam().add(createCvParam("MS:1001467", cvParam.getAccession(), "taxonomy: NCBI TaxID", MS_CV));
            list.getSpecies().add(species);
        }

        return list;
    }

    /**
     * Method to add Dataset identifier information
     * @param projectAccession the submission summary object of the project
     * @param withDOI including a DOU or not.
     * @return DatasetIdentifierList.
     */
    // ToDo: take submissions into account that refer to previous datasets/submissions
     DatasetIdentifierList getDatasetIdentifierList(String projectAccession, boolean withDOI) {
        DatasetIdentifierList datasetIdentifierList = new DatasetIdentifierList();
        DatasetIdentifier px = new DatasetIdentifier(); // add the PX accession
        px.getCvParam().add(createCvParam("MS:1001919", projectAccession, "ProteomeXchange accession number", MS_CV));
        datasetIdentifierList.getDatasetIdentifier().add(px);
        if (withDOI) {  // add a corresponding DOI record if requested
            DatasetIdentifier DOI = new DatasetIdentifier();
            DOI.getCvParam().add(createCvParam("MS:1001922", DOI_PREFFIX + "/" + projectAccession, "Digital Object Identifier (DOI)", MS_CV));
            datasetIdentifierList.getDatasetIdentifier().add(DOI);
        }
        return datasetIdentifierList;
    }

    /**
     * Method to create a CV Param.
     * @param accession the term's accession number
     * @param value the term's value
     * @param name the term's name
     * @param cvRef the term's ontology
     * @return
     */
    CvParam createCvParam(String accession, String value, String name, Cv cvRef) {
        CvParam cvParam = new CvParam();
        cvParam.setAccession(accession.trim());
        cvParam.setValue(value == null ? null : value.trim());
        cvParam.setName(name.trim());
        cvParam.setCvRef(cvRef);
        return cvParam;
    }

    /**
     * Convert a uk.ac.ebi.pride.data.model.CvParam to a uk.ac.ebi.pride.archive.px.model.CvParam.
     * @param cvParam The CV Parameter to convert
     * @return The converted CV Parameter
     */
    private CvParam convertCvParam(uk.ac.ebi.pride.data.model.CvParam cvParam) {
        if (cvParam.getCvLabel().trim().equalsIgnoreCase(MS_CV.getId()) || cvParam.getCvLabel().trim().equalsIgnoreCase("PSI-MS")) {
            return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), MS_CV);
        } else if (cvParam.getCvLabel().trim().equalsIgnoreCase(PRIDE_CV.getId())) {
            return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), PRIDE_CV);
        } else if (cvParam.getCvLabel().trim().equalsIgnoreCase(UNIMOD_CV.getId())) {
            return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), UNIMOD_CV);
        } else if (cvParam.getCvLabel().trim().equalsIgnoreCase(MOD_CV.getId())) {
            return createCvParam(cvParam.getAccession(),cvParam.getValue(), cvParam.getName(), MOD_CV);
        } else {
            throw new IllegalArgumentException("Not a valid CV :" + cvParam.getCvLabel() + "! PX XML only supports the following CVs: MS, PRIDE, MOD, UNIMOD.");
        }
    }

    /**
     * Creates a list of files for the dataset
     * @param submissionSummary the submission summary object of the project
     * @param datasetPathFragment the path fragment
     * @return DatasetFileList.
     */
    protected DatasetFileList createDatasetFileList(Submission submissionSummary, String datasetPathFragment) {
        DatasetFileList list = new DatasetFileList();
        // create a link to the public FTP location for each file of the dataset
        for (DataFile dataFile : submissionSummary.getDataFiles()) {
            DatasetFile df = new DatasetFile();
            CvParam extraUrlLink = null;
            df.setId("FILE_"+dataFile.getFileId()); // ID to uniquely identify the DatasetFile
            String fileName = dataFile.getFile().getName();
            df.setName(fileName);
            String fileUri = FTP + "/" + datasetPathFragment + "/" + fileName;
            CvParam fileParam;
            Set<String> allowedAltDomains = new HashSet<>();
            allowedAltDomains.add(FRED_LAVANDER_LAB_SWE);
            switch (dataFile.getFileType()) {
                case RAW    : fileParam = createCvParam("PRIDE:0000404", fileUri, "Associated raw file URI", PRIDE_CV);
                    if (dataFile.getUrl()!=null && dataFile.getUrl().toString().trim().length()>0) {
                        try {
                            URI uri = new URI(dataFile.getUrl().toString().trim());
                            String domain = uri.getHost();
                            if (allowedAltDomains.contains(domain)) {
                                extraUrlLink =  createCvParam("PRIDE:0000448", dataFile.getUrl().toString().trim(), "Additional associated raw file URI", PRIDE_CV);
                            } else {
                                logger.error("Alternative URL's domain not allowed: " + domain);                                        }
                        } catch (URISyntaxException urise) {
                            logger.error("Error checking alternative URL: " +  dataFile.getUrl().toString().trim());
                            logger.error(urise.toString());
                        }
                    }
                    break;
                case RESULT : fileParam = createCvParam("PRIDE:0000407", fileUri, "Result file URI", PRIDE_CV);
                    break;
                case SEARCH : fileParam = createCvParam("PRIDE:0000408", fileUri, "Search engine output file URI", PRIDE_CV);
                    break;
                case PEAK   : fileParam = createCvParam("PRIDE:0000409", fileUri, "Peak list file URI", PRIDE_CV);
                    break;
                case GEL   : fileParam = createCvParam("PRIDE:0000449", fileUri, "Gel image file URI", PRIDE_CV);
                    break;
                case OTHER  : fileParam = createCvParam("PRIDE:0000410", fileUri, "'Other' type file URI", PRIDE_CV);
                    break;
                default     : fileParam = createCvParam("PRIDE:0000403", fileUri, "Associated file URI", PRIDE_CV);
                    break;
            }
            df.getCvParam().add(fileParam);
            if (extraUrlLink!=null) {
                df.getCvParam().add(extraUrlLink);
            }
            // ToDo (imminently): extra filetype support for 'fasta' and 'spectrum library'
            // ToDo (future): calculate and add checksum for file
            list.getDatasetFile().add(df);
        }
        return list;
    }

    /**
     * Creates a repository record list,
     * @param submissionSummary the submission summary object of the project
     * @param pxAccession the PX accession number
     * @return
     */
     RepositoryRecordList createRepositoryRecordList(Submission submissionSummary, String pxAccession) {
        RepositoryRecordList list = new RepositoryRecordList(); // create a PRIDE repository link for the whole project
        RepositoryRecord record = new RepositoryRecord();
        record.setRepositoryID(HostingRepositoryType.PRIDE);
        record.setUri(PRIDE_REPO_PROJECT_BASE_URL + pxAccession);
        record.setLabel("PRIDE project");
        record.setName(submissionSummary.getProjectMetaData().getProjectTitle());
        record.setRecordID(pxAccession);
        list.getRepositoryRecord().add(record);
        // ToDo (future): create a PRIDE repository link for each assay of the project? (the project link already allows navigation to the assays...)
        return list;
    }

    /**
     * The DatasetOriginList, at the moment, it is hardcoded, all are new submissions who's origin is in the PRIDE PX repository
     * @return DatasetOriginList.
     */
    protected DatasetOriginList getDatasetOriginList() {
        DatasetOriginList list = new DatasetOriginList();
        CvParam cvParam = new CvParam();
        cvParam.setAccession("PRIDE:0000402");
        cvParam.setName("Original data");
        cvParam.setCvRef(PRIDE_CV);
        DatasetOrigin prideOrigin = new DatasetOrigin();
        prideOrigin.getCvParam().add(cvParam);
        list.setDatasetOrigin(prideOrigin);
        return list;
    }

    /**
     * Helper method to return full DatasetLink with FTP location of the dataset
     * @param datasetPathFragment the path fragment
     * @param pxAccession the PX accession number
     * @return
     */
    protected FullDatasetLinkList createFullDatasetLinkList(String datasetPathFragment, String pxAccession)  {
        FullDatasetLinkList fullDatasetLinkList = new FullDatasetLinkList();
        FullDatasetLink prideFtpLink = new FullDatasetLink();
        CvParam ftpParam = createCvParam("PRIDE:0000411", FTP + "/" + datasetPathFragment, "Dataset FTP location", PRIDE_CV);
        prideFtpLink.setCvParam(ftpParam);
        FullDatasetLink prideRepoLink = new FullDatasetLink();
        CvParam repoParam = createCvParam("MS:1001930", PRIDE_REPO_PROJECT_BASE_URL + pxAccession, "PRIDE project URI", MS_CV);
        prideRepoLink.setCvParam(repoParam);
        fullDatasetLinkList.getFullDatasetLink().add(prideFtpLink);
        fullDatasetLinkList.getFullDatasetLink().add(prideRepoLink);
        return fullDatasetLinkList;
    }

    /**
     * Gets the dataset summary for a submission summary
     * @param submissionSummary the submission summary object of the project
     * @return DatasetSummary.
     */
     DatasetSummary getDatasetSummary(Submission submissionSummary) {
        DatasetSummary datasetSummary = new DatasetSummary();
        datasetSummary.setTitle(submissionSummary.getProjectMetaData().getProjectTitle());
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

    /**
     *  Helper method to create RepositorySupportType for either complete or partial submissions (other types are currently not supported and will return null).
     * @param type the submission type
     * @return RepositorySupportType
     */
    protected RepositorySupportType createRepositorySupport(SubmissionType type) {
        RepositorySupportType repositorySupport = new RepositorySupportType();
        CvParam cvparam;
        if (type == SubmissionType.COMPLETE) {
            cvparam = createCvParam("PRIDE:0000416", null, "Supported dataset by repository", PRIDE_CV);
        } else if (type == SubmissionType.PARTIAL) {
            cvparam = createCvParam("PRIDE:0000417", null, "Unsupported dataset by repository", PRIDE_CV);
        } else {
            logger.error("Encoutered unexpected submission type: " + type.name());
            return null;
        }
        repositorySupport.setCvParam(cvparam);
        return repositorySupport;
    }

    /**
     * Helper method to create a ReviewLevelType, either peer-reviewed or non-peer-reviewed
     * @param peerReviewed peer reviewed dataet, or not.
     * @return ReviewLevelType.
     */
    protected ReviewLevelType createReviewLevel(boolean peerReviewed) {
        ReviewLevelType reviewLevel = new ReviewLevelType();
        CvParam cvparam ;
        if (peerReviewed) {
            cvparam = createCvParam("PRIDE:0000414", null, "Peer-reviewed dataset", PRIDE_CV);
        } else {
            cvparam = createCvParam("PRIDE:0000415", null, "Non peer-reviewed dataset", PRIDE_CV);
        }
        reviewLevel.setCvParam(cvparam);
        return reviewLevel;
    }

    /**
     *  Method to extract the contact list from the summary file
     * @param submissionSummary  the submission summary object of the project
     * @return ContactList
     */
     ContactList getContactList(Submission submissionSummary) {
        ContactList list = new ContactList();
        uk.ac.ebi.pride.data.model.Contact auxSubmitter = submissionSummary.getProjectMetaData().getSubmitterContact();
        Contact submitter = new Contact();
        submitter.setId("project_submitter"); // assign a unique ID to this contact
        submitter.getCvParam().add(createCvParam("MS:1000586", auxSubmitter.getName(), "contact name", MS_CV));
        submitter.getCvParam().add(createCvParam("MS:1000589", auxSubmitter.getEmail(), "contact email", MS_CV));
        submitter.getCvParam().add(createCvParam("MS:1000590", auxSubmitter.getAffiliation(), "contact affiliation", MS_CV));
        submitter.getCvParam().add(createCvParam("MS:1002037", null, "dataset submitter", MS_CV));
        list.getContact().add(submitter);
        uk.ac.ebi.pride.data.model.Contact auxLabHead = submissionSummary.getProjectMetaData().getLabHeadContact();
        if (auxLabHead != null && auxLabHead.getName() != null && !auxLabHead.getName().trim().isEmpty()) {
            Contact labHead = new Contact();
            labHead.setId("project_lab_head"); // assign a unique ID to this contact
            labHead.getCvParam().add(createCvParam("MS:1002332", null, "lab head", MS_CV));
            labHead.getCvParam().add(createCvParam("MS:1000586", auxLabHead.getName(), "contact name", MS_CV));
            if (auxLabHead.getEmail() != null && !auxLabHead.getEmail().trim().isEmpty()) {
                labHead.getCvParam().add(createCvParam("MS:1000589", auxLabHead.getEmail(), "contact email", MS_CV));
            }
            if (auxLabHead.getAffiliation() != null && !auxLabHead.getAffiliation().trim().isEmpty()) {
                labHead.getCvParam().add(createCvParam("MS:1000590", auxLabHead.getAffiliation(), "contact affiliation", MS_CV));
            }
            list.getContact().add(labHead);
        } else {
            logger.warn("No lab head information found while generating PX XML!");
        }
        return list;
    }
}
