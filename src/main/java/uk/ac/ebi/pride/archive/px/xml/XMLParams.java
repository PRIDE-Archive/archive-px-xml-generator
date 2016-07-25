package uk.ac.ebi.pride.archive.px.xml;

/**
 * Class to configure options when posting PX XML to Proteome Central.
 * Refactored from the PX Submission pipeline originally.
 *
 * @author Tobias Ternent
 */
public class XMLParams {

    private String pxPartner;
    private String authentication;
    private String method = "submitDataset";
    private String test = "no";
    private String verbose = "no";
    private String noEmailBroadcast = "false";

    /**
     * Gets the PX Partner.
     * @return the PX Partner.
     */
    public String getPxPartner() {
        return pxPartner;
    }

    /**
     * Sets the PX Partner
     * @param pxPartner the PX Partner
     */
    public void setPxPartner(String pxPartner) {
        this.pxPartner = pxPartner;
    }

    /**
     * Gets the authentication
     * @return the authentication
     */
    public String getAuthentication() {
        return authentication;
    }

    /**
     * Sets the authentication
     * @param authentication the authentication
     */
    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    /**
     * Gets the method value
     * @return the method value
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the method value
     * @param method the method value
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Gets the test value
     * @return the test value
     */
    public String getTest() {
        return test;
    }

    /**
     * Sets the test value
     * @param test the test value
     */
    public void setTest(String test) {
        this.test = test;
    }

    /**
     * Gets the verbose value
     * @return the verbose value
     */
    public String getVerbose() {
        return verbose;
    }

    /**
     * Sets the verbose value
     * @param verbose the verbose value
     */
    public void setVerbose(String verbose) {
        this.verbose = verbose;
    }

    /**
     * Gets the noEmailBroadcast
     * @return the noEmailBroadcast
     */
    public String getNoEmailBroadcast() {
        return noEmailBroadcast;
    }

    /**
     * Sets the noEmailBroadcast.
     * @param noEmailBroadcast the noEmailBroadcast.
     */
    public void setNoEmailBroadcast(String noEmailBroadcast) {
        this.noEmailBroadcast = noEmailBroadcast;
    }


}
