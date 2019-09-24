package uk.ac.ebi.pride.archive.px.reader;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.pride.archive.px.model.*;
import uk.ac.ebi.pride.archive.px.xml.PxUnmarshaller;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** @author Suresh Hewapathirana */
public class ReadMessageTest {

  @Before
  public void setUp() throws Exception {}

  /**
   * Read the revision number from the PX XML
   *
   * @throws IOException
   */
  @Test
  public void readRevisionNumberTest() throws IOException {
    boolean isAccessionRecordFound = false;
    int revisionNumber = 1;
    File pxFile = new File("src/test/resources/PXD014829.xml");
    ProteomeXchangeDataset proteomeXchangeDataset = new PxUnmarshaller().unmarshall(pxFile);
    DatasetIdentifierList datasetIdentifierList = proteomeXchangeDataset.getDatasetIdentifierList();
    List<DatasetIdentifier> datasetIdentifiers = datasetIdentifierList.getDatasetIdentifier();
    for (DatasetIdentifier datasetIdentifier : datasetIdentifiers) {
      List<CvParam> cvParams = datasetIdentifier.getCvParam();
        for (CvParam cvParam : cvParams) {
          if (cvParam.getAccession().equals("MS:1001919") && cvParam.getValue().equals("PXD014829")) { // ProteomeXchange accession number
            isAccessionRecordFound = true;
          }
          if (cvParam.getAccession().equals("MS:1001921")) { // ProteomeXchange accession number version number
            revisionNumber = Integer.parseInt(cvParam.getValue());
            break;
          }
        }
        if(isAccessionRecordFound) break;
    }
    assertEquals(2, revisionNumber);
  }
}
