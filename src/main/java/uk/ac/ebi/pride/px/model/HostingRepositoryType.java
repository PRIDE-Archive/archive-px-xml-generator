
package uk.ac.ebi.pride.px.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HostingRepositoryType.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="HostingRepositoryType">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="PRIDE"/>
 *     &lt;enumeration value="PeptideAtlas"/>
 *     &lt;enumeration value="PASSEL"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "HostingRepositoryType")
@XmlEnum
public enum HostingRepositoryType {

    PRIDE("PRIDE"),
    @XmlEnumValue("PeptideAtlas")
    PEPTIDE_ATLAS("PeptideAtlas"),
    PASSEL("PASSEL");
    private final String value;

    HostingRepositoryType(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static HostingRepositoryType fromValue(String v) {
        for (HostingRepositoryType c: HostingRepositoryType.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
