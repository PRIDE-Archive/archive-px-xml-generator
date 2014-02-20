package uk.ac.ebi.pride.px.xml;

/**
 * Created with IntelliJ IDEA.
 * User: tobias
 * Date: 19/02/14
 * Time: 16:21
 * To change this template use File | Settings | File Templates.
 */
public class XMLParams {

    private String pxPartner;
    private String authentication;
    private String method = "submitDataset";
    private String test = "no";
    private String verbose = "no";

    public String getPxPartner() {
        return pxPartner;
    }

    public void setPxPartner(String pxPartner) {
        this.pxPartner = pxPartner;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }

    public String getVerbose() {
        return verbose;
    }

    public void setVerbose(String verbose) {
        this.verbose = verbose;
    }

}
