package uk.ac.ebi.pride.px;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.pride.px.Mail.PropertiesHelper;
import uk.ac.ebi.pride.px.Mail.ProteomExchangePropertyType;


import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: javizca
 * Date: Oct 28, 2008
 * Time: 11:36:40 AM
 * To change this template use File | Settings | File Templates.
 * ToDo: this should not be in this package!
 */
@Deprecated
public class Emailer {

    private static final Logger logger = LoggerFactory.getLogger(Emailer.class);
    private final static SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

    public Emailer(){

    }


    /**
     * The argument is the File name of the output file.
     * @param args
     */
    public static void main(String[] args) {

        Emailer mail = new Emailer();
        mail.sendEmail(args[0]);
    }


    /**
     * This is the method that will send the e-mail to the RSS feed.
     * @return: boolean to indicate whether the sending of email worked.
     */

    public boolean sendEmail(String fileName) {

        boolean emailSent = false;

        Collection<String> emailAddresses = new ArrayList<String>();
        emailAddresses.add("dani@ebi.ac.uk");
        //emailAddresses.add("lennart.martens@ebi.ac.uk");
        //emailAddresses.add("proteomexchange@googlegroups.com");

        // We have to provide the test to be read:
        String httpLink = null;
        String announcement = null;
        String rssContent = null;


        // Test the possibility of the HTTP link:
        announcement = "There is a new ProteomExchange experiment.\n"+
                       "The proteomExchange XML file can be downloaded from the following link: \n";

//        httpLink = "http://www.ebi.ac.uk/pride/proteomexchange/" + extractDateForDirectoryStructureLink() + fileName;
        httpLink = "http://www.ebi.ac.uk/pride/simpleSearch.do?simpleSearchValue=" + fileName;
        rssContent = announcement + httpLink;

        // Now, send the e-mail
        try {
            //Emailer.sendPlainTextMessage("Testing ProteomExchange pipeline", finalResult, emailAddresses, null, null, false);
            Emailer.sendPlainTextMessage("Testing ProteomExchange pipeline", rssContent, emailAddresses, null, null, false);
            emailSent = true;
        } catch (MessagingException e) {
            logger.error("Unable to send email!", e);
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return emailSent;
    }

    /**
     * Static method that attempts to send a plain text email to a collection of recipients, cc recipients and bcc recipients.
     *
     * @param subject            being the subject line of the message.
     * @param message            being the message being sent.
     * @param recipientAddresses Collection of Strings containing valid email addresses to whome the email will be sent
     * @param ccAddresses        (Can be null or empty) Collection of Strings containing valid email addresses to whome the email will be cc'd
     * @param bccAddresses       (Can be null or empty) Collection of Strings containing valid email addresses to whome the email will be bcc'd
     * @param copyInPrideSupport boolean to indicate if the Pride Support email account should be copied in.
     * @return String indicating any errors that occurred during this process.
     * @throws javax.mail.MessagingException in the event of a problem when sending the email.
     */
    public static String sendPlainTextMessage(String subject, String message, Collection<String> recipientAddresses, Collection ccAddresses, Collection bccAddresses, boolean copyInPrideSupport) throws MessagingException {

        String retval;
        StringBuffer messages = new StringBuffer();

        // Have minimum properties been set?
        if (
                StringUtils.isEmpty(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_PROTOCOL)) ||
                        StringUtils.isEmpty(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_MAILSERVER)) ||
                        StringUtils.isEmpty(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_NAME)) ||
                        StringUtils.isEmpty(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_SUPPORT_EMAIL_ADDRESS)) ||
                        StringUtils.isEmpty(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ADMINISTRATION_EMAIL_ADDRESS))) {
            return "The email settings are not complete and so no emails can be sent.";
        }

        // Check if the send email flag has been set.
        if (!("true".equalsIgnoreCase(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_SEND_AUTOMATIC_EMAILS)) ||
                "yes".equalsIgnoreCase(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_SEND_AUTOMATIC_EMAILS)))) {
            return "Email functionality is turned off so no emails will be sent.";
        }

        //Set the host smtp address
        Properties emailprops = new Properties();
        emailprops.put("mail.smtp.host", PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_MAILSERVER));
        emailprops.put("mail.smtp.user", PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_NAME));

        //Get the default Session
        Session session = Session.getInstance(emailprops, null);
        // create a message
        Message msg = new MimeMessage(session);

        // Set all the recipients (
        boolean noRecipients = true;

        if (copyInPrideSupport || (recipientAddresses != null && recipientAddresses.size() > 0)) {
            if (copyInPrideSupport) {
                if (recipientAddresses == null) {
                    recipientAddresses = new ArrayList<String>();
                } else {
                    // Just in case one of those pesky immutable Collection classes has been passed in
                    recipientAddresses = new ArrayList<String>(recipientAddresses);
                }
                recipientAddresses.add(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_SUPPORT_EMAIL_ADDRESS));
            }
            InternetAddress[] addressesTo = buildAddressArray(recipientAddresses, messages);
            if (addressesTo != null && addressesTo.length > 0) {
                msg.setRecipients(Message.RecipientType.TO, addressesTo);
                noRecipients = false;
            }
        }

        if (ccAddresses != null && ccAddresses.size() > 0) {
            InternetAddress[] addressesCc = buildAddressArray(ccAddresses, messages);
            if (addressesCc != null && addressesCc.length > 0) {
                msg.setRecipients(Message.RecipientType.CC, addressesCc);
                noRecipients = false;
            }
        }

        if (bccAddresses != null && bccAddresses.size() > 0) {
            InternetAddress[] addressesBcc = buildAddressArray(bccAddresses, messages);
            if (addressesBcc != null && addressesBcc.length > 0) {
                msg.setRecipients(Message.RecipientType.BCC, addressesBcc);
                noRecipients = false;
            }
        }

        if (noRecipients) {
            messages.append("There are no valid recipients for this email.");
            return messages.toString();
        }

        // Create the transport for the email.
        Transport tr = session.getTransport(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_PROTOCOL));
        tr.connect(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_MAILSERVER),
                PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_NAME),
                ("".equals(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_PASSWORD))) ? null
                        : PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ACCOUNT_PASSWORD)
        );

        // Specify the sender address:
        InternetAddress senderAddress = new InternetAddress("driostorres@gmail.com");


        // Setting the Subject and Content Type
        msg.setSubject(subject);
        msg.setFrom(senderAddress);
        msg.setContent(message, "text/plain");
        msg.saveChanges();
        tr.send(msg);
        tr.close();
        retval = messages.toString();
        return retval;
    }

    /**
     * Private static method that creates an array of InternetAddress objects from a Collection
     * of Strings that should contain valid email addresses.
     *
     * @param emailAddressStrings being a Collection of String objects that should all be valid email addresses (this
     *                            is checked in the process of this method and any errors are appended to the errorStringBuffer object).
     * @param errorStringBuffer   is a StringBuffer being created by the calling method that has any errors appended to it.
     * @return an array of InternetAddress objects.
     */
    private static InternetAddress[] buildAddressArray(Collection emailAddressStrings, StringBuffer errorStringBuffer) {
        List<InternetAddress> addressArrayList = new ArrayList<InternetAddress>();
        for (Object emailAddressString : emailAddressStrings) {
            String address = (String) emailAddressString;
            try {
                if (!address.matches("^(([A-Za-z0-9]+_+)|([A-Za-z0-9]+\\-+)|([A-Za-z0-9]+\\.+)|([A-Za-z0-9]+\\++))*[A-Za-z0-9]+@((\\w+\\-+)|(\\w+\\.))*\\w{1,63}\\.[a-zA-Z]{2,6}$")) {
                    throw new AddressException("This does not look like a valid email address", address);
                }
                InternetAddress addressTo = new InternetAddress(address);
                addressArrayList.add(addressTo);
            }
            catch (AddressException ae) {
                if (errorStringBuffer.length() == 0) {
                    errorStringBuffer.append("The following email addresses are poorly formed and have not been contacted: ");
                }
                errorStringBuffer.append(address)
                        .append(" (")
                        .append(ae.getMessage())
                        .append("), ");
            }
        }
        // Now build the array and return it.
        InternetAddress[] returnArray = new InternetAddress[addressArrayList.size()];
        for (int i = 0; i < addressArrayList.size(); i++) {
            returnArray[i] = addressArrayList.get(i);
        }
        return returnArray;
    }



//    public static String sendAdminPlainTextMessage(String subject, String message) throws MessagingException {
//        Collection<String> recipients = Collections.singletonList(PropertiesHelper.getProperty(ProteomExchangePropertyType.EMAIL_ADMINISTRATION_EMAIL_ADDRESS));
//        return sendPlainTextMessage(subject, message, recipients, null, null, true);
//    }

//    public static String sendNewCurationCollaborationEmail(Collaboration curation)
//            throws EncryptionException, KeyStoreException, MessagingException {
//
//        StringBuffer mailMessage = new StringBuffer("Dear Administrator ,\n\n");
//
//        mailMessage.append("A user account has been created for curator access with the following information:\n\n");
//
//        mailMessage.append("username: ").append(curation.getOwner().getUsername()).append('\n');
//        mailMessage.append("password: ").append(curation.getOwner().getPassword()).append('\n');
//
//        mailMessage.append("\nThis account will can now administer the default PRIDE curation collaboration.  \n\nYours sincerely,\n\n\nThe PRIDE Team.\n\nPlease note that this is an automatic email.  Please do not attempt to reply to this email address.");
//
//        return Emailer.sendAdminPlainTextMessage(
//                "Curator Account created for PRIDE",
//                mailMessage.toString());
//
//    }


}
