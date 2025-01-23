package execution.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class DockerOperations extends CommonOperations {

	/**
	 * Start given nodes in parallel
	 * 
	 * @param xmlfiles array of files
	 * @return boolean
	 */
	public boolean startParallelNode(File[] xmlfiles) {

		Operations.log.debug("Started creating parallel node");

		Arrays.stream(xmlfiles).parallel().forEach(xmlFile -> {

			String fileName = xmlFile.getName().replace(".xml", "");
			int port = allocatePort(fileName);

			String downloadFolder = Operations.rootDir.replace("\\", "/").replace("C:", "c")
					+ readJsonData(ProcessStrings.downloadFolder, ProcessStrings.sanityConfigJsonFile)
					+ Operations.clientFolder + "/" + fileName;

			String nodeCommand = readJsonData(ProcessStrings.parallelNode, ProcessStrings.sanityConfigJsonFile)
					.replace("%fileName%", fileName).replace("%port%", String.valueOf(port))
					.replace("%downloadPath%", downloadFolder).replace("%networkName%",
							readJsonData(ProcessStrings.networkName, ProcessStrings.sanityConfigJsonFile));

			// run command
			runCommandWithoutOutput(nodeCommand);
		});
		Operations.log.debug("Ended creating parallel node");

		int nodeCount = 0;
		int expectedNodeCount = xmlfiles.length;

		API api = new API();
		int timeoutInSeconds = Integer.parseInt(
				readJsonData(ProcessStrings.timeoutToVerifyNodeAndSession, ProcessStrings.sanityConfigJsonFile));

		Operations.log.debug("Started verifying nodes created in the selenium grid");
		// loop till all the nodes creating in the selenium grid
		do {
			nodeCount = api.getNodesCount();
			pause(Operations.veryLowWait);
			timeoutInSeconds--;
		} while (nodeCount != expectedNodeCount && timeoutInSeconds > 0);
		Operations.log.debug("Total nodes created in the grid " + nodeCount + " & timeout is " + timeoutInSeconds);
		Operations.log.debug("Ended verifying nodes created in the selenium grid");
		return nodeCount == expectedNodeCount;
	}

	/**
	 * Run docker command with process
	 * 
	 * @param command        docker command to run
	 * @param expectedOutPut expected output if any, else empty string
	 * @param closeCMD       true or false for closing cmd
	 * @param logFileName    file name to write logs
	 * @return boolean
	 */
	public boolean runDockerCommand(String command, String expectedOutPut, boolean closeCMD, String... logFileName) {

		// get docker command based on WSL condition
		String dockerCommand = command;

		// other logFile name
		String logFile = logFileName.length > 0 ? logFileName[0] : ProcessStrings.dockerLogFileName;

		return runCommand(dockerCommand, expectedOutPut, closeCMD, logFile);
	}

	/**
	 * Run any command with process
	 * 
	 * @param command        command to run
	 * @param expectedOutPut expected output if any, else empty string
	 * @param closeCMD       true or false for closing cmd
	 * @param logFileName    file name to write logs
	 * @return boolean
	 */
	public boolean runCommand(String command, String expectedOutPut, boolean closeCMD, String... logFileName) {

		Operations.log.debug("Started running command : " + command);

		boolean result = false;

		try {

			// run command
			Process process = Runtime.getRuntime().exec(command);
			/*
			 * Runtime.getRuntime() .exec(readJsonData(ProcessStrings.runDockerCommand,
			 * ProcessStrings.sanityConfigJsonFile) .replace("%dockerFolderPath%",
			 * readJsonData(ProcessStrings.logsFolder, ProcessStrings.sanityConfigJsonFile))
			 * .replace("%command%", command).replace("%logFilePath%", logFilePath));
			 */
			pause(Operations.moderateWait);

			boolean checkDockerLogs = false;

			if (!expectedOutPut.isEmpty()) {

				checkDockerLogs = dockerLogs(process.getInputStream(), expectedOutPut, command);
				if (checkDockerLogs) {
					result = true;

				}
			} else {
				Operations.log.warn("Expected output is empty for command " + command);
				result = true;
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while running command", e);
			return false;
		}

		// to close command prompt
		// it will close all command prompts
		if (closeCMD) {
			try {
				Runtime.getRuntime().exec("osascript -e 'tell application \"Terminal\" to close front window'");
				pause(Operations.moderateHighWait);
			} catch (Exception e) {
				Operations.log.error("Error occured while closing cmd " + e);
			}

		}
		Operations.log.debug("Ended running docker command : " + command);
		return result;
	}

	/**
	 * Execute command without logs in the file
	 * 
	 * @param command command to execute
	 * @return true or false
	 */
	public boolean runCommandWithoutOutput(String command) {

		Operations.log.debug("Started running command without output : " + command);
		String dockerCommand = command;

		// run command
		try {
			Runtime.getRuntime()
					.exec(readJsonData(ProcessStrings.runCommandWithoutLogs, ProcessStrings.sanityConfigJsonFile)
							.replace("%command%", dockerCommand));
			pause(Operations.lowWait);

			try {
				ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
				processBuilder.redirectErrorStream(true); // Redirects error stream to output
				Process process = processBuilder.start();

				// Capture output
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}

				// Wait for process to finish
				int exitCode = process.waitFor();
				System.out.println("Exited with code: " + exitCode);

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			return true;
		} catch (Exception e) {
			Operations.log.error("Error occur while executing command", e);
		}

		Operations.log.debug("Ended running command without output : " + command);
		return false;

	}

	/**
	 * Invoke powershell file through cmd
	 * 
	 * @param String filePath
	 */
	public boolean envokePowershell(String filePath) {

		boolean result = false;

		Operations.log.debug("Started running Powershell command : " + filePath);

		try {
			// run powershell command
			Runtime.getRuntime().exec("/usr/local/bin/pwsh " + filePath);

			pause(3);

			result = true;
		} catch (Exception e) {
			Operations.log.error("Error occured while envoking Powershell file", e);
			result = false;
		}

		Operations.log.debug("Ended running Powershell command : " + filePath);
		return result;
	}

	/**
	 * Check for logs to verify expected output
	 * 
	 * @param actualOutPut   actual output
	 * @param expectedOutPut expected output to verify
	 * @param command        command for logging
	 * @return true or false
	 */
	public boolean dockerLogs(InputStream actualOutPut, String expectedOutPut, String command) {

		boolean result = false;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(actualOutPut))) {
			String st;

			while ((st = br.readLine()) != null) {

				if (expectedOutPut.isEmpty()) {
					result = !st.isEmpty();
					break;
				}
				if (st.contains(expectedOutPut)) {
					Operations.log.debug("Docker command :" + command + " run successfully");
					result = true;
					break;
				}
			}
		} catch (IOException e) {
			Operations.log.error("Error occured while reading the grid log file", e);
		}
		return result;
	}

	/**
	 * Start the docker from CMD
	 * 
	 * @return true if docker start successfully.
	 */
	public boolean startDocker() {

		boolean result = false;
		Operations.log.debug("Started verifying and invoking Docker");

		if (!runDockerCommand(readJsonData(ProcessStrings.info, ProcessStrings.sanityConfigJsonFile), "Running",
				true)) {

			// if docker is not started in first time
			int waitToggle = Operations.moderateHighWait;
			while (waitToggle > 0 && (!result)) {

				// waiting for docker to start for 1 minute
				pause(Operations.explicitLowWait);
				waitToggle--;

				result = runDockerCommand(readJsonData(ProcessStrings.info, ProcessStrings.sanityConfigJsonFile),
						"Running", true);
			}
		} else {
			Operations.log.warn("Docker is running already");
			result = true;
		}
		Operations.log.debug("Ended verifying and invoking Docker");
		return result;
	}

	/**
	 * To create a docker network with name 'Grid'
	 * 
	 * @return true if network is created, else false
	 */
	public boolean createNetwork() {

		boolean networkCreated = false;
		Operations.log.debug("Started creating network");

		networkCreated = runDockerCommand(
				readJsonData(ProcessStrings.network, ProcessStrings.sanityConfigJsonFile).replace("%networkName%",
						readJsonData(ProcessStrings.networkName, ProcessStrings.sanityConfigJsonFile)),
				"", true);

		// verify network is created or not
		Operations.log.debug("Ended creating network");

		return networkCreated;
	}

	/**
	 * To create a docker hub
	 * 
	 * @return boolean
	 */
	public boolean createHub() {

		boolean hubCreated = false;

		Operations.log.debug("Started creating hub");

		hubCreated = runDockerCommand(
				readJsonData(ProcessStrings.hub, ProcessStrings.sanityConfigJsonFile).replace("%networkName%",
						readJsonData(ProcessStrings.networkName, ProcessStrings.sanityConfigJsonFile)),
				"Started Selenium Hub", false, "hubLogs.txt");

		Operations.log.debug("Ended creating hub");

		return hubCreated;
	}

	/**
	 * To create a node with specific download folder and port
	 * 
	 * @param fileName file Name to create a node
	 * @param portNum  port num
	 * @return boolean
	 */
	public boolean startNode(String fileName, String portNum) {

		Operations.log.debug("Started creating node for " + fileName);
		boolean nodeStarted = false;
		String downloadFolder;

		// select download folder path
		downloadFolder = Operations.rootDir
				+ readJsonData(ProcessStrings.downloadFolder, ProcessStrings.sanityConfigJsonFile)
				+ Operations.clientFolder + "/" + fileName;

		String logFolder = fileName + ".txt";

		String nodeCommand = readJsonData(ProcessStrings.node, ProcessStrings.sanityConfigJsonFile)
				.replace("%fileName%", fileName).replace("%port%", portNum).replace("%downloadPath%", downloadFolder)
				.replace("%networkName%",
						readJsonData(ProcessStrings.networkName, ProcessStrings.sanityConfigJsonFile));

		nodeStarted = runDockerCommand(nodeCommand, "Node has been added", false, logFolder);
		Operations.log.debug("Ended creating node for " + fileName);
		return nodeStarted;

	}

	/**
	 * To invoke and arrange vncs after starting execution
	 * 
	 * @param vncPowershellFile powershell file to invoke vnc
	 */
	public void startAndArrangeVNCs(String vncPowershellFile) {

		Operations.log.debug("Strated arranging vncs");

		envokePowershell(Operations.rootDir.replace("\\", "/") + vncPowershellFile);

		Operations.log.debug("Ended arranging vncs");

	}

	/**
	 * Remove all the containers from the list Stop the selenium-hub Remove vnc.exe
	 * Remove cmd.exe
	 */
	public void tearDownDockerGridExecution() {

		Operations.log.debug("Started tear down execution on: " + LocalDateTime.now());

		Operations.log.debug("Started removing containers in docker");
		API api = new API();

		// verify available node counts
		int availableNodeCount = api.getNodesCount();

		// if any node present, then stop that container
		if (availableNodeCount != 0) {
			List<String> availableNodes = api.getAvailableNodes();
			String stopContainerCommand = readJsonData(ProcessStrings.stopGivenContainer,
					ProcessStrings.sanityConfigJsonFile);

			for (String containerName : availableNodes) {
				// stop all the container again
				runDockerCommand(stopContainerCommand.replace("%containerName%", containerName.replace(".xml", "")),
						containerName.replace(".xml", ""), true);
			}
		}

		runDockerCommand(readJsonData(ProcessStrings.stopGivenContainer, ProcessStrings.sanityConfigJsonFile)
				.replace("%containerName%", "selenium-hub"), "selenium-hub", true);

		runDockerCommand(readJsonData(ProcessStrings.removeAllSetup, ProcessStrings.sanityConfigJsonFile), "", true);

		Operations.log.debug("Ended removing containers in docker");

		Operations.log.debug("Started removing vnc viewers");

		runCommand(readJsonData(ProcessStrings.killTasks, ProcessStrings.sanityConfigJsonFile).replace("%taskName%",
				"vnc.exe"), "", true);

		Operations.log.debug("Ended removing vnc viewers");

		Operations.log.debug("Started removing all command prompts");

		runCommand(readJsonData(ProcessStrings.killTasks, ProcessStrings.sanityConfigJsonFile).replace("%taskName%",
				"cmd.exe"), "", true);

		Operations.log.debug("Ended removing all command prompts");

		Operations.log.debug("Ended tear down execution on: " + LocalDateTime.now());
	}

	/**
	 * Remove all the containers from the list Stop the selenium-hub Remove vnc.exe
	 * Remove cmd.exe
	 */
	public void tearDownFailedContainers(List<String> failContainerList) {

		Operations.log.debug("Started removing failed containers in docker");

		for (String containerName : failContainerList) {

			// stop the container
			runDockerCommand(readJsonData(ProcessStrings.stopGivenContainer, ProcessStrings.sanityConfigJsonFile)
					.replace("%containerName%", containerName), containerName, false, "failContainerLog.txt");

			// delete the container
			runDockerCommand(readJsonData(ProcessStrings.deleteGivenContainer, ProcessStrings.sanityConfigJsonFile)
					.replace("%containerName%", containerName), containerName, false, "failContainerLog.txt");
		}

		Operations.log.debug("Ended removing failed containers in docker");
	}
}
