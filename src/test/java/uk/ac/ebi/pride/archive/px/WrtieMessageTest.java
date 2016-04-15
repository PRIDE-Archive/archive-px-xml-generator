package uk.ac.ebi.pride.archive.px;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.archive.px.model.CvParam;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Daniel Rios
 * @author Florian Reisinger
 */
public class WrtieMessageTest {

    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public File directory;
    public File submissionFile;
    public ProteomeXchangeDataset proteomeXchangeDataset;

    @Before
    public void setUp() throws Exception {
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage();
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001");
        proteomeXchangeDataset = unmarshalFile(file);
    }

    @Test
    public void testPxCvListFromFile(){
        assertEquals(proteomeXchangeDataset.getCvList().getCv().size(), 4);
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(0).getId(), "MS");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(1).getId(), "PRIDE");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(2).getId(), "MOD");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(3).getId(), "UNIMOD");

    }

    @Test
    public void testPxContactFromFile(){
        assertEquals(proteomeXchangeDataset.getContactList().getContact().get(0).getId(),"project_submitter");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(0).getCvParam(), "MS:1000586"), "PRIDE");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(0).getCvParam(), "MS:1000589"), "pride-support@ebi.ac.uk");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(0).getCvParam(), "MS:1000590"), "Proteomics");
        assertEquals(proteomeXchangeDataset.getContactList().getContact().get(1).getId(), "project_lab_head");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(1).getCvParam(), "MS:1000586"), "The boss");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(1).getCvParam(), "MS:1000589"), "boss@ebi.ac.uk");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getContactList().getContact().get(1).getCvParam(), "MS:1000590"), "EBI");

    }

    @Test
    public void testPxMetadataFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getTitle(),"Test project title");
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getDescription(),"Description for the test project");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(),WriteMessage.MS_1001925),"test, project");
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
        assertEquals(getNameCvParam(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getCvParam(),"MS:1001742"),"LTQ Orbitrap Velos");
    }

    @Test
    public void testPxModificationFromFile(){
        assertEquals(proteomeXchangeDataset.getModificationList().getCvParam().size(),1);
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
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getRepositorySupport().getCvParam().getAccession(),"PRIDE:0000416");
    }

    @Test
    public void testPxFullDatasetLinkListFromFile(){
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getAccession(),"PRIDE:0000411");
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getValue(),"ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001");
    }

    @Test
    public void testPxDatasetFileLink(){
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(0).getCvParam().get(0).getAccession(), "PRIDE:0000410");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(0).getCvParam().get(0).getValue(), "ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001/database.fasta");

        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(0).getAccession(), "PRIDE:0000404");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(0).getValue(), "ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001/sample_1_replicate_1.RAW");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(1).getAccession(), "PRIDE:0000448");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(1).getValue(), "ftp://webdav.swegrid.se/test_1.raw");
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
    }

    private ProteomeXchangeDataset unmarshalFile(File pxXML){
        ProteomeXchangeDataset proteomeXchangeDataset = null;
        try {
            JAXBContext jc = JAXBContext.newInstance(ProteomeXchangeDataset.class);
            Unmarshaller u = jc.createUnmarshaller();
            proteomeXchangeDataset = (ProteomeXchangeDataset) u.unmarshal(pxXML);
        } catch (UnmarshalException ue) {
            System.out.println("Caught UnmarshalException");
        } catch (JAXBException je) {
            je.printStackTrace();
        }
        return proteomeXchangeDataset;
    }




}
