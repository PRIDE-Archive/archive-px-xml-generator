package uk.ac.ebi.pride.px.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.model.*;
import uk.ac.ebi.pride.px.model.Ref;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: dani
 * Date: 11/10/11
 * Time: 15:25
 * To change this template use File | Settings | File Templates.
 */
public class DBController {
    /**
     * Database connection object
     */
    public static final String PRIDE_URL = "http://www.ebi.ac.uk/pride/simpleSearch.do?simpleSearchValue=";
    //will use that map to store the relation between experiment_id->publication_ref
    private Map<Long, PXObject> publicationMap = new HashMap<Long, PXObject>();
    private Map<Long, PXObject> instrumentMap = new HashMap<Long, PXObject>();
    private Connection DBConnection = null;

    //    Logger object
    Logger logger = LoggerFactory.getLogger(DBController.class);

    public DBController(DataSource dataSource) throws SQLException{

        DBConnection = dataSource.getConnection();

    }

    public DBController() {
//        get properties file
        Properties properties = new Properties();
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("database.properties");
        try {
            properties.load(is);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        //create connection
        //load driver

        try {
            Class.forName(properties.getProperty("driver"));
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
        String url_connection = properties.getProperty("protocol") + ':' + properties.getProperty("subprotocol") + ':' + properties.getProperty("alias");
        logger.debug("Connecting to " + url_connection);
        try {
            DBConnection = DriverManager.getConnection(url_connection, properties.getProperty("user"), properties.getProperty("password"));
        } catch (SQLException err) {
            logger.error(err.getMessage(), err);
        }
    }

    //helper method, for a pxAccession will return all the experimentIds to report
    public List<Long> getExperimentIds(String pxAccession) {
        List<Long> expIds = new ArrayList<Long>();
        try {
            String query = "SELECT parent_element_fk " +
                    "FROM pride_experiment_param " +
                    "WHERE value = ? ";

            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setString(1, pxAccession);

            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                expIds.add(rs.getLong(1));
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return expIds;
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
    

    //helper method that will add all the experimentId, separated by comma, to the Map for that particular id
    private static void addKeysMap(Map<Long, PXObject> idMap, PXObject object, String experimentIDs) {
        String[] expIds = experimentIDs.split(",");
        for (String expId : expIds) {
            idMap.put(new Long(expId), object);
        }
    }

    public DatasetSummary getDatasetSummary(long experimentID) {
        DatasetSummary datasetSummary = new DatasetSummary();
        try {
            String query = "SELECT pe.title, ppp.value " +
                    "FROM pride_experiment pe LEFT JOIN pride_experiment_param ppp ON pe.experiment_id = ppp.parent_element_fk " +
                    "WHERE pe.experiment_id = ? and " +
                    "ppp.accession = 'PRIDE:0000097'";
            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setLong(1, experimentID);

            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String title = rs.getString(1);
                String description = rs.getString(2);
                datasetSummary.setTitle(title);
                datasetSummary.setDescription(description);
                datasetSummary.setAnnounceDate(Calendar.getInstance());
                datasetSummary.setBroadcaster(BroadcasterType.PRIDE);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return datasetSummary;
    }

    //returns SpeciesList for this experiment
    public SpeciesList getSpecies(List<Long> experimentIDs) {
        SpeciesList speciesList = new SpeciesList();
        Species species = new Species();
        String query = "SELECT DISTINCT (ms.name), ms.accession " +
                "from pride_experiment pe, mzdata_sample_param ms " +
                "where ms.cv_label = 'NEWT' and " +
                "ms.parent_element_fk = pe.mz_data_id and " +
                "pe.experiment_id IN (%s)";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String taxonomyID = rs.getString(2);
                // we create 2 Params for the Species: one with the scientific name, the other with the taxonomyID
                //     <cvParam accession="MS:1001469" name="taxonomy: scientific name" cvRef="PSI-MS" value="Manduca sexta"/>
                //     <cvParam accession="MS:1001467" name="taxonomy: NCBI TaxID" cvRef="PSI-MS" value="7130"/>

                CvParam cvParam = new CvParam();
                cvParam.setCvRef("PSI-MS");
                cvParam.setName("taxonomy: scientific name");
                cvParam.setAccession("MS:1001469");
                cvParam.setValue(name);
                species.getCvParam().add(cvParam);
                CvParam cvParam2 = new CvParam();
                cvParam2.setCvRef("PSI-MS");
                cvParam2.setName("taxonomy: NCBI TaxID");
                cvParam2.setAccession("MS:1001467");
                cvParam2.setValue(taxonomyID);
                species.getCvParam().add(cvParam2);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        speciesList.setSpecies(species);
        return speciesList;
    }

    public InstrumentList getInstrumentList(List<Long> experimentIDs) {
        InstrumentList instrumentList = new InstrumentList();
        //TODO:: how can we get the right description for the instrument ??
        String query = "SELECT md.instrument_name, map.name, map.accession, map.cv_label, GROUP_CONCAT(DISTINCT(pe.experiment_id)) " +
                "FROM pride_experiment pe, mzdata_mz_data md LEFT JOIN mzdata_analyzer ma LEFT JOIN mzdata_analyzer_param map ON  ma.analyzer_id = map.parent_element_fk ON md.mz_data_id = ma.mz_data_id " +
                "WHERE pe.mz_data_id = md.mz_data_id " +
                "AND pe.experiment_id IN (%s) " +
                "AND (map.cv_label = 'PSI' or map.cv_label = 'MS')" +
                "GROUP BY md.instrument_name";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());
            ResultSet rs = st.executeQuery();

            while (rs.next()) {
                String id = rs.getString(1);

                //instrumentMap.put(rs.getLong(5), id); //add it to the map for latter reference
                Instrument instrument = new Instrument();
                instrument.setId(id.replaceAll(" ","_"));
                String name = rs.getString(2);
                String accession = rs.getString(3);
                String cvRef = rs.getString(4);

                //and add the params
                CvParam cvParam = new CvParam();
                cvParam.setCvRef(cvRef);
                cvParam.setName(name);
                cvParam.setAccession(accession);
                instrument.setCvParam(cvParam);
                //helper method to add the instrument to all the experiments
                addKeysMap(instrumentMap, instrument, rs.getString(5));
                instrumentList.getInstrument().add(instrument);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return instrumentList;
    }

    public ModificationList getModificationList(List<Long> experimentIDs) {
        ModificationList modificationList = new ModificationList();
        String query = "SELECT distinct(ppm.accession), ppm.name, ppm.cv_label, ppm.value " +
                "FROM pride_identification pi, pride_peptide pp, pride_modification pm, pride_modification_param ppm " +
                "WHERE pi.experiment_id IN (%s) " +
                "AND pi.identification_id = pp.identification_id " +
                "AND pm.peptide_id=pp.peptide_id " +
                "AND ppm.parent_element_fk = pm.modification_id";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {

            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());

            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                //and add the params
                CvParam cvParam = new CvParam();
                cvParam.setAccession(rs.getString(1));
                cvParam.setName(rs.getString(2));
                cvParam.setCvRef(rs.getString(3));
                cvParam.setValue(rs.getString(4));
                modificationList.getCvParam().add(cvParam);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
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

    public ContactList getContactList(List<Long> experimentIDs) {
        ContactList contactList = new ContactList();
        String query = "SELECT DISTINCT(c.contact_name), c.institution, c.contact_info  " +
                "FROM pride_experiment pe, mzdata_contact c, mzdata_mz_data m " +
                "WHERE pe.experiment_id IN (%s) " +
                "AND m.accession_number = pe.accession " +
                "AND m.mz_data_id = c.mz_data_id";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Contact contact = new Contact();
                //set contact name as the ID
                contact.setId(rs.getString(1).replaceAll(" ","_"));
                //and add it as a Param as well....
                CvParam nameParam = new CvParam();
                nameParam.setValue(rs.getString(1));
                nameParam.setCvRef("MS"); //MS cv for contact address
                nameParam.setAccession("MS:1000586");
                nameParam.setName("contact name");
                contact.getCvParam().add(nameParam);
                //and add the params, if email and institution present
                if (rs.getString(2) != null) {
                    //TODO: no cvparam for affiliation
                    //TODO: where is the address, role and URL ??
                    //add the institution as cvParam
                    CvParam cvParam = new CvParam();
                    cvParam.setValue(rs.getString(2));
                    cvParam.setCvRef("MS"); //will use CV for contact organization as the affiliation
                    cvParam.setAccession("MS:1000590");
                    cvParam.setName("contact affiliation");
                    contact.getCvParam().add(cvParam);
                }
                if (rs.getString(3) != null) {
                    CvParam cvParam = new CvParam();
                    cvParam.setValue(rs.getString(3));
                    cvParam.setCvRef("MS");
                    cvParam.setAccession("MS:1000589"); //MS param for contact email
                    cvParam.setName("contact email");
                    contact.getCvParam().add(cvParam);
                }
                contactList.getContact().add(contact);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return contactList;
    }

    public PublicationList getPublicationList(List<Long> experimentIDs) {
        PublicationList publicationList = new PublicationList();
        String query = "SELECT ppm.name, ppm.cv_label, pr.reference_line, GROUP_CONCAT(pl.experiment_id) " +
                "FROM pride_reference_exp_link pl, pride_reference pr " +
                "LEFT JOIN  pride_reference_param ppm ON pr.reference_id = ppm.parent_element_fk " +
                "WHERE pl.experiment_id IN (%s) " +
                "AND pl.reference_id = pr.reference_id " +
                "AND ppm.cv_label IN ('PubMed','DOI') " +
                "GROUP BY ppm.name";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());
            boolean refSubmitted = false;
            ResultSet rs = st.executeQuery();
            int index = 1; //this index will be used to differentiate the different unpublished/submitted publications
            while (rs.next()) {
                Publication publication = new Publication();
                refSubmitted = true;

                //create the reference Param
                CvParam refCvParam = new CvParam();
                //Dataset associated manuscript, might have or not PubMedID
                refCvParam.setCvRef("PRIDE");
                refCvParam.setAccession("PRIDE:0000400");
                refCvParam.setName("Reference");
                refCvParam.setValue(rs.getString(3));
                publication.getCvParam().add(refCvParam);
                if (rs.getString(1) == null) {
                    //there is no data in PubMed yet, but paper has been submitted
                    publication.setId("accepted" + index);
                    //and add new CvParam to indicated has been published but waiting for PubMed
                    CvParam pubAccepted = new CvParam();
                    pubAccepted.setCvRef("PRIDE");
                    pubAccepted.setAccession("PRIDE:0000399");
                    pubAccepted.setName("Accepted manuscript");
                    publication.getCvParam().add(pubAccepted);
                    index++;
                } else {
                    //has a pubMed or DOI
                    publication.setId(rs.getString(1).replaceAll(" ","_"));
                    //and add it as a cvParam as well
                    CvParam pubMed = new CvParam();
                    pubMed.setCvRef("MS");
                    pubMed.setName(rs.getString(2));
                    pubMed.setValue(rs.getString(1));
                    if (rs.getString(1).equals("PubMed identifier")) {
                        //add the PubMed accession
                        pubMed.setAccession("MS:1000879");
                    } else {
                        //add the DOI accession
                        pubMed.setAccession("MS:1001922");
                    }
                    publication.getCvParam().add(pubMed);
                }
                //helper method to add the instrument to all the experiments
                addKeysMap(publicationMap, publication, rs.getString(4));
                publicationList.getPublication().add(publication);
            }
            rs.close();
            //if some experiments did not have a publication, add the special element
            if (publicationMap.size() != experimentIDs.size()) {
                //nothing in pride_reference yet, unpublished data
                Publication publication = new Publication();
                publication.setId("unpublished");
                CvParam cvParam = new CvParam();
                cvParam.setCvRef("MS");
                cvParam.setName("unpublished data");
                cvParam.setAccession("MS:100????");
                publication.getCvParam().add(cvParam);
                //if there is no publication, add the special param in the map
                publicationMap.put(new Long(0), publication);
                publicationList.getPublication().add(publication);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return publicationList;
    }

    public FullDatasetLinkList getFullDataSetLinkList(List<Long> experimentIDs) {
        FullDatasetLinkList datasetLinkList = new FullDatasetLinkList();
        //add the PRIDE URI
        List<String> accessions = getAccessions(experimentIDs);
        for (String accession : accessions) {
            FullDatasetLink datasetLink = new FullDatasetLink();
            CvParam cvParam = new CvParam();
            cvParam.setCvRef("MS");
            cvParam.setName("PRIDE experiment URI");
            cvParam.setAccession("MS:1001929");
            cvParam.setValue(PRIDE_URL + accession);
            datasetLink.setCvParam(cvParam);
            datasetLinkList.getFullDatasetLink().add(datasetLink);
        }
        //and now add the Tranche if present
        String query = "SELECT p.accession, pe.value " +
                "FROM pride_experiment p LEFT JOIN pride_experiment_param pe ON p.experiment_id = pe.parent_element_fk " +
                "WHERE p.experiment_id IN (%s) " +
                "AND pe.name = 'Tranche link to raw file'";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                //add the tranche only if present
                if (rs.getString(2) != null) {
                    FullDatasetLink datasetLinkTranche = new FullDatasetLink();
                    String tranche_url = rs.getString(2);
                    //extract the hash from the url
                    String tranche_hash = tranche_url.substring(tranche_url.indexOf("hash=") + 5, tranche_url.length());
                    //and create the param
                    CvParam trancheParam = new CvParam();
                    trancheParam.setCvRef("MS");
                    trancheParam.setName("Tranche project hash");
                    trancheParam.setAccession("MS:1001928");
                    trancheParam.setValue(tranche_hash);
                    datasetLinkTranche.setCvParam(trancheParam);
                    datasetLinkList.getFullDatasetLink().add(datasetLinkTranche);
                }
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return datasetLinkList;
    }

    public RepositoryRecord getRepositoryRecord(long experimentID) {
        RepositoryRecord repositoryRecord = new RepositoryRecord();
        String query = "SELECT p.accession, p.short_label, p.title " +
                "FROM pride_experiment p  " +
                "WHERE p.experiment_id = ? ";
        try {
            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setLong(1, experimentID);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                repositoryRecord.setRecordID(rs.getString(1)); //set the accession
                repositoryRecord.setRepositoryID("PRIDE");
                repositoryRecord.setUri(PRIDE_URL + rs.getString(1)); //set link to experiment
                repositoryRecord.setLabel(rs.getString(2)); //set label
                repositoryRecord.setName(rs.getString(3));
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return repositoryRecord;
    }

    public SampleList getSampleList(long experimentID) {
        SampleList sampleList = new SampleList();

        String query = "SELECT m.sample_name, ms.name, ms.cv_label, ms.accession " +
                "FROM pride_experiment p, mzdata_mz_data m LEFT JOIN mzdata_sample_param ms ON m.mz_data_id=ms.parent_element_fk " +
                "WHERE p.experiment_id = ? " +
                "AND p.accession = m.accession_number ";
        try {
            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setLong(1, experimentID);
            ResultSet rs = st.executeQuery();
            Sample sample = new Sample();
            while (rs.next()) {
                //TODO: assuming only 1 sample in PRIDE at the moment
                sample.setId(rs.getString(1).replaceAll(" ","_"));
                //TODO: using id and name same value, sample_name
                sample.setName(rs.getString(1));
                if (rs.getString(4) != null){
                    CvParam cvParam = new CvParam();
                    cvParam.setName(rs.getString(2));
                    cvParam.setCvRef(rs.getString(3));
                    cvParam.setAccession(rs.getString(4));
                    sample.getCvParam().add(cvParam);
                }
            }
            rs.close();
            sampleList.getSample().add(sample);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return sampleList;
    }

    //helper method, will return a Ref for a certain type of elements (right now only publication or instrument)
    public Ref getRef(String type, long experimentID) {
        Ref ref = new Ref();

        if (type.equals("instrument")) {
            ref.setRef(instrumentMap.get(experimentID));
        } else if (type.equals("publication")) {
            if (publicationMap.containsKey(experimentID)) {
                ref.setRef(publicationMap.get(experimentID));
            } else {
                //there is no publication, add the special param for that
                ref.setRef(publicationMap.get(0));
            }
        } else {
            logger.error("Trying to return an invalid ref: allowed types \"publication\" and \"instrument\"");
        }
        return ref;
    }

    public KeywordList getKeywordList(List<Long> experimentIDs) {
        KeywordList keywordList = new KeywordList();
        String[] accessions = new String[]{"MS:1001923", "MS:1001924", "MS:1001925", "MS:1001926"};
        keywordList.getCvParam().addAll(getExperimentParams(experimentIDs, Arrays.asList(accessions)));
        return keywordList;
    }
    
    //helper method, for a list pf experiments and accessions, will get from the pride_experiment_param all the Params
    // associated. Very useful in ProteomeXchange, most data stored in that table
    private Set<CvParam> getExperimentParams(List<Long> experimentIDs, List<String> accessions){
        Set<CvParam> cvParams = new HashSet<CvParam>();
        String query = "SELECT ppp.accession, ppp.value, ppp.name, ppp.cv_label " +
                "FROM pride_experiment pe LEFT JOIN pride_experiment_param ppp ON pe.experiment_id = ppp.parent_element_fk " +
                "WHERE pe.experiment_id IN (%s) and " +
                "ppp.accession IN (%s)";
        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()), preparePlaceHolders(accessions.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            //copy both arrays, experimentIds and accession in a single Object array for method to work
            setValues(st, concatArrays(experimentIDs.toArray(), accessions.toArray()));
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                //a keyword list is nothing but a list of CvParam
                CvParam cvParam = new CvParam();
                cvParam.setAccession(rs.getString(1));
                cvParam.setValue(rs.getString(2));
                cvParam.setName(rs.getString(3));
                cvParam.setCvRef(rs.getString(4));
                cvParams.add(cvParam);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return cvParams;
    }

    //helper method to concatenate 2 Object arrays in a single one
    private Object[] concatArrays(Object[] array1, Object[] array2){
        Object[] newArray= new Object[array1.length + array2.length];
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
    
    //helper method to return all accessions in the submissions
    private List<String> getAccessions(List<Long> experimentIDs){
        List<String> accessions = new ArrayList<String>();
        String query = "SELECT p.accession " +
                "FROM pride_experiment p " +
                "WHERE p.experiment_id IN (%s) ";

        String sql = String.format(query, preparePlaceHolders(experimentIDs.size()));
        try {
            PreparedStatement st = DBConnection.prepareStatement(sql);
            setValues(st, experimentIDs.toArray());
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                accessions.add(rs.getString(1));
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return accessions;
    }
}
