package execution.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import framework.input.Configuration;

public class Operations {

	public static File dataFile;
	public static String rootDir;

	// global waits for project
	public static int veryLowWait = Integer.parseInt(Configuration.getProperty("veryLowWait"));
	public static int lowWait = Integer.parseInt(Configuration.getProperty("lowWait"));
	public static int moderateWait = Integer.parseInt(Configuration.getProperty("moderateWait"));
	public static int highWait = Integer.parseInt(Configuration.getProperty("highWait"));
	public static int moderateHighWait = Integer.parseInt(Configuration.getProperty("moderateHighWait"));
	public static int explicitLowWait = Integer.parseInt(Configuration.getProperty("explicitLowWait"));

	public static int timeoutToReadExpectedOutput;
	public static int sessionToStartTimeout;

	public DockerOperations dockerOps;
	public FileOperations fileOps;
	public CommonOperations commonOps;
	public EmailOperations emailOps;
	public UploadOperation uploadOps;
	public ReportOperations reportOps;
//	public UpdateGSheet updateGSheet;
	public API api;

	public static Logger log;
	public static List<String> runningContainerList = new ArrayList();
	public static String clientFolder = "";
	public static String branch = "";

	/**
	 * 1. Create 'Generated' folder if not created 2. Create 'Download' folder in
	 * Shared folder if not created 3. Create folder with client name & timestamp
	 * into Download folder 4. Create 'Port List' file if not created 5. Create
	 * 'Fail Container List' file if not created 6. Create 'Running Container List'
	 * file if not created 7. Create 'QC_Sanity_parallel_logs' folder for docker
	 * logs if not created 8. Create 'Docker Logs' folder if not created, where logs
	 * of node and hub will be stored
	 * 
	 * @param isStartFailExecution - true if to call constructor for
	 *                             StartFailedExecutionsSuite else if to call
	 *                             constructor for SanityMainSuite
	 */
	public Operations(boolean isStartFailExecution) {

		dockerOps = new DockerOperations();
		fileOps = new FileOperations();
		commonOps = new CommonOperations();
		emailOps = new EmailOperations();
		// updateGSheet = new UpdateGSheet();
		api = new API();

		try {

			rootDir = System.getProperty("user.dir").replace("/", "\\");
			timeoutToReadExpectedOutput = Integer.parseInt(commonOps
					.readJsonData(ProcessStrings.timeoutToReadExpectedOutput, ProcessStrings.sanityConfigJsonFile));
			sessionToStartTimeout = Integer.parseInt(
					commonOps.readJsonData(ProcessStrings.sessionToStartTimeout, ProcessStrings.sanityConfigJsonFile));

			// generate timestamp
			String timeStamp = new SimpleDateFormat(ProcessStrings.dateFormat).format(new Date());

			if (isStartFailExecution) {

				// If logs folder does not exist, create one
				File parallelLogsFolder = fileOps.createNewFolder(ProcessStrings.parallelLogsFolder,
						ProcessStrings.sanityConfigJsonFile);

				// Create log folder
				String logFileName = "qc_sanityDocker_failExecution_logs_" + timeStamp + ".txt";

				// create new log file
				dataFile = new File(parallelLogsFolder + "//" + logFileName);
				dataFile.createNewFile();

			} else {

				// If 'generated' folder does not exist than create it
				fileOps.createNewFolder(ProcessStrings.generatedFolder, ProcessStrings.sanityConfigJsonFile);

				// If logs folder does not exist, create one
				File parallelLogsFolder = fileOps.createNewFolder(ProcessStrings.parallelLogsFolder,
						ProcessStrings.sanityConfigJsonFile);

				// Create log folder
				String logFileName = "Automation_Docker_logs_" + timeStamp + ".txt";

				// create new log file
				dataFile = new File(parallelLogsFolder + "//" + logFileName);
				dataFile.createNewFile();

				// create a folder for logs of node and hub
				fileOps.createNewFolder(ProcessStrings.logsFolder, ProcessStrings.sanityConfigJsonFile);

				// If 'download' in Shared folder does not exist than create it
				fileOps.createNewFolder(ProcessStrings.downloadFolder, ProcessStrings.sanityConfigJsonFile);

				// Create the folder into download folder with client name and timestamp
				String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

				fileOps.createNewFolder(ProcessStrings.downloadFolder,
						ProcessStrings.sanityConfigJsonFile + "/" + clientFolder);

				// Create txt file for Port list
				fileOps.createNewFile(ProcessStrings.portListFile, ProcessStrings.sanityConfigJsonFile);

				// Create txt file for fail container list
				fileOps.createNewFile(ProcessStrings.failContainersFile, ProcessStrings.sanityConfigJsonFile);

				// Create txt file for running container list
				fileOps.createNewFile(ProcessStrings.runningContainersFile, ProcessStrings.sanityConfigJsonFile);

			}
		} catch (Exception e) {
			Operations.log.error("Error occured while creating a file or folder in Operations.", e);
		}
	}

