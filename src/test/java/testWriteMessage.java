import junit.framework.TestCase;
import uk.ac.ebi.pride.px.Reader.DBController;
import uk.ac.ebi.pride.px.WriteMessage;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: dani
 * Date: 12/03/12
 * Time: 13:44
 * To change this template use File | Settings | File Templates.
 */
public class testWriteMessage extends TestCase{

    public void testMessage(){
        DBController dbController = new DBController();
        WriteMessage messageWriter = new WriteMessage(dbController);
        File directory = new File(System.getProperty("user.dir"));
        File file = messageWriter.createXMLMessage("PXD000001", directory);
        System.out.println("File created: " + file.getAbsolutePath());
    }
}
