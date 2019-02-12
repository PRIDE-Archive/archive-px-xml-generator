package uk.ac.ebi.pride.archive.px;

import uk.ac.ebi.pride.archive.px.writer.SchemaOnePointFourStrategy;
import uk.ac.ebi.pride.archive.px.writer.SchemaOnePointThreeStrategy;
import uk.ac.ebi.pride.archive.px.writer.MessageWriter;

/**
 * @author Suresh Hewapathirana
 */
public class Util {

    public static MessageWriter getSchemaStrategy(String version){
        switch (version) {
            case "1.3.0":
                return new SchemaOnePointThreeStrategy(version);
            case "1.4.0":
                return new SchemaOnePointFourStrategy(version);
        }
        return null;
    }
}
