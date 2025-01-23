package execution.operations;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class CommonOperations {

	/**
	 * Logs the steps in a file
	 * 
	 * @param logMessage
	 * @param Exception(optional)
	 */
	public void log(String logMessage, Exception... e) {
		try (FileWriter fr = new FileWriter(Operations.dataFile, true);
				BufferedWriter br = new BufferedWriter(fr);
				PrintWriter writer = new PrintWriter(br);) {

			// add log message in file
			writer.println(new SimpleDateFormat("HH_mm_ss").format(new Date()) + " " + logMessage);

			if (e.length > 0) {
				e[0].printStackTrace(writer);
			}
		} catch (Exception io) {
			System.out.println(new SimpleDateFormat("HH_mm_ss").format(new Date()) + " Error occured while logging - "
					+ logMessage);
			io.printStackTrace();
		}
	}

	/**
	 * Wait for given seconds
	 * 
	 * @param timeInSeconds
	 */
	public void pause(int timeInSeconds) {
		try {
			Thread.sleep(timeInSeconds * 1000);
		} catch (InterruptedException ie) {
			Operations.log.error("Error occured while waiting for execution to complete", ie);
		}
	}

	/**
	 * Calculate the time difference between to dates
	 * 
	 * @param from from date
	 * @param to   to date
	 * @return difference
	 */
	public String calculateTime(LocalDateTime from, LocalDateTime to) {

		// count the difference between to dates into miliseconds
		double miliseconds = Duration.between(from, to).toMillis();

		String difference = "";

		SimpleDateFormat sdf = new SimpleDateFormat("SSS");
		try {
			Date dt = sdf.parse(String.valueOf(miliseconds));

			// convert into specific date format hour:minutes:seconds:miliseconds
			sdf = new SimpleDateFormat(ProcessStrings.timeFormat);

			difference = sdf.format(dt);

		} catch (ParseException e) {
			Operations.log.error("Error occured while caluculating the difference between from and to time");
		}

		return difference;
	}

	/**
	 * To read the json file property value
	 * 
	 * @param jsonKey      json key
	 * @param jsonFilePath path for json
	 * @return
	 */
	public String readJsonFile(String jsonKey, String jsonFilePath) {

		// getting current execution Sub Modules
		String jsonValue = "";

		try (FileReader reader = new FileReader(jsonFilePath)) {
			JSONParser jsonParser = new JSONParser();
			JSONObject json = null;
			json = (JSONObject) jsonParser.parse(reader);
			if (json != null && json.containsKey(jsonKey)) {
				jsonValue = (String) json.get(jsonKey);
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while reading json file ", e);
		}

		return jsonValue;
	}

	/**
	 * To get json data from json file
	 * 
	 * @param filePath    - Json file path
	 * @param jsonKeyPath - Json key path eg. key1/key2
	 * @return String - value of given json key
	 */
	public String readJsonData(String jsonKeyPath, String filePath) {
		String value = "";
		try (FileReader reader = new FileReader(filePath)) {
			// read json file
			JSONParser jsonParser = new JSONParser();
			JSONObject json = (JSONObject) jsonParser.parse(reader);

			// get keys array
			String[] keys = jsonKeyPath.split("/");

			// get key value
			value = getJsonObjValue(json, keys);
		} catch (Exception e) {
			Operations.log.error("Error occured while reading json file for key " + jsonKeyPath, e);
		}
		return value;
	}

	/**
	 * To get values from json object
	 * 
	 * @param jsonObject
	 * @param keys       - Array of the keys
	 * @return value if given key found else empty string
	 */
	private String getJsonObjValue(JSONObject jsonObject, String[] keys) {
		String currentKey = keys[0];

		if (keys.length == 1 && jsonObject.containsKey(currentKey)) {
			return jsonObject.get(currentKey).toString();
		} else if (!jsonObject.containsKey(currentKey)) {
			Operations.log.debug(currentKey + "is not a valid key.");
			return "";
		}

		// create json object of nested json object
		JSONObject nestedJsonObjectVal = (JSONObject) jsonObject.get(currentKey);

		// get remaining keys array
		int nextKeyIdx = 1;
		String[] remainingKeys = Arrays.copyOfRange(keys, nextKeyIdx, keys.length);

		// recursive call
		return getJsonObjValue(nestedJsonObjectVal, remainingKeys);
	}

	/**
	 * Check the running process and kill it
	 *
	 * @param serviceName Give name of the process that you want to kill
	 *
	 * @return Boolean
	 */

	public boolean isProcessRunningAndKill(String serviceName) {
		boolean boolRunning = false;
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.equals("linux")) {
				boolRunning = true;
				serviceName = serviceName.replace(".exe", "");
				Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "killall -KILL " + serviceName });

			} else {
				boolRunning = true;
				Runtime.getRuntime().exec("taskkill /f /IM " + serviceName);
			}

		} catch (IOException e) {
			Operations.log.error("Process - " + serviceName + " killing got failed", e);
		}
		return boolRunning;
	}

	/**
	 * To get the available port
	 * 
	 * @param fileName
	 * @return port number
	 */
	public int allocatePort(String fileName) {

		int allocatedPort = -1;

		try {
			fileName = fileName.replace(".xml", "");
			ServerSocket socket = null;

			// Create a server socket with port 0, which automatically allocates an
			// available port
			socket = new ServerSocket(0);

			socket.close();
			allocatedPort = socket.getLocalPort();

			FileOperations fileOps = new FileOperations();

			// Enter the port number in the Port List txt file
			fileOps.appendData(fileName + " - " + String.valueOf(allocatedPort),
					readJsonData("path/docker/portListFile", ProcessStrings.sanityConfigJsonFile));

		} catch (IOException e) {
			Operations.log.error("Error occured while searching free port for " + fileName, e);
		}

		return allocatedPort;
	}
}
