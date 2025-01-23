package base;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import corelibrary.CommonActions;
import framework.LogUtil;
import framework.email.Email;
import framework.email.EmailSection;
import framework.email.execInfo.ExecutionInfo;
import framework.input.Configuration;
import framework.input.ExcelUtils;
import framework.input.Input;
import framework.reporter.Description;
import framework.reporter.Reporter;
import framework.reporter.ResultType;
import framework.reporter.ScreenshotType;
import framework.setup.SetUp;

/**
 * 
 * This class is extended by every suite in template project and takes care of -
 * Logs initialization Driver initialization Reporting initialization Data
 * initialization multiple input handle
 * 
 *
 */
public class BaseSuite {

	// needed objects for managing logs
	// logger object
	public static LogUtil logUtil;
	public static Logger log;

	// Result object common for all
	public static Reporter RESULT;

	// INPUT object for inputs for suite
	public static Input INPUT;
	// suite name local variable
	private static String suiteName;

	// for managing the setup of browser
	public static SetUp setUp;

	// flags to check if we have the dataprovider available for a test
	// made it protected for custom data provider
	protected static boolean multipleIterationFlag = false;
	// counter to be used to check if we have the iterations
	protected static int runIteration = 0;
	// counter to generate the numbering
	protected static int numCounter = 0;
	// data provider data to be used for iteration
	protected static Object[][] data = null;

	// check if the execution is rerun
	protected static boolean reRun = false;
	// re run text to be appended in result name
	private static final String reRunText = " - ReRun ";
	// re run count given by user
	private static int reRunCountByUser = 0;
	// maintain the rerun count
	private static int reRunCount = 0;
	// maintain the context(1 context per class) list to get failed test
	// Set as when two classes in one test need to have unique one test context
	private Set<ITestContext> contextSet = new HashSet<>();

	// rerun on grid
	private static String browserName = "";
	private static String hubName = "";
	// make download folder global for grid execution
	public static String downloadFolder = "";

	/////////////////////////// pages object creation
	/////////////////////////// /////////////////////////////////

	// map to store page and object of page
	public static Map<String, Object> objectMap = new HashMap<String, Object>();

	// map to store key value pair - page name and package name
	public static Map<String, String> pagesMap = new HashMap<String, String>();

	// Path of all different pages
	// to refer the .class files
	public static String pagesPath = "." + File.separator + "target" + File.separator + "classes";

	// initialize email section
	protected EmailSection emailSection;

	/**
	 * To store page and package name (key value pair) in map
	 * 
	 */
	private void initializeObjects() {

		// check if pagesPath is given by user in configuration
		// Enhancement done based on the request of ShowItBig
		String pagesPathGiven = Configuration.getProperty("pagesPath");
		if (pagesPathGiven != null && !pagesPathGiven.trim().isEmpty())
			pagesPath = pagesPathGiven;

		// for consistent path
		if (pagesPath.contains("\\"))
			pagesPath = pagesPath.replace("\\", File.separator);

		// create file object
		File directory = new File(pagesPath);

		// variables to store file details
		String pageName = null;
		String pageAbsolutePath = null;
		String packageName = null;

		// list to store absolute path of pages
		List<File> pages = new ArrayList<File>();

		// files with specified extensions to be added in list
		String[] extensions = new String[] { "class" };

		try {
			// get absolute path of all files
			pages = (List<File>) FileUtils.listFiles(directory, extensions, true);

			// create loop to generate key value pair
			for (File page : pages) {

				// get file name
				pageName = page.getName().toString();

				// get absolute path
				pageAbsolutePath = page.getPath();

				// excluding OR files AND exclude suites
				if (!pageName.contains("_OR")
						&& !pageAbsolutePath.contains(File.separator + "suites" + File.separator)) {

					// get Page name
					pageName = pageName.replace(".class", "");

					// get package name of page
					packageName = StringUtils.difference(pagesPath, pageAbsolutePath);
					packageName = packageName.substring(1, packageName.length()).replace(".class", "");
					packageName = packageName.replace(File.separator, ".");

					// store key value pair
					pagesMap.put(pageName, packageName);
				}
			}
		} catch (Exception e) {
			log.fatal("Error while parsing and storing pages classes for object creation - SINGLETON - ", e);
		}
	}// end of initializeObjects

