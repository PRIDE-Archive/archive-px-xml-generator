
package uk.ac.ebi.pride.archive.px.model;

import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * A single entry from an ontology or a controlled vocabulary.
 *
 * <p>Java class for CvParamType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="CvParamType">
 *   &lt;complexContent>
 *     &lt;extension base="{}AbstractParamType">
 *       &lt;attribute name="cvRef" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="accession" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CvParamType")
public class CvParam
    extends AbstractParam
    implements Serializable
{

    private final static long serialVersionUID = 100L;
    @XmlAttribute(required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object cvRef;
    @XmlAttribute(required = true)
    protected String accession;

    /**
     * Gets the value of the cvRef property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getCvRef() {
        return cvRef;
    }

    /**
     * Sets the value of the cvRef property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setCvRef(Object value) {
        this.cvRef = value;
    }

    /**
     * Gets the value of the accession property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getAccession() {
        return accession;
    }

    /**
     * Sets the value of the accession property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setAccession(String value) {
        this.accession = value;
    }

}
