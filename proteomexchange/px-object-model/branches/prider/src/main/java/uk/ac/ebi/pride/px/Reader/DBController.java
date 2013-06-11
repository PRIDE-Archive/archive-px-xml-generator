package uk.ac.ebi.pride.px.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.prider.repo.assay.*;
import uk.ac.ebi.pride.prider.repo.param.CvParamRepository;
import uk.ac.ebi.pride.prider.repo.project.*;
import uk.ac.ebi.pride.prider.repo.user.User;
import uk.ac.ebi.pride.pubmed.PubMedFetcher;
import uk.ac.ebi.pride.pubmed.model.PubMedSummary;
import uk.ac.ebi.pride.px.model.Contact;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.util.PrideInspectorUrlGenerator;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Dani Rios
 * @author Jose A. Dianes (PRIDE-R updates and refactoring)
 * @version $Id$
 *
 */
public class DBController {

    ProjectRepository projectRepository;
    AssayRepository assayRepository;
    CvParamRepository cvParamRepository;

    /**
     * Database connection object
     */
    public static final String PRIDE_URL = "http://www.ebi.ac.uk/pride/simpleSearch.do?simpleSearchValue="; // TODO - Update this with final PRIDE-R, put it as a parameter as well
    private static final String NCBI_URL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi";

    //will use that map to store the relation between String->publication_ref
    private Map<String, Publication> publicationMap = new HashMap<String, Publication>();
    private Map<Long, Instrument> instrumentMap = new HashMap<Long, Instrument>(); // map to keep track of the instruments per assay
//    private Connection DBConnection = null;

    //    Logger object
    Logger logger = LoggerFactory.getLogger(DBController.class);

    //counter for publication ID
//    private int publicationCounter = 1;

//    public DBController(DataSource dataSource) throws SQLException {
//
//        DBConnection = dataSource.getConnection();
//
//    }
//
//    public DBController() {
////        get properties file
//        Properties properties = new Properties();
//        InputStream is = this.getClass().getClassLoader().getResourceAsStream("database.properties");
//        try {
//            properties.load(is);
//        } catch (IOException e) {
//            logger.error(e.getMessage(), e);
//        }
//        //create connection
//        //load driver
//
//        try {
//            Class.forName(properties.getProperty("driver"));
//        } catch (ClassNotFoundException e) {
//            logger.error(e.getMessage(), e);
//        }
//        String url_connection = properties.getProperty("protocol") + ':' + properties.getProperty("subprotocol") + ':' + properties.getProperty("alias");
//        logger.debug("Connecting to " + url_connection);
//        try {
//            DBConnection = DriverManager.getConnection(url_connection, properties.getProperty("user"), properties.getProperty("password"));
//        } catch (SQLException err) {
//            logger.error(err.getMessage(), err);
//        }
//    }