	/**
	 * Initialize log file, move old log files to archive
	 * 
	 */
	public Operations(String logFileName) {

		commonOps = new CommonOperations();
		fileOps = new FileOperations();
		emailOps = new EmailOperations();
		uploadOps = new UploadOperation();
		reportOps = new ReportOperations();

		try {

			// generate "generated" folder if folder not exists
			File generatedFolder = new File(
					commonOps.readJsonFile("generatedFolder", ProcessStrings.crudConfigJsonFile));

			if (!generatedFolder.exists())
				generatedFolder.mkdir();

			// move old log files to archive
			String logsFolder = commonOps.readJsonFile("logsFolder", ProcessStrings.crudConfigJsonFile);
			File logs = new File(logsFolder);
			fileOps.moveLogsReportsToArchive(logs);

			File rootLogsFolder = new File(Configuration.getProperty("logsFilePath"));
			fileOps.moveLogsReportsToArchive(rootLogsFolder);

			// generate CRUD_parallel_logs folder if folder not exists
			File parallelLogs = new File(commonOps.readJsonFile("parallelFolder", ProcessStrings.crudConfigJsonFile));

			if (!parallelLogs.exists())
				parallelLogs.mkdir();

			// create new log file inside
			dataFile = new File(parallelLogs + "//" + logFileName);
			dataFile.createNewFile();

		} catch (Exception e) {
			Operations.log.error("Error occured while creating log file.", e);
		}
	}