	/**
	 * Create Object of specified page
	 * 
	 * @param pageName Page name as String and it is case-sensitive
	 * 
	 * @return Object It returns object of specified page
	 * 
	 * 
	 */
	public static <pageReference extends Object> pageReference createObject(String pageName) {

		// To store page
		Class pageReference;

		// To store page object
		Object pageObject;

		try {
			// check specified page
			if (pagesMap.containsKey(pageName)) {

				// get reference of specified page
				pageReference = Class.forName(pagesMap.get(pageName));

				// check object of specified page - already created
				if (objectMap.containsKey(pageName)) {
					return (pageReference) objectMap.get(pageName);
				} else {
					try {

						// create object of specified page
						// Get the constructor (assumes a no-argument constructor is available)
						Constructor<pageReference> constructor = pageReference.getDeclaredConstructor();

						// Set accessible to true to allow invoking the private constructor
						constructor.setAccessible(true);

						// Create an instance of the class using the constructor
						pageObject = constructor.newInstance();

						// store in global object map
						objectMap.put(pageName, pageObject);

						return (pageReference) pageObject;

					} catch (InstantiationException e) {
						RESULT.FAIL("Not able to create instance of class - '" + pageName + "'", false,
								ScreenshotType.fullScreen);
					} catch (IllegalAccessException e) {
						RESULT.FAIL(
								"Exception occured as current method might have not access to create an instance of class -'"
										+ pageName + "'",
								false, ScreenshotType.fullScreen);
					}
				}
			} else {
				RESULT.FAIL("Page not exist in project", false, ScreenshotType.fullScreen);
			}
		} catch (ClassNotFoundException e) {
			RESULT.FAIL("Exception occured as specified class not found -'" + pageName + "'", false,
					ScreenshotType.fullScreen);
		} catch (Exception e) {
			RESULT.ERROR(("Exception occured in creating object for specified class - '" + pageName + "'"), e, false,
					ScreenshotType.fullScreen);
		}

		return null;

	}// end of createObject

	/**
	 * Delete objects of pages
	 * 
	 * @return void
	 * 
	 */
	private void flushObject() {
		// flush object map
		if (!objectMap.isEmpty())
			objectMap.clear();

		// flush pages class map
		if (!pagesMap.isEmpty())
			pagesMap.clear();

	}// end of flushObject

	/////////////////////////// end of object handling
	/////////////////////////// code/////////////////////////////////////

	// added parameter for the grid execution
	@Parameters({ "browser", "hub", "nodeDownloadFolder", "nodeName" })
	@BeforeSuite
	public void setUp(ITestContext suite, @Optional("browser") String browser, @Optional("hub") String hub,
			@Optional("nodeDownloadFolder") String nodeDownloadFolder, @Optional("nodeName") String nodeName) {
		// do it if not the rerun
		if (!reRun) {

			// generating log & report name early as need to pass it to it's objects
			// generate the report name
			String reportSuite = suite.getCurrentXmlTest().getSuite().getName();
			// to have the suite name of given suite in xml
			// using java it's 'Default suite'
			// using command line it's 'command line suite'
			if (!reportSuite.contains(" ")) {
				suiteName = reportSuite;
				// this makes xml nodeName parameter optional
				nodeName = suiteName;
			} else
				// get the single class run suite name as class name
				suiteName = this.getClass().getSimpleName();

			// generate to use the common timestamp in logs & report name
			String commonTimeStamp = new SimpleDateFormat("dd_MMMM_yyyy_HH_mm_ss_SSS").format(new Date());

			// need to create log util and use it's object every where
			logUtil = new LogUtil(commonTimeStamp, suiteName);
			log = logUtil.logger;

			log.debug("Report suite name - " + suiteName);

			log.debug("Initialize log and started execution");

			// initialize email section
			emailSection = new EmailSection();

			// initialize driver
			try {
				// store browser & node for rerun
				browserName = browser;
				hubName = hub;
				// store node download folder as need to have separate download folder
				// container/execution wise and refer the same in automation code
				downloadFolder = !nodeDownloadFolder.equals("nodeDownloadFolder") ? nodeDownloadFolder
						: Configuration.getProperty("downloadFolder");

				// added parameter for grid
				setUp = new SetUp();
				setUp.setUp(browser, hub, nodeDownloadFolder, nodeName);

				// get the parent window handle for reseting the execution on exception
				log.debug("Getting global parent window handle");

				// get the parent window only 1st time
				CommonActions.globalWinHandle = SetUp.driver.getWindowHandle();
				log.debug("Global parent window handle - " + CommonActions.globalWinHandle);

			} catch (Exception e) {
				log.error("Error encountered while initializing web driver", e);
			}

			// initializer result considering multiple suites
			log.debug("Trying to Initializing reporter");
			// passing node for grid
			RESULT = new Reporter(commonTimeStamp, suiteName, hub);
			log.debug("Reporter initialized successfully");

			// initialize the pages/components object data
			initializeObjects();

			// initialize OR file map for reflection
			CommonActions.storeORFiles();

		} // end of rerun check
	}// end of setup

