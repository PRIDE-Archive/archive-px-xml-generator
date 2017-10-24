package uk.ac.ebi.pride.archive.px.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.px.model.PXObject;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.Writer;

public class PxMarshaller {
  private static final Logger logger = LoggerFactory.getLogger(PxMarshaller.class);

  /**
   * PX Marshaller.
   * @param object Object to marshall
   * @param out output writer
   * @param <T> PXObject
   */
  public <T extends PXObject> void marshall(T object, Writer out) {
    if (object == null) {
      throw new IllegalArgumentException("Cannot marshall a NULL object");
    }
    try {
      Marshaller marshaller = MarshallerFactory.getInstance().initializeMarshaller();
      marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "proteomeXchange-1.3.0.xsd");
      marshaller.marshal(object, out);
    } catch (JAXBException e) {
      logger.error("PxMarshaller.marshall", e);
      throw new IllegalStateException("Error while marshalling object:" + object.toString());
    }
  }
}
