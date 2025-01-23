package execution.operations;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ReportOperations extends CommonOperations {

	/**
	 * Creates single report from multiple reports and deletes all the report
	 * folders
	 * 
	 * @param multipleReportsFolder
	 */
	public void createSingleReport(String multipleReportsFolder, String executionTime) {

		Operations.log.debug("Started creating single report for " + multipleReportsFolder);

		File multipleReportsDirectory = new File(multipleReportsFolder);

		// creating screenshots folder
		String singleScreenshotFolderPath = multipleReportsFolder + "/screenshots";
		File singleScreenshotFolder = new File(singleScreenshotFolderPath);
		if (singleScreenshotFolder.mkdir()) {

			Operations.log.debug("Single screenshot folder created");

			Operations.log.debug("Started getting screenshots");

			// getting all screenshots
			String[] screenshotExtension = new String[] { "jpg" };
			List<File> screenshots = (List<File>) FileUtils.listFiles(multipleReportsDirectory, screenshotExtension,
					true);

			Operations.log.debug("Ended getting screenshots");
			Operations.log.debug("Started moving screenshots");

			// moving the screenshots
			screenshots.stream().forEach(screenshot -> {
				Path currentPath = screenshot.toPath();
				String currentScreenshotName = screenshot.getName();
				Path newPath = Paths.get(singleScreenshotFolder.toPath().toString() + "/" + currentScreenshotName);
				try {
					Files.move(currentPath, newPath);
					Operations.log.debug("Moved screenshot - " + currentScreenshotName);
				} catch (IOException e) {
					Operations.log.error("Failed to move screenshot - " + currentScreenshotName, e);
				}
			});

			Operations.log.debug("Ended moving screenshots");

		}

		Operations.log.debug("Started getting reports");

		// getting all reports
		String[] reportExtension = new String[] { "html" };
		List<File> reports = (List<File>) FileUtils.listFiles(multipleReportsDirectory, reportExtension, true);

		Operations.log.debug("Ended getting reports");

		// creating single combined report file
		if (reports != null && !reports.isEmpty()) {
			Path firstReportPath = reports.get(0).toPath();
			Path newReportPath = Paths.get(multipleReportsDirectory.toPath() + "/" + multipleReportsDirectory.getName()
					+ "." + reportExtension[0]);

			try {
				// remove Logout test from first report and move to parent folder
				newReportPath = removeLogoutFirstReport(reports, firstReportPath, newReportPath);
				// remove Login/Logout testCases except first login and appending rest
				removeLoginLogoutTests(reports, newReportPath);

				Operations.log.debug("Started deleting report directories");

				// deleting the report directories
				File[] reportDirectories = multipleReportsDirectory.listFiles(File::isDirectory);
				for (File reportFolder : reportDirectories) {
					String reportFolderName = reportFolder.getName();
					if (!reportFolderName.equals("screenshots")) {
						FileUtils.deleteDirectory(reportFolder);
						Operations.log.debug("Deleted report directory - " + reportFolderName);
					}
				}

				Operations.log.debug("Ended deleting report directories");
				// re arranging test case as per CRUD sheet column order
				rearrangeTestCasesCRUD(newReportPath, executionTime);

			} catch (IOException e) {
				Operations.log.error("Failed to create single report", e);
			} catch (Exception e) {
				Operations.log.error("Failed to create single report", e);
			}

		} else {
			Operations.log.error("No reports present");
		}

		Operations.log.debug("Ended creating single report for " + multipleReportsFolder + "");

	}

	/**
	 * Remove Login/Logout testCases except first login and appending rest
	 * 
	 * @param reports    list of report Files
	 * @param reportPath path of reports
	 * @throws IOException
	 */
	private void removeLoginLogoutTests(List<File> reports, Path reportPath) throws IOException {
		Operations.log.debug("Started parsing report and removing unnecessary login/logout tests");
		// parsing the new report
		Document newReportDoc = Jsoup.parse(reportPath.toFile(), "UTF-8");
		Element tests = newReportDoc.body().selectFirst("ul#test-collection");

		// adding the tests of old reports to new report
		if (reports.size() > 1) {
			for (int i = 1; i < reports.size(); i++) {
				Elements testCases = getElementsReport(reports, i);
				// remove Login/Logout testCases except first login and appending rest
				for (int j = 0; j < testCases.size(); j++) {
					if (!(i >= 1 && testCases.get(j).text().contains("QuickCapBaseSuite - Login"))
							&& !(i != (reports.size() - 1)
									&& testCases.get(j).text().contains("QuickCapBaseSuite - Logout"))) {

						testCases.get(j).appendTo(tests);
					}
				}
			}
			// write html to report file on path
			FileUtils.writeStringToFile(reportPath.toFile(), newReportDoc.outerHtml(), "UTF-8");

			Operations.log.debug("Ended parsing report and removing unnecessary login/logout tests ");
		}
	}

	/**
	 * Remove Logout test from first report and move to parent report path
	 * 
	 * @param reports
	 * @param firstReportPath
	 * @param targetReportPath
	 * @return
	 * @throws IOException
	 */
	private Path removeLogoutFirstReport(List<File> reports, Path firstReportPath, Path targetReportPath)
			throws IOException {
		// started removing Logout test case in first report
		Document firstHtml = Jsoup.parse(reports.get(0), "UTF-8");
		Elements firstTestCases = getElementsReport(reports, 0);

		for (int j = 0; j < firstTestCases.size(); j++) {
			if (firstTestCases.get(j).text().contains("QuickCapBaseSuite - Logout")) {
				firstTestCases.remove(j);
				break;
			}
		}
		Element firstTests = firstHtml.body().selectFirst("ul#test-collection");
		// remove all existing old tests from report html
		firstTests.select("ul#test-collection li.collection-item").remove();
		// attach updated tests to report html
		firstTestCases.forEach(tc -> tc.appendTo(firstTests));
		// write html to report file on path
		FileUtils.writeStringToFile(firstReportPath.toFile(), firstHtml.outerHtml(), "UTF-8");
		// ended removing Logout test case in first report
		targetReportPath = Files.move(firstReportPath, targetReportPath);
		return targetReportPath;
	}

	/**
	 * get test case elements from report
	 * 
	 * @param reports list of report files
	 * @param i       file index
	 * @return
	 * @throws IOException
	 */
	private Elements getElementsReport(List<File> reports, int i) throws IOException {
		File currentReport = reports.get(i);
		Document html = Jsoup.parse(currentReport, "UTF-8");
		Elements testCases = html.body().select("ul#test-collection li.collection-item");
		return testCases;
	}

	/**
	 * Rearrange Test cases as per CRUD sheet column order, referenced from text
	 * file
	 * 
	 * @param reportPath Report path
	 * @throws IOException
	 */

	private void rearrangeTestCasesCRUD(Path reportPath, String executionTime) throws IOException {

		Operations.log.debug("Started rearranging test cases as per CRUD sheet column order");

		// parsing & rearranging test cases inside the newly created report
		Document finalReportDoc = Jsoup.parse(reportPath.toFile(), "UTF-8");
		Elements finalTestCases = finalReportDoc.body().select("ul#test-collection li.collection-item");
		List<String> listToSort = new ArrayList<String>();
		finalTestCases.forEach(item -> listToSort.add(item.selectFirst("span.test-name").ownText()));

		// extract test cases from text file to List
		List<String> listWithOrder = Files.readAllLines(new File("Data/Utility/CRUD.txt").toPath(),
				Charset.defaultCharset());

		// sort test cases as per order of test cases from text file
		Collections.sort(finalTestCases,
				Comparator.comparing(item -> listWithOrder.indexOf(item.selectFirst("span.test-name").ownText())));

		// remove all existing old tests from report html
		finalReportDoc.body().select("li.collection-item").remove();

		// attach updated tests to report html
		finalTestCases.forEach(item -> item.appendTo(finalReportDoc.body().selectFirst("ul#test-collection")));

		Document finalReportDocTemp = finalReportDoc;

		// update maximum execution time in report
		finalReportDocTemp = updateExecutionTime(finalReportDocTemp, executionTime);

		// write html to report file on path
		FileUtils.writeStringToFile(reportPath.toFile(), finalReportDoc.outerHtml(), "UTF-8");

		Operations.log.debug("Ended rearranging test cases as per CRUD sheet column order");
	}

	/**
	 * Gets the maximum time of tests
	 * 
	 * @param document
	 * @return maximum time or empty string
	 */

	private Document updateExecutionTime(Document document, String executionTime) {

		Operations.log.debug("Started updating the execution time");

		Elements startedTimes = document.body().select("span.test-started-time");
		Elements endedTimes = document.body().select("span.test-ended-time");

		Elements testTimes = document.body().select("span[title='Time taken to finish']");
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("H'h' m'm' s's+'S'ms'");

		try {

			List<Date> times = new ArrayList<>();
			for (int i = 0; i < testTimes.size(); i++) {
				if (!testTimes.get(i).text().isEmpty())
					times.add(simpleDateFormat.parse(testTimes.get(i).text()));
			}

			int maxTimeIndex = times.indexOf(Collections.max(times));

			String startTime = startedTimes.get(maxTimeIndex).text();
			String endTime = endedTimes.get(maxTimeIndex).text();

			document.body().selectFirst("div#dashboard-view span.suite-started-time").text(startTime);
			document.body().selectFirst("div#dashboard-view span.suite-ended-time").text(endTime);
			document.body().selectFirst("span.suite-total-time-taken").text(executionTime);
		} catch (Exception e) {
			Operations.log.error("Failed to update execution time", e);
		}

		Operations.log.debug("Ended updating the execution time");
		return document;
	}

}