	/**
	 * To generate the map for failed tests corresponding to each unique class in
	 * the list. Both the param size must be same.
	 * 
	 * @param failedMethod      list of method who got failed
	 * @param failedMethodClass list of class of above every method
	 * @return - map of every unique class with the methods failed in it
	 */
	private Map<String, Map<String, List<String>>> failedClassWithMethods(List<String> failedMethod,
			List<String> failedMethodClass, List<String> failedClassTest) {
		// generate the class and related methods map to generate the xml
		// programmatically
		Map<String, Map<String, List<String>>> testsWithClassesWithMethods = new LinkedHashMap<>();
		for (int i = 0; i < failedMethodClass.size(); i++) {

			// check if class is available in map
			if (testsWithClassesWithMethods.containsKey(failedClassTest.get(i))) {
				// get the class and method list map
				Map<String, List<String>> classWithMethods = testsWithClassesWithMethods.get(failedClassTest.get(i));

				// check if it's same class
				if (classWithMethods.containsKey(failedMethodClass.get(i))) {

					// get the methods list
					List<String> methods = classWithMethods.get(failedMethodClass.get(i));

					// add the new method into it
					methods.add(failedMethod.get(i));

					// update the list value of class and method map
					classWithMethods.replace(failedMethodClass.get(i), methods);

					// update the list value of test, class and method map
					testsWithClassesWithMethods.replace(failedClassTest.get(i), classWithMethods);
				} else {
					// have to add new class for given tests
					// create the list of methods
					// create the list of methods
					List<String> methods = new ArrayList<>();
					methods.add(failedMethod.get(i));

					// add new class
					classWithMethods.put(failedMethodClass.get(i), methods);

					// update the map
					testsWithClassesWithMethods.replace(failedClassTest.get(i), classWithMethods);
				}

			} else {
				// create the list of methods
				List<String> methods = new ArrayList<>();
				methods.add(failedMethod.get(i));

				// create the map of class and list
				Map<String, List<String>> classWithMethods = new LinkedHashMap<>();
				classWithMethods.put(failedMethodClass.get(i), methods);

				// put the class and method entry in map
				testsWithClassesWithMethods.put(failedClassTest.get(i), classWithMethods);
			}

		}
		return testsWithClassesWithMethods;
	}// end of failedClassWithMethods

