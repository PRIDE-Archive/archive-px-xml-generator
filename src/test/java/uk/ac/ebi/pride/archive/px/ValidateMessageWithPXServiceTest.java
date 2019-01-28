package uk.ac.ebi.pride.archive.px;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

/** @author Suresh Hewapathirana */
public class ValidateMessageWithPXServiceTest {

  private File file;
  private String user;
  private String pass;

  @Before
  public void setUp() throws Exception {
    URL url = ValidateMessageWithPXServiceTest.class.getClassLoader().getResource("PXD000001.xml");
    if (url == null) {
      throw new IllegalStateException("no file for input found!");
    }
    this.file = new File(url.toURI());
    this.user = "TestRepo";
    this.pass = "********"; // replace with the password
  }

  @Test
  public void postFileTest() {
    assertFalse(this.pass.equals("********"));
    String response = ValidateMessageWithPXService.postFile(this.file, this.user, this.pass);
    System.out.println(response);
    assertTrue(response.startsWith("result=SUCCESS"));
    assertFalse(response.contains("message=ERROR"));
  }
}
