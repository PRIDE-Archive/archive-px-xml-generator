package uk.ac.ebi.pride.px.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.model.PXObject;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.Writer;

/**
 * Created by IntelliJ IDEA.
 * User: dani
 * Date: 11/10/11
 * Time: 15:23
 * To change this template use File | Settings | File Templates.
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
            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "proteomeXchange-draft-07.xsd");

//            marshaller.marshal( new JAXBElement(new QName("uri","local"), object.getClass(), object), out );
             marshaller.marshal(object, out);
        } catch (JAXBException e) {
            logger.error("PxMarshaller.marshall", e);
            throw new IllegalStateException("Error while marshalling object:" + object.toString());
        }

    }
}
