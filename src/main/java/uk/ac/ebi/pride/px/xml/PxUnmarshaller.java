package uk.ac.ebi.pride.px.xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.model.PXObject;
import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: tobias
 * Date: 12/02/14
 * Time: 11:22
 * To change this template use File | Settings | File Templates.
 */
public class PxUnmarshaller {
    private static final Logger logger = LoggerFactory.getLogger(PxUnmarshaller.class);

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
