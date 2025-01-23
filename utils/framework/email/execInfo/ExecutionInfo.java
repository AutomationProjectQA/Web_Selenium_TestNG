package framework.email.execInfo;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import base.BaseSuite;
import framework.LogUtil;

public class ExecutionInfo {

	private Logger log;

	public ExecutionInfo() {
		// for framework utilize the BaseSuite initialize logger else create new for
		// using it separately
		log = BaseSuite.log == null ? new LogUtil(Strings.EMPTY, Strings.EMPTY).logger : BaseSuite.log;
	}

	/**
	 * To get the total number of test in report
	 * 
	 * @param reportPath
	 * 
	 * @return
	 */
	public String totalTests(String reportPath) {
		String count = "";

		try {
			Document html = Jsoup.parse(new File(reportPath), "UTF-8");

			// parse to get the text of total test
			count = String.valueOf(html.body().getElementById("test-collection").getElementsByTag("li").size());
			log.debug(reportPath + " having total tests - " + count);
		} catch (IOException e) {
			log.error("Failed to read the HTML report file at - " + reportPath);
		}

		return count;
	}

	/**
	 * To get the failed number of test in report : Fail + Error
	 * 
	 * @param reportPath
	 * 
	 * @return
	 */
	public String failedTests(String reportPath) {
		String count = "";

		try {
			Document html = Jsoup.parse(new File(reportPath), "UTF-8");

			// maintain the map of test and test-status
			Map<String, String> tests = new HashMap<>();

			// get the li list of test
			Elements testsList = html.body().getElementById("test-collection").getElementsByTag("li");

			// loop through to generate failedTests
			// override the test status based on last rerun
			testsList.stream()
					.forEach(test -> tests.put(
							test.getElementsByAttributeValue("class", "test-name").get(0).text().split("- ReRun")[0]
									.trim(),
							test.getElementsByAttributeValueContaining("class", "outline").get(0).text()));

			count = String.valueOf(
					Collections.frequency(tests.values(), "fail") + Collections.frequency(tests.values(), "error"));

			log.debug(reportPath + " having total failed tests - " + count);
		} catch (IOException e) {
			log.error("Failed to read the HTML report file at - " + reportPath);
		}

		return count;
	}

	/**
	 * To get the execution time of the report
	 * 
	 * @param reportPath
	 * 
	 * @return
	 */
	public String executionTime(String reportPath) {
		String timeTaken = "";

		try {
			Document html = Jsoup.parse(new File(reportPath), "UTF-8");

			// parse to get the text of total test
			timeTaken = String.valueOf(html.body().getElementById("dashboard-view")
					.getElementsMatchingOwnText("Total Time Taken").get(0).lastElementSibling().text());
			log.debug(reportPath + " time taken to execute - " + timeTaken);
		} catch (IOException e) {
			log.error("Failed to read the HTML report file at - " + reportPath);
		}

		return timeTaken;
	}

}
