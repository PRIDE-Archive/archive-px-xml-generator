package uk.ac.ebi.pride.px.jaxb.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.Calendar;

/**
 * Adapter for JAXB to automatically transform xs:date XML schema elements
 * into a Java Calendar objects.
 * @author Florian Reisinger
 *         Date: 10/10/11
 * @since 0.1
 */
public class CalendarAdapter extends XmlAdapter<String, Calendar> {

    public Calendar unmarshal(String value) {
        return (javax.xml.bind.DatatypeConverter.parseDate(value));
    }

    public String marshal(Calendar value) {
        if (value == null) {
            return null;
        }
        return (javax.xml.bind.DatatypeConverter.printDate(value));
    }

}
