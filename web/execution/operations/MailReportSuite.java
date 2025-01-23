package execution.operations;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import framework.Cyfer;
import framework.email.EmailSection;
import framework.email.constant.FrameworkData;
import framework.input.Configuration;

public class MailReportSuite {

	/**
	 * To send mail in case of AXES is down and report is not uploaded * To send
	 * mail first upload report on portal and then update the config file parameter
	 * - CRUDParallelSendEmail , CRUDreportPortalURL, reportPortalFolder, toemail
	 */
	public static void main(String[] args) {

		// assign value in these parameters for add report details in email
		String reportName = "";
		String reportPath = "";
		String executionTime = "";
		String machineName = "";
		String userName = "";

		sendEmail(reportName, reportPath, executionTime, machineName, userName);

	}

	/**
	 * To send an email using email configuration given in config.properties
	 * 
	 * @param reportName    name of the report
	 * @param reportPath    path of the report in local
	 * @param executionTime
	 * @param machineName
	 * @param userName
	 */
	private static void sendEmail(String reportName, String reportPath, String executionTime, String machineName,
			String userName) {

		// get the configuration property for sending the email
		String host_name = Configuration.getProperty("host");
		String user_name = Configuration.getProperty("emailusername");
		String password = Cyfer.decrypt(Configuration.getProperty("emailpassword"), Configuration.getProperty("key"));
		String from_email = Configuration.getProperty("fromemail");
		String[] to_email = Configuration.getProperty("toemail").split(",");

		// set ssl protocol version to be used
		// for specific machine issue on 465
		System.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

		// create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(host_name);
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(user_name, password));
		email.setSSLOnConnect(true);

		try {

			// set the email to and from
			email.setFrom(from_email);
			email.addTo(to_email);

			Document html = Jsoup.parse(new File(reportPath), "UTF-8");

			// get the li list of test
			Elements testsList = html.body().getElementById("test-collection").getElementsByTag("li");

			// get total tests executed
			String totalTests = String.valueOf(testsList.size());

			// get failed test count
			Map<String, String> tests = new HashMap<>();

			// loop through to generate failedTests
			// override the test status based on last rerun
			testsList.stream()
					.forEach(test -> tests.put(
							test.getElementsByAttributeValue("class", "test-name").get(0).text().split("- ReRun")[0]
									.trim(),
							test.getElementsByAttributeValueContaining("class", "outline").get(0).text()));

			String failedTests = String.valueOf(
					Collections.frequency(tests.values(), "fail") + Collections.frequency(tests.values(), "error"));

			// check for fail for subject
			String checkFail = failedTests.equals("0") ? "PASSED" : "FAILED";

			// get project name for subject
			String[] pathSplits = System.getProperty("user.dir").split("\\\\");
			String projectName = pathSplits[pathSplits.length - 1];

			// get report portal url and report portal folder to show in body
			String CRUDreportPortalURL = Configuration.getProperty("CRUDreportPortalURL").split("/uploads")[0]
					+ "/Central?selected=" + reportName.split(".html")[0];
			String reportPortalFolder = Configuration.getProperty("reportPortalFolder")
					+ Configuration.getProperty("clientCode");

			// get the suite name
			String suiteName = reportName.split("_")[0];

			// set email subject
			email.setSubject("(" + checkFail + ") " + projectName + " - " + reportPortalFolder + " - " + suiteName);

			EmailSection emailsection = new EmailSection();

			// set the email message
			EnumMap<FrameworkData, String> data = new EnumMap<>(FrameworkData.class);
			data.put(FrameworkData.MACHINE, machineName);
			data.put(FrameworkData.USER, userName);
			// data.put(FrameworkData.HIERARCHY, reportPortalFolder);
			data.put(FrameworkData.REPORT_NAME, reportName);
			data.put(FrameworkData.TOTAL_TEST, totalTests);
			data.put(FrameworkData.FAIL_TEST, failedTests);
			data.put(FrameworkData.TIME, executionTime);

			String emailTemplate = emailsection.frameworkDefault(data) + emailsection.button(CRUDreportPortalURL);

			// set the email body
			email.setHtmlMsg(emailTemplate);

			// send the email
			email.send();

		} catch (EmailException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
