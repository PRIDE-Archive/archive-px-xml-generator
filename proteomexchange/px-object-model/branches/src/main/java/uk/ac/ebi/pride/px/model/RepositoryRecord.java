
package uk.ac.ebi.pride.px.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RepositoryRecordType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RepositoryRecordType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SourceFileRef" type="{}RefType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="PublicationRef" type="{}RefType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="InstrumentRef" type="{}RefType" maxOccurs="unbounded"/>
 *         &lt;element name="SampleList" type="{}SampleListType" maxOccurs="unbounded"/>
 *         &lt;element name="ModificationList" type="{}ModificationListType" minOccurs="0"/>
 *         &lt;element name="AnnotationList" type="{}AdditionalInformationType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="recordID" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="repositoryID" use="required" type="{}HostingRepositoryType" />
 *       &lt;attribute name="uri" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RepositoryRecordType", propOrder = {
    "sourceFileRef",
    "publicationRef",
    "instrumentRef",
    "sampleList",
    "modificationList",
    "annotationList"
})
public class RepositoryRecord
    implements Serializable, PXObject
{

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "SourceFileRef")
    protected List<Ref> sourceFileRef;
    @XmlElement(name = "PublicationRef")
    protected List<Ref> publicationRef;
    @XmlElement(name = "InstrumentRef", required = true)
    protected List<Ref> instrumentRef;
    @XmlElement(name = "SampleList", required = true)
    protected List<SampleList> sampleList;
    @XmlElement(name = "ModificationList")
    protected ModificationList modificationList;
    @XmlElement(name = "AnnotationList")
    protected AdditionalInformation annotationList;
    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute
    protected String label;
    @XmlAttribute(required = true)
    protected String recordID;
    @XmlAttribute(required = true)
    protected HostingRepositoryType repositoryID;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String uri;

    /**
     * Gets the value of the sourceFileRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sourceFileRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSourceFileRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Ref }
     * 
     * 
     */
    public List<Ref> getSourceFileRef() {
        if (sourceFileRef == null) {
            sourceFileRef = new ArrayList<Ref>();
        }
        return this.sourceFileRef;
    }

    /**
     * Gets the value of the publicationRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the publicationRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPublicationRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Ref }
     * 
     * 
     */
    public List<Ref> getPublicationRef() {
        if (publicationRef == null) {
            publicationRef = new ArrayList<Ref>();
        }
        return this.publicationRef;
    }

    /**
     * Gets the value of the instrumentRef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the instrumentRef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInstrumentRef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Ref }
     * 
     * 
     */
    public List<Ref> getInstrumentRef() {
        if (instrumentRef == null) {
            instrumentRef = new ArrayList<Ref>();
        }
        return this.instrumentRef;
    }

    /**
     * Gets the value of the sampleList property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the sampleList property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSampleList().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SampleList }
     * 
     * 
     */
    public List<SampleList> getSampleList() {
        if (sampleList == null) {
            sampleList = new ArrayList<SampleList>();
        }
        return this.sampleList;
    }

    /**
     * Gets the value of the modificationList property.
     * 
     * @return
     *     possible object is
     *     {@link ModificationList }
     *     
     */
    public ModificationList getModificationList() {
        return modificationList;
    }

    /**
     * Sets the value of the modificationList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ModificationList }
     *     
     */
    public void setModificationList(ModificationList value) {
        this.modificationList = value;
    }

    /**
     * Gets the value of the annotationList property.
     * 
     * @return
     *     possible object is
     *     {@link AdditionalInformation }
     *     
     */
    public AdditionalInformation getAnnotationList() {
        return annotationList;
    }

    /**
     * Sets the value of the annotationList property.
     * 
     * @param value
     *     allowed object is
     *     {@link AdditionalInformation }
     *     
     */
    public void setAnnotationList(AdditionalInformation value) {
        this.annotationList = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the label property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the value of the label property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLabel(String value) {
        this.label = value;
    }

    /**
     * Gets the value of the recordID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRecordID() {
        return recordID;
    }

    /**
     * Sets the value of the recordID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRecordID(String value) {
        this.recordID = value;
    }

    /**
     * Gets the value of the repositoryID property.
     * 
     * @return
     *     possible object is
     *     {@link HostingRepositoryType }
     *     
     */
    public HostingRepositoryType getRepositoryID() {
        return repositoryID;
    }

    /**
     * Sets the value of the repositoryID property.
     * 
     * @param value
     *     allowed object is
     *     {@link HostingRepositoryType }
     *     
     */
    public void setRepositoryID(HostingRepositoryType value) {
        this.repositoryID = value;
    }

    /**
     * Gets the value of the uri property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUri(String value) {
        this.uri = value;
    }

}
