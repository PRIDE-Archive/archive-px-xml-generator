package uk.ac.ebi.pride.px;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.px.model.CvParam;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * @author dani@ebi.ac.uk
 * @author florian@ebi.ac.uk
 * Date: 01/05/12
 *
 * ToDo: test PARTIAL and COMPLETE cases!
 * ToDo: change name of class!
 */
public class TestPxMessageFromFile extends TestCase {
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

//    public DBController dbController;
    public File directory;
    public File submissionFile;
    public ProteomeXchangeDataset proteomeXchangeDataset;

    @Before
    public void setUp() throws Exception {
        //initialize resources: Dbcontroller and File
//        dbController = new DBController();
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage();
//        File file = messageWriter.createXMLMessage("PXDTEST1", directory, submissionFile);
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001");
        //and unmarshal XML file
        proteomeXchangeDataset = unmarshalFile(file);
    }

    @Test
    public void testPxCvListFromFile(){
        assertEquals(proteomeXchangeDataset.getCvList().getCV().size(), 4);
        assertEquals(proteomeXchangeDataset.getCvList().getCV().get(0).getId(), "MS");
        assertEquals(proteomeXchangeDataset.getCvList().getCV().get(1).getId(), "PRIDE");
        assertEquals(proteomeXchangeDataset.getCvList().getCV().get(2).getId(), "MOD");
        assertEquals(proteomeXchangeDataset.getCvList().getCV().get(3).getId(), "UNIMOD");

    }

    @Test
    public void testPxContactFromFile(){
        assertEquals(proteomeXchangeDataset.getContactList().getContact().get(0).getId(),"project_submitter");
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
        assertEquals(proteomeXchangeDataset.getSpeciesList().getSpecies().get(0).getCvParam().size(), 2);
        assertEquals(getAccessionCvParamValue(proteomeXchangeDataset.getSpeciesList().getSpecies().get(0).getCvParam(), "9606"),"MS:1001467");
        assertEquals(getAccessionCvParamValue(proteomeXchangeDataset.getSpeciesList().getSpecies().get(0).getCvParam(), "Homo sapiens (Human)"),"MS:1001469");
    }

    @Test
    public void testPxInstrumentFromFile(){
        assertEquals(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getId(),"Instrument_1");
        assertEquals(proteomeXchangeDataset.getInstrumentList().getInstrument().size(),1);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getCvParam(),"MS:1000121"),"AB SCIEX instrument model");
    }

    @Test
    public void testPxModificationFromFile(){
        assertEquals(proteomeXchangeDataset.getModificationList().getCvParam().size(),2);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getModificationList().getCvParam(),"MOD:00198"),"D-alanine");
    }

    @Test
    public void testPxPubMedFromFile(){
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().size(),1);
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getId(),"PMID12345");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getCvParam(),"MS:1000879"),"12345");
    }

    @Test
    public void testPxReviewLevelFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getReviewLevel().getCvParam().getAccession(),"PRIDE:0000414");
    }

    @Test
    public void testPxRepositorySupportFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getRepositorySupport().getCvParam().getAccession(),"PRIDE:0000416");
    }

    @Test
    public void testPxFullDatasetLinkListFromFile(){
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getAccession(),"PRIDE:0000411");
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getValue(),"ftp://ftp.pride.ebi.ac.uk/2013/07/PXT000001");
    }

    //helper method to retrieve accession for a specific value
    private String getAccessionCvParamValue(List<CvParam> cvParams, String value){
        String accession = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getValue().equalsIgnoreCase(value)) accession = cvParam.getAccession();
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
