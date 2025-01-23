package framework.email;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumMap;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import base.BaseSuite;
import framework.Cyfer;
import framework.LogUtil;
import framework.email.constant.FrameworkData;
import framework.input.Configuration;

public class Email {

	private Logger log;

	public Email() {
		// for framework utilize the BaseSuite initialize logger else create new for
		// using it separately
		log = BaseSuite.log == null ? new LogUtil(Strings.EMPTY, Strings.EMPTY).logger : BaseSuite.log;
	};

	/**
	 * To send an email using email configuration given in config.properties
	 * 
	 * @param reportName
	 * @param totalTests
	 * @param failedTests
	 * @param executionTime
	 */
	public void send(String reportName, String totalTests, String failedTests, String executionTime) {
		// get the configuration property for sending the email
		String host_name = Configuration.getProperty("host");
		String user_name = Configuration.getProperty("emailusername");

		// check if 'key' available to encrypt the password
		String encryptionKey = Configuration.getProperty("key");
		String password;
		if (encryptionKey != null && !encryptionKey.trim().isEmpty())
			password = Cyfer.decrypt(Configuration.getProperty("emailpassword"), encryptionKey);
		else
			password = Configuration.getProperty("emailpassword");

		String from_email = Configuration.getProperty("fromemail");
		String to_emails = Configuration.getProperty("toemail").trim();

		// get the project name
		String pathSplitter = File.separator.equals("\\") ? File.separator + File.separator : File.separator;
		String pathSplits[] = System.getProperty("user.dir").split(pathSplitter);
		String project_name = pathSplits[pathSplits.length - 1];

		// email details
		// for setting ssl protol
		System.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

		// Create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(host_name);
		// email.setSmtpPort(465); will take 25 port as setting ssl true
		email.setAuthenticator(new DefaultAuthenticator(user_name, password));
		email.setSSLOnConnect(true);

		try {
			// set the email to and from
			email.setFrom(from_email);
			// add to_emails ',' separated
			email.addTo(to_emails.split(","));

			// get the pc name for subject
			String pcName = "";
			try {
				pcName = InetAddress.getLocalHost().toString();
			} catch (UnknownHostException e) {
				log.error("Exception while getting system name for email subject", e);
			}

			// get the logged in user
			String pcUser = System.getProperty("user.name");

			// check for fail for subject
			String checkFail = failedTests.equals("0") ? "PASSED" : "FAILED";

			// set the email subject
			// get the suite name
			String suiteName = reportName.split("_")[0];
			email.setSubject("(" + checkFail + ") " + project_name + " - " + suiteName);

			// set the email message
			EnumMap<FrameworkData, String> data = new EnumMap<>(FrameworkData.class);
			data.put(FrameworkData.USER, pcUser);
			data.put(FrameworkData.REPORT_NAME, reportName);
			data.put(FrameworkData.MACHINE, pcName);
			data.put(FrameworkData.TIME, executionTime);
			data.put(FrameworkData.TOTAL_TEST, totalTests);
			data.put(FrameworkData.FAIL_TEST, failedTests);

			// generate email HTML
			EmailSection emailSection = new EmailSection();
			String executionDetailSection = emailSection.frameworkDefault(data);

			String emailBodyHTML = executionDetailSection;

			email.setHtmlMsg(emailBodyHTML);

			// send the email
			email.send();

			log.debug("Email sent successfully");

		} catch (EmailException e) {
			log.trace("Exception while sending an email", e);
		}
	}

	/**
	 * To send an email using email configuration given in config.properties
	 * 
	 * @param serverURL
	 * @param serverFolderPath
	 * @param reportName
	 * @param totalTests
	 * @param failedTests
	 */
	public void send(String subject, String body) {
		// get the configuration property for sending the email
		String host_name = Configuration.getProperty("host");
		String user_name = Configuration.getProperty("emailusername");

		// check if 'key' available to encrypt the password
		String encryptionKey = Configuration.getProperty("key");
		String password;
		if (encryptionKey != null && !encryptionKey.trim().isEmpty())
			password = Cyfer.decrypt(Configuration.getProperty("emailpassword"), encryptionKey);
		else
			password = Configuration.getProperty("emailpassword");

		String from_email = Configuration.getProperty("fromemail");
		String to_emails = Configuration.getProperty("toemail").trim();

		// Create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(host_name);
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(user_name, password));
		email.setSSLOnConnect(true);

		try {
			// set the email to and from
			email.setFrom(from_email);
			email.addTo(to_emails.split(","));

			// set the email subject
			email.setSubject(subject);

			// set the email body
			email.setHtmlMsg(body);

			// send the email
			email.send();

			log.debug("Email sent successfully");

		} catch (EmailException e) {
			log.trace("Exception while sending an email", e);
		}
	}
}
