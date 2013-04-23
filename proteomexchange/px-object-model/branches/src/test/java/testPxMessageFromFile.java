import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.WriteMessage;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import uk.ac.ebi.pride.px.model.CvParam;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;


/**
 * Created with IntelliJ IDEA.
 * User: dani
 * Date: 01/05/12
 * Time: 11:32
 *
 *
 * todo: these tests need to be rewritten
 */
public class testPxMessageFromFile extends TestCase {
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public DBController dbController;
    public File directory;
    public File submissionFile;
    public ProteomeXchangeDataset proteomeXchangeDataset;

    @Before
    public void setUp() throws Exception {
        //initialize resources: Dbcontroller and File
        dbController = new DBController();
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage(dbController);
        File file = messageWriter.createXMLMessage("PXDTEST1", directory, submissionFile);
        //and unmarshal XML file
        proteomeXchangeDataset = unmarshalFile(file);
    }

    @Test
    public void testPxContactFromFile(){
        assertEquals(proteomeXchangeDataset.getContactList().getContact().get(0).getId(),"John_Arthur_Smith");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(0).getCvParam(), "MS:1000586"),"John Arthur Smith");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(0).getCvParam(),"MS:1000589"),"john.smith@cam.edu");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(0).getCvParam(),"MS:1000590"),"University of Cambridge");

    }

    @Test
    public void testPxMetadataFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getTitle(),"Human proteome");
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getDescription(),"An experiment about human proteome");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(),"MS:1001925"),"human, proteome");
    }

    @Test
    public void testPxSpeciesFromFile(){
        assertEquals(proteomeXchangeDataset.getSpeciesList().getSpecies().getCvParam().size(), 4);
        assertEquals(getAccessionCvParam(proteomeXchangeDataset.getSpeciesList().getSpecies().getCvParam(),"9606"),"MS:1001467");
        assertEquals(getAccessionCvParam(proteomeXchangeDataset.getSpeciesList().getSpecies().getCvParam(),"Homo sp. Altai"),"MS:1001469");
    }

    @Test
    public void testPxInstrumentFromFile(){
        assertEquals(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getId(),"Instrument_1");
        assertEquals(proteomeXchangeDataset.getInstrumentList().getInstrument().size(),1);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getCvParam(),"MS:1000122"),"AB SCIEX instrument test model");
    }

    @Test
    public void testPxModificationFromFile(){
        assertEquals(proteomeXchangeDataset.getModificationList().getCvParam().size(),2);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getModificationList().getCvParam(),"MOD:00198"),"D-alanine");
    }

    @Test
    public void testPxPubMedFromFile(){
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().size(),2);
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getId(),"PMID12345");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getCvParam(),"MS:1000879"),"12345");
    }

    @Test
    public void testPxReviewLevelFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getReviewLevel().getCvParam().getAccession(),"PRIDE:0000414");
    }

    @Test
    public void testPxRepositorySupportFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getRepositorySupport().getCvParam().getAccession(),"PRIDE:0000417");
    }

    @Test
    public void testPxFullDatasetLinkListFromFile(){
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getAccession(),"PRIDE:0000411");
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getValue(),"/path/to/result/files/result-1.xml");
    }

    //helper method to retrieve accession for a specific value
    private String getAccessionCvParam(List<CvParam> cvParams, String value){
        String accession = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getValue().equals(value)) accession = cvParam.getAccession();
        }
        return accession;
    }

    //helper method, for a list of params, returns name for a particular accession
    private String getNameCvParam(List<CvParam> cvParams, String accession){
        String name = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getAccession().equals(accession)) name = cvParam.getName();
        }
        return name;
    }

    //helper method, for a list of params, returns the value for that particular accession, if found
    private String getValueCvParam(List<CvParam> cvParams, String accession){
        String value = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getAccession().equals(accession)) value = cvParam.getValue();
        }
        return value;
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(directory);
    }

    private ProteomeXchangeDataset unmarshalFile(File pxXML){
        Unmarshaller u = null;
        ProteomeXchangeDataset proteomeXchangeDataset = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(ProteomeXchangeDataset.class);
            u = jc.createUnmarshaller();
            proteomeXchangeDataset = (ProteomeXchangeDataset) u.unmarshal(pxXML);


        } catch (UnmarshalException ue) {
            System.out.println("Caught UnmarshalException");
        } catch (JAXBException je) {
            je.printStackTrace();
        }
        return proteomeXchangeDataset;
    }


}