    public void setProjectRepository(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void setAssayRepository(AssayRepository assayRepository) {
        this.assayRepository = assayRepository;
    }

    public void setCvParamRepository(CvParamRepository cvParamRepository) {
        this.cvParamRepository = cvParamRepository;
    }


    /**
     * Get all the assay IDs for a given project accession
     *
     * @param projectAccession the accession for the project
     * @return a List<Long> containing the assay accessions belonging to the specified project.
     */
    public List<Long> getAssayIds(String projectAccession) {
        List<Long> assayIds = new ArrayList<Long>();

        Long projectId = projectRepository.findByAccession(projectAccession).getId();

        for (Assay assay: assayRepository.findAllByProjectId(projectId)) {
            assayIds.add(assay.getId());
        }

        return assayIds;
    }

    /**
     * Get the list of instruments for a given collection of assay IDs
     *
     * @param assayIds
     * @return
     */
    public InstrumentList getInstrumentList(Collection<Long> assayIds) {
        InstrumentList instrumentList = new InstrumentList();

        int i = 1; // counter of all instruments across all assays
        for (Long assayId: assayIds) {
            Collection<uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument> priderInstruments = assayRepository.findOne(assayId).getInstruments();
            for (uk.ac.ebi.pride.prider.repo.assay.instrument.Instrument priderInstrument: priderInstruments) {
                String id = "INSTRUMENT_" + i;
                i++;
                uk.ac.ebi.pride.px.model.Instrument instrument = new uk.ac.ebi.pride.px.model.Instrument();
                instrument.setId(id);

                //and add the params
                CvParam cvParam = new CvParam();
                cvParam.setCvRef(priderInstrument.getCvParam().getCvLabel());
                cvParam.setName(priderInstrument.getCvParam().getName());
                cvParam.setAccession(priderInstrument.getCvParam().getAccession());
                // add the param to the instrument
                instrument.getCvParam().add(cvParam);

                // keep track of the instrument for this assay
                // ToDo: decide what to do if there are multiple instrument annotations for one assay
                instrumentMap.put(assayId, instrument);


//                addKeysMap(instrumentMap, instrument, assayId);
                instrumentList.getInstrument().add(instrument);
            }
        }
        return instrumentList;
    }




    /**
     * Get list of species for a given project accession
     * @param projectAccession
     * @return
     */
    public SpeciesList getSpecies(String projectAccession) {
        SpeciesList speciesList = new SpeciesList();

        logger.info("Retrieving species information for project: " + projectAccession);
        Collection<ProjectSampleCvParam> priderSampleCvParams = projectRepository.findByAccession(projectAccession).getSamples();
        for (ProjectSampleCvParam priderSampleCvParam: priderSampleCvParams) {
            if ("NEWT".equals(priderSampleCvParam.getCvLabel()))  {
                Species species = new Species();

                // and add the params
                // The guidelines for PX XML specifiy that a species has to be represented with the following MS terms:
                // MS:1001469; "taxonomy: scientific name" and
                // MS:1001467; "taxonomy: NCBI TaxID"
                CvParam cvParam = new CvParam();
                cvParam.setCvRef("MS");
                cvParam.setName("taxonomy: scientific name");
                cvParam.setAccession("MS:1001469");
                cvParam.setValue(priderSampleCvParam.getName());
                species.getCvParam().add(cvParam);
                CvParam cvParam2 = new CvParam();
                cvParam2.setCvRef("MS");
                cvParam2.setName("taxonomy: NCBI TaxID");
                cvParam2.setAccession("MS:1001467");
                cvParam2.setValue(priderSampleCvParam.getAccession());

                // add the params defining the species
                species.getCvParam().add(cvParam);
                species.getCvParam().add(cvParam2);

                speciesList.setSpecies(species);
            }
        }

        return speciesList;
    }

    /**
     * Get a ModificationList for a given project accession
     * @param projectAccession
     * @return
     */
    public ModificationList getModificationList(String projectAccession) {
        ModificationList modificationList = new ModificationList();

        Collection<ProjectPTM> priderProjectPtms = projectRepository.findByAccession(projectAccession).getPtms();

        for (ProjectPTM prideProjectPtm: priderProjectPtms) {
            //and add the params
            CvParam cvParam = new CvParam();
            cvParam.setAccession(prideProjectPtm.getAccession());
            cvParam.setName(prideProjectPtm.getName());
            cvParam.setCvRef(prideProjectPtm.getCvLabel());
            cvParam.setValue(prideProjectPtm.getValue());
            modificationList.getCvParam().add(cvParam);
        }

        if (modificationList.getCvParam().isEmpty()) {
            //if there are no modifications, add new CV param
            CvParam cvParam = new CvParam();
            cvParam.setAccession("PRIDE:0000398");
            cvParam.setName("No applicable mass modifications");
            cvParam.setCvRef("PRIDE");
            modificationList.getCvParam().add(cvParam);
        }

        return modificationList;
    }

    /**
     * Get a ModificationList for a given assay id
     * @param assayId
     * @return
     */
    public ModificationList getAssayModificationList(Long assayId) {
        ModificationList modificationList = new ModificationList();

        Collection<AssayPTM> assayPtms = assayRepository.findOne(assayId).getPtms();

        for (AssayPTM assayPtm: assayPtms) {
            //and add the params
            CvParam cvParam = new CvParam();
            cvParam.setAccession(assayPtm.getAccession());
            cvParam.setName(assayPtm.getName());
            cvParam.setCvRef(assayPtm.getCvLabel());
            cvParam.setValue(assayPtm.getValue());
            modificationList.getCvParam().add(cvParam);
        }

        if (modificationList.getCvParam().isEmpty()) {
            //if there are no modifications, add new CV param
            CvParam cvParam = new CvParam();
            cvParam.setAccession("PRIDE:0000398");
            cvParam.setName("No applicable mass modifications");
            cvParam.setCvRef("PRIDE");
            modificationList.getCvParam().add(cvParam);
        }

        return modificationList;
    }

    /**
     * Get a contact list for a given project accession, excluding those included in contactEmails
     *
     * @param projectAccession
     * @return
     */
    public List<Contact> getContactList(String projectAccession) {
        //ToDo: check where the filter for contactEmails is used
        List<Contact> list = new ArrayList<Contact>();
        User submitter = projectRepository.findByAccession(projectAccession).getSubmitter();

        // create submitter contact cvparam
        Contact contact = new Contact();
        contact.setId(submitter.getEmail());
        // add name as a CV param
        if (submitter.getFirstName() != null & submitter.getLastName()!= null) {
            CvParam nameParam = new CvParam();
            nameParam.setValue(submitter.getFirstName() + " " + submitter.getLastName());
            nameParam.setCvRef("MS"); //MS cv for contact address
            nameParam.setAccession("MS:1000586");
            nameParam.setName("contact name");
            contact.getCvParam().add(nameParam);
        }
        // add affiliation as a CV param
        if (submitter.getAffiliation() != null) {
            CvParam cvParam = new CvParam();
            cvParam.setValue(submitter.getAffiliation());
            cvParam.setCvRef("MS"); //will use CV for contact organization as the affiliation
            cvParam.setAccession("MS:1000590");
            cvParam.setName("contact affiliation");
            contact.getCvParam().add(cvParam);
        }
        // add email as a CV param
        CvParam cvParamEmail = new CvParam();
        cvParamEmail.setValue(submitter.getEmail());
        cvParamEmail.setCvRef("MS");
        cvParamEmail.setAccession("MS:1000589"); //MS param for contact email
        cvParamEmail.setName("contact email");
        contact.getCvParam().add(cvParamEmail);

        // ToDo: once available repeat for other contacts like the Lab-Head
        // add the contact to the list
        list.add(contact);

        return list;
    }


    //private method that will convert a List<String> of accessions
    //in a String separating accessions with commas for the SQL

    private static String preparePlaceHolders(int length) {
        StringBuilder builder = new StringBuilder(length * 2 - 1);
        for (int i = 0; i < length; i++) {
            if (i > 0) builder.append(',');
            builder.append('?');
        }
        return builder.toString();
    }

    private static void setValues(PreparedStatement preparedStatement, Object[] values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            preparedStatement.setObject(i + 1, values[i]);
        }
    }


//    //helper method that will add all the assayId to the Map for that particular id
//    private static <T extends PXObject> void addKeysMap(Map<Long, T> idMap, T object, Long assayId) {
//        if (idMap.containsKey(assayId)) {
//
//            idMap.put(assayId, object);
//        }
//    }
//
    //this information comes from summary file at the moment
//    public DatasetSummary getDatasetSummary(long experimentID) {
//        DatasetSummary datasetSummary = new DatasetSummary();
//        try {
//            String query = "SELECT pe.title, ppp.value " +
//                    "FROM pride_experiment pe LEFT JOIN pride_experiment_param ppp ON pe.experiment_id = ppp.parent_element_fk " +
//                    "WHERE pe.experiment_id = ? and " +
//                    "ppp.accession = 'PRIDE:0000097'";
//            PreparedStatement st = DBConnection.prepareStatement(query);
//            st.setLong(1, experimentID);
//
//            ResultSet rs = st.executeQuery();
//            while (rs.next()) {
//                String title = rs.getString(1);
//                String description = rs.getString(2);
//                datasetSummary.setTitle(title);
//                datasetSummary.setDescription(description);
//                datasetSummary.setAnnounceDate(Calendar.getInstance());
//                datasetSummary.setHostingRepository(HostingRepositoryType.PRIDE);
//            }
//            rs.close();
//        } catch (SQLException e) {
//            logger.error(e.getMessage(), e);
//        }
//        return datasetSummary;
//    }









