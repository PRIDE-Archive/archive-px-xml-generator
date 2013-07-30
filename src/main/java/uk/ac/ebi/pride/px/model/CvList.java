
package uk.ac.ebi.pride.px.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * The list of controlled vocabularies used in the file.
 * 
 * <p>Java class for CvListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="CvListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="CV" type="{}CvType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CvListType", propOrder = {
    "cv"
})
public class CvList
    implements Serializable, PXObject
{

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "CV", required = true)
    protected List<Cv> cv;

    /**
     * Gets the value of the cv property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the cv property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCV().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Cv }
     * 
     * 
     */
    public List<Cv> getCV() {
        if (cv == null) {
            cv = new ArrayList<Cv>();
        }
        return this.cv;
    }

}
