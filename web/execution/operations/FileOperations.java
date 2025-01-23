package execution.operations;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import framework.input.Configuration;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class FileOperations extends CommonOperations {

	/**
	 * 1. Move previous reports folder in archive folder 2. Move previous logs
	 * folder in archive folder 3. Delete previous docker logs 4. Delete previous
	 * vnc files 5. Move download folder in archive folder 6. Delete temp folder 7.
	 * Delete user specific temp folder
	 */
	public void cleanUp() {
		try {
			// moving reports to archive folder
			Operations.log.debug("Started moving reports to archive");
			String reportsFolder = Configuration.getProperty("reportPath");
			moveLogsReportsToArchive(new File(reportsFolder));
			Operations.log.debug("Ended moving reports to archive");

			// moving logs to archive folder
			Operations.log.debug("Started moving logs to archive");
			String logsFolder = Configuration.getProperty("logsFilePath");
			moveLogsReportsToArchive(new File(logsFolder));
			Operations.log.debug("Ended moving logs to archive");

			// delete previous docker logs
			Operations.log.debug("Started deleting previous logs of docker");
			clearFolders(readJsonData(ProcessStrings.logsFolder, ProcessStrings.sanityConfigJsonFile));
			Operations.log.debug("Ended deleting previous logs of docker");

			// delete previous vnc files
			Operations.log.debug("Started deleting previous vnc files of docker");
			clearFolders(readJsonData(ProcessStrings.vncFiles, ProcessStrings.sanityConfigJsonFile));
			Operations.log.debug("Ended deleting previous vnc files of docker");

			// moving download to archive folder
			Operations.log.debug("Started moving download folder to archive");
			String downloadFolder = Operations.rootDir
					+ readJsonData(ProcessStrings.downloadFolder, ProcessStrings.sanityConfigJsonFile);
			moveLogsReportsToArchive(new File(downloadFolder));
			Operations.log.debug("Ended moving download folder to archive");

			/*
			 * // delete temp folder Operations.log.debug("Started deleting temp folder");
			 * deleteTemp(readJsonData(ProcessStrings.tempFolder,
			 * ProcessStrings.sanityConfigJsonFile));
			 * Operations.log.debug("Ended deleting temp folder");
			 * 
			 * // get the logged in user and delete user specific temp folder
			 * Operations.log.debug("Started deleting %temp% folder"); String pcUser =
			 * System.getProperty("user.name");
			 * deleteTemp(readJsonData(ProcessStrings.userTempFolder,
			 * ProcessStrings.sanityConfigJsonFile) .replace("%user%", pcUser));
			 * Operations.log.debug("Ended deleting %temp% folder");
			 */

		} catch (Exception e) {
			Operations.log.error("Error occured while clean up folders.", e);
		}
	}

	/**
	 * Delete the previous files from folders
	 * 
	 * @param folderName path of foldername
	 */
	public void clearFolders(String folderName) {

		File files = new File(folderName);
		if (files.exists()) {
			for (File file : files.listFiles()) {

				if (!deleteFile(file.getAbsolutePath())) {
					Operations.log.error("Error occured in deleting the file" + file.getName());
				}
			}
		} else {
			Operations.log.error("No files are present in the " + folderName);
		}

	}

	/**
	 * To delete a temp files and all the directories in it
	 * 
	 * @param folderName folder path
	 */
	public void deleteTemp(String folderName) {

		File files = new File(folderName);
		try {
			FileUtils.cleanDirectory(files);
		} catch (IOException e) {
			Operations.log.error("Error occured in deleting the directory at path " + folderName);
		}
	}

	/**
	 * To delete the file path given
	 * 
	 * @param filePath
	 * @return file deleted or not
	 */
	public boolean deleteFile(String filePath) {

		Operations.log.debug("Started deleting file at path - " + filePath);
		boolean fileDeleted = false;

		try {
			File file = new File(filePath);
			file.delete();
			fileDeleted = true;
		} catch (Exception e) {
			Operations.log.error("Failed to delete the file ", e);
		}

		Operations.log.debug("Ended deleting file at path - " + filePath);
		return fileDeleted;
	}

	/**
	 * Updates the download parameter in XML file
	 * 
	 * @param fileAbsolutePath file path
	 * @param downloadFolder   download folder value
	 */
	public void updateDownloadXMLPara(String fileAbsolutePath, String downloadFolder, String xmlFolderPath) {

		Operations.log.debug("Started updating download xml parameter for file " + fileAbsolutePath);
		Map<String, Map<String, String>> browserAndDownloadfolderPara = new HashMap<>();
		Map<String, String> paraValues = new HashMap<>();

		// adding values in parameters
		paraValues.put("nodeDownloadFolder", downloadFolder);
		browserAndDownloadfolderPara.put("Parameter", paraValues);

		File executeXMLFolder = new File(
				readJsonData(xmlFolderPath, ProcessStrings.sanityConfigJsonFile).replace("%userType%", ""));

		if (executeXMLFolder.exists()) {
			// update download folder xml parameter
			updateXMLParameter(fileAbsolutePath, browserAndDownloadfolderPara);
		}

		Operations.log.debug("Ended updating download xml parameter for file " + fileAbsolutePath);
	}

	/**
	 * Updates the download parameters in given XML file
	 * 
	 * @param xmlPath    xml file path
	 * @param parameters parameters to update
	 */
	public void updateXMLParameter(String xmlPath, Map<String, Map<String, String>> parameters) {

		try {
			// getting the XML data
			File xml = new File(xmlPath);
			String xmlText = getXMLData(xmlPath);
			String updateValue;
			String searchValue;
			String tag;

			// updating the parameters
			Document xmlDoc = Jsoup.parse(xmlText, "UTF-8", Parser.xmlParser());

			for (Entry<String, Map<String, String>> paramToUpdate : parameters.entrySet()) {

				// get the tag
				tag = paramToUpdate.getKey();

				// get the valules of tag from map
				for (Entry<String, String> keyValue : paramToUpdate.getValue().entrySet()) {
					searchValue = keyValue.getKey();
					updateValue = keyValue.getValue();

					if (tag.equals("Parameter")) {
						xmlDoc.select("Parameter[name='" + searchValue + "']").attr("value", updateValue);
					}

				}

			}

			// storing in the file
			FileUtils.writeStringToFile(xml, xmlDoc.outerHtml(), "UTF-8");

		} catch (Exception e) {
			Operations.log.error("Error occured while updating the parameter in xml file  " + xmlPath, e);
		}
	}

	/**
	 * Gets the data of passed XML
	 * 
	 * @param xmlPath
	 * @return data of the file
	 */
	private String getXMLData(String xmlPath) {

		Operations.log.debug("Started getting XML data at path - " + xmlPath);

		StringBuilder xmlText = new StringBuilder();
		try (FileReader fr = new FileReader(xmlPath); BufferedReader bfr = new BufferedReader(fr);) {
			String currentLine;
			while ((currentLine = bfr.readLine()) != null) {
				xmlText.append(currentLine);
			}
		} catch (Exception e) {
			Operations.log.error("Error occured in reading the file at path - " + xmlPath, e);
		}

		Operations.log.debug("Ended getting XML data at path - " + xmlPath);
		return xmlText.toString();
	}

	/**
	 * Reading the node logs and verifying for 'Stopping session' to check execution
	 * is stopped or not
	 * 
	 * @return count of expected result
	 */
	public int nodeLogs() {

		int executionCompleteCount = 0;
		DockerOperations dockerOps = new DockerOperations();

		// get the node logs
		File nodeFilesDirectory = new File(
				Operations.rootDir + readJsonData(ProcessStrings.logsFolder, ProcessStrings.sanityConfigJsonFile));
		File[] filesPresent = nodeFilesDirectory.listFiles();
		for (File file : filesPresent) {

			String fileName = file.getName();

			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				String st;

				while ((st = br.readLine()) != null) {

					// verify for Stopping session to check execution is completed or not
					if (st.contains("Stopping session")) {
						executionCompleteCount++;

						String containerName = fileName.replace(".txt", "");

						// remove XML file entry from running container file
						removeLine(containerName, readJsonData(ProcessStrings.runningContainersFile,
								ProcessStrings.sanityConfigJsonFile));

						// stop the container if execution is completed
						dockerOps.runDockerCommand(
								readJsonData(ProcessStrings.stopGivenContainer, ProcessStrings.sanityConfigJsonFile)
										.replace("%containerName%", containerName),
								containerName, false);
						break;
					}
				}
			} catch (IOException e) {
				Operations.log.error("Error occured while reading the docker logs file", e);
			}
		}

		return executionCompleteCount;
	}

	/**
	 * Reading the node logs and verifying for 'Stopping session' to check execution
	 * is stopped or not
	 * 
	 * @return count of expected result
	 */
	public int failedNodeLogs() {

		int executionCompleteCount = 0;
		DockerOperations dockerOps = new DockerOperations();

		// get the node logs
		File nodeFilesDirectory = new File(
				Operations.rootDir + readJsonData(ProcessStrings.logsFolder, ProcessStrings.sanityConfigJsonFile));
		File[] filesPresent = nodeFilesDirectory.listFiles();
		for (File file : filesPresent) {

			String fileName = file.getName().replace(".txt", "");

			try (BufferedReader br = new BufferedReader(new FileReader(file))) {

				/*
				 * if (StartFailedExecutionsSuite.failContainerList.contains(fileName)) { String
				 * st;
				 * 
				 * while ((st = br.readLine()) != null) {
				 * 
				 * // verify for Stopping session to check execution is completed or not if
				 * (st.contains("Stopping session")) { executionCompleteCount++;
				 * 
				 * String containerName = fileName.replace(".txt", "");
				 * 
				 * // remove XML file entry from running container file
				 * removeLine(containerName, readJsonData(ProcessStrings.runningContainersFile,
				 * ProcessStrings.sanityConfigJsonFile));
				 * 
				 * // stop the container if execution is completed dockerOps.runDockerCommand(
				 * readJsonData(ProcessStrings.stopGivenContainer,
				 * ProcessStrings.sanityConfigJsonFile) .replace("%containerName%",
				 * containerName), containerName, false); break; } } }
				 */
			} catch (IOException e) {
				Operations.log.error("Error occured while reading the docker failed logs file", e);
			}
		}

		return executionCompleteCount;
	}

	/**
	 * Move already existing logs file to the archive folder
	 * 
	 * @param logsFolder
	 */
	public void moveLogsReportsToArchive(File logsFolder) {

		if (!logsFolder.exists()) {
			logsFolder.mkdir();
			return;
		}
		// if there is any log file available
		if (logsFolder.exists() && logsFolder.listFiles().length >= 1) {

			// generate the archive data
			File archiveFolder = new File(logsFolder + "//" + ProcessStrings.archiveFolder);

			if (!archiveFolder.exists()) {
				archiveFolder.mkdir();
			}

			// loop if more then 1 log file
			for (File f : logsFolder.listFiles()) {
				try {
					// skip the archive file
					if (!f.getName().contains(ProcessStrings.archiveFolder)) {
						if (f.isFile()) {
							FileUtils.moveFile(f.getAbsoluteFile(), new File(archiveFolder, f.getName()));
						} else {
							// move logs folder
							FileUtils.moveDirectory(f.getAbsoluteFile(), new File(archiveFolder, f.getName()));
						}
					}
				} catch (Exception e) {
					Operations.log.error("Unable to move the existing logs & reports", e);
				}
			}
		}
	}

	/**
	 * Get Email from json file and update in config properties file
	 * 
	 * @param toEmail
	 */
	public void updateEmailInConfig(String currentJsonKey, String emailMapPath) {

		Operations.log.debug("Started getting config email property from jsonfile for file " + currentJsonKey);
		String toEmail = readJsonData(currentJsonKey,
				readJsonData(emailMapPath, ProcessStrings.sanityConfigJsonFile).replace("%userType%", ""));
		Operations.log.debug("Ended getting config email property from jsonfile for file " + currentJsonKey);

		Operations.log.debug("Started updating email property of configfile for " + currentJsonKey);
		try {
			PropertiesConfiguration conf = new PropertiesConfiguration(ProcessStrings.configProperties);
			conf.setProperty(ProcessStrings.emailProperty, toEmail);
			conf.save();
			Operations.log.debug("Successfully updated the config toemail property for file " + currentJsonKey
					+ " toemail is " + toEmail);
		} catch (Exception e) {
			Operations.log.error("Error occured while updating properties file for toEmail");
		}
		Operations.log.debug("Ended updating email property of configfile for " + currentJsonKey);

	}

	/**
	 * Create a single folder for logs and reports with client name
	 * 
	 * @param newFolder         new folder name with client code and timestamp
	 * @param sourceFolder      folderName except archive
	 * @param destinationFolder
	 */
	public String createSingleFolder(String newFolder, String sourceFolder, String... destinationFolder) {

		Operations.log.debug("Started moving files to new folder for " + sourceFolder);

		File[] files = new File(sourceFolder).listFiles();

		int totalFiles = files.length;
		String singleFolderPath = "";
		if (totalFiles >= 1) {

			// creating single log folder
			// get the destination folder if it's given in parameter, append the new folder
			// name inside the destination folder
			String newFolderPath = (destinationFolder.length > 0 ? destinationFolder[0] : sourceFolder) + "//"
					+ newFolder;

			Operations.log.debug("Started creating single folder");
			Path logsFolderPath = Paths.get(newFolderPath);
			if (!logsFolderPath.toFile().exists()) {
				try {
					Files.createDirectory(logsFolderPath);
					singleFolderPath = logsFolderPath.toFile().getAbsolutePath();
					Operations.log.debug("Created single logs folder at path - " + newFolderPath);
				} catch (IOException e) {
					Operations.log.error("Error occured while creating single logs folder at path - " + newFolderPath,
							e);
				}
			}
			Operations.log.debug("Ended creating single folder");

			Operations.log.debug("Started moving files to folder");

			// moving folder in single reports folder
			for (File oldFile : files) {

				// getting file or directory name
				String fileName = oldFile.getName();

				// path for new file
				File newFile = new File(newFolderPath + "/" + fileName);

				// if file
				if (oldFile.isFile() && !fileName.contains(".zip")) {
					try {
						FileUtils.moveFile(oldFile, newFile);
						Operations.log.debug("Moved file from " + oldFile + " to " + newFile);
					} catch (IOException e) {
						Operations.log.error(
								"Error occured while moving file to single folder at path - " + newFile.getPath(), e);
					}
				}
				// for reports to move directory
				// check directory name starts with _ for download folder
				else if (oldFile.isDirectory() && !(fileName.contains(ProcessStrings.archiveFolder))) {
					try {
						FileUtils.moveDirectory(oldFile, newFile);
						Operations.log.debug("Moved file from " + oldFile + " to " + newFile);
					} catch (IOException e) {
						Operations.log.error(
								"Error occured while moving folder to single folder at path - " + newFile.getPath(), e);
					}
				}
			}
			Operations.log.debug("Ended moving files to folder");

		}

		Operations.log.debug("Ended moving files to new folder for " + sourceFolder);
		return singleFolderPath;
	}

	/**
	 * Move all the logs, reports & download folders to single client folder
	 * 
	 */
	public void moveFilesToSingleFolder() {
		Operations.log.debug("Started moving reports to single folder");

		createSingleFolder(Operations.clientFolder, Configuration.getProperty("reportPath"));

		Operations.log.debug("Ended moving reports to single folder");

		Operations.log.debug("Started moving logs to single folder");

		createSingleFolder(Operations.clientFolder, Configuration.getProperty("logsFilePath"));

		Operations.log.debug("Ended moving logs to single folder");
	}

	/**
	 * Creates new zip file from the given folder.
	 * 
	 * @param folderPath - Path to the folder to be archived
	 * @param zipPath    - Path where the zip file is to be placed
	 * 
	 * @return boolean
	 */
	public boolean createZipFile(String folderPath, String zipPath) {

		Operations.log.debug("Started creating report zip file");

		boolean isZipped = false;

		try {

			// Initiate ZipFile object with the path/name of the zip file.
			ZipFile zipFile = new ZipFile(zipPath);

			// Initiate Zip Parameters which define various properties such
			// as compression method, etc.
			ZipParameters parameters = new ZipParameters();

			// set compression method to store compression
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);

			// Set the compression level
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

			// Add folder to the zip file
			zipFile.addFolder(folderPath, parameters);

			isZipped = true;
		} catch (Exception e) {
			Operations.log.error("Failed to create zip file", e);
		}

		Operations.log.debug("Ended creating report zip file");

		return isZipped;
	}

	/**
	 * To get report path from report name
	 * 
	 * @param reportName report name
	 * @return absolute report path
	 */
	public String getReportFromReportFolder(String report) {

		Operations.log.debug("Started getting report path from report folder");
		File reportsFolder = new File(Configuration.getProperty("reportPath"));
		String reportPath = "";

		if (reportsFolder.exists()) {

			for (File f : reportsFolder.listFiles()) {

				// check for specific report folder, it'll not be zip file & it should be folder
				if (f.getName().contains(report) && !f.getName().contains(".zip") && f.isDirectory()) {
					reportPath = f.getAbsolutePath();
					Operations.log.debug(report + " is at path: " + reportPath);
				}
			}
		}

		Operations.log.debug("Ended getting report path from report folder");
		return reportPath;

	}

	/**
	 * Move config file with project config
	 * 
	 * @param configFileKey JSON key for selecting IPA/TPA config
	 */
	public void moveConfig() {

		Operations.log.debug("Started moving config file to project");
		File srcFile = new File(readJsonData(ProcessStrings.configFile, ProcessStrings.sanityConfigJsonFile));
		File trgFile = new File(ProcessStrings.configProperties);

		try {
			FileUtils.copyFile(srcFile, trgFile);
		} catch (IOException e) {
			Operations.log.error("Failed to copy config file from shared folder to target folder ", e);
		}

		Operations.log.debug("Ended moving config file to project");
	}

	/**
	 * Moves the reports to archive folder and kills the driver and browser
	 * instances if running
	 */
	public void cleanUpCRUD() {

		Operations.log.debug("Started moving reports to archive");

		String reportsFolder = Configuration.getProperty("reportPath");

		// moving reports to archive folder
		moveLogsReportsToArchive(new File(reportsFolder));

		Operations.log.debug("Ended moving reports to archive");

		Operations.log.debug("Started killing driver and browser instances if running");

		isProcessRunningAndKill("geckodriver.exe");

		Operations.log.debug("Ended killing driver and browser instances if running");
	}

	/**
	 * Move the Email Map config file to the project level from Shared folder
	 */
	public void moveEmailConfig() {

		Operations.log.debug("Started moving email config file to project");
		File srcFile = new File(
				readJsonData(ProcessStrings.emailMap, ProcessStrings.sanityConfigJsonFile).replace("%userType%", ""));
		File trgFile = new File(
				readJsonData(ProcessStrings.toEmailMap, ProcessStrings.sanityConfigJsonFile).replace("%userType%", ""));

		try {
			FileUtils.copyFile(srcFile, trgFile);
		} catch (IOException e) {
			Operations.log.error("Failed to copy email map configuration file from Shared folder to data folder", e);
		}

		Operations.log.debug("Ended moving email config file to project");
	}

	/**
	 * Append the data in the file
	 * 
	 * @param data     as data to enter in file
	 * @param filePath path of file with name of the file
	 */
	public void appendData(String data, String filePath) {
		Operations.log.debug("Started appending data to specific file: " + filePath);

		try (FileWriter fw = new FileWriter(filePath, true)) {
			fw.write(data + "\n");
		} catch (IOException e) {
			Operations.log.error("Failed to append the data: " + data + " in the " + filePath + " file path", e);
		}

		Operations.log.debug("Ended appending data to specific file: " + filePath);
	}

	/**
	 * Remove specific line from file
	 * 
	 * @param removeLine
	 * @param reportFileName
	 */
	public void removeLine(String removeLine, String reportFileName) {
		try {
			File file = new File(reportFileName);
			List<String> out = Files.lines(file.toPath()).filter(line -> !line.equals(removeLine))
					.collect(Collectors.toList());
			Files.write(file.toPath(), out, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			Operations.log.error("Error in removing line into file - ", e);
		}
	}

	/**
	 * Create a new folder if don't exists already
	 * 
	 * @param string JSON key from the configurations
	 */
	public File createNewFolder(String jsonKey, String jsonFilePath) {

		try {
			File newFolder = new File(readJsonData(jsonKey, jsonFilePath));

			if (!newFolder.exists()) {
				newFolder.mkdir();
			}
			return newFolder;
		} catch (Exception e) {
			Operations.log.error("Folder with JSON key :" + jsonKey + " is not created");
			return null;
		}
	}

	/**
	 * Create a new file if not present else will remove the content of file
	 * 
	 * @param string
	 * @param sanityconfigjsonfile
	 */
	public void createNewFile(String jsonKey, String jsonFilePath) {

		try {
			File newFile = new File(readJsonData(jsonKey, jsonFilePath));
			if (!newFile.exists()) {

				newFile.createNewFile();
			} else {

				PrintWriter writer = new PrintWriter(readJsonData(jsonKey, jsonFilePath));
				writer.print("");
				writer.close();
			}
		} catch (IOException e) {
			Operations.log.error("File with JSON key :" + jsonKey + " is not created");
		}
	}
}