	/**
	 * To generate the xml file based on the map of classes and the failed methods
	 * in it and execute the same for rerun.
	 * 
	 * @param classesWithFailedMethods - map of every unique class with the methods
	 *                                 failed in it.
	 * 
	 * @param browser                  - for grid, collected from setup
	 * 
	 * @param node                     - for grid, collected from setup
	 */
	private void generateAndExecuteTestNGXML(Map<String, Map<String, List<String>>> testsWithClassesWithFailedMethods,
			String browser, String node) {
		TestNG testng = new TestNG();

		List<XmlSuite> xmlSuites = new ArrayList<>();
		// create suite
		XmlSuite xmlSuite = new XmlSuite();

		// add parameter if grid
		if (!node.equals("node")) {
			// create parameter map
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put("browser", browser);
			parameters.put("node", node);
			// add to suite
			xmlSuite.setParameters(parameters);
		}

		// loop through tests and add it to suite
		for (String testKey : testsWithClassesWithFailedMethods.keySet()) {

			String currentTest = testKey;
			Map<String, List<String>> classWithMethods = testsWithClassesWithFailedMethods.get(currentTest);

			// create test
			XmlTest xmlTest = new XmlTest(xmlSuite);
			xmlTest.setName(testKey);

			// add class to list
			List<XmlClass> xmlClasses = new ArrayList<>();

			// loop through tests and add it to suite
			for (String classKey : classWithMethods.keySet()) {

				List<String> classMethods = classWithMethods.get(classKey);

				// preserve order at test level to match the previous execution
				xmlTest.setPreserveOrder(true);

				// create class
				XmlClass xmlClass = new XmlClass(classKey);

				// create methods included
				List<XmlInclude> includeMethods = new ArrayList<>();
				for (int i = 0; i < classMethods.size(); i++)
					includeMethods.add(new XmlInclude(classMethods.get(i)));

				// add methods to class
				xmlClass.setIncludedMethods(includeMethods);

				// add class to list
				xmlClasses.add(xmlClass);
			}

			// add list of class to test
			xmlTest.setXmlClasses(xmlClasses);
		}

		// add suite to suite list
		xmlSuites.add(xmlSuite);

		// add suite list to testng
		testng.setXmlSuites(xmlSuites);

		testng.run();
	}

	/**
	 * To upload the report based on user input > If uploaded email the report to
	 * given email address in configuration.
	 * 
	 */
	public void uploadEmailReport() {

		// store the local path of the report to create the zip
		String reportsFolder = Configuration.getProperty("reportPath");
		log.debug("Local report path - " + reportsFolder);

		// get the report html file path
		String reportHTMLFile = RESULT.getReportPath() + "/" + RESULT.getReportName() + ".html";

		// if uploaded email it out as per the configuration
		if (Configuration.getProperty("sendEmail").equalsIgnoreCase("true")) {
			// send email based on configuration flag
			Email email = new Email();

			// get the total test count
			ExecutionInfo execInfo = new ExecutionInfo();
			String totalTests = execInfo.totalTests(reportHTMLFile);

			// get the total failed count
			String failedTests = execInfo.failedTests(reportHTMLFile);

			// get the execution time
			String executionTime = execInfo.executionTime(reportHTMLFile);

			// for config sendEmailOnFailOnly - to check if want to send email on fail only
			if (!(Boolean.parseBoolean(Configuration.getProperty("sendEmailOnFailOnly")) && failedTests.equals("0")))
				// send the email
				email.send(RESULT.getReportName(), totalTests, failedTests, executionTime);
			else
				log.info("Skipped sending email as sendEmailOnFailOnly is true");
		}
	}// end of uploadEmailReport

