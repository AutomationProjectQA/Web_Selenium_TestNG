package execution.operations;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
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

public class EmailOperations extends CommonOperations {

	// GSheetOperation gSheetOperation = new GSheetOperation();

	/**
	 * To send the fail container email
	 * 
	 * @param containerName
	 */
	public void sendFailContainerEmail(String containerName) {
		EmailSection emailSection = new EmailSection();

		sendDeveloperEmail("URGENT: Failed to start " + containerName + " container",
				emailSection.afterExecutionWithoutData("Container Name: " + containerName));
	}

	/**
	 * To send email when timeout has reached in execution
	 */
	public void sendExecutionTimeoutEmail() {
		EmailSection emailSection = new EmailSection();
		sendDeveloperEmail("URGENT:  Automation Execution timeout: ",
				emailSection.afterExecutionWithoutData("Execution timeout completed"));
	}

	/**
	 * To send the fail login email
	 * 
	 * @param executionTime - time format HH:mm:ss:sss
	 */
	public void sendFailLoginEmail(String executionTime) {
		EmailSection emailSection = new EmailSection();
		String[] time = executionTime.split(":");
		String companyName = Configuration.getProperty("company");

		// check execution time and send email
		if (Integer.parseInt(time[0]) == 00 && Integer.parseInt(time[1]) <= 20)
			sendDeveloperEmail("URGENT: Login failed ", "");
	}

	/**
	 * To send an email for failed container using email configuration given in
	 * config.properties
	 * 
	 * @param emailSubject
	 * @param emailBody
	 */
	private void sendDeveloperEmail(String emailSubject, String emailBody) {

		Operations.log.debug("Started sending developer email for failed cases");

		// get the configuration property for sending the email
		String hostName = Configuration.getProperty("host");
		String userName = Configuration.getProperty("emailusername");

		// check if 'key' available to encrypt the password
		String encryptionKey = Configuration.getProperty("key");
		String password;
		if (encryptionKey != null && !encryptionKey.trim().isEmpty())
			password = Cyfer.decrypt(Configuration.getProperty("emailpassword"), encryptionKey);
		else
			password = Configuration.getProperty("emailpassword");

		String fromEmail = Configuration.getProperty("fromemail");
		String toEmails = readJsonData(ProcessStrings.devEmailsForFailedCases, ProcessStrings.sanityConfigJsonFile)
				.trim();

		// Create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(hostName);
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(userName, password));
		email.setSSLOnConnect(true);

		try {
			// set the email to and from
			email.setFrom(fromEmail);
			email.addTo(toEmails.split(","));

			// set the email subject
			email.setSubject(emailSubject);

			// set the email body
			email.setHtmlMsg(emailBody);

			// send the email
			email.send();

			Operations.log.debug("Developer email sent successfully");

		} catch (EmailException e) {
			Operations.log.error("Exception occured while sending a developer email", e);
		}

