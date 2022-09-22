package uk.ac.ebi.pride.archive.px;


import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.model.CvParam;
import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.writer.SchemaOnePointFourStrategy;
import uk.ac.ebi.pride.data.io.SubmissionFileParser;

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
public class UpdateMessageOnePointThreeTest {

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
        final String SCHEMA_VERSION = "1.3.0";
        directory = temporaryFolder.newFolder("pxMessage");
        File submissionFile = new File("src/test/resources/submission_update.px");
        File submissionFileWithPubmed = new File("src/test/resources/submission.px");
        MessageWriter messageWriter = Util.getSchemaStrategy(SCHEMA_VERSION);
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001", SCHEMA_VERSION);
        proteomeXchangeDataset = unmarshalFile(file);
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getId(), "pending");
        file = UpdateMessage.updateReferencesPxXml(directory, "PXT000001", "2013/07/PXT000001", SCHEMA_VERSION, SubmissionFileParser.parse(submissionFileWithPubmed));
        proteomeXchangeDataset = unmarshalFile(file);

        file = UpdateMessage.updateMetadataPxXml(SubmissionFileParser.parse(submissionFileWithPubmed), directory, "PXT000001", "2013/07/PXT000001", true, SCHEMA_VERSION);
        proteomeXchangeDataset = unmarshalFile(file);

        file = UpdateMessage.updateMetadataPxXml(SubmissionFileParser.parse(submissionFileWithPubmed), directory, "PXT000001", "2013/07/PXT000001", false, SCHEMA_VERSION);
        proteomeXchangeDatasetNoChangeLogEntry = unmarshalFile(file);
    }

    /**
     * Test PX CV List.
     */
    @Test
    public void testPxCvListFromFile(){
        assertEquals(proteomeXchangeDataset.getCvList().getCv().size(), 4);
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(0).getId(), "MS");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(1).getId(), "PRIDE");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(2).getId(), "MOD");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(3).getId(), "UNIMOD");

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
     * Tests PX Metadata
     */
    @Test
    public void testPxMetadataFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getTitle(),"Test project title-PIM1 kinase promotes gallbladder cancer cell proliferation via inhibition of proline-rich Akt substrate of 40 kDa (PRAS40)");
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getDescription(),"Description for the test project - Gallbladder cancer (GBC) is associated with poor disease prognosis with a survival of less than 5 years in 90% the cases. This has been attributed to late presentation of the disease, lack of early diagnostic markers and limited efficiency of therapeutic interventions. Elucidation of the molecular events in GBC carcinogenesis can contribute in better management of the disease by aiding in identification of therapeutic targets. To identify the aberrantly activated signaling events in GBC, tandem mass tag-based quantitative phosphoproteomic analysis of five GBC cell lines based on the invasive property was carried out. Using a panel of five GBC cell lines, a total of 2,623 phosphosites from 1,343 proteins were identified. Of these, 55 phosphosites were hyperphosphorylated and 39 phosphosites were hypophosphorylated in both replicates and all the 4 invasive GBC cell lines. Proline-rich Akt substrate 40 kDa (PRAS40) was one of the proteins found to be hyperphosphorylated in all the invasive GBC cell lines. Tissue microarray-based immunohistochemical labeling of phospho-PRAS40 (T246) revealed moderate to strong staining in 77% of the primary gallbladder adenocarcinoma cases. Inhibition of PRAS40 phosphorylation using inhibitors of its upstream kinases, PIM1 and AKT resulted in a significant decrease in cell proliferation, colony forming and invasive ability of the GBC cells. Our findings support the role of PRAS40 phosphorylation in tumor cell survival and aggressiveness in GBC and suggest its potential as a therapeutic target for GBC.");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(), SchemaOnePointFourStrategy.MS_1001925),"test, project");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(), SchemaOnePointFourStrategy.MS_1002340), "PRIME-XS Project");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(),SchemaOnePointFourStrategy.MS_1001926), "Biological");
    }

    /**
     * Tests PX species.
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
     * Tests PX Modification.
     */
    @Test
    public void testPxModificationFromFile(){
        assertEquals(proteomeXchangeDataset.getModificationList().getCvParam().size(),1);
        assertEquals(getNameCvParam(proteomeXchangeDataset.getModificationList().getCvParam(),"MOD:00198"),"D-alanine (Ala)");
    }

    /**
     * Tests PX PubMed.
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
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getReviewLevel().getCvParam().getAccession(),"PRIDE:0000414");
    }

    /**
     * Tets PX Repo Support.
     */
    @Test
    public void testPxRepositorySupportFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getRepositorySupport().getCvParam().getAccession(),"PRIDE:0000416");
    }

    /**
     * Test PX full dataset link.
     */
    @Test
    public void testPxFullDatasetLinkListFromFile(){
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getAccession(),"PRIDE:0000411");
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