	@AfterSuite
	public void tearDown() {

		// check if re run count is given by user in configuration
		String reRunCountGiven = Configuration.getProperty("reRunCount");
		if (reRunCountGiven != null && !reRunCountGiven.trim().isEmpty())
			reRunCountByUser = Integer.valueOf(reRunCountGiven);

		// if user has given re run count then go for re run
		if (reRunCountByUser > 0) {
			log.debug("Starting to check for rerun");

			log.debug("Starting to generate the methods and corresponding the classes from context");
			// to store failed method and corresponding class
			List<String> failedMethod = new ArrayList<>();
			List<String> failedMethodClass = new ArrayList<>();
			// store test for screenshot names from xml file
			List<String> failedMethodTest = new ArrayList<>();

			// iterate thorough entire context and generate lists
			for (int i = 0; i < contextSet.size(); i++) {
				// get the first context failed methods
				ITestContext currentContext = (ITestContext) contextSet.toArray()[i];

				// get the failed method collection
				List<ITestNGMethod> failedTests = new ArrayList<ITestNGMethod>(
						currentContext.getFailedTests().getAllMethods());

				// get the result of failed method array to skip the asserted method
				List<ITestResult> failedTestResults = new ArrayList<ITestResult>(
						currentContext.getFailedTests().getAllResults());

				// loop though all the test methods & their result
				for (int j = 0; j < failedTests.size(); j++) {

					// get the throwable message
					// it will come in case of assert failure only
					String failedTestMsg = "";
					try {
						failedTestMsg = failedTestResults.get(j).getThrowable().getMessage();
						// if null replace it with blank
						if (failedTestMsg == null)
							failedTestMsg = "";
					} catch (NullPointerException e) {
						// failure is not because of assertion
						// no need to get the failedTestMsg
					}

					// check for the failed test is not warning asserted test
					if (failedTestMsg.contains("expected [true] but found [false]")
							& failedTestMsg.startsWith(ResultType.WARNING.toString())) {
						// skip this test
					} else {
						// store the method name
						failedMethod.add(failedTests.get(j).getMethodName());
						// store the method class name with package
						String className = failedTests.get(j).getInstance().toString().split("@")[0];
						failedMethodClass.add(className);
						// store the method test name to consider the xml case - screenshot uniqueness
						String xmlTestName = currentContext.getCurrentXmlTest().getName();
						// if execution is using class name update it
						if (xmlTestName.contains("Default test"))
							xmlTestName = failedTests.get(j).getMethod().getDeclaringClass().getSimpleName();
						failedMethodTest.add(xmlTestName);
					}
				} // end of test loop
			} // end of context loop

			log.debug("Ended to generate the methods and corresponding the classes from context");

			// generate the class and related methods map to generate the xml
			// programmatically
			// if there is any failed method in entire execution
			int failedMethodCount = failedMethod.size();
			log.debug("Failed method in current run " + reRunCount + " - " + failedMethodCount);

			if (failedMethodCount > 0) {
				log.debug("Starting to generate map of unique classes and related methods");
				Map<String, Map<String, List<String>>> classesWithFailedMethods = new LinkedHashMap<>();
				classesWithFailedMethods = failedClassWithMethods(failedMethod, failedMethodClass, failedMethodTest);
				log.debug("Ended to generate map of unique classes and related methods");

				// limit the rerun based on user input
				if (reRunCount < reRunCountByUser) {
					log.debug("Starting to generate xml and execution for rerun iteration - " + (reRunCount + 1));
					// update the rerun flag
					reRun = true;

					// update the rerun count to limit the times
					reRunCount++;

					generateAndExecuteTestNGXML(classesWithFailedMethods, browserName, hubName);
					log.debug("Ended to generate xml and execution for rerun iteration - " + (reRunCount + 1));
				}
			}
			log.debug("Ended rerun");
		} // end of re run check

		log.debug("Terminating suite execution");

		// need to skip driver termination in case driver session timeout
		if (!executionTimeout)
			setUp.tearDown();

		// kill driver in case of execution timeout
		if (executionTimeout)
			setUp.cleanDrivers(true);

		// dump report
		RESULT.terminate();
		log.debug("Suite execution has ended");

		// upload & send email based on user input
		// do it only once at the last return of the execution
		// reduce the count for every re run iteration
		reRunCount--;
		if (reRunCount == -1)
			uploadEmailReport();

		// release pages object
		flushObject();

	}// end of tear down

	@BeforeClass
	public void initializeData() {
		// to get the data for while running multiple classes in a single suite
		suiteName = this.getClass().getSimpleName();

		// for logger to be updated for given class suite
		logUtil.updateLogger(this.getClass());
		log = logUtil.logger;

		// initialize INPUT
		log.debug("Initializing data");
		// using suiteName as for every class/suite it will contain its class
		// name
		String dataSuite = suiteName;
		log.debug("Initializing data for - " + dataSuite);
		INPUT = new Input(dataSuite);
		log.debug("Data initialized successfully");
	}

	/**
	 * To release properties object & store context for rerun failed tests.
	 * 
	 * @param context
	 */
	@AfterClass
	public void releaseDataObject(ITestContext context) {
		// make input null to re-utilize
		INPUT = null;

		// for logger to update to root
		logUtil.updateLogger();
		log = logUtil.logger;

		// store context in the list
		contextSet.add(context);
		log.debug("Added context in list to identify rerun - " + context.toString());
	}

