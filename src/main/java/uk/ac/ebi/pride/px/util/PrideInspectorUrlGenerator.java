package uk.ac.ebi.pride.px.util;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.data.exception.SubmissionFileException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Generate tiny url for pride inspector webstart access
 *
 * @author Rui Wang
 * @version $Id$
 * ToDo: should not be in this package!
 */
@Deprecated
public class PrideInspectorUrlGenerator {
    public static final Logger logger = LoggerFactory.getLogger(PrideInspectorUrlGenerator.class);
    public static final String RANGE_SEPARATOR = "-";
    public static final String COMMA_SEPARATOR = ",";

    public String generate(Set<String> experimentAccessions) throws SubmissionFileException {
        String accessionString = compact(experimentAccessions);
        try {
            return getTinyURL(accessionString);
        } catch (IOException e) {
            String msg = "Failed to create tiny URL for pride inspectror";
            logger.error(msg, e);
            throw new SubmissionFileException(msg, e);
        }
    }

    private String getTinyURL(String accessionString) throws IOException {
        String rawURL = "http://www.ebi.ac.uk/pride/q.do?accession=" + accessionString;
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("url", rawURL);

        String tinyURL = null;
        InputStream stream = makeGetRequest("http://tinyurl.com/api-create.php", parameters);
        BufferedReader reader;

        reader = new BufferedReader(new InputStreamReader(stream));
        try {
            while ((tinyURL = reader.readLine()) != null) {
                if (!"".equals(tinyURL.trim())) {
                    break;
                }
            }
        } finally {
            reader.close();
        }

        return tinyURL;
    }

    private String compact(Collection<String> accs) {
        if (accs == null || accs.isEmpty()) {
            String msg = "Input list of accessions cannot be null or empty";
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // create an integer list based the input accession list
        List<Integer> accsInt = new ArrayList<Integer>();
        for (String acc : accs) {
            int a = Integer.parseInt(acc);
            if (!accsInt.contains(a)) {
                accsInt.add(a);
            }
        }

        // sort the list to ascending order
        Collections.sort(accsInt);
        String accStr = "";
        for (int i = 0; i < accsInt.size(); i++) {
            if ("".equals(accStr)) {
                accStr += accsInt.get(i);
            } else if (accsInt.get(i) != (accsInt.get(i - 1) + 1)) {
                if (!accStr.endsWith(accsInt.get(i - 1).toString())) {
                    accStr += RANGE_SEPARATOR + accsInt.get(i - 1);
                }
                accStr += COMMA_SEPARATOR + accsInt.get(i);
            } else if (i == accsInt.size() - 1) {
                accStr += RANGE_SEPARATOR + accsInt.get(i);
            }
        }

        return accStr;
    }

    @SuppressWarnings("unchecked")
    private InputStream makeGetRequest(String baseURL,
                                       Map<String, String> parameters) throws IOException {
        HttpClient httpClient = new HttpClient();

        GetMethod get = new GetMethod(baseURL);
        NameValuePair[] httpParameters = new NameValuePair[parameters.size()];
        int i = 0;
        for (Map.Entry<String, String> e : parameters.entrySet()) {
            NameValuePair pair = new NameValuePair(e.getKey(), e.getValue());
            httpParameters[i] = pair;
            i++;
        }
        get.setQueryString(httpParameters);


        int statusCode = httpClient.executeMethod(get);
        if (statusCode == HttpStatus.SC_OK) {
            return get.getResponseBodyAsStream();
        }

        get.releaseConnection();

        return null;
    }
}
