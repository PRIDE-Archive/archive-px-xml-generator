package uk.ac.ebi.pride.px.Mail;

/**
 * Created by IntelliJ IDEA.
 * User: javizca
 * Date: Oct 28, 2008
 * Time: 2:26:46 PM
 * To change this template use File | Settings | File Templates.
 */
public enum ProteomExchangePropertyType {

    EMAIL_SEND_AUTOMATIC_EMAILS			("email.send.automatic.emails"),
	EMAIL_ACCOUNT_PROTOCOL				("email.account.protocol"),
	EMAIL_MAILSERVER					("email.mailserver"),
	EMAIL_ACCOUNT_NAME					("email.account.name"),
	EMAIL_ACCOUNT_PASSWORD				("email.account.password"),
	EMAIL_ADMINISTRATION_EMAIL_ADDRESS	("email.administration.email.address"),
	EMAIL_SUPPORT_EMAIL_ADDRESS			("email.support.email.address")
    ;


	private final String key;

	private ProteomExchangePropertyType (String key){
		this.key = key;
	}

	public String toString(){
		return this.key;
	}

}
