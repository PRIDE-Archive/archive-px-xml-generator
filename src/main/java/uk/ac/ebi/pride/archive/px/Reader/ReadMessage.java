package uk.ac.ebi.pride.archive.px.reader;

import uk.ac.ebi.pride.archive.px.model.ProteomeXchangeDataset;
import uk.ac.ebi.pride.archive.px.xml.PxUnmarshaller;
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
     * @param pxFile the PX XML file to read in.
     * @return a ProteomeXchangeDataset ready for changes to then be marshalled into a PX XML file.
     * @throws IOException
     */
    public static ProteomeXchangeDataset readPxXml(File pxFile) throws IOException {
        return new PxUnmarshaller().unmarshall(pxFile);
    }
}
