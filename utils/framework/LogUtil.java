package framework;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.spi.LoggerContext;
import framework.input.Configuration;

/**
 * 
 * This class contains the logger functions to be used
 * 
 */
public class LogUtil {

	private String logFilePath, logFileFolder, logFileName, timeStamp, testSuiteName;
	public Logger logger;
	private Level logLevel;

	// move logs file only for first suite execution to show all suite run logs
	// in the folder
	public boolean isFirstSuite = true;

	/**
	 * To generate the logs. In case of separate usage of the class, pass the arguments
	 * 'Strings.EMPTY' for both @param
	 * 
	 * @param timeStamp
	 *        String
	 * @param suiteName
	 *        String
	 */
	public LogUtil(String timeStamp, String testSuiteName) {
		// if not parameterized custroctor called then check and initialize
		this.timeStamp = (timeStamp.trim().isEmpty() || timeStamp == null)
				? new SimpleDateFormat("dd_MMMM_yyyy_HH_mm_ss_SSS").format(new Date())
				: timeStamp;

		this.testSuiteName = (testSuiteName.trim().isEmpty() || testSuiteName == null) ? this.getClass().getSimpleName()
				: testSuiteName;

		// log folder
		String logFolder = Configuration.getProperty("logsFilePath");

		logFileFolder = (logFolder != null && !logFolder.trim().isEmpty())
				? logFolder.endsWith("/") ? logFolder : logFolder + "/"
				: "./logs/";

		// log file name
		String logFileNameStarting = Configuration.getProperty("logsFileStartingName");

		logFileName = (logFileNameStarting != null && !logFileNameStarting.trim().isEmpty()) ? logFileNameStarting
				: this.testSuiteName + "_log";

		// generate log filename
		logFilePath = logFileFolder + logFileName + "_" + this.timeStamp + ".html";

		// initialize logger
		intializeLogger();
	}// end of constructor

	/**
	 * To initialize the logger based on the configuration
	 */
	private void intializeLogger() {

		if (isFirstSuite) {
			// move if any existing logs to archive folder
			// if not parallel execution
			if (!Boolean.valueOf(Configuration.getProperty("parallelExecution")))
				moveToArchive();
			isFirstSuite = false;
		}
		// log based on the user given configuration
		if (Boolean.parseBoolean(Configuration.getProperty("logs")))
			logLevel = Level.DEBUG;
		else
			logLevel = Level.OFF;

		// "%5p %d{HH:mm:ss,SSS} (%F:%L) - %M() - %m%n"
		// configure logger
		buildLogConfiguration(logFilePath, "dd-MM-yyyy HH:mm:ss,SSSS", "%CustomPatternLayout", logLevel);

		// get the logger
		logger = LogManager.getRootLogger();

	}// end of intializeLogger

	/**
	 * To update the logger for indicating for which class logging happening
	 * 
	 * @param XMLClass
	 *        Class
	 */
	public void updateLogger(Class<?>... XMLClass) {
		LoggerContext curLogContext = LogManager.getContext();
		LogManager.shutdown(curLogContext);
		if (XMLClass.length > 0)
			logger = LogManager.getLogger(XMLClass[0].getSimpleName());
		else
			logger = LogManager.getRootLogger();
	}

	/**
	 * Programmatically build configuration of log4j 2
	 * Ref:
	 * https://www.studytonight.com/post/log4j2-programmatic-configuration-in-java-class
	 * https://www.digitalocean.com/community/tutorials/log4j2-example-tutorial-configuration-levels-appenders
	 * 
	 * @param filePath
	 *        log file name
	 * @param datePattern
	 *        html logs date pattern
	 * @param patternLayoutPattern
	 *        pattern for console prints
	 * @param logLevel
	 *        We can turn on by Level.TRACE or off by Level.OFF
	 */
	private void buildLogConfiguration(String filePath, String datePattern, String patternLayoutPattern,
			Level logLevel) {
		ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();

		builder.setStatusLevel(Level.ERROR);
		builder.setConfigurationName("FrameworkLogger");

		// create a console appender with layout and given pattern for console
		LayoutComponentBuilder patternLayoutBuilder = builder.newLayout("PatternLayout").addAttribute("pattern",
				patternLayoutPattern);
		AppenderComponentBuilder consoleAppenderBuilder = builder.newAppender("Console", "CONSOLE")
				.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT).add(patternLayoutBuilder);
		// add to builder
		builder.add(consoleAppenderBuilder);

		// create a file appender using custom HTML Layout
		LayoutComponentBuilder htmlLayoutBuilder = builder.newLayout("CustomHTMLLayout")
				.addAttribute("locationInfo", true).addAttribute("title", "Automation Execution Logs")
				.addAttribute("datePattern", datePattern);
		AppenderComponentBuilder fileAppenderBuilder = builder.newAppender("FileLogs", "File")
				.addAttribute("fileName", filePath).add(htmlLayoutBuilder);
		// add to builder
		builder.add(fileAppenderBuilder);

		// add to root logger to builder
		RootLoggerComponentBuilder rootLogger = builder.newRootLogger(logLevel);
		rootLogger.add(builder.newAppenderRef("Console"));
		rootLogger.add(builder.newAppenderRef("FileLogs"));
		builder.add(rootLogger);

		// for Plugins to get detected for finding custom layout or pattern in builder
		builder.setPackages("framework.logging");
		Configurator.initialize(builder.build());

		// set level for driver dependencies
		Configurator.setLevel("io.netty", Level.OFF);
		Configurator.setLevel("org.asynchttpclient.netty", Level.OFF);
		Configurator.setLevel("org.apache.poi", Level.OFF);
	}

	// function to move already existing logs file to the archive folder
	private void moveToArchive() {
		// get logs folder
		File logsFolder = new File(logFileFolder);

		// if there is any log file available
		if (logsFolder.exists() && logsFolder.listFiles().length >= 1) {

			// then move the existing logs to archive folder

			// generate the archive data
			File archiveFolder = new File(logFileFolder + "/archive");

			// loop if more then 1 log file
			for (File f : logsFolder.listFiles()) {
				try {
					// move the log files, ignore archive folder
					if (f.isFile())
						FileUtils.moveFile(f.getAbsoluteFile(), new File(archiveFolder, f.getName()));
				} catch (IOException e) {
					System.out.println("Unable to move the existing logs");
				}
			}

			// recycle archivefolder object
			archiveFolder = null;

		}

		// recycle logsFolder object
		logsFolder = null;
	}// end of moveToArchive

	/**
	 * To get the log file name for having it in report env details
	 * 
	 * @return logFileName
	 *         String
	 */
	public String getLogFileName() {
		String filePathSplit[] = logFilePath.split("/");

		return filePathSplit[filePathSplit.length - 1];
	}
}