    public static String extractEmail(String email) {
        Pattern emailPattern = Pattern.compile(
                "([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)");
        Matcher matcher = emailPattern.matcher(email);
        if (matcher.find()) return matcher.group();
        else return "";
    }

    /**
     * Returns the PubMedId for the first project reference or NULL if no references
     *
     * @param projectAccession
     * @return
     * @throws SQLException
     */
    public String getPubmedID(String projectAccession) throws SQLException {
        Iterator<Reference> referencesId = projectRepository.findByAccession(projectAccession).getReferences().iterator();
        if (referencesId.hasNext())
            return ""+referencesId.next().getPubmedId();
        else
            return null;
//        String query = "SELECT pubmed_id from px_submission where px_accession = ?";
//
//        PreparedStatement st = DBConnection.prepareStatement(query);
//        st.setString(1, projectAccession);
//
//        ResultSet rs = st.executeQuery();
//        rs.next();
//        return rs.getString(1);
    }

    public PublicationList getPublicationList(String projectAccession) throws SQLException {
        PublicationList publicationList = new PublicationList();
        Publication publication = new Publication();
        String pubmedID = getPubmedID(projectAccession);
        if (pubmedID == null) {
            //no pubmed, so no publication
            CvParam cvParam = new CvParam();
            cvParam.setCvRef("PRIDE");
            cvParam.setName("Dataset with its publication pending");
            cvParam.setAccession("PRIDE:0000432");
            publication.setId("pending");
            publication.getCvParam().add(cvParam);
            publicationList.getPublication().add(publication);
        } else {
            //add pubmedID
            PubMedFetcher pubMedFetcher = new PubMedFetcher(NCBI_URL);
            publication.setId("PMID" + pubmedID);
            publication.getCvParam().add(createCvParam("MS:1000879", pubmedID, "PubMed identifier", "MS"));
            //and the reference
            //get reference line using external library
            PubMedSummary pubMedSummary = null;
            try {
                pubMedSummary = pubMedFetcher.getPubMedSummary(pubmedID);
            } catch (IOException e) {
                logger.error("Problems getting reference line from pubMed " + e.getMessage());
            }
            String reference_line = pubMedSummary.getReference();
            publication.getCvParam().add(createCvParam("PRIDE:0000400", reference_line, "Reference", "PRIDE"));
            publicationList.getPublication().add(publication);
        }
        publicationMap.put(projectAccession, publication);
        return publicationList;
    }