	/**
	 * To setup everything before any test. Define static to access it form the
	 * project specific base suite.
	 * 
	 * @param method
	 * 
	 * @param ITestContext      for getting xml information - pass null in case want
	 *                          to ignore the xml
	 * 
	 * @param callingFrmProject pass true if this calls from the project level apart
	 *                          from @test annotation methods
	 * 
	 */
	@BeforeMethod
	public void setUpTest(Method method, ITestContext suite) {
		String testMethodName = method.getName();

		// generate the report test suite name
		String testSuiteName = "";
		// get the xml test name
		String reportTest = "";

		// get the single class run suite name as class name
		testSuiteName = method.getDeclaringClass().getSimpleName();

		// check for suite if available due to project specific suite
		if (suite != null && !testSuiteName.contains("BaseSuite"))
			reportTest = suite.getCurrentXmlTest().getName();

		// to have the test name for screenshots
		// Broken Link same screenshot name problem
		// using java it's 'Default test' for class &
		// empty due to project specific suite
		if (!(reportTest.contains("Default test") || reportTest.isEmpty()))
			testSuiteName = reportTest;

		String testName = testSuiteName + " - " + testMethodName.replace("_", " ");
		// testName = deCamelCasealize(testName);

		// check and add description
		String testDescription = null;
		try {
			// taking description from ITestContext as needs to update it from runtime for
			// UFT project
			testDescription = Arrays.asList(suite.getAllTestMethods()).stream()
					.filter(m -> m.getMethodName().equals(method.getName())).findFirst().get().getDescription();
			log.debug(testName + " - Description - " + testDescription);

			// convert to html for formatting
			testDescription = new Description().generate(testDescription);
			log.debug(testName + " - HTML Description - " + testDescription);
			// get the parameter
		} catch (NullPointerException e) {
			testDescription = "";
			log.debug("There is not description attached for given test - " + testName);
		}

		log.debug("Starting Test named: " + testName);

		// create new test name for every iteration if enabled
		if (multipleIterationFlag) {// if iteration flag is enabled
			log.debug("Multiple iteration flag is enabled");
			if (runIteration > 0) // and if run iteration counter has value
			// other then 0
			{
				int iterationNumber = (numCounter - runIteration);
				log.debug("Iteration number - " + iterationNumber);
				// add test with first column data
				testName += " - " + data[iterationNumber - 1][0];

				// add the parameter values to description
				testDescription += getParaValueForDesc(getParameterNames(method), iterationNumber);
				log.debug("Complete description for given description - " + testDescription);
			}
			runIteration--;
			// disable the iteration flag once there is no iteration
			if (runIteration == 0)
				multipleIterationFlag = false;
		} else if (!testMethodName.toLowerCase().contains("logout") && data != null && data.length == 1) {
			// for handling single iteration name excluding logout method
			testName += " - " + data[0][0];
		}

		// update test name if it's rerun
		if (reRun)
			testName += reRunText + reRunCount;

		log.debug("Started Test named: " + testName);

		// based on description added
		// to start the logging test in report

		if (testDescription != null && testDescription.trim().length() > 1)
			RESULT.addTest(testName, testDescription);
		else
			RESULT.addTest(testName);
		log.debug("Test setup completed");

		// reset the test case failed & warning flag
		Reporter.isTestCaseFail = false;
		Reporter.isTestCaseWarning = false;

		// after driver session identify, further @test needs to be skipped
		if (executionTimeout) {
			RESULT.SKIP("Execution timeout, thus skipping this flow", false, ScreenshotType.browser);
			// skip throwing this as project level apart from @Test annotation if used to
			// call this method will skip the framework level before after annotation
			// methods
			if (!setUpTestFromProject)
				throw new SkipException("Execution Terminated, thus skipping execution for - " + testName);
		}

		// reinitialize for next set up test from project for apart from @Test
		// annotation
		setUpTestFromProject = false;

	}// end of set up test

