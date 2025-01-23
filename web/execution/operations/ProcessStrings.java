package execution.operations;

public interface ProcessStrings {

	// date formats
	String dateFormat = "dd_MMMM_yyyy_HH_mm_ss.SSS";
	String timeFormat = "HH:mm:ss:SSS";

	// file operation
	String archiveFolder = "archive";
	String dockerLogFileName = "log.txt";
	String configProperties = "config.properties";
	String emailProperty = "toemail";
	String sanityConfigJsonFile = "./libs/MainConfigurations.json";
	String crudConfigJsonFile = "./libs/CRUD/crudConfigurations.json";
	String sanityModulesInfoFile = "./libs/sanityModulesInfo.json";
	String wslString = "wsl ";

	String reportFolderPath = "Generated/Report/";
	String executionTime = "Execution Time";
	String testCount = "Test Count";
	String pass = "Pass";
	String warning = "Warning";
	String error = "Error";
	String fail = "Fail";
	String failedTest = "Failed Test";
	String modulesCovered = "Modules Covered";
	String assignedQA = "Assigned QA";
	String sdet = "SDET";
	String copyOfTemplateSheet = "Copy of TemplateSheet";
	String copyofAUTO_Dashboard = "Copy of AUTO_Dashboard";
	String PENDING = "PENDING";

	// email table header values
	String[] headerColumns = { "Assigned Person | Report Path", "Modules Covered", "Test Count | Time", "Failed Test" };

	// json key
	/////////// docker ///////////
	// setup
	String startWSL = "docker/setup/startWSL";
	String start = "docker/setup/start";
	String info = "docker/setup/info";
	String version = "docker/setup/version";
	String statusWSL = "docker/setup/statusWSL";
	// grid
	String networkName = "docker/grid/networkName";
	String network = "docker/grid/network";
	String hub = "docker/grid/hub";
	String node = "docker/grid/node";
	String parallelNode = "docker/grid/parallelNode";
	// teardown
	String killTasks = "docker/teardown/killTasks";
	String removeAllSetup = "docker/teardown/removeAllSetup";
	String stopGivenContainer = "docker/teardown/stopGivenContainer";
	String deleteGivenContainer = "docker/teardown/deleteGivenContainer";
	// execute
	String executeXMLFile = "docker/execute/commands/executeXMLFile";
	String runDockerCommand = "docker/execute/commands/runDockerCommand";
	String executionTimeout = "docker/execute/timeoutInSecond/executionTimeout";
	String executionVerificationFrequency = "docker/execute/timeoutInSecond/executionVerificationFrequency";
	String sessionToStartTimeout = "docker/execute/timeoutInSecond/sessionToStartTimeout";
	String timeoutToReadExpectedOutput = "docker/execute/timeoutInSecond/timeoutToReadExpectedOutput";
	String runCommandWithoutLogs = "docker/execute/commands/runCommandWithoutLogs";
	String timeoutToVerifyNodeAndSession = "docker/execute/timeoutInSecond/timeoutToVerifyNodeAndSession";
	/////////// path ///////////
	// docker
	String logsFolder = "path/docker/logsFolder";
	String portListFile = "path/docker/portListFile";
	String runningContainersFile = "path/docker/runningContainersFile";
	String failContainersFile = "path/docker/failContainersFile";
	// vnc
	String vncFiles = "path/vnc/vncFiles";
	String vncArrangePowershellFile = "path/vnc/vncArrangePowershellFile";
	// shared
	String configFile = "path/shared/configFile";
	String emailMap = "path/shared/emailMap";
	String downloadFolder = "path/shared/downloadFolder";
	// project
	String generatedFolder = "path/project/generatedFolder";
	String parallelLogsFolder = "path/project/parallelLogsFolder";
	String executeXMLsFolder = "path/project/executeXMLsFolder";
	String toEmailMap = "path/project/toEmailMap";
	String tempFolder = "path/project/tempFolder";
	String userTempFolder = "path/project/userTempFolder";
	/////////// projectConfig ///////////
	String devEmailsForFailedCases = "projectConfig/devEmailsForFailedCases";
	String attachSheetInSummaryEmail = "projectConfig/attachSheetInSummaryEmail";

}
