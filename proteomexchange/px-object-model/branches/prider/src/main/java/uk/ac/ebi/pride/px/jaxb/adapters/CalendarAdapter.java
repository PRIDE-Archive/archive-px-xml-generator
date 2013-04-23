package uk.ac.ebi.pride.px.jaxb.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Adapter for JAXB to automatically transform xs:date XML schema elements
 * into a Java Calendar objects.
 * @author Florian Reisinger
 *         Date: 10/10/11
 * @since 0.1
 */
public class CalendarAdapter extends XmlAdapter<String, Calendar> {

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public Calendar unmarshal(String value) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateFormat.parse(value));
        return calendar;
    }

    public String marshal(Calendar value) {
        if (value == null) {
            return null;
        }
        return dateFormat.format(value.getTime());
    }

}
