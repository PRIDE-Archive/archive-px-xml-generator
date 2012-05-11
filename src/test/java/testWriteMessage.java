import junit.framework.TestCase;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.WriteMessage;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: dani
 * Date: 12/03/12
 * Time: 13:44
 * To change this template use File | Settings | File Templates.
 */
public class testWriteMessage extends TestCase{

    public void testMessage() throws Exception, SubmissionFileException {
        DBController dbController = new DBController();
        WriteMessage messageWriter = new WriteMessage(dbController);
        File directory = new File(System.getProperty("user.dir"));
        File submissionFile = new File("src/test/resources/submission_csordas.px");
        File file = messageWriter.createXMLMessage("PXD000002", directory, submissionFile);
        System.out.println("File created: " + file.getAbsolutePath());
    }
}
