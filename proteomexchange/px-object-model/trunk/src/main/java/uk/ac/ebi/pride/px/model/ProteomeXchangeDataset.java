
package uk.ac.ebi.pride.px.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ProteomeXchangeDatasetType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProteomeXchangeDatasetType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ChangeLog" type="{}ChangeLogType" minOccurs="0"/>
 *         &lt;element name="DatasetSummary" type="{}DatasetSummaryType"/>
 *         &lt;element name="DatasetIdentifierList" type="{}DatasetIdentifierListType"/>
 *         &lt;element name="DatasetOriginList" type="{}DatasetOriginListType"/>
 *         &lt;element name="SpeciesList" type="{}SpeciesListType"/>
 *         &lt;element name="InstrumentList" type="{}InstrumentListType"/>
 *         &lt;element name="ModificationList" type="{}ModificationListType"/>
 *         &lt;element name="ContactList" type="{}ContactListType"/>
 *         &lt;element name="PublicationList" type="{}PublicationListType"/>
 *         &lt;element name="KeywordList" type="{}KeywordListType"/>
 *         &lt;element name="FullDatasetLinkList" type="{}FullDatasetLinkListType"/>
 *         &lt;element name="DatasetFileList" type="{}DatasetFileListType" minOccurs="0"/>
 *         &lt;element name="RepositoryRecordList" type="{}RepositoryRecordListType" minOccurs="0"/>
 *         &lt;element name="AdditionalInformation" type="{}AdditionalInformationType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="formatVersion" use="required" type="{}versionRegex" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProteomeXchangeDatasetType", propOrder = {
    "changeLog",
    "datasetSummary",
    "datasetIdentifierList",
    "datasetOriginList",
    "speciesList",
    "instrumentList",
    "modificationList",
    "contactList",
    "publicationList",
    "keywordList",
    "fullDatasetLinkList",
    "datasetFileList",
    "repositoryRecordList",
    "additionalInformation"
})
public class ProteomeXchangeDataset
    extends PXObject
    implements Serializable
{

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "ChangeLog")
    protected ChangeLogType changeLog;
    @XmlElement(name = "DatasetSummary", required = true)
    protected DatasetSummary datasetSummary;
    @XmlElement(name = "DatasetIdentifierList", required = true)
    protected DatasetIdentifierList datasetIdentifierList;
    @XmlElement(name = "DatasetOriginList", required = true)
    protected DatasetOriginList datasetOriginList;
    @XmlElement(name = "SpeciesList", required = true)
    protected SpeciesList speciesList;
    @XmlElement(name = "InstrumentList", required = true)
    protected InstrumentList instrumentList;
    @XmlElement(name = "ModificationList", required = true)
    protected ModificationList modificationList;
    @XmlElement(name = "ContactList", required = true)
    protected ContactList contactList;
    @XmlElement(name = "PublicationList", required = true)
    protected PublicationList publicationList;
    @XmlElement(name = "KeywordList", required = true)
    protected KeywordList keywordList;
    @XmlElement(name = "FullDatasetLinkList", required = true)
    protected FullDatasetLinkList fullDatasetLinkList;
    @XmlElement(name = "DatasetFileList")
    protected DatasetFileList datasetFileList;
    @XmlElement(name = "RepositoryRecordList")
    protected RepositoryRecordList repositoryRecordList;
    @XmlElement(name = "AdditionalInformation")
    protected AdditionalInformation additionalInformation;
    @XmlAttribute(required = true)
    protected String id;
    @XmlAttribute(required = true)
    protected String formatVersion;

    /**
     * Gets the value of the changeLog property.
     * 
     * @return
     *     possible object is
     *     {@link ChangeLogType }
     *     
     */
    public ChangeLogType getChangeLog() {
        return changeLog;
    }

    /**
     * Sets the value of the changeLog property.
     * 
     * @param value
     *     allowed object is
     *     {@link ChangeLogType }
     *     
     */
    public void setChangeLog(ChangeLogType value) {
        this.changeLog = value;
    }

    /**
     * Gets the value of the datasetSummary property.
     * 
     * @return
     *     possible object is
     *     {@link DatasetSummary }
     *     
     */
    public DatasetSummary getDatasetSummary() {
        return datasetSummary;
    }

    /**
     * Sets the value of the datasetSummary property.
     * 
     * @param value
     *     allowed object is
     *     {@link DatasetSummary }
     *     
     */
    public void setDatasetSummary(DatasetSummary value) {
        this.datasetSummary = value;
    }

    /**
     * Gets the value of the datasetIdentifierList property.
     * 
     * @return
     *     possible object is
     *     {@link DatasetIdentifierList }
     *     
     */
    public DatasetIdentifierList getDatasetIdentifierList() {
        return datasetIdentifierList;
    }

    /**
     * Sets the value of the datasetIdentifierList property.
     * 
     * @param value
     *     allowed object is
     *     {@link DatasetIdentifierList }
     *     
     */
    public void setDatasetIdentifierList(DatasetIdentifierList value) {
        this.datasetIdentifierList = value;
    }

    /**
     * Gets the value of the datasetOriginList property.
     * 
     * @return
     *     possible object is
     *     {@link DatasetOriginList }
     *     
     */
    public DatasetOriginList getDatasetOriginList() {
        return datasetOriginList;
    }

    /**
     * Sets the value of the datasetOriginList property.
     * 
     * @param value
     *     allowed object is
     *     {@link DatasetOriginList }
     *     
     */
    public void setDatasetOriginList(DatasetOriginList value) {
        this.datasetOriginList = value;
    }

    /**
     * Gets the value of the speciesList property.
     * 
     * @return
     *     possible object is
     *     {@link SpeciesList }
     *     
     */
    public SpeciesList getSpeciesList() {
        return speciesList;
    }

    /**
     * Sets the value of the speciesList property.
     * 
     * @param value
     *     allowed object is
     *     {@link SpeciesList }
     *     
     */
    public void setSpeciesList(SpeciesList value) {
        this.speciesList = value;
    }

    /**
     * Gets the value of the instrumentList property.
     * 
     * @return
     *     possible object is
     *     {@link InstrumentList }
     *     
     */
    public InstrumentList getInstrumentList() {
        return instrumentList;
    }

    /**
     * Sets the value of the instrumentList property.
     * 
     * @param value
     *     allowed object is
     *     {@link InstrumentList }
     *     
     */
    public void setInstrumentList(InstrumentList value) {
        this.instrumentList = value;
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
     * Gets the value of the contactList property.
     * 
     * @return
     *     possible object is
     *     {@link ContactList }
     *     
     */
    public ContactList getContactList() {
        return contactList;
    }

    /**
     * Sets the value of the contactList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ContactList }
     *     
     */
    public void setContactList(ContactList value) {
        this.contactList = value;
    }

    /**
     * Gets the value of the publicationList property.
     * 
     * @return
     *     possible object is
     *     {@link PublicationList }
     *     
     */
    public PublicationList getPublicationList() {
        return publicationList;
    }

    /**
     * Sets the value of the publicationList property.
     * 
     * @param value
     *     allowed object is
     *     {@link PublicationList }
     *     
     */
    public void setPublicationList(PublicationList value) {
        this.publicationList = value;
    }

    /**
     * Gets the value of the keywordList property.
     * 
     * @return
     *     possible object is
     *     {@link KeywordList }
     *     
     */
    public KeywordList getKeywordList() {
        return keywordList;
    }

    /**
     * Sets the value of the keywordList property.
     * 
     * @param value
     *     allowed object is
     *     {@link KeywordList }
     *     
     */
    public void setKeywordList(KeywordList value) {
        this.keywordList = value;
    }

    /**
     * Gets the value of the fullDatasetLinkList property.
     * 
     * @return
     *     possible object is
     *     {@link FullDatasetLinkList }
     *     
     */
    public FullDatasetLinkList getFullDatasetLinkList() {
        return fullDatasetLinkList;
    }

    /**
     * Sets the value of the fullDatasetLinkList property.
     * 
     * @param value
     *     allowed object is
     *     {@link FullDatasetLinkList }
     *     
     */
    public void setFullDatasetLinkList(FullDatasetLinkList value) {
        this.fullDatasetLinkList = value;
    }

    /**
     * Gets the value of the datasetFileList property.
     * 
     * @return
     *     possible object is
     *     {@link DatasetFileList }
     *     
     */
    public DatasetFileList getDatasetFileList() {
        return datasetFileList;
    }

    /**
     * Sets the value of the datasetFileList property.
     * 
     * @param value
     *     allowed object is
     *     {@link DatasetFileList }
     *     
     */
    public void setDatasetFileList(DatasetFileList value) {
        this.datasetFileList = value;
    }

    /**
     * Gets the value of the repositoryRecordList property.
     * 
     * @return
     *     possible object is
     *     {@link RepositoryRecordList }
     *     
     */
    public RepositoryRecordList getRepositoryRecordList() {
        return repositoryRecordList;
    }

    /**
     * Sets the value of the repositoryRecordList property.
     * 
     * @param value
     *     allowed object is
     *     {@link RepositoryRecordList }
     *     
     */
    public void setRepositoryRecordList(RepositoryRecordList value) {
        this.repositoryRecordList = value;
    }

    /**
     * Gets the value of the additionalInformation property.
     * 
     * @return
     *     possible object is
     *     {@link AdditionalInformation }
     *     
     */
    public AdditionalInformation getAdditionalInformation() {
        return additionalInformation;
    }

    /**
     * Sets the value of the additionalInformation property.
     * 
     * @param value
     *     allowed object is
     *     {@link AdditionalInformation }
     *     
     */
    public void setAdditionalInformation(AdditionalInformation value) {
        this.additionalInformation = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the formatVersion property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFormatVersion() {
        return formatVersion;
    }

    /**
     * Sets the value of the formatVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFormatVersion(String value) {
        this.formatVersion = value;
    }

}
