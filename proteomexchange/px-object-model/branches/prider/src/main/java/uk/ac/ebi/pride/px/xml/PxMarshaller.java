package uk.ac.ebi.pride.px.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.model.PXObject;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.Writer;

/**
 * @author dani
 * Date: 11/10/11
 */
public class PxMarshaller {
    private static final Logger logger = LoggerFactory.getLogger(PxMarshaller.class);

     public <T extends PXObject> void marshall(T object, Writer out) {

        if (object == null) {
            throw new IllegalArgumentException("Cannot marshall a NULL object");
        }

        try {
            Marshaller marshaller = MarshallerFactory.getInstance().initializeMarshaller();

            // Set JAXB_FRAGMENT_PROPERTY to true for all objects that do not have
            // a @XmlRootElement annotation

//            if (!(object instanceof PXObject)) {
//                marshaller.setProperty("jaxb.fragment", true);
//                if (logger.isDebugEnabled()) logger.debug("Object '" + object.getClass().getName() +
//                                                          "' will be treated as root element.");
//            } else {
//                if (logger.isDebugEnabled()) logger.debug("Object '" + object.getClass().getName() +
//                                                          "' will be treated as fragment.");
//            }
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "proteomeXchange-1.1.0.xsd");

//            marshaller.marshal( new JAXBElement(new QName("uri","local"), object.getClass(), object), out );
             marshaller.marshal(object, out);
        } catch (JAXBException e) {
            logger.error("PxMarshaller.marshall", e);
            throw new IllegalStateException("Error while marshalling object:" + object.toString());
        }

    }
}