    private static CvParam createCvParam(String accession, String value, String name, String cvRef) {

        CvParam cvParam = new CvParam();
        cvParam.setAccession(accession);
        cvParam.setValue(value);
        cvParam.setName(name);
        cvParam.setCvRef(cvRef);

        return cvParam;
    }

//    public FullDatasetLinkList getDatasetIdentifier(List<Long> assayIds) {
//        FullDatasetLinkList datasetLinkList = new FullDatasetLinkList();
//
//        //add the PRIDE URI
//        List<String> accessions = getAssayAccessions(assayIds);
//        for (String accession : accessions) {
//            FullDatasetLink datasetLink = new FullDatasetLink();
//            CvParam cvParam = new CvParam();
//            cvParam.setCvRef("MS");
//            cvParam.setName("PRIDE experiment URI");
//            cvParam.setAccession("MS:1001929");
//            cvParam.setValue(PRIDE_URL + accession);
//            datasetLink.setCvParam(cvParam);
//            datasetLinkList.getFullDatasetLink().add(datasetLink);
//        }
//        //and now add the Tranche if present
//        String query = "SELECT p.accession, pe.value " +
//                "FROM pride_experiment p LEFT JOIN pride_experiment_param pe ON p.experiment_id = pe.parent_element_fk " +
//                "WHERE p.experiment_id IN (%s) " +
//                "AND pe.name = 'Tranche link to raw file'";
//        String sql = String.format(query, preparePlaceHolders(assayIds.size()));
//        try {
//            PreparedStatement st = DBConnection.prepareStatement(sql);
//            setValues(st, assayIds.toArray());
//            ResultSet rs = st.executeQuery();
//            while (rs.next()) {
//                //add the tranche only if present
//                if (rs.getString(2) != null) {
//                    FullDatasetLink datasetLinkTranche = new FullDatasetLink();
//                    String tranche_url = rs.getString(2);
//                    //extract the hash from the url
//                    String tranche_hash = tranche_url.substring(tranche_url.indexOf("hash=") + 5, tranche_url.length());
//                    //and create the param
//                    CvParam trancheParam = new CvParam();
//                    trancheParam.setCvRef("MS");
//                    trancheParam.setName("Tranche project hash");
//                    trancheParam.setAccession("MS:1001928");
//                    trancheParam.setValue(tranche_hash);
//                    datasetLinkTranche.setCvParam(trancheParam);
//                    datasetLinkList.getFullDatasetLink().add(datasetLinkTranche);
//                }
//            }
//            rs.close();
//        } catch (SQLException e) {
//            logger.error(e.getMessage(), e);
//        }
//        return datasetLinkList;
//    }

