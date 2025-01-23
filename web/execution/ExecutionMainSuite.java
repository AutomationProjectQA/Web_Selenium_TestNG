package execution;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import base.BaseSuite;
import execution.operations.EmailOperations;
import execution.operations.Operations;
import execution.operations.ProcessStrings;
import framework.LogUtil;
import framework.input.Configuration;

public class ExecutionMainSuite {

	Operations ops;
	LocalDateTime from = null, to = null;

	static {

		// generate to use the common timestamp in logs & report name
		String commonTimeStamp = new SimpleDateFormat("dd_MMMM_yyyy_HH_mm_ss_SSS").format(new Date());
		Operations.log = new LogUtil(commonTimeStamp, "DockerExecution").logger;
		BaseSuite.log = Operations.log;

		// FileOperations fileOps = new FileOperations();
		EmailOperations emailOps = new EmailOperations();

		// send before execution email
		// emailOps.sendBeforeExecutionEmail();
	}

	/**
	 * Clean up all the folders and check the docker is installed or not
	 */
	@BeforeSuite
	public void cleanUpAndCheckDocker() {

		ops = new Operations(false);

		// get start execution time
		from = LocalDateTime.now();
		Operations.log.debug("Started execution on: " + from);

		// moving the reports to archive folder. Deleting reports details and previous
		// execution logs files.
		ops.fileOps.cleanUp();

		// check for docker installation, status command will check status and
		// installation both
		if ((!ops.dockerOps.runDockerCommand(
				ops.commonOps.readJsonData(ProcessStrings.version, ProcessStrings.sanityConfigJsonFile), "version",
				true))) {
			Operations.log.error("Error occured while checking Docker is installed or not");
			ops.assertExecution(false);
		}
	}

	/**
	 * Start the docker and create the network and hub
	 */
	@BeforeClass
	public void startDockerNetworkHub() {

		Operations.log.debug("Started docker, network and hub on: " + LocalDateTime.now());

		// start the docker then check docker daemon is running or not, else in wsl
		// check status .
		if (ops.dockerOps.startDocker()) {

			// create network and start the hub
			if (!(ops.dockerOps.createNetwork() && ops.dockerOps.createHub())) {
				Operations.log.error("Error occured while creating network or hub");
				ops.assertExecution(false);
			}
		} else {
			Operations.log.error("Error occured while starting the docker");
			ops.assertExecution(false);
		}

		Operations.log.debug("Ended docker, network and hub on: " + LocalDateTime.now());

	}

	/**
	 * Start all the nodes parallely, Move the email config and update all the
	 * download and toEmail parameter in the config file, start the execution,
	 * Arrange the VNC in the screen
	 */
	@Test
	public void executeSanity() {

		LocalDateTime executionStart, executionEnd;

		try {

			// move emailConfig file from shared folder
			// ops.fileOps.moveEmailConfig();

			executionStart = LocalDateTime.now();
			Operations.log.debug("Started executions of XML on: " + executionStart);

			// start execution
			Map<String, Process> executionContainersAndProcess = ops.startExecutionWithParallelNode();

			// check execution started or not
			if (!executionContainersAndProcess.isEmpty()) {
				Operations.log.debug("Started arranging VNCs on: " + LocalDateTime.now());

				// start and arrange vnc from powershell file
				ops.dockerOps.startAndArrangeVNCs(ops.commonOps.readJsonData(ProcessStrings.vncArrangePowershellFile,
						ProcessStrings.sanityConfigJsonFile));

				Operations.log.debug("Ended arranging VNCs on: " + LocalDateTime.now());

				ops.waitForExecutionToComplete(executionContainersAndProcess);
			}

			executionEnd = LocalDateTime.now();
			Operations.log.debug("Ended executions of XML on: " + executionEnd);
		} catch (Exception e) {
			Operations.log.error("Error occured while execution on docker " + e);
		}
	}

	/**
	 * Close the cmd, vncs, containers and move the reports, logs and download into
	 * single folder. And send the summary email at last.
	 */
	@AfterSuite(alwaysRun = true)
	public void stopDockerAndMoveFiles() {

		// tear down all the containers and vnc
		ops.dockerOps.tearDownDockerGridExecution();

		// get ended execution time
		to = LocalDateTime.now();
		Operations.log.debug("Execution completed on: " + to);

		// calculate total execution time
		String executionTime = ops.commonOps.calculateTime(from, to);
		Operations.log.debug("Total Execution completed : " + executionTime);

		// send fail login email
		ops.emailOps.sendFailLoginEmail(executionTime);

		// move reports and logs files to single folder with client name
		ops.fileOps.moveFilesToSingleFolder();

		// send summary email and sheet
		// ops.updateGSheet.sendSummaryEmailWithSheet(Configuration.getProperty("reportPath"));

		// shutdown logger and move log file to client folder
		LogManager.shutdown();
		ops.fileOps.createSingleFolder(Operations.clientFolder, Configuration.getProperty("logsFilePath"));

		// delete runningContainerList file
		ops.fileOps.deleteFile(
				ops.fileOps.readJsonData(ProcessStrings.runningContainersFile, ProcessStrings.sanityConfigJsonFile));
	}

}
