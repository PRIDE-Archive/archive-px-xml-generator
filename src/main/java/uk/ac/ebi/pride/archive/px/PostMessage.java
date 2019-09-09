package uk.ac.ebi.pride.archive.px;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Class to post a PX XML file to Proteome Central.
 *
 * @author Tobias Ternent
 */
public class PostMessage {
  public static final Logger logger = LoggerFactory.getLogger(PostMessage.class);

  public static final String URL = "http://proteomecentral.proteomexchange.org/cgi/Dataset";
  public static final String CHARSET = "UTF-8";

    /**
     * Method to send a supplied PX XML file to Proteome Central.
     *
     * @param file the PX XML file to be validated.
     * @param params the XMLParams needed for configuring the options when sending.
     * @return a String which lists the output from the HTTP service used for posting the PX XML file.
     */
    public static String postFile(File file, XMLParams params, String formatVersion) {
        String serverResponse = null; // server response if we don't run into errors
        try {

            PostMessage.checkVersionCompatibility(formatVersion);

            // create the POST attributes
            MultipartEntity builder = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart("ProteomeXchangeXML", new FileBody(file));
            builder.addPart("PXPartner", new StringBody(params.getPxPartner(), "text/plain", Charset.forName(CHARSET)));
            builder.addPart("authentication", new StringBody(params.getAuthentication(), "text/plain", Charset.forName(CHARSET)));
            builder.addPart("method",  new StringBody(params.getMethod(), "text/plain", Charset.forName(CHARSET)));
            builder.addPart("test",  new StringBody(params.getTest(), "text/plain", Charset.forName(CHARSET)));
            builder.addPart("verbose",  new StringBody(params.getVerbose(), "text/plain", Charset.forName(CHARSET)));
            builder.addPart("noEmailBroadcast",  new StringBody(params.getNoEmailBroadcast(), "text/plain", Charset.forName(CHARSET)));

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(URL);
            httppost.setEntity(builder);
            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);

            // retrieve and inspect the response
            HttpEntity entity = response.getEntity();
            serverResponse = EntityUtils.toString(entity);

            // check the response status code
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                logger.error("Error " + statusCode + " from server: " + serverResponse); // responseBody will have the error response
            }else{
                logger.info(file + " with :" + statusCode + " OK");
            }
        } catch (IOException e) {
            logger.error("ERROR executing command! " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverResponse;
    }


    private static void checkVersionCompatibility(String formatVersion) throws Exception {
        if(!formatVersion.equals("1.4.0")){
            throw new Exception("Unsupported ProteomeXchange version. Currently PX supports 1.4.0");

        }
    }
}