    public RepositoryRecord getRepositoryRecord(long assayId) {
        RepositoryRecord repositoryRecord = new RepositoryRecord();

        Assay assay = assayRepository.findOne(assayId);

        repositoryRecord.setRecordID(assay.getAccession());
        repositoryRecord.setRepositoryID(HostingRepositoryType.PRIDE);
        repositoryRecord.setUri(PRIDE_URL + assay.getAccession()); //set link to experiment in PRIDE-R
        repositoryRecord.setLabel(assay.getShortLabel());
        repositoryRecord.setName(assay.getTitle());

        return repositoryRecord;
    }

    /**
     * Get a Sample list for a given assay id
     *
     * @param assayId
     * @return
     */
    public SampleList getSampleList(long assayId) {
        SampleList sampleList = new SampleList();
        // in PRIDE we only have one sample per assay
        Sample sample = new Sample();
        sample.setName("sample_" + assayId);
        sampleList.getSample().add(sample);

        // now populate the sample with the cvparams describing it
        Assay assay = assayRepository.findOne(assayId);
        Collection<AssaySampleCvParam> assaySampleCvParams = assay.getSamples();
        for (AssaySampleCvParam assaySampleCvParam: assaySampleCvParams) {
            CvParam cvParam = new CvParam();
            cvParam.setName(assaySampleCvParam.getName());
            cvParam.setCvRef(assaySampleCvParam.getCvLabel());
            cvParam.setAccession(assaySampleCvParam.getAccession());
            sample.getCvParam().add(cvParam);
        }

        return sampleList;
    }

    //method to return a datasetlink to point to PrideInspector url for given experiments
    public FullDatasetLink generatePrideInspectorURL(List<Long> experimentIDs) throws SubmissionFileException {
        List<String> accessions = getAssayAccessions(experimentIDs);
        Set<String> accessionsSet = new HashSet<String>(accessions);
        PrideInspectorUrlGenerator prideInspectorUrlGenerator = new PrideInspectorUrlGenerator();
        String tinyURL = prideInspectorUrlGenerator.generate(accessionsSet);
        FullDatasetLink fullDatasetLink = new FullDatasetLink();
        CvParam cvParam = new CvParam();
        cvParam.setName("Tiny URL");
        cvParam.setCvRef("PRIDE");
        cvParam.setAccession("PRIDE:????");
        cvParam.setValue(tinyURL);
        fullDatasetLink.setCvParam(cvParam);
        return fullDatasetLink;
    }

    //helper method, will return a Ref for a certain type of elements (right now only publication or instrument)
//    public Ref getRef(String type, long assayId) {
//        Ref ref = new Ref();
//
//        if (type.equals("instrument")) {
//            ref.setRef(instrumentMap.get(assayId));
//        } else if (type.equals("publication")) {
//            if (publicationMap.containsKey(assayId)) {
//                ref.setRef(publicationMap.get(assayId));
//            } else {
//                //there is no publication, add the special param for that
//                ref.setRef(publicationMap.get(new Long(0)));
//            }
//        } else {
//            logger.error("Trying to return an invalid ref: allowed types \"publication\" and \"instrument\"");
//        }
//        return ref;
//    }

    public Ref getPublicationRef(String projectAccession) {
        Ref ref = new Ref();

        if (publicationMap.containsKey(projectAccession)) {
            ref.setRef(publicationMap.get(projectAccession));
        } else {
            //there is no publication, add the special param for that
            ref.setRef(publicationMap.get(new Long(0)));
        }

        return ref;
    }

    public Ref getInstrumentRef(long assayId) {
        Ref ref = new Ref();
        Instrument instrument = instrumentMap.get(assayId);
        if (instrument != null) {
            logger.info("Setting instrument reference for Assay " + assayId + " and instrument: " + instrument.getId());
        } else {
            logger.info("No instrument for Assay: " + assayId);
        }
        ref.setRef(instrument);
        return ref;
    }

