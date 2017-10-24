package uk.ac.ebi.pride.archive.px.xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.px.model.PXObject;
import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Class to unmarshall a PX XML file into a ProteomeXchangeDataset object.
 *
 * @author Tobias Ternent
 */
public class PxUnmarshaller {
  private static final Logger logger = LoggerFactory.getLogger(PxUnmarshaller.class);

  /**
   * Unmarshalls PX XML.
   * @param f input file to unmarshall
   * @param <T> PX Object
   * @return
   */
  public <T extends PXObject> ProteomeXchangeDataset unmarshall(File f) {

    try {
      Unmarshaller unmarshaller = UnmarshallerFactory.getInstance().initializeUnmarshaller();
      return (ProteomeXchangeDataset) unmarshaller.unmarshal(f);
    } catch (JAXBException e) {
      logger.error("PxUnmarshaller.unmarshal", e);
      throw new IllegalStateException("Error while unmarshalling file:" + f.getAbsolutePath());
    }
  }
}
