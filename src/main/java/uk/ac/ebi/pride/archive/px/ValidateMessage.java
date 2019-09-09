package uk.ac.ebi.pride.archive.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import uk.ac.ebi.pride.tools.ErrorHandlerIface;
import uk.ac.ebi.pride.tools.GenericSchemaValidator;
import uk.ac.ebi.pride.tools.ValidationErrorHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * Class to validate a PX XML file according to the PX XML schema.
 *
 * @author Tobias Ternent
 */

public class ValidateMessage {

  private static final Logger logger = LoggerFactory.getLogger(ValidateMessage.class);

  /**
   * Method to validate a supplied PX XML file using the default PX Schema location.
   *
   * @param file the PX XML file to be validated.
   * @return String which lists any errors that occurred during validation. If there were none, the String will be empty.
   * @throws SAXException
   * @throws MalformedURLException
   * @throws FileNotFoundException
   * @throws URISyntaxException
   */
  public static String validateMessage(File file, String version) throws URISyntaxException, SAXException, MalformedURLException, FileNotFoundException {
    URL url;

    if(version.equals("1.3.0")){
      url = ValidateMessage.class.getClassLoader().getResource("proteomeXchange-1.3.0.xsd");
    }else{
      url = ValidateMessage.class.getClassLoader().getResource("proteomeXchange-1.4.0.xsd");
    }
    if (url == null) {
      throw new IllegalStateException("No proteomeXchange schema file found!");
    }
    return validatePXMessage(file, url, version);
  }

  /**
   * Method to validate a supplied PX XML file.
   *
   * @param file the PX XML file to be validated.
   * @param schemaLocation the PX XML schema
   * @return String which lists any errors that occurred during validation. If there were none, the String will be empty.
   * @throws SAXException
   * @throws MalformedURLException
   * @throws FileNotFoundException
   * @throws URISyntaxException
   */
  private static String  validatePXMessage(File file, URL schemaLocation, String version) throws SAXException, MalformedURLException, FileNotFoundException, URISyntaxException{
    StringBuilder errorOutput = new StringBuilder();
    GenericSchemaValidator genericValidator = new GenericSchemaValidator();
    genericValidator.setSchema(schemaLocation.toURI());
    logger.info("XML schema validation on " + file.getName() + " against schema: " + version);
    ErrorHandlerIface handler = new ValidationErrorHandler();
    genericValidator.setErrorHandler(handler);
    BufferedReader br = new BufferedReader(new FileReader(file));
    genericValidator.validate(br);
    List<String> errorMsgs = handler.getErrorMessages(); // ToDo: make ErrorHandlerIface type safe
    for (String content : errorMsgs) {
      errorOutput.append(content);
      errorOutput.append(System.getProperty("line.separator"));
    }
    return errorOutput.toString();
  }
}