		Operations.log.debug("Ended sending developer email for failed cases");
	}

	/**
	 * To send a summary email
	 * 
	 * @param emailSubject
	 * @param emailBody
	 */
	private void sendSummaryEmail(String emailSubject, String emailBody) {

		Operations.log.debug("Started sending email");

		// get the configuration property for sending the email
		String hostName = Configuration.getProperty("host");
		String userName = Configuration.getProperty("emailusername");

		// check if 'key' available to encrypt the password
		String encryptionKey = Configuration.getProperty("key");
		String password;
		if (encryptionKey != null && !encryptionKey.trim().isEmpty())
			password = Cyfer.decrypt(Configuration.getProperty("emailpassword"), encryptionKey);
		else
			password = Configuration.getProperty("emailpassword");

		String fromEmail = Configuration.getProperty("fromemail");
		String toEmails = Configuration.getProperty("adminEmail");

		System.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

		// Create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(hostName);
		// email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(userName, password));
		email.setSSLOnConnect(true);

		try {
			// set the email to and from
			email.setFrom(fromEmail);
			email.addTo(toEmails.split(","));

			// set the email subject
			email.setSubject(emailSubject);

			// set the email body
			email.setHtmlMsg(emailBody);

			// send the email
			email.send();

			Operations.log.debug("Email sent successfully");

		} catch (EmailException e) {
			Operations.log.error("Exception occured while sending a email", e);
		}

		Operations.log.debug("Ended sending email");
	}

	/**
	 * Sends email after verification from config parameters
	 * 
	 * @param executionTime
	 */
	public void sendEmail(String executionTime, String multipleReportsFolder) {

		boolean sendEmail = Boolean.parseBoolean(Configuration.getProperty("CRUDParallelSendEmail"));

		if (sendEmail) {
			Operations.log.debug("Started sending email");

			Operations.log.debug("Started getting failed reports");

			String htmlFilePath = "";

			String singleReport = "";

			File multiplerReportsDirectory = new File(multipleReportsFolder);
			if (multiplerReportsDirectory.exists()) {
				List<File> reports = (List<File>) FileUtils.listFiles(multiplerReportsDirectory,
						new String[] { "html" }, true);

				singleReport = reports.get(0).getName();
				htmlFilePath = reports.get(0).getAbsolutePath();

			}

			Operations.log.debug("Ended getting failed reports");

			sendEmail(singleReport, htmlFilePath, executionTime);

			Operations.log.debug("Ended sending email");
		}
	}

	/**
	 * To send an email using email configuration given in config.properties
	 * 
	 * @param reportName    name of the report
	 * @param reportPath    path of the report in local
	 * @param executionTime
	 */
	private void sendEmail(String reportName, String reportPath, String executionTime) {

		Operations.log.debug("Started to send email");

		// get the configuration property for sending the email
		String hostName = Configuration.getProperty("host");
		String userName = Configuration.getProperty("emailusername");
		String password = Cyfer.decrypt(Configuration.getProperty("emailpassword"), Configuration.getProperty("key"));
		String fromEmail = Configuration.getProperty("fromemail");
		String[] toEmail = Configuration.getProperty("toemail").split(",");

		Operations.log.debug("Started creating email message");

		// set ssl protocol version to be used
		// for specific machine issue on 465
		System.setProperty("mail.smtp.ssl.protocols", "TLSv1.2");

		// create the email message
		HtmlEmail email = new HtmlEmail();
		email.setHostName(hostName);
		email.setSmtpPort(465);
		email.setAuthenticator(new DefaultAuthenticator(userName, password));
		email.setSSLOnConnect(true);

		try {

			// set the email to and from
			email.setFrom(fromEmail);
			email.addTo(toEmail);

			// get the pc name for subject
			String pcName = "";
			try {
				pcName = InetAddress.getLocalHost().getCanonicalHostName();
			} catch (UnknownHostException e) {
				Operations.log.error("Exception while getting system name for email subject", e);
			}

			// get the logged in user
			String pcUser = System.getProperty("user.name");

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
			data.put(FrameworkData.MACHINE, pcName);
			data.put(FrameworkData.USER, pcUser);
			// data.put(FrameworkData.HIERARCHY, reportPortalFolder);
			data.put(FrameworkData.REPORT_NAME, reportName);
			data.put(FrameworkData.TOTAL_TEST, totalTests);
			data.put(FrameworkData.FAIL_TEST, failedTests);
			data.put(FrameworkData.TIME, executionTime);

			String emailTemplate = emailsection.frameworkDefault(data) + emailsection.button(CRUDreportPortalURL);

			Operations.log.debug("Ended creating email message");

			// set the email body
			email.setHtmlMsg(emailTemplate);

			// send the email
			email.send();

		} catch (EmailException e) {
			Operations.log.error("Error occured while sending the email", e);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Operations.log.debug("Ended to send email");
	}

	/**
	 * To send the start execution email
	 */
	public void sendBeforeExecutionEmail() {
		EmailSection emailSection = new EmailSection();

		String emailBody = "Automation Execution Started on - "
				+ new SimpleDateFormat("MMMM dd, yyyy hh:mm a").format(Calendar.getInstance().getTime());

		sendSummaryEmail(" Execution Started ", emailSection.beforeExecutionWithoutData(emailBody));
	}

	/**
	 * To get modules details by parsing html reports
	 * 
	 * @param reportsName  - List of reports name
	 * @param clientFolder - Name of client folder
	 * @return Map of modules details
	 */
	public Map<String, Map<String, String>> getModulesDetails(List<String> reportsName, String clientFolder) {

		Map<String, Map<String, String>> allModulesDetails = new HashMap<>();

		for (String reportName : reportsName) {
			HashMap<String, String> moduleDetails = new HashMap<>();
			String reportPath = clientFolder + "/" + reportName + "/" + reportName + ".html";
			String xmlName = reportName.split("_")[0];

			File report = new File(reportPath);

			if (report.exists()) {
				try {
					// parse html report
					Document html = Jsoup.parse(report, "UTF-8");

					// get execution time
					String executionTime = String.valueOf(html.body().getElementById("dashboard-view")
							.getElementsMatchingOwnText("Total Time Taken").get(0).lastElementSibling().text());
					moduleDetails.put(ProcessStrings.executionTime, executionTime);

					// get total test
					String testCount = String
							.valueOf(html.body().getElementById("test-collection").getElementsByTag("li").size());
					moduleDetails.put(ProcessStrings.testCount, testCount);

					// get pass count
					String passCount = String.valueOf(html.body().select("li.collection-item.test.pass").size());
					moduleDetails.put(ProcessStrings.pass, passCount);

					// get warning count
					String warningCount = String.valueOf(html.body().select("li.collection-item.test.warning").size());
					moduleDetails.put(ProcessStrings.warning, warningCount);

					// get error count
					String errorCount = String.valueOf(html.body().select("li.collection-item.test.error").size());
					moduleDetails.put(ProcessStrings.error, errorCount);

					// get fail count
					Elements testsList = html.body().select("li.collection-item.test.fail");
					String failCount = String.valueOf(testsList.size());
					moduleDetails.put(ProcessStrings.fail, failCount);

					// get fail test name
					ArrayList<String> failedTest = new ArrayList<>();
					testsList.stream().forEach(test -> failedTest
							.add(test.getElementsByAttributeValue("class", "test-name").get(0).text().trim()));
					moduleDetails.put(ProcessStrings.failedTest,
							failedTest.toString().replace("[", "").replace("]", ""));

					// get covered modules
					String modulesCovered = readJsonData(xmlName + "/Modules Covered",
							ProcessStrings.sanityModulesInfoFile);
					moduleDetails.put(ProcessStrings.modulesCovered, modulesCovered);

					// get assigned QA
					String assignedQA = readJsonData(xmlName + "/Assigned QA", ProcessStrings.sanityModulesInfoFile);
					moduleDetails.put(ProcessStrings.assignedQA, assignedQA);

					// get SDET
					String sdet = readJsonData(xmlName + "/SDET", ProcessStrings.sanityModulesInfoFile);
					moduleDetails.put(ProcessStrings.sdet, sdet);

					// put module details
					allModulesDetails.put(xmlName, moduleDetails);

				} catch (IOException e) {
					Operations.log.error("Failed to read the HTML report file at - " + reportName);
				}
			} else {
				Operations.log.error("Report is not present for - " + xmlName);
			}
		}

		return allModulesDetails;
	}

	/**
	 * To create template and send summary email
	 * 
	 * @param modulesDetails
	 * @param reportsName    - List of reports name
	 * @param attachSheet    - true if to add a summary sheet link to the email,
	 *                       otherwise false
	 */
	public void createAndSendSummaryEmail(Map<String, Map<String, String>> modulesDetails, List<String> reportsName,
			boolean attachSheet) {

		EmailSection emailSection = new EmailSection();
		ArrayList<LinkedHashMap<String, String>> tableData;
		List<String> tempRowList = new ArrayList<>();
		String sheetLink = "";

		int totalPass = 0;
		int totalFail = 0;
		int totalWarning = 0;
		int totalError = 0;

		for (String reportName : reportsName) {
			// get xml name
			String xmlName = reportName.split("_")[0];

			// get data based on xml name
			Map<String, String> moduleInfo = modulesDetails.get(xmlName);

			// get total Pass, Fail, Warning and Error
			totalPass += Integer.parseInt(moduleInfo.get(ProcessStrings.pass));
			totalFail += Integer.parseInt(moduleInfo.get(ProcessStrings.fail));
			totalWarning += Integer.parseInt(moduleInfo.get(ProcessStrings.warning));
			totalError += Integer.parseInt(moduleInfo.get(ProcessStrings.error));

			// get table row data
			Collections.addAll(tempRowList,
					moduleInfo.get("Assigned QA") + "<br>" + "<a href=\"https://axes.meditab.in/Central?selected="
							+ reportName + "\">" + xmlName + "</a>",
					moduleInfo.get("Modules Covered").replace("\n", "<br>"),
					"<span style='padding-right: 25px'><font style=\"color:#32cd32; font-size:1rem; font-weight:900;\"> &#10003; </font>:"
							+ moduleInfo.get(ProcessStrings.pass) + "</span>"
							+ "<span style='padding-right: 25px;'>&#10060;:" + moduleInfo.get(ProcessStrings.fail)
							+ "</span>"
							+ "<span style='padding-right: 25px;'> <span style='color:orange'>&#x26A0;</span>:"
							+ moduleInfo.get(ProcessStrings.warning) + "</span>"
							+ "<span style='padding-right: 25px;'> <span style='color:tomato; font-size:larger;'><b>!</b></span>:"
							+ moduleInfo.get(ProcessStrings.error) + "</span>" + "<span><br><br> &#x23F1;:</span>"
							+ moduleInfo.get(ProcessStrings.executionTime),
					moduleInfo.get(ProcessStrings.failedTest).replace(",", "<br>"));
		}

		// convert data into ArrayList<LinkedHashMap<String, String>>
		String[] rowData = Arrays.copyOf(tempRowList.toArray(), tempRowList.size(), String[].class);
		tableData = emailSection.makeList(ProcessStrings.headerColumns, rowData);

		if (attachSheet) {
			/*
			 * // get client spreadsheet and sheet link String clientSpreadsheetLink =
			 * "https://docs.google.com/spreadsheets/d/" + UpdateGSheet.clientSpreadsheetId;
			 * String clientSheetLink = "https://docs.google.com/spreadsheets/d/" +
			 * UpdateGSheet.clientSpreadsheetId + "/edit#gid=" + UpdateGSheet.clientSheetId;
			 * 
			 * sheetLink = "<a href=\"" + clientSheetLink +
			 * "\" style='text-decoration: none;'>Status Update Sheet - " +
			 * UpdateGSheet.clientSheetName + "</a>" + "<br>" + "<br>" + " <a href=\"" +
			 * clientSpreadsheetLink + "\" style='text-decoration: none;'>Summary Sheet</a>"
			 * + "<br>" + "<br>";
			 */ }

		String otherData = (attachSheet ? sheetLink : "")
				+ "<span><span style=\"font-size:larger;font-weight:bolder\">&#9432;</span> : Pass - <font style=\"color:#32cd32; font-size:1rem; font-weight:900;\"> &#10003; </font> &nbsp; | &nbsp; Fail - &#10060; &nbsp; | &nbsp; Warning - <span style=\"color:orange\">&#x26A0;</span> &nbsp; | &nbsp; Error - <span style=\"color:tomato;font-size:larger\"><b>!</b></span></span>"
				+ "<br>" + "<br>"
				+ "<span>Total counts: <font style=\"color:#32cd32; font-size:1rem; font-weight:900;\"> &#10003; </font> - "
				+ totalPass + " &nbsp; | &nbsp; &#10060; - " + totalFail
				+ " &nbsp; | &nbsp; <span style=\"color:orange\">&#x26A0;</span> - " + totalWarning
				+ " &nbsp; | &nbsp; <span style=\"color:tomato;font-size:larger\"><b>!</b></span> - " + totalError
				+ "</span>" + "<br>" + "<br>"
				+ "<b> Please analyse the reports for further verification. Links for the reports are provided in the summary below </b>";

		// send summary email
		sendSummaryEmail(" Automation Execution - ",
				emailSection.plainText(otherData) + emailSection.table(tableData, true));

	}

	/**
	 * To get report list from given folder path
	 * 
	 * @param reportFolderPath
	 * @return List of reports name
	 */
	public List<String> getReportsName(String reportFolderPath) {
		List<String> reportsName = new ArrayList<>();

		try {
			// Creating a File object for directory
			File directoryPath = new File(reportFolderPath);
			// List of all files and directories
			reportsName = Arrays.asList(directoryPath.list());
			Operations.log.debug("List of files and directories in the specified directory: " + reportsName);
		} catch (Exception e) {
			Operations.log.error("Error occurred while getting reports name" + e);
		}

		return reportsName;
	}

}
