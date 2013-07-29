package uk.ac.ebi.pride.px.Mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: javizca
 * Date: Oct 28, 2008
 * Time: 2:36:18 PM
 * ToDo: chould not be in this package!
 */
@Deprecated
public class PropertiesHelper {

	/**
     * Define a static logger variable so that it references the
     * Logger instance named "PropertiesHelper".
     */
    private static final Logger logger = LoggerFactory.getLogger(PropertiesHelper.class);

	private static Properties PRIDE_APPLICATION_PROPERTIES;

    // Define any properties files to be loaded here and add them to the PROPERTIES_FILE array.
	// They will then all be loaded in as required.
	private static final String PRIDE_EMAIL_PROPERTIES_FILENAME = "email.properties";

	private static final String[] PROPERTIES_FILES = new String[1];
	static{
		PROPERTIES_FILES[0] = PRIDE_EMAIL_PROPERTIES_FILENAME;
	}

	public static String getProperty (ProteomExchangePropertyType type){
	    readPrideWebProperties();
		return (String) PRIDE_APPLICATION_PROPERTIES.get(type.toString());
	}
	/**
     * This method reads the known properties from the 'pride_database.properties'
     */
    static void readPrideWebProperties() {
		if (PRIDE_APPLICATION_PROPERTIES == null){
			PRIDE_APPLICATION_PROPERTIES = new Properties();
			for (String propertiesFile : PROPERTIES_FILES){
				logger.debug("Reading properties from file: " + propertiesFile);
				InputStream is = PropertiesHelper.class.getClassLoader().getResourceAsStream(propertiesFile);
				if(is != null) {
					try {
						PRIDE_APPLICATION_PROPERTIES.load(is);
						logger.debug("All PRIDE properties read from file " + propertiesFile);
					} catch(IOException e) {
						logger.error("Error loading properties from the '" + propertiesFile + "' file.");
					}
					finally {
						try {
							is.close();
						} catch (IOException e) {
							logger.error ("Could not close the stream for the pride web properties file.");
						}
					}
				} else {
					logger.error("Unable to load file '" + propertiesFile + "' from the web application classpath.");
				}
			}
		}
	}
}

