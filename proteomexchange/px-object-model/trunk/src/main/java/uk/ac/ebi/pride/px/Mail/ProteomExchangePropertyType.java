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
	EMAIL_SUPPORT_EMAIL_ADDRESS			("email.support.email.address"),
    //I think those are not needed....
//	CURATION_ACCOUNT_EMAIL_ADDRESS		("curation.account.email.address"),
//	CURATION_ACCOUNT_USERNAME			("curation.account.username"),
//	CURATION_ACCOUNT_PASSWORD			("curation.account.password"),
//	BIOMART_USE_BIOMART					("biomart.use.biomart"),
//	BIOMART_URL							("biomart.url"),
//	OLS_USE_SERVICE						("ols.use.service"),
//	OLS_SERVICE_NAME					("ols.service.name"),
//	OLS_SERVICE_URL						("ols.service.url"),
//	WEBAPP_ALL_WEBSERVERS				("webapp.all.webservers.whitespace.separated"),
//	WEBAPP_TIMOUT						("webapp.timeout"),
//	PRIDE_WEB_VERSION					("pride.web.version"),
//	PRIDE_CORE_VERSION					("pride.core.version"),
//	DATE_COMPILED						("pride.date.compiled"),
//	TEMP_FOLDER							("pride.temp.folder.path"),
//	DBALIAS 							("pride.dbalias"),
//
//    HTTP_PROXY_SET                      ("http.proxySet"),
//    HTTP_PROXY_HOST                     ("http.proxyHost"),
//    HTTP_PROXY_PORT                     ("http.proxyPort"),
//    HTTP_PROXY_USER                     ("http.proxyUser"),
//    HTTP_PROXY_PASSWORD                 ("http.proxyPassword"),
//    HTTP_NON_PROXY_HOSTS                ("http.nonProxyHosts"),
//    RSS_NEWSFEED_URL                    ("rss.newsfeed.url"),
    PROTOCOL                            ("protocol"),
    SUBPROTOCOL                         ("subprotocol"),
    DRIVER                              ("driver"),
    ALIAS                               ("alias"),
    USER                                ("user"),
    PASSWORD                            ("password")
    ;


	private final String key;

	private ProteomExchangePropertyType (String key){
		this.key = key;
	}

	public String toString(){
		return this.key;
	}

}
