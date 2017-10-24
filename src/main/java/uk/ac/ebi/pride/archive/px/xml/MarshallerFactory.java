package uk.ac.ebi.pride.archive.px.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.px.model.PXObject;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

public class MarshallerFactory {

    private static final Logger logger = LoggerFactory.getLogger(MarshallerFactory.class);
    private static MarshallerFactory instance = new MarshallerFactory();
    private static JAXBContext jc = null;

    public static MarshallerFactory getInstance() {
        return instance;
    }

    private MarshallerFactory() {
    }

    /**
     * Initialises the marshaller.
     * @return Marshaller object.
     */
    public Marshaller initializeMarshaller() {
        logger.debug("Initializing Marshaller for ProteomeXchange.");
        try {
            if(jc == null) { // Lazy caching of JAXB context.
                jc = JAXBContext.newInstance(PXObject.class.getPackage().getName());
            }
            Marshaller marshaller = jc.createMarshaller(); //create marshaller and set basic properties
            marshaller.setProperty("jaxb.encoding", "UTF-8");
            marshaller.setProperty("jaxb.formatted.output", true);
            logger.info("Marshaller initialized");
            return marshaller;
        } catch (JAXBException e) {
            logger.error("MarshallerFactory.initializeMarshaller", e);
            throw new IllegalStateException("Can't initialize marshaller: " + e.getMessage());
        }
    }

}
