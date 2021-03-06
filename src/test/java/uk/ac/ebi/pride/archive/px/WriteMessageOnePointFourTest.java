package uk.ac.ebi.pride.archive.px;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.pride.archive.px.model.CvParam;
import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;
import uk.ac.ebi.pride.archive.px.writer.SchemaCommonStrategy;

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
public class WriteMessageOnePointFourTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public File directory;
    public File submissionFile;
    public ProteomeXchangeDataset proteomeXchangeDataset;
    final String SCHEMA_VERSION = "1.4.0";

    @Before
    public void setUp() throws Exception {
        directory = temporaryFolder.newFolder("pxMessage");
        submissionFile = new File("src/test/resources/submission.px");
        MessageWriter messageWriter = Util.getSchemaStrategy(SCHEMA_VERSION);
        File file = messageWriter.createIntialPxXml(submissionFile, directory, "PXT000001", "2013/07/PXT000001", SCHEMA_VERSION);
        proteomeXchangeDataset = unmarshalFile(file);
    }

    @Test
    public void tesFormatVersion(){
        assertEquals(proteomeXchangeDataset.getFormatVersion(), SCHEMA_VERSION);
    }

    @Test
    public void testPxCvListFromFile(){
        assertEquals(proteomeXchangeDataset.getCvList().getCv().size(), 3);
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(0).getId(), "MS");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(1).getId(), "MOD");
        assertEquals(proteomeXchangeDataset.getCvList().getCv().get(2).getId(), "UNIMOD");
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
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getTitle(),"Test project title-PIM1 kinase promotes gallbladder cancer cell proliferation via inhibition of proline-rich Akt substrate of 40 kDa (PRAS40)");
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getDescription(),"Description for the test project - Gallbladder cancer (GBC) is associated with poor disease prognosis with a survival of less than 5 years in 90% the cases. This has been attributed to late presentation of the disease, lack of early diagnostic markers and limited efficiency of therapeutic interventions. Elucidation of the molecular events in GBC carcinogenesis can contribute in better management of the disease by aiding in identification of therapeutic targets. To identify the aberrantly activated signaling events in GBC, tandem mass tag-based quantitative phosphoproteomic analysis of five GBC cell lines based on the invasive property was carried out. Using a panel of five GBC cell lines, a total of 2,623 phosphosites from 1,343 proteins were identified. Of these, 55 phosphosites were hyperphosphorylated and 39 phosphosites were hypophosphorylated in both replicates and all the 4 invasive GBC cell lines. Proline-rich Akt substrate 40 kDa (PRAS40) was one of the proteins found to be hyperphosphorylated in all the invasive GBC cell lines. Tissue microarray-based immunohistochemical labeling of phospho-PRAS40 (T246) revealed moderate to strong staining in 77% of the primary gallbladder adenocarcinoma cases. Inhibition of PRAS40 phosphorylation using inhibitors of its upstream kinases, PIM1 and AKT resulted in a significant decrease in cell proliferation, colony forming and invasive ability of the GBC cells. Our findings support the role of PRAS40 phosphorylation in tumor cell survival and aggressiveness in GBC and suggest its potential as a therapeutic target for GBC.");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getKeywordList().getCvParam(),SchemaCommonStrategy.MS_1001925),"test, project");
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
        assertEquals(getNameCvParam(proteomeXchangeDataset.getModificationList().getCvParam(),"MOD:00198"),"D-alanine (Ala)");
    }

    @Test
    public void testPxPubMedFromFile(){
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().size(),2);
        assertEquals(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getId(),"PMID12345");
        assertEquals(getValueCvParam(proteomeXchangeDataset.getPublicationList().getPublication().get(0).getCvParam(),"MS:1000879"),"12345");
    }

    @Test
    public void testPxReviewLevelFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getReviewLevel().getCvParam().getAccession(),"MS:1002854");
    }

    @Test
    public void testPxRepositorySupportFromFile(){
        assertEquals(proteomeXchangeDataset.getDatasetSummary().getRepositorySupport().getCvParam().getAccession(),"MS:1002856");
    }

    @Test
    public void testPxFullDatasetLinkListFromFile(){
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getAccession(),"MS:1002852");
        assertEquals(proteomeXchangeDataset.getFullDatasetLinkList().getFullDatasetLink().get(0).getCvParam().getValue(),"ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001");
    }

    @Test
    public void testPxDatasetFileLink(){
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(0).getCvParam().get(0).getAccession(), "MS:1002851");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(0).getCvParam().get(0).getValue(), "ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001/database.fasta");

        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(0).getAccession(), "MS:1002846");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(0).getValue(), "ftp://ftp.pride.ebi.ac.uk/pride/data/archive/2013/07/PXT000001/sample_1_replicate_1.RAW");
        assertEquals(proteomeXchangeDataset.getDatasetFileList().getDatasetFile().get(6).getCvParam().get(1).getAccession(), "MS:1002859");
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
