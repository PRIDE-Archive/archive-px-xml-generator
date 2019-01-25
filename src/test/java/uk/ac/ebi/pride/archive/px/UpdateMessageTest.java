package uk.ac.ebi.pride.archive.px;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.model.CvParam;
import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * This test class uses a summary file without a PubMed ID to generate a new PX XML file,
 * which is then "updated" by using the other summary file which has a PubMed ID.
 *
 * @author Tobias Ternent
 */
public class UpdateMessageTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    public File directory;
    public ProteomeXchangeDataset proteomeXchangeDataset, proteomeXchangeDatasetNoChangeLogEntry;

    /**
     * Sets up unit tests.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        directory = temporaryFolder.newFolder("pxMessage");
        File submissionFile = new File("src/test/resources/submission_update.px");
        File submissionFileWithPubmed = new File("src/test/resources/submission.px");
        WriteMessage messageWriter = new WriteMessage();
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001");
        proteomeXchangeDataset = unmarshalFile(file);
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getId(), "pending");
        file = UpdateMessage.updateReferencesPxXml(submissionFileWithPubmed,  directory, "PXT000001", "2013/07/PXT000001");
        proteomeXchangeDataset = unmarshalFile(file);

        file = UpdateMessage.updateMetadataPxXml(submissionFileWithPubmed, directory, "PXT000001", "2013/07/PXT000001", true);
        proteomeXchangeDataset = unmarshalFile(file);

        file = UpdateMessage.updateMetadataPxXml(submissionFileWithPubmed, directory, "PXT000001", "2013/07/PXT000001", false);
        proteomeXchangeDatasetNoChangeLogEntry = unmarshalFile(file);
    }

    /**
     * Test PX CV List.
     */
    @Test
    public void testPxCvListFromFile(){
        assertEquals(proteomeXchangeDataset.getCvList().getCv().size(), 3);
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(0).getId(), "MS");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(1).getId(), "MOD");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(2).getId(), "UNIMOD");

    }

    /**
     * Tests the PX Contact.
     */
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

    /**
     * Tests PX Meetadata
     */
    @Test
    public void testPxMetadataFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getTitle(),"Test project title");
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getDescription(),"Description for the test project");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(), WriteMessage.MS_1001925),"test, project");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(), WriteMessage.MS_1002340), "PRIME-XS Project");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(),WriteMessage.MS_1001926), "Biological");
    }

    /**
     * Tetss PX species.
     */
    @Test
    public void testPxSpeciesFromFile(){
        assertEquals(proteomeXchangeDataset.getSpeciesList().getSpecies().get(0).getCvParam().size(), 2);
        assertEquals(getAccessionCvParamValue(proteomeXchangeDataset.getSpeciesList().getSpecies().get(0).getCvParam(), "9606"),"MS:1001467");
        assertEquals(getAccessionCvParamValue(proteomeXchangeDataset.getSpeciesList().getSpecies().get(0).getCvParam(), "Homo sapiens (Human)"),"MS:1001469");
    }

    /**
     * Tests PX instrument.
     */
    @Test
    public void testPxInstrumentFromFile(){
        assertEquals(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getId(),"Instrument_1");
        assertEquals(proteomeXchangeDataset.getInstrumentList().getInstrument().size(),1);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getInstrumentList().getInstrument().get(0).getCvParam(),"MS:1001742"),"LTQ Orbitrap Velos");
    }

    /**
     * Tests PX Moedificiation.
     */
    @Test
    public void testPxModificationFromFile(){
        assertEquals(proteomeXchangeDataset.getModificationList().getCvParam().size(),1);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getModificationList().getCvParam(),"MOD:00198"),"D-alanine");
    }

    /**
     * Tets PX PubMed.
     */
    @Test
    public void testPxPubMedFromFile(){
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().size(),2);
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getId(),"PMID12345");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getCvParam(),"MS:1000879"),"12345");
    }

    /**
     * Tests PX review level.
     */
    @Test
    public void testPxReviewLevelFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getReviewLevel().getCvParam().getAccession(),"MS:1002854");
    }

    /**
     * Tets PX Repo Support.
     */
    @Test
    public void testPxRepositorySupportFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getRepositorySupport().getCvParam().getAccession(),"MS:1002856");
    }

    /**
     * Test PX full dataset link.
     */
    @Test
    public void testPxFullDatasetLinkListFromFile(){
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getAccession(),"MS:1002852");
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getValue(),"ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001");
    }

    /**
     * Tests Change Log Entry.
     */
    @Test
    public void testChangeLogEntry(){
        assertTrue(proteomeXchangeDataset.getChangeLog().getChangeLogEntry().size() > 0);
    }

    /**
     * Tests no Change Log Entry.
     */
    @Test
    public void testNoChangeLogEntry(){
        assertTrue(proteomeXchangeDatasetNoChangeLogEntry.getChangeLog() == null );
    }

    /**
     * Helper method to retrieve accession for a specific value
     * @param cvParams the cv params to extract
     * @param value the values
     * @return the accession
     */
    private String getAccessionCvParamValue(List<CvParam> cvParams, String value){
        String accession = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getValue().equalsIgnoreCase(value)) accession = cvParam.getAccession();
        }
        return accession;
    }


    /**
     * Helper method: for a list of params, returns name for a particular accession
     * @param cvParams the cv params to extract
     * @param accession the accession
     * @return the names
     */
    private String getNameCvParam(List<CvParam> cvParams, String accession){
        String name = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getAccession().equals(accession)) name = cvParam.getName();
        }
        return name;
    }

    /**
     * Helper method: for a list of params, returns the value for that particular accession, if found
     * @param cvParams the cv params to extract
     * @param accession the accession
     * @return tha values
     */
    private String getValueCvParam(List<CvParam> cvParams, String accession){
        String value = null;
        for (CvParam cvParam : cvParams) {
            if (cvParam.getAccession().equals(accession)) value = cvParam.getValue();
        }
        return value;
    }

    /**
     * Tears down the unit tests.
     * @throws IOException
     */
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
