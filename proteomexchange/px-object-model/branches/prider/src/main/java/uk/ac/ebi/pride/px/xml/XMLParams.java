package uk.ac.ebi.pride.px.xml;

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
