package framework.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import base.BaseSuite;

/**
 * 
 * This class is for reading the suite's properties file and excel file
 *
 */
public class Input {

	static Properties prop = new Properties();
	static InputStream input = null;
	private String testSuiteData;
	private String propertiesFile;
	// BaseSuite.log.ger object taking ref from BaseSuite.log.util BaseSuite.log.ger
	// private static BaseSuite.log.ger BaseSuite.log. = BaseSuite.log.Util.BaseSuite.log.ger;

	/**
	 * 
	 * to initialize the data reading from .properties file
	 * 
	 * @param testSuiteName
	 *        Suite name for which the data to be read.
	 * 
	 */
	public Input(String testSuiteName) {
		try {

			BaseSuite.log.debug("Test suite name is - " + testSuiteName);

			propertiesFile = testSuiteName + ".properties";
			BaseSuite.log.debug("Property file name is - " + propertiesFile);

			testSuiteData = getFilePath(new File("./Data"), propertiesFile);
			BaseSuite.log.debug("Test suite data path is - " + testSuiteData);

			input = new FileInputStream(testSuiteData);
			BaseSuite.log.debug("Input stream reading done");
			// load a properties file
			prop.load(input);

		} catch (FileNotFoundException e) {
			// to make the property file optional
			BaseSuite.log.debug("Property file " + propertiesFile + " is not available, INPUT object will be null");
			prop.clear();
		} catch (NullPointerException e) {
			BaseSuite.log.debug("Property file " + testSuiteData + " is not available, INPUT object will be null");
			prop.clear();
		} catch (IOException e) {
			BaseSuite.log.trace("Error encountered while readind data for " + testSuiteName, e);
		} finally {
			BaseSuite.log.debug("Closing the input reader");
			if (input != null) {
				try {
					input.close();
					BaseSuite.log.debug("Reader closed successfully");
				} catch (IOException e) {
					BaseSuite.log.trace("Error encountered while closing the input reader", e);
				}
			}
		}
	}

	/**
	 * 
	 * To get the value for a given data key from .properties
	 * 
	 * @param propertyName
	 *        data to be read from .properties file
	 * 
	 * @return String the value of a given data
	 * 
	 */
	public String getInput(String propertyName) {
		String value = null;
		try {
			BaseSuite.log.debug("Reading data property for - " + propertyName);
			value = prop.getProperty(propertyName).trim();
			BaseSuite.log.debug("Value is - " + value);
		} catch (Exception e) {
			BaseSuite.log.trace("Error encountered while for input + " + propertyName, e);
		}
		return value;
	}

	/**
	 * Method to read data from Excel for a given test in suite
	 * 
	 * @param suiteName
	 *        Excel file name for a given suite
	 * 
	 * @param testMethodName
	 *        Excel sheet name for a given test
	 * 
	 * @return object[][] two dimensional array of string data
	 */
	public Object[][] readExcel(String suiteName, String testMethodName) {
		BaseSuite.log.debug("Getting data from Excel file - " + suiteName);
		File excelFile = null;
		Object[][] data = new Object[0][0];

		try {
			String excelPath = null;
			String generateExcelPath;

			generateExcelPath = getFilePath(new File("./Data"), propertiesFile.replace(".properties", ".xlsx"));
			// if no '.xlsx' then check and get '.xls' data
			excelPath = (generateExcelPath != null) ? generateExcelPath
					: getFilePath(new File("./Data"), propertiesFile.replace(".properties", ".xls"));

			BaseSuite.log.debug("Suite Excel path is - " + excelPath);
			excelFile = new File(excelPath);

			BaseSuite.log.debug("Getting data from Excel sheet - " + testMethodName);

			data = ExcelUtils.importFromFile(excelFile, testMethodName);
			
		} catch (Exception e) {
			BaseSuite.log.trace("Not able to find the excel sheet for suite - " + suiteName, e);
		}
		
		return data;
	}

	/**
	 * Method to write data to Excel for a given test in suite
	 * 
	 * @param suiteName
	 *        Excel file name for a given suite
	 * 
	 * @param testMethodName
	 *        Excel sheet name for a given test
	 * 
	 * @param rowID
	 *        row id of the iteration to change the execute flag
	 * 
	 */
	public void writeExcel(String suiteName, String testMethodName, int rowID) {
		BaseSuite.log.debug("Writing data to Excel file - " + suiteName);
		File excelFile = null;
		try {
			String excelPath = null;
			String generateExcelPath;

			generateExcelPath = getFilePath(new File("./Data"), propertiesFile.replace(".properties", ".xlsx"));
			// if no '.xlsx' then check and get '.xls' data
			excelPath = (generateExcelPath != null) ? generateExcelPath
					: getFilePath(new File("./Data"), propertiesFile.replace(".properties", ".xls"));

			BaseSuite.log.debug("Suite Excel path is - " + excelPath);
			excelFile = new File(excelPath);
		} catch (Exception e) {
			BaseSuite.log.trace("Not able to find the excel sheet for suite - " + suiteName, e);
		}
		BaseSuite.log.debug("Writing data to Excel sheet - " + testMethodName);
		ExcelUtils.importToFile(excelFile, testMethodName, rowID);
	}

	/**
	 * To get the path of given file from a specified directory
	 * 
	 * @param dir
	 * @param fileName
	 * @return file path
	 */
	public static String getFilePath(File dir, String fileName) {

		// file path to be return
		String filePath = null;

		try {
			// get all the files and folders of given directory
			File[] files = dir.listFiles();

			// go through each file/folder
			for (File file : files) {

				// if it's a folder
				if (file.isDirectory()) {
					// recuresively go again for file
					filePath = getFilePath(file, fileName);

					// we get the desired filepath then exit loop
					if (filePath != null)
						break;
				} else { // if it's the file
					// check with the desired file name
					if (file.getName().equals(fileName))
						return file.getCanonicalPath();
				}
			}
		} catch (IOException e) {
			BaseSuite.log.fatal("Exception occured while generating data file path", e);
		}

		return filePath;
	}

}
