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
    //http://central.proteomexchange.org/cgi/GetDataset?ID=PXD000001
    public static final String CHARSET = "UTF-8";

    /**
     * Method to send a supplied PX XML file to Proteome Central.
     *
     * @param file the PX XML file to be validated.
     * @param params the XMLParams needed for configuring the options when sending.
     * @return a String which lists the output from the HTTP service used for posting the PX XML file.
     */
    public static String postMessage(File file, XMLParams params)throws IOException {
        String serverResponse;
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(URL);

        FileBody bin = new FileBody(file);
        MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, null, Charset.forName(CHARSET));

        reqEntity.addPart("ProteomeXchangeXML", bin);
        reqEntity.addPart("PXPartner", new StringBody(params.getPxPartner(), "text/plain", Charset.forName(CHARSET)));
        reqEntity.addPart("authentication", new StringBody(params.getAuthentication(), "text/plain", Charset.forName(CHARSET)));
        reqEntity.addPart("method", new StringBody(params.getMethod(), "text/plain", Charset.forName(CHARSET)));
        reqEntity.addPart("test", new StringBody(params.getTest(), "text/plain", Charset.forName(CHARSET)));
        reqEntity.addPart("verbose", new StringBody(params.getVerbose(), "text/plain", Charset.forName(CHARSET)));
        reqEntity.addPart("noEmailBroadcast", new StringBody(params.getNoEmailBroadcast(), "text/plain", Charset.forName(CHARSET)));

        httppost.setEntity(reqEntity);

        // Execute HTTP Post Request

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