	protected boolean setUpTestFromProject = false;

	/**
	 * While using BeforeSuite/AfterSuite for login/logout to add test in report
	 * 
	 * @param methodName
	 */
	public void setUpProjectTest(String methodName) {
		Method method = null;
		try {
			method = this.getClass().getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			RESULT.addTest(methodName);
			RESULT.ERROR("Error while starting the suite", e, false, ScreenshotType.browser);
		}

		// to avoid throwing skip exception for 'UnreachableBrowserException' which
		// timeout execution
		setUpTestFromProject = true;

		// setup for reporting
		// added this to refer this file method
		this.setUpTest(method, null);
	}

	/**
	 * To get the parameter names of given test method
	 * 
	 * @param method
	 * 
	 * @return List of string - parameter names
	 */
	private static List<String> getParameterNames(Method method) {
		// get the parameter array
		Parameter[] parameters = method.getParameters();

		// result list for storing parameter name
		List<String> parameterNames = new ArrayList<>();

		// get name one by one
		for (Parameter parameter : parameters) {
			// get parameter name and store into list
			String parameterName = parameter.getName();
			parameterNames.add(parameterName);
		}

		return parameterNames;
	}

	/**
	 * To generate the string for parameter name and value
	 * 
	 * @param parameterNames  List of all parameters
	 * 
	 * @param iterationNumber For given iteration number
	 * @return
	 */
	private static String getParaValueForDesc(List<String> parameterNames, int iterationNumber) {

		// add break after normal description
		String paraValueDesc = "<br/>";

		// add parameter name value combination
		for (int i = 0; i < parameterNames.size(); i++) {

			// add start incline statement
			paraValueDesc += "<i>";

			// add parameter name
			paraValueDesc += parameterNames.get(i) + " - ";

			// add value from the data[][] of iteration
			paraValueDesc += data[iterationNumber - 1][i];

			// add end incline statement
			paraValueDesc += "</i>";

			// add line break for next
			paraValueDesc += "<br/>";
		}

		paraValueDesc += "<br/>";

		log.debug("iteration extra description - " + paraValueDesc);

		return paraValueDesc;
	}

	// to track driver session timeout
	protected static boolean executionTimeout = false;

