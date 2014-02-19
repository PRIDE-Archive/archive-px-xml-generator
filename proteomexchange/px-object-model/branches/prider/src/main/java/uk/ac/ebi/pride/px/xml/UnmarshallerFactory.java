package uk.ac.ebi.pride.px.xml;
import org.apache.log4j.Logger;
import uk.ac.ebi.pride.px.model.PXObject;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;


/**
 * Created with IntelliJ IDEA.
 * User: tobias
 * Date: 12/02/14
 * Time: 11:14
 * To change this template use File | Settings | File Templates.
 */
public class UnmarshallerFactory {
    private static final Logger logger = Logger.getLogger(UnmarshallerFactory.class);
    private static UnmarshallerFactory instance = new UnmarshallerFactory();
    private static JAXBContext jc = null;

    public static UnmarshallerFactory getInstance() {
        return instance;
    }

    private UnmarshallerFactory() {
    }

    public Unmarshaller initializeUnmarshaller() {
        logger.debug("Initializing Unmarshaller for ProteomeXchange.");
        try {
            // Lazy caching of JAXB context.
            if(jc == null) {
                //jc = JAXBContext.newInstance(PXObject.class.getPackage().getName());
                jc = JAXBContext.newInstance(ProteomeXchangeDataset.class);
            }
            //create unmarshaller
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            logger.info("Unmarshaller initialized");

            return unmarshaller;

        } catch (JAXBException e) {
            logger.error("UnmarshallerFactory.initializeUnmarshaller", e);
            throw new IllegalStateException("Can't initialize unmarshaller: " + e.getMessage());
        }
    }

}
