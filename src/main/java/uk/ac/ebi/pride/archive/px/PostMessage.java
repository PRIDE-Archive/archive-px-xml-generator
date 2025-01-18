package uk.ac.ebi.pride.archive.px;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.archive.px.xml.XMLParams;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;

/**
 * Class to post a PX XML file to Proteome Central.
 *
 * @author Tobias Ternent
 */
public class PostMessage {
    public static final Logger logger = LoggerFactory.getLogger(PostMessage.class);
    public static final String CHARSET = "UTF-8";

    /**
     * Method to send a supplied PX XML file to Proteome Central.
     *
     * @param file   the PX XML file to be validated.
     * @param params the XMLParams needed for configuring the options when sending.
     * @return a String which lists the output from the HTTP service used for posting the PX XML file.
     */
    public static String postFile(File file, XMLParams params, String formatVersion) throws Exception {
        String serverResponse = null; // server response if we don't run into errors

        try {
            PostMessage.checkVersionCompatibility(formatVersion);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(Charset.forName(CHARSET));
            builder.addBinaryBody("ProteomeXchangeXML", file);
            builder.addTextBody("PXPartner", params.getPxPartner(), ContentType.create("text/plain", CHARSET));
            builder.addTextBody("authentication", params.getAuthentication(), ContentType.create("text/plain", CHARSET));
            builder.addTextBody("method", params.getMethod(), ContentType.create("text/plain", CHARSET));
            builder.addTextBody("test", params.getTest(), ContentType.create("text/plain", CHARSET));
            builder.addTextBody("verbose", params.getVerbose(), ContentType.create("text/plain", CHARSET));
            builder.addTextBody("noEmailBroadcast", params.getNoEmailBroadcast(), ContentType.create("text/plain", CHARSET));

            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial((X509Certificate[] chain, String authType) -> true) // Trust all certs
                    .build();
            org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory sslConFactory = new SSLConnectionSocketFactory(sslContext);
            HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(sslConFactory)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build()) {

                ClassicHttpRequest httppost = new HttpPost(Constants.PROTEOME_EXCHANGE_URL + "Dataset");
                httppost.setEntity(builder.build());
                // Execute HTTP Post Request
                try (CloseableHttpResponse response = httpClient.execute(httppost)) {
                    // retrieve and inspect the response
                    HttpEntity entity = response.getEntity();
                    serverResponse = EntityUtils.toString(entity);

                    // check the response status code
                    int statusCode = response.getCode();
                    if (statusCode != 200) {
                        logger.error("Error " + statusCode + " from server: " + serverResponse); // responseBody will have the error response
                    } else {
                        logger.info(file + " with :" + statusCode + " OK");
                    }
                }
            }

        } catch (Exception e) {
            throw new Exception("ERROR in posting PXXML file to Proteome Central! " + e.getMessage());
        }
        return serverResponse;
    }


    private static void checkVersionCompatibility(String formatVersion) throws Exception {
        if (!formatVersion.equals("1.4.0")) {
            throw new Exception("Unsupported ProteomeXchange version. Currently PX supports 1.4.0");

        }
    }
}