	@AfterMethod
	public void tearDownTest(Method method, ITestResult result, ITestContext context) {

		String testName = method.getName();

		// to complete project specific BeforeSuite & AfterSuite need to skip check
		// based on result & context
		if (result != null && context != null) {

			// added as to capture the failure part if the exception occurs in test
			// and to remove the try, catch
			// added second condition to skip the assert fail
			if (result.getStatus() == ITestResult.FAILURE) {
				// to skip assert fail
				boolean assertFail = false;
				boolean timeoutFail = false;
				// to get the trace of abrupt failure
				Throwable stackTrace = null;
				try {
					String failMessage = result.getThrowable().getMessage();
					stackTrace = result.getThrowable();
					if (failMessage.contains("expected [true] but found [false]"))
						assertFail = true;
					if (failMessage.contains("time-out")) {
						timeoutFail = true;
						String timeout = failMessage.split("time-out")[1];
						// converts ms into minutes
						timeout = String.valueOf(Integer.valueOf(timeout.trim()) / 60000);
						RESULT.FAIL(
								"'" + testName + "' didn't finish within the given time-out - " + timeout + " Minutes",
								true, ScreenshotType.browser);
					}
				} catch (Exception e) {
					if (e instanceof WebDriverException || e instanceof NoSuchSessionException) {
						log.error("Abrupt failure, Exception occured - ", e);
					}
					// nothing if no message in throwable as it will be normal exception and not
					// assert one
				}
				if (!assertFail && !timeoutFail) {
					// to handle the skip scenario when driver session timeout
					if (stackTrace instanceof UnreachableBrowserException
							|| stackTrace instanceof NoSuchSessionException) {
						executionTimeout = true;
						log.error(
								"Abrupt failure, Unreachable browser - execution session timedout or No such session - ",
								stackTrace);
						RESULT.FAIL("Test Case \'" + testName + "\' failed as execution timeout", true,
								ScreenshotType.fullScreen);
					} else {
						// removing HTML tags from the message to avoid affect to report
						log.error("Abrupt failure, Exception occured - ", stackTrace);
						RESULT.FAIL(
								"Test Case \'" + testName + "\' failed due to: "
										+ stackTrace.toString().replaceAll("<", "").replaceAll("<", ""),
								true, ScreenshotType.fullScreen);
					}
				}

				// in case of driver session timeout skip reset
				if (!executionTimeout)
					// reset the execution on exception
					new CommonActions().resetExecution();
			}

			// to make testng failed count same as report count
			// if TC has any fail step in report
			// and not failed due to exception or assertion
			if (Reporter.isTestCaseFail && result.getStatus() != ITestResult.FAILURE) {
				// remove the current result from failed list
				context.getPassedTests().removeResult(result);

				// set the current result as failure and add to failed TCs
				result.setStatus(ITestResult.FAILURE);
				context.getFailedTests().addResult(result, result.getMethod());

			}

			// change the value of executeColumn if iteration available and contains execute
			// column plus TC should not be failed
			if (ExcelUtils.executeAvailable && !Reporter.isTestCaseFail) {

				// remove the rerun from the testName and pass it to excel
				String originalTestName = testName.split(reRunText)[0];

				int rowID;
				// if only 1 iteration use the runIteration to get row id
				if (numCounter == 2)
					rowID = ExcelUtils.executeRowIds.get(runIteration);
				else // else apply the formula
					rowID = ExcelUtils.executeRowIds.get(numCounter - (runIteration + 2));
				INPUT.writeExcel(suiteName, originalTestName, rowID);

				// flush the execute row ids & make executeAvailable false for next TC
				// once reached last iteration
				if (runIteration == 0) {
					ExcelUtils.executeRowIds.clear();
					// ExcelUtils.executeAvailable = false;
				}
			}

			// make executeAvailable false for next scenario | took outside above while
			// doing rerun update
			if (runIteration == 0) {
				ExcelUtils.executeAvailable = false;
				// make the data null as it affected while there is only single iteration
				data = null;
			}
		} else {
			log.debug("Skipped checking for test scenarios checks");
		}

		log.debug("Ending Test name: " + testName);
		// to end test and log it to report
		RESULT.endTest();
		log.debug("Test tear down completed");
	}// end of tear down test

	/**
	 * While using BeforeSuite/AfterSuite for login/logout to end test in report
	 * 
	 * @param methodName
	 * 
	 * 
	 */
	public void tearDownProjectTest(String methodName) {
		Method method = null;
		try {
			method = this.getClass().getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			RESULT.addTest(methodName);
			RESULT.ERROR("Error while starting the suite", e, false, ScreenshotType.browser);
		}
		// setup for reporting
		// added this to refer this file method
		this.tearDownTest(method, null, null);
	}

	@DataProvider
	public Object[][] multipleInput(Method method) {
		String testMethodName = method.getName().trim();
		String testSuiteName = method.getDeclaringClass().getSimpleName();
		log.debug("Entered into Dataprovider for " + testMethodName);

		// if input is null than initialize for Class with TC having dataProvider only
		if (INPUT == null)
			INPUT = new Input(testSuiteName);

		data = INPUT.readExcel(testSuiteName, testMethodName);

		runIteration = data.length; // number of iteration
		// enable flag for reporting purpose to be used in before method
		if (runIteration >= 1) {
			multipleIterationFlag = true;
			numCounter = runIteration + 1; // to generate the proper iteration
			// number
		}
		return data;
	}// end of multiple input data provider

	// to convert the test name with spaces
	public static String deCamelCasealize(String camelCasedString) {
		if (camelCasedString == null || camelCasedString.isEmpty())
			return camelCasedString;

		StringBuilder result = new StringBuilder();
		result.append(camelCasedString.charAt(0));
		for (int i = 1; i < camelCasedString.length(); i++) {
			if (Character.isUpperCase(camelCasedString.charAt(i)))
				result.append(" ");
			result.append(camelCasedString.charAt(i));
		}
		return result.toString();
	}

}