
package uk.ac.ebi.pride.px.model;

import java.io.Serializable;
import java.util.Calendar;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import uk.ac.ebi.pride.px.jaxb.adapters.CalendarAdapter;


/**
 * <p>Java class for DatasetSummaryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DatasetSummaryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Description" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *       &lt;attribute name="announceDate" use="required" type="{http://www.w3.org/2001/XMLSchema}date" />
 *       &lt;attribute name="broadcaster" use="required" type="{}BroadcasterType" />
 *       &lt;attribute name="title" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DatasetSummaryType", propOrder = {
    "description"
})
public class DatasetSummary
    extends PXObject
    implements Serializable
{

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "Description", required = true)
    protected String description;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CalendarAdapter.class)
    @XmlSchemaType(name = "date")
    protected Calendar announceDate;
    @XmlAttribute(required = true)
    protected BroadcasterType broadcaster;
    @XmlAttribute(required = true)
    protected String title;

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the announceDate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public Calendar getAnnounceDate() {
        return announceDate;
    }

    /**
     * Sets the value of the announceDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAnnounceDate(Calendar value) {
        this.announceDate = value;
    }

    /**
     * Gets the value of the broadcaster property.
     * 
     * @return
     *     possible object is
     *     {@link BroadcasterType }
     *     
     */
    public BroadcasterType getBroadcaster() {
        return broadcaster;
    }

    /**
     * Sets the value of the broadcaster property.
     * 
     * @param value
     *     allowed object is
     *     {@link BroadcasterType }
     *     
     */
    public void setBroadcaster(BroadcasterType value) {
        this.broadcaster = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

}
