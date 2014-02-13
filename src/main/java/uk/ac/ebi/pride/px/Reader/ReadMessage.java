package uk.ac.ebi.pride.px.Reader;

import uk.ac.ebi.pride.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.px.xml.PxUnmarshaller;
import java.io.File;
import java.io.IOException;

/**
 * Class to read in an existing PX XML file.
 *
 * @author Tobias Ternent
 */

public class ReadMessage {

    /**
     * Method to read in an existing PX XML file, by unmarshalling it to a ProteomeXchangeDataset.
     *
     * @param pxFile the PX XML file to read in.
     * @return a ProteomeXchangeDataset ready for changes to then be marshalled into a PX XML file.
     */

    public static ProteomeXchangeDataset readPxXml(File pxFile) throws IOException {
        return new PxUnmarshaller().unmarshall(pxFile);
    }
}
