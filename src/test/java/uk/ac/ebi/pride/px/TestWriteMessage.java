package uk.ac.ebi.pride.px;

import junit.framework.TestCase;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.WriteMessage;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: dani
 * Date: 12/03/12
 * Time: 13:44
 *
 * todo: these tests need to be rewritten
 */
public class TestWriteMessage extends TestCase{

    public void testMessage() throws Exception {
//        DBController dbController = new DBController();
        WriteMessage messageWriter = new WriteMessage();
        File directory = new File(System.getProperty("user.dir"));
        File submissionFile = new File("src/test/resources/submission.px");
        File file = messageWriter.createXMLMessage("PXD000002", directory, submissionFile);
        System.out.println("File created: " + file.getAbsolutePath());
    }
}
