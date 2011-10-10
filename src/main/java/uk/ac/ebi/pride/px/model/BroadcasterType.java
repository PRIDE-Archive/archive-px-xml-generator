
package uk.ac.ebi.pride.px.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for BroadcasterType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="BroadcasterType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="PRIDE"/>
 *     &lt;enumeration value="PeptideAtlas"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "BroadcasterType")
@XmlEnum
public enum BroadcasterType {

    @XmlEnumValue("PRIDE")
    PRIDE("PRIDE"),
    @XmlEnumValue("PeptideAtlas")
    PEPTIDE_ATLAS("PeptideAtlas");
    private final String value;

    BroadcasterType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static BroadcasterType fromValue(String v) {
        for (BroadcasterType c: BroadcasterType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