	/**
	 * Start execution with parallel nodes
	 * 
	 * @return list of process
	 */
	public Map<String, Process> startExecutionWithParallelNode() {
		Operations.log.debug("Started creating docker nodes on : " + LocalDateTime.now());
		Map<String, Process> executionProcess = new HashMap<>();

		try {

			File directory = new File(
					commonOps.readJsonData(ProcessStrings.executeXMLsFolder, ProcessStrings.sanityConfigJsonFile));

			File[] xmlFiles = FileUtils.listFiles(directory, new String[] { "xml" }, false).toArray(new File[0]);

			// start all the nodes parallely
			if (dockerOps.startParallelNode(xmlFiles)) {

				// start the execution
				executionProcess = startExecution(xmlFiles);

				// wait for executions to start and verify all the sessions have been started
				/*
				 * if (waitForSessionToStart(xmlFiles.length)) {
				 * Operations.log.debug("Successfully all the sessions started in the grid"); }
				 * else {
				 * Operations.log.debug("Error occured while waiting for session to start"); }
				 */
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while starting parallel execution  ", e);
		}
		Operations.log.debug("Ended creating docker nodes on : " + LocalDateTime.now());
		return executionProcess;

	}

	/**
	 * Wait for session to start with status endpoint of grid
	 * 
	 * @param expectedSessionsToStart count of expected sessions to start
	 * @return boolean
	 */
	public boolean waitForSessionToStart(int expectedSessionsToStart) {

		int sessionCount = api.getSessionCount();
		int timeoutInSeconds = Integer.parseInt(commonOps.readJsonData(ProcessStrings.timeoutToVerifyNodeAndSession,
				ProcessStrings.sanityConfigJsonFile));

		Operations.log.debug("Started verifying session created in the selenium grid");

		// loop till all the nodes creating in the selenium grid
		do {
			sessionCount = api.getSessionCount();
			commonOps.pause(Operations.veryLowWait);
			timeoutInSeconds--;
		} while (sessionCount != expectedSessionsToStart && timeoutInSeconds > 0);

		Operations.log
				.debug("Total sessions created in the grid " + sessionCount + " & timeout is " + timeoutInSeconds);
		Operations.log.debug("Ended verifying session created in the selenium grid");

		return sessionCount == expectedSessionsToStart;
	}

	/**
	 * Start execution for given xmls
	 * 
	 * @param xmlFiles list of xml files
	 * @return list of process and its file name map
	 */
	public Map<String, Process> startExecution(File[] xmlFiles) {

		Operations.log.debug("Started execution for " + xmlFiles.length + " files" + LocalDateTime.now());

		// map of execution file name and its process
		Map<String, Process> executionNameAndProcess = new HashMap<>();

		try {

			// loop through the xml files to start the execution
			for (File file : xmlFiles) {

				String fileName = file.getName().replace(".xml", "");

				Operations.log.debug("Started session " + fileName + " nodes on : " + LocalDateTime.now());

				commonOps.pause(lowWait);

				// add into container list for further verification and to stop the container by
				// its name in last
				runningContainerList.add(fileName);

				// get client download folder path
				String clientDownloadFolder = commonOps.readJsonData(ProcessStrings.downloadFolder,
						ProcessStrings.sanityConfigJsonFile) + Operations.clientFolder + "/" + fileName;

				// update the download parameter in xml file
				fileOps.updateDownloadXMLPara(file.getAbsolutePath(), rootDir + clientDownloadFolder,
						ProcessStrings.executeXMLsFolder);

				// update email in config file from json file
				fileOps.updateEmailInConfig(fileName, ProcessStrings.toEmailMap);

				Operations.log.debug("Starting process for file: " + file.getName());

				// start the process
				Process p = Runtime.getRuntime()
						.exec(commonOps.readJsonData(ProcessStrings.executeXMLFile, ProcessStrings.sanityConfigJsonFile)
								.replace("%rootDir%", rootDir).replace("%filePath%", file.getAbsolutePath()));

				// add process and file name into map
				executionNameAndProcess.put(fileName, p);

				// add the started container name in the runningContainerList.txt file
				fileOps.appendData(fileName, fileOps.readJsonData(ProcessStrings.runningContainersFile,
						ProcessStrings.sanityConfigJsonFile));

				commonOps.pause(moderateWait);

				Operations.log.debug("Ended session " + fileName + " nodes on : " + LocalDateTime.now());

			}
			Operations.log.debug("Ended execution for " + xmlFiles.length + " files" + LocalDateTime.now());

		} catch (Exception e) {
			Operations.log.debug("Error occured while starting parallel execution", e);
		}

		return executionNameAndProcess;
	}

	/**
	 * Create node with available port and download folder Update 'toemail' in
	 * config.properties file Start the execution from execute folder Add container
	 * name in runningContainerList or failContainerList
	 * 
	 * @return List of running processes
	 */
	public List<Process> startExecution() {
		Operations.log.debug("Started creating docker nodes on : " + LocalDateTime.now());

		List<Process> executionProcess = new CopyOnWriteArrayList<>();

		File directory = new File(
				commonOps.readJsonData(ProcessStrings.executeXMLsFolder, ProcessStrings.sanityConfigJsonFile)
						.replace("%userType%", ""));

		// List the only xmls extension files in the mentioned directory
		File[] xmlFiles = FileUtils.listFiles(directory, new String[] { "xml" }, false).toArray(new File[0]);

		try {
			// loop through the xml files to start the execution
			for (File file : xmlFiles) {

				boolean failedToStart = false;
				String fileName = file.getName();

				Operations.log.debug("Started creating " + fileName + " nodes on : " + LocalDateTime.now());

				int port = commonOps.allocatePort(fileName);

				// check for xml files only
				if (port != -1) {

					fileName = fileName.replace(".xml", "");
					// start the node with specific port and download folder and different log files
					// for each node
					if (dockerOps.startNode(fileName, String.valueOf(port))) {

						commonOps.pause(moderateWait);
						// add into container list for further verification and to stop the container by
						// its name in last
						runningContainerList.add(fileName);

						// get client download folder path
						String clientDownloadFolder = commonOps.readJsonData(ProcessStrings.downloadFolder,
								ProcessStrings.sanityConfigJsonFile) + Operations.clientFolder + "/" + fileName;

						// update the download parameter in xml file
						fileOps.updateDownloadXMLPara(file.getAbsolutePath(), rootDir + clientDownloadFolder,
								ProcessStrings.executeXMLsFolder);

						// update email in config file from json file
						fileOps.updateEmailInConfig(fileName, ProcessStrings.toEmailMap);

						Operations.log.debug("Starting process for file: " + file.getName());

						commonOps.pause(moderateWait);

						// start the process
						Process p = Runtime.getRuntime().exec(commonOps
								.readJsonData(ProcessStrings.executeXMLFile, ProcessStrings.sanityConfigJsonFile)
								.replace("%rootDir%", rootDir).replace("%filePath%", file.getAbsolutePath()));

						executionProcess.add(p);

						// wait for session to start and call started execution api
						if (waitForSessionToStart(fileName)) {

							// Enter the port number in the Port List txt file
							fileOps.appendData(fileName + " - " + String.valueOf(port), commonOps
									.readJsonData(ProcessStrings.portListFile, ProcessStrings.sanityConfigJsonFile));

						} else
							failedToStart = true;

						commonOps.pause(veryLowWait);

					} else {
						failedToStart = true;
						Operations.log.error("Failed to start node, skipping to start execution for file: " + fileName);
					}

					if (failedToStart) {
						// send fail container email
						emailOps.sendFailContainerEmail(fileName);

						// add the fail container name in the failContainerList.txt file
						fileOps.appendData(fileName, fileOps.readJsonData(ProcessStrings.failContainersFile,
								ProcessStrings.sanityConfigJsonFile));
					} else {
						// add the started container name in the runningContainerList.txt file
						fileOps.appendData(fileName, fileOps.readJsonData(ProcessStrings.runningContainersFile,
								ProcessStrings.sanityConfigJsonFile));
					}
				} else {
					Operations.log.error("Not able to find a free port so skipping the executions of the XMLs.");
				}
				commonOps.pause(moderateHighWait);
				Operations.log.debug("Ended creating " + fileName + " nodes on : " + LocalDateTime.now());
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while starting execution  ", e);
		}

		Operations.log.debug("Ended creating docker nodes on : " + LocalDateTime.now());

		return executionProcess;
	}

	/**
	 * For FAILED XMLs: Create node with available port and download folder Update
	 * 'toemail' in config.properties file Start the execution from execute folder
	 * Add container name in runningContainerList or failContainerList
	 * 
	 * @return process list
	 */
	public List<Process> startFailedExecution() {

		List<Process> executionProcess = new CopyOnWriteArrayList<>();

		File directory = new File(
				commonOps.readJsonData(ProcessStrings.executeXMLsFolder, ProcessStrings.sanityConfigJsonFile)
						.replace("%userType%", ""));

		// List the only xmls extension files in the mentioned directory
		File[] xmlFiles = FileUtils.listFiles(directory, new String[] { "xml" }, false).toArray(new File[0]);

		try {
			// loop through the xml files to start the execution
			for (File file : xmlFiles) {

				boolean failedToStart = false;

				String fileName = file.getName();
				fileName = fileName.replace(".xml", "");

				/*
				 * if (StartFailedExecutionsSuite.failContainerList.contains(fileName)) {
				 * 
				 * int port = commonOps.allocatePort(fileName);
				 * 
				 * // check for xml files only if (port != -1) {
				 * 
				 * // start the node with specific port and download folder and different log
				 * files // for each node if (dockerOps.startNode(fileName,
				 * String.valueOf(port))) {
				 * 
				 * commonOps.pause(moderateWait); // add into container list for further
				 * verification and to stop the container by // its name in last
				 * runningContainerList.add(fileName);
				 * 
				 * // update the download parameter in xml file
				 * fileOps.updateDownloadXMLPara(file.getAbsolutePath(), rootDir +
				 * commonOps.readJsonData(ProcessStrings.downloadFolder,
				 * ProcessStrings.sanityConfigJsonFile) + Operations.clientFolder + "/" +
				 * fileName, ProcessStrings.executeXMLsFolder);
				 * 
				 * // update email in config file from json file
				 * fileOps.updateEmailInConfig(fileName, ProcessStrings.toEmailMap);
				 * 
				 * Operations.log.debug("Starting process for file: " + file.getName());
				 * 
				 * commonOps.pause(moderateWait);
				 * 
				 * // start the process Process p = Runtime.getRuntime().exec(commonOps
				 * .readJsonData(ProcessStrings.executeXMLFile,
				 * ProcessStrings.sanityConfigJsonFile) .replace("%rootDir%",
				 * rootDir).replace("%filePath%", file.getAbsolutePath()));
				 * 
				 * executionProcess.add(p);
				 * 
				 * // wait for session to start and call started execution api if
				 * (waitForSessionToStart(fileName)) {
				 * 
				 * // Enter the port number in the Port List txt file
				 * fileOps.appendData(fileName + " - " + String.valueOf(port),
				 * commonOps.readJsonData( ProcessStrings.portListFile,
				 * ProcessStrings.sanityConfigJsonFile));
				 * 
				 * } else failedToStart = true;
				 * 
				 * commonOps.pause(veryLowWait);
				 * 
				 * } else { failedToStart = true; Operations.log
				 * .error("Failed to start node, skipping to start execution for file: " +
				 * fileName); }
				 * 
				 * if (failedToStart) { // send fail container email
				 * emailOps.sendFailContainerEmail(fileName);
				 * 
				 * } else { // add the started container name in the runningContainerList.txt
				 * file fileOps.appendData(fileName,
				 * fileOps.readJsonData("path/docker/runningContainerFile",
				 * ProcessStrings.sanityConfigJsonFile));
				 * 
				 * // remove container name from failContainerList.txt file
				 * fileOps.removeLine(fileName,
				 * fileOps.readJsonData("path/docker/failContainerFile",
				 * ProcessStrings.sanityConfigJsonFile)); } } else { Operations.log.
				 * error("Not able to find a free port so skipping the executions of the XMLs."
				 * ); } commonOps.pause(moderateHighWait); }
				 */
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while starting execution  ", e);
		}
		return executionProcess;
	}

	/**
	 * Wait for session to create
	 * 
	 * @param logFileName logFile name
	 * @return true or false
	 */
	public boolean waitForSessionToStart(String logFileName) {

		boolean executionStarted = false;

		// timeout to read expected output
		int timeoutInSeconds = sessionToStartTimeout;

		// get the node logs
		File nodeFilesDirectory = new File(Operations.rootDir
				+ commonOps.readJsonData(ProcessStrings.logsFolder, ProcessStrings.sanityConfigJsonFile));
		File[] filesPresent = nodeFilesDirectory.listFiles();

		for (File file : filesPresent) {

			String fileName = file.getName();

			// node logs files start with 'XML name' and check if file is already verified
			// for starting execution
			if (fileName.contains(logFileName)) {
				Operations.log.debug("Started waiting for " + fileName + " session on : " + LocalDateTime.now());

				// loop till expected output match in logs
				outer: do {
					try (BufferedReader br = new BufferedReader(new FileReader(file))) {

						String st;

						while ((st = br.readLine()) != null) {

							// verify for Stopping session to check execution is completed or not
							if (st.contains("Session created by the Node")) {
								Operations.log.debug("Session started for file " + logFileName);
								executionStarted = true;
								break outer;
							}
						}

						timeoutInSeconds = timeoutInSeconds - lowWait;
						commonOps.pause(lowWait);
					} catch (IOException e) {
						Operations.log.error("Error occured while reading the grid log file", e);
					}
				} while (timeoutInSeconds > 0);
				Operations.log.debug("Ended waiting for " + fileName + " session on : " + LocalDateTime.now());

			}
		}

		if (executionStarted) {
			Operations.log.debug("Successfully created session by file " + logFileName);
		} else {
			Operations.log.error("Fail to create session by file " + logFileName);
		}
		return executionStarted;
	}

	/**
	 * Wait for execution to complete based on process is alive or not
	 * 
	 * @param executionProcess List of running processes
	 * @param failExecutionXML true if, to run failed execution XMLs
	 * @return true if execution completed
	 */
	public boolean waitForExecutionToComplete(Map<String, Process> executionProcess, boolean... failExecutionXML) {

		boolean allExecutionEnded = false;
		File runningContainerFile = new File(
				commonOps.readJsonData(ProcessStrings.runningContainersFile, ProcessStrings.sanityConfigJsonFile));

		try {
			int totalExecutions = executionProcess.size();
			int timeoutInSeconds = Integer.parseInt(
					commonOps.readJsonData(ProcessStrings.executionTimeout, ProcessStrings.sanityConfigJsonFile));

			// to check after every given seconds
			int gapToVerifyExecutionCompleteInSeconds = Integer.parseInt(commonOps
					.readJsonData(ProcessStrings.executionVerificationFrequency, ProcessStrings.sanityConfigJsonFile));

			// loop till all the executions gets started in grid
			do {
				timeoutInSeconds = timeoutInSeconds - gapToVerifyExecutionCompleteInSeconds;
				commonOps.pause(gapToVerifyExecutionCompleteInSeconds);
			} while (isProcessAlive(executionProcess) && timeoutInSeconds > 0);

			// it will give session info, if count is 0 then all the session are ended in
			// grid
			int totalEndedExecution = api.getSessionCount();

			// wait for timeout and running container list to be empty
			while ((runningContainerFile.length() > 0) && timeoutInSeconds > 0) {
				timeoutInSeconds = timeoutInSeconds - gapToVerifyExecutionCompleteInSeconds;
			}

			// Verify all the executions ended or not
			if (totalEndedExecution == 0) {
				Operations.log.debug(
						"All the executions are ended in docker grid successfully. Total Executions started are: "
								+ totalExecutions);

				// true if execution ended successfully
				allExecutionEnded = true;
			} else {
				Operations.log.error(
						"Error occured in ending the executions in the docker grid. Total Expected Executions to end are: "
								+ totalExecutions + ". Ended executions are: " + totalEndedExecution
								+ " in remaining timeout (seconds): " + timeoutInSeconds);
				emailOps.sendExecutionTimeoutEmail();
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while waiting for executions to be complete", e);
		}
		return allExecutionEnded;
	}

	/**
	 * Check any process is alive or not
	 * 
	 * @param executionNameAndProcess map of container name and its process
	 * @return true or false, if any process alive or not
	 */
	public boolean isProcessAlive(Map<String, Process> executionNameAndProcess) {

		// find out the process which is not alive and store it into set
		Set<String> nonAliveKeys = executionNameAndProcess.entrySet().stream()
				.filter(entry -> !entry.getValue().isAlive()).map(Map.Entry::getKey).collect(Collectors.toSet());

		// if any process is completed, then stop that container and remove its entry
		// from container list
		if (!nonAliveKeys.isEmpty()) {

			Operations.log.debug("The processes with keys " + nonAliveKeys + " are not alive");

			for (String key : nonAliveKeys) {
				if (runningContainerList.contains(key)) {
					// stop the specific container
					dockerOps.runCommandWithoutOutput(commonOps
							.readJsonData(ProcessStrings.stopGivenContainer, ProcessStrings.sanityConfigJsonFile)
							.replace("%containerName%", key));
					runningContainerList.remove(key);
				}
			}
		}

		return executionNameAndProcess.size() != nonAliveKeys.size();
	}

	/**
	 * Check if any process is alive from given process list
	 * 
	 * @param executionProcess List of running processes
	 * 
	 * @return true if any process is alive,else false
	 */
	public boolean isProcessAlive(List<Process> executionProcess) {
		return executionProcess.stream().anyMatch(str -> str.isAlive());
	}

	/**
	 * Executes XML files present in XML/Performance folder in loop and wait for all
	 * execution to complete
	 * 
	 * @return Boolean
	 * @throws InterruptedException
	 */
	public boolean startCRUDExecution(String os) {

		commonOps.log("Starting execution");

		List<Process> aliveExectuions = new CopyOnWriteArrayList<>();
		int waitInvokeXml = 15;
		try {

			File directory = new File(commonOps.readJsonFile("executeXMLFolder", ProcessStrings.crudConfigJsonFile));

			File[] filesPresent = directory.listFiles();
			for (File file : filesPresent) {
				// do not touch performance.xml which runs daily
				if (file.getName().trim().equals("performance.xml"))
					continue;

				// add wait in every process to avoid same timestamp for screenshot images to
				// prevent FileAlreadyExist exception
				commonOps.pause(moderateWait);

				if (file.getName().toLowerCase().contains("xml")) {
					Operations.log.debug("Starting process for file: " + file.getName());

					// update config properties
					setProperty(new String[] { "downloadFolder" }, new String[] {
							"Generated/Download/" + (file.getName().substring(0, file.getName().indexOf("."))) });

					if (os.equals("linux")) {

						String commandForXML = "mvn test -Dfile=" + file.getAbsolutePath();
						String logFileLocation = directory.getAbsolutePath() + "/../../generated/logs/" + file.getName()
								+ "_log_" + new SimpleDateFormat("dd_MMMM_yyyy_HH_mm_ss").format(new Date()) + ".txt";

						Process p = Runtime.getRuntime()
								.exec(new String[] { "/bin/sh", "-c", commandForXML + "> " + logFileLocation });

						aliveExectuions.add(p);

					} else {

						String commandForXML = (commonOps.readJsonFile("executeFile",
								ProcessStrings.crudConfigJsonFile)).replace("%fileName%", file.getName())
								.replace("%filePath%", file.getAbsolutePath())
								+ new SimpleDateFormat("dd_MMMM_yyyy_HH_mm_ss").format(new Date()) + ".txt\"";

						Process p = Runtime.getRuntime().exec(commandForXML);

						aliveExectuions.add(p);
					}
				}

				commonOps.pause(moderateHighWait);

			}

			Operations.log.debug("Waiting for execution to complete");

			// waiting for the execution to complete
			while (isProcessAlive(aliveExectuions)) {
				commonOps.pause(30);

			}

			Operations.log.debug("Ended execution");

		} catch (IOException io) {
			Operations.log.error("Error occurred while executing", io);
			return false;
		} catch (Exception e) {
			Operations.log.error("Error occurred while executing", e);
			return false;
		}

		return true;

	}

	/**
	 * Exit the code according to annotations mapped i.e. BeforeSuite & AfterSuite
	 * 
	 * @param isAssert false if, don't want to run code from that point
	 */
	public void assertExecution(boolean isAssert) {
		Assert.assertTrue(isAssert);
	}

	/**
	 * Update config file
	 * 
	 * @param keys
	 * @param newValues
	 */
	private void setProperty(String[] keys, String[] newValues) {

		PropertiesConfiguration configFile;
		File file = new File("config.properties");

		if (file.exists()) {
			try {

				configFile = new PropertiesConfiguration("config.properties");

				// check given key is present or not
				for (int i = 0; i < keys.length; i++) {

					if (configFile.getProperty(keys[i]) != null) {

						// update key value in config file
						configFile.setProperty(keys[i], newValues[i]);

					} else {
						Operations.log.error("In config file key -" + keys[i] + " was not found");
					}

				}
				// save config file
				configFile.save();
				commonOps.pause(1);

				Operations.log.debug("Config file updated successfully");

			} catch (ConfigurationException e) {
				Operations.log.error("Error occurred while updating config file", e);
			}
		} else {
			Operations.log.error("Config file was not found");
		}
	}

}
