package uk.ac.ebi.pride.archive.px;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;

import java.io.File;
import java.io.IOException;

/**
 * Class to post a PX XML file to Proteome Central.
 *
 * @author Tobias Ternent
 */
public class PostMessage {
  public static final Logger logger = LoggerFactory.getLogger(PostMessage.class);

//  public static final String URL = "http://proteomecentral.proteomexchange.org/cgi/Dataset";
//  public static final String URL = "http://proteomecentral.proteomexchange.org/beta/cgi/Dataset";
//  http://central.proteomexchange.org/cgi/GetDataset?ID=PXD000001

  /**
   * Method to send a supplied PX XML file to Proteome Central.
   *
   * @param file the PX XML file to be validated.
   * @param params the XMLParams needed for configuring the options when sending.
   * @return a String which lists the output from the HTTP service used for posting the PX XML file.
   */
  public static String postFile(File file, XMLParams params, String formatVersion) {

    // Currently we have "1.3.0" and "1.4.0" in beta version
    String URL = (formatVersion.equals("1.3.0"))? "http://proteomecentral.proteomexchange.org/cgi/Dataset": "http://proteomecentral.proteomexchange.org/beta/cgi/Dataset";
    String serverResponse = null; // server response if we don't run into errors
    try {
      // create the POST attributes
      MultipartEntityBuilder builder = MultipartEntityBuilder.create();
      builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
      builder.addPart("ProteomeXchangeXML", new FileBody(file));
      builder.addTextBody("PXPartner", params.getPxPartner());
      builder.addTextBody("authentication", params.getAuthentication());
      builder.addTextBody("method",  params.getMethod());
      builder.addTextBody("test",  params.getTest());
      builder.addTextBody("verbose",  params.getVerbose());

      int CONNECTION_TIMEOUT_MS = 5 * 1000; // Timeout in millis.
      RequestConfig requestConfig = RequestConfig.custom()
              .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
              .setConnectTimeout(CONNECTION_TIMEOUT_MS)
              .setSocketTimeout(CONNECTION_TIMEOUT_MS)
              .build();

      // create the POST request
      HttpPost httpPost = new HttpPost(URL);
      httpPost.setConfig(requestConfig);
      httpPost.setEntity(builder.build());

      // execute the POST request
      HttpClient client = HttpClientBuilder.create().build();
      HttpResponse response = client.execute(httpPost);

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
    }
    return serverResponse;
  }
}
