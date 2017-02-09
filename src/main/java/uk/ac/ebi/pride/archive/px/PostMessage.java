package uk.ac.ebi.pride.archive.px;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClients;
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

    public static final String URL = "http://proteomecentral.proteomexchange.org/cgi/Dataset";
    //http://central.proteomexchange.org/cgi/GetDataset?ID=PXD000001
    public static final String CHARSET = "UTF-8";

    /**
     * Method to send a supplied PX XML file to Proteome Central.
     *
     * @param file the PX XML file to be validated.
     * @param params the XMLParams needed for configuring the options when sending.
     * @return a String which lists the output from the HTTP service used for posting the PX XML file.
     * @throws IOException
     */
    public static String postMessage(File file, XMLParams params)throws IOException {
        String serverResponse;
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost(URL);

        FileBody bin = new FileBody(file);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        multipartEntityBuilder.addPart("ProteomeXchangeXML", bin);
        multipartEntityBuilder.addPart("PXPartner", new StringBody(params.getPxPartner(), ContentType.TEXT_PLAIN));
        multipartEntityBuilder.addPart("authentication", new StringBody(params.getAuthentication(), ContentType.TEXT_PLAIN));
        multipartEntityBuilder.addPart("method", new StringBody(params.getMethod(), ContentType.TEXT_PLAIN));
        multipartEntityBuilder.addPart("test", new StringBody(params.getTest(), ContentType.TEXT_PLAIN));
        multipartEntityBuilder.addPart("verbose", new StringBody(params.getVerbose(), ContentType.TEXT_PLAIN));
        multipartEntityBuilder.addPart("noEmailBroadcast", new StringBody(params.getNoEmailBroadcast(), ContentType.TEXT_PLAIN));

        httppost.setEntity(multipartEntityBuilder.build());

        // Execute HTTP Post Request
        logger.info("REQUEST URL ---> " + httppost.getURI());
        logger.info("REQUEST LINE ---> " + httppost.getRequestLine().toString());
        // ResponseHandler<String> responseHandler=new BasicResponseHandler();
        HttpResponse response = httpclient.execute(httppost);
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        serverResponse = EntityUtils.toString(entity);
        if (statusCode != 200) {
            // responseBody will have the error response
            logger.error("Error from server: " + statusCode + serverResponse);
        }
        return serverResponse;
    }
}