    public Date getPublicationDate(String projectAccession) {
        return projectRepository.findByAccession(projectAccession).getPublicationDate();
    }

    //   at the moment keyword list will always come from the submission file
//   public KeywordList getKeywordList(List<Long> assayIds) {
//        KeywordList keywordList = new KeywordList();
//        String[] paramAccessions = new String[]{"MS:1001923", "MS:1001924", "MS:1001925", "MS:1001926"};
//        keywordList.getCvParam().addAll(getExperimentParams(assayIds, Arrays.asList(paramAccessions)));
//        return keywordList;
//    }
//
    //helper method, for a list pf experiments and paramAccessions, will get from the pride_experiment_param all the Params
    // associated. Very useful in ProteomeXchange, most data stored in that table
    private Set<CvParam> getExperimentParams(List<Long> assayIds, List<String> paramAccessions) {
        Set<CvParam> cvParams = new HashSet<CvParam>();

        for (Long assayId: assayIds) {
            Assay assay = assayRepository.findOne(assayId);
            Collection<AssayGroupCvParam> assayCvParams = assay.getAssayGroupCvParams();
            for (AssayGroupCvParam assayGroupCvParam: assayCvParams) {
                if (paramAccessions.contains(assayGroupCvParam.getAccession())) {
                    CvParam cvParam = new CvParam();
                    cvParam.setAccession(assayGroupCvParam.getAccession());
                    cvParam.setValue(assayGroupCvParam.getValue());
                    cvParam.setName(assayGroupCvParam.getName());
                    cvParam.setCvRef(assayGroupCvParam.getCvLabel());
                    cvParams.add(cvParam);
                }
            }
        }

        return cvParams;
    }

    //helper method to concatenate 2 Object arrays in a single one
    private Object[] concatArrays(Object[] array1, Object[] array2) {
        Object[] newArray = new Object[array1.length + array2.length];
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

    public DatasetIdentifierList getDatasetIdentifierList(List<Long> experimentIDs) {
        DatasetIdentifierList datasetIdentifierList = new DatasetIdentifierList();
        //first, get Dataset for ProteomeXchange ID, if present
        String[] accessionsPX = new String[]{"MS:1001919", "MS:1001921"};
        DatasetIdentifier px = new DatasetIdentifier();
        px.getCvParam().addAll(getExperimentParams(experimentIDs, Arrays.asList(accessionsPX)));
        datasetIdentifierList.getDatasetIdentifier().add(px);
        //now dataset for DOI, if present
        String[] accessionsDOI = new String[]{"MS:1001922"};
        DatasetIdentifier DOI = new DatasetIdentifier();
        DOI.getCvParam().addAll(getExperimentParams(experimentIDs, Arrays.asList(accessionsDOI)));
        datasetIdentifierList.getDatasetIdentifier().add(DOI);
        return datasetIdentifierList;
    }

//    public CvParam getLdcFtpLink(long experimentID) {
//        CvParam cvParam = null;
//
//        String query =
//                "SELECT ppp.accession, ppp.value, ppp.name, ppp.cv_label " +
//                        "FROM pride_experiment pe LEFT JOIN pride_reference_param ppp ON pe.experiment_id = ppp.parent_element_fk " +
//                        "WHERE pe.experiment_id = ?";
//
//        try {
//            PreparedStatement st = DBConnection.prepareStatement(query);
//            st.setLong(1, experimentID);
//            ResultSet rs = st.executeQuery();
//            // process results
//            if (rs.next()) {
//                cvParam = new CvParam();
//                cvParam.setAccession(rs.getString(1));
//                cvParam.setValue(rs.getString(2));
//                cvParam.setName(rs.getString(3));
//                cvParam.setCvRef(rs.getString(4));
//            }
//            rs.close();
//        } catch (SQLException e) {
//            logger.error(e.getMessage(), e);
//        }
//
//        return cvParam;
//
//    }

    /**
     * Helper method to get assay accessions from assay IDs
     *
     * @param assayIds
     * @return
     */
    private List<String> getAssayAccessions(List<Long> assayIds) {
        List<String> accessions = new ArrayList<String>();

        for (Long assayId: assayIds) {
            accessions.add(assayRepository.findOne(assayId).getAccession());
        }

        return accessions;
    }
}
