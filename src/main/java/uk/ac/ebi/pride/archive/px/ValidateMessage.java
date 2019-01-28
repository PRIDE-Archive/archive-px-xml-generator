package uk.ac.ebi.pride.archive.px;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import uk.ac.ebi.pride.tools.ErrorHandlerIface;
import uk.ac.ebi.pride.tools.GenericSchemaValidator;
import uk.ac.ebi.pride.tools.ValidationErrorHandler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Class to validate a PX XML file according to the PX XML schema.
 *
 * @author Tobias Ternent
 */

public class ValidateMessage {

  private static final Logger logger = LoggerFactory.getLogger(ValidateMessage.class);
  private static final String SCHEMA_LOCATION = "http://ftp.pride.ebi.ac.uk/pride/resources/schema/proteomexchange/proteomeXchange-1.4.0.xsd";

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
  public static String validateMessage(File file) throws SAXException, MalformedURLException, FileNotFoundException, URISyntaxException{
    return validateMessage(file, SCHEMA_LOCATION);
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
  public static String validateMessage(File file, String schemaLocation) throws SAXException, MalformedURLException, FileNotFoundException, URISyntaxException{
    StringBuilder errorOutput = new StringBuilder();
    GenericSchemaValidator genericValidator = new GenericSchemaValidator();
    genericValidator.setSchema(new URI(StringUtils.isEmpty(schemaLocation) ? SCHEMA_LOCATION : schemaLocation));
    logger.info("XML schema validation on " + file.getName());
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
