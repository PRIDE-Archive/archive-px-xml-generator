
package uk.ac.ebi.pride.archive.px.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * The list of species from which the data has been generated.
 *
 * <p>Java class for SpeciesListType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="SpeciesListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Species" type="{}SpeciesType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SpeciesListType", propOrder = {
    "species"
})
public class SpeciesList
    implements Serializable, PXObject
{

    private final static long serialVersionUID = 100L;
    @XmlElement(name = "Species", required = true)
    protected List<Species> species;

    /**
     * Gets the value of the species property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the species property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSpecies().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Species }
     *
     *
     */
    public List<Species> getSpecies() {
        if (species == null) {
            species = new ArrayList<Species>();
        }
        return this.species;
    }

}
