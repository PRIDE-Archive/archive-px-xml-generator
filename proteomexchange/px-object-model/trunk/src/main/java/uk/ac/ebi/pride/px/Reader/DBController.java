package uk.ac.ebi.pride.px.Reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Calendar;
import java.util.Properties;

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
    private Connection DBConnection = null;
    //    Logger object
    Logger logger = LoggerFactory.getLogger(DBController.class);

    public DBController() {
        //get properties file
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
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }
        String url_connection = properties.getProperty("protocol") + ':' + properties.getProperty("subprotocol") +
                ':' + properties.getProperty("alias");
        logger.debug("Connecting to " + url_connection);
        try {
            DBConnection = DriverManager.getConnection(url_connection, properties.getProperty("user"), properties.getProperty("password"));
        } catch (SQLException err) {
            logger.error(err.getMessage(), err);
        }
    }

    public DatasetSummary getDatasetSummary(String accessionNumber) {
        DatasetSummary datasetSummary = new DatasetSummary();
        try {
            String query = "SELECT pe.title, ppp.value " +
                    "FROM pride_experiment pe LEFT JOIN pride_experiment_param ppp ON pe.experiment_id = ppp.parent_element_fk " +
                    "WHERE pe.accession = ? and " +
                    "ppp.accession = 'PRIDE:0000040'";
            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setString(1, accessionNumber);

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
    public SpeciesList getSpecies(String accessionNumber) {
        SpeciesList speciesList = new SpeciesList();
        Species species = new Species();
        try {
            String query = "SELECT ms.name, ms.accession " +
                    "from pride_experiment pe, mzdata_sample_param ms " +
                    "where ms.cv_label = 'NEWT' and " +
                    "ms.parent_element_fk = pe.mz_data_id and " +
                    "pe.accession = ?";
            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setString(1, accessionNumber);

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

    public InstrumentList getInstrumentList(String accessionNumber) {
        InstrumentList instrumentList = new InstrumentList();
        try {
            String query = "SELECT md.instrument_name, map.name, map.accession, map.cv_label " +
                            "FROM mzdata_mz_data md LEFT JOIN mzdata_analyzer ma LEFT JOIN mzdata_analyzer_param map ON  ma.analyzer_id = map.parent_element_fk ON md.mz_data_id = ma.mz_data_id " +
                            "WHERE md.accession_number = ?";
            PreparedStatement st = DBConnection.prepareStatement(query);
            st.setString(1, accessionNumber);

            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                Instrument instrument = new Instrument();
                String id = rs.getString(1);
                String name = rs.getString(2);
                String accession = rs.getString(3);
                String cvRef = rs.getString(4);
                instrument.setId(id);
                //and add the params
                CvParam cvParam = new CvParam();
                cvParam.setCvRef(cvRef);
                cvParam.setName(name);
                cvParam.setAccession(accession);
                instrument.setCvParam(cvParam);
                instrumentList.getInstrument().add(instrument);
            }
            rs.close();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
        return instrumentList;
    }
}
