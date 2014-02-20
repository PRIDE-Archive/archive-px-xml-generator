package uk.ac.ebi.pride.px;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.xml.sax.SAXException;
import uk.ac.ebi.pride.tools.ErrorHandlerIface;
import uk.ac.ebi.pride.tools.GenericSchemaValidator;
import uk.ac.ebi.pride.tools.ValidationErrorHandler;

/**
 * Created with IntelliJ IDEA.
 * User: tobias
 * Date: 19/02/14
 * Time: 16:26
 * To change this template use File | Settings | File Templates.
 */
public class ValidateMessage {

    private static final Logger logger = LoggerFactory.getLogger(ValidateMessage.class);
    private static final String SCHEMA_LOCATION = "http://proteomecentral.proteomexchange.org/schemas/proteomeXchange-1.2.0.xsd";

    public static String validateMessage(File file) throws SAXException, MalformedURLException, FileNotFoundException, URISyntaxException{
        StringBuilder errorOutput = new StringBuilder();
        GenericSchemaValidator genericValidator = new GenericSchemaValidator();
        genericValidator.setSchema(new URI(SCHEMA_LOCATION));

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
