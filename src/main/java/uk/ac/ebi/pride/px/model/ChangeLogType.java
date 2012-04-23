
package uk.ac.ebi.pride.px.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * A collection of all change log messages to record the changes and updates related to this dataset.
 * 
 * <p>Java class for ChangeLogType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ChangeLogType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ChangeLogEntry" type="{}ChangeLogEntryType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ChangeLogType", propOrder = {
    "changeLogEntry"
})
public class ChangeLogType
    extends PXObject
    implements Serializable
{

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "ChangeLogEntry", required = true)
    protected List<ChangeLogEntryType> changeLogEntry;

    /**
     * Gets the value of the changeLogEntry property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the changeLogEntry property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getChangeLogEntry().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ChangeLogEntryType }
     * 
     * 
     */
    public List<ChangeLogEntryType> getChangeLogEntry() {
        if (changeLogEntry == null) {
            changeLogEntry = new ArrayList<ChangeLogEntryType>();
        }
        return this.changeLogEntry;
    }

}
