package framework;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriverException;

public class Encrypt {

	public static void main(String[] args) {

		String filePath = "./ecrypt.txt";

		// get the data
		String passwords[] = getStringData(filePath).split(",");

		// empty the file
		saveData(filePath, "", false);

		Map<String, String> encryptedMap = new HashMap<>();

		// loop through to generate the ecrypted password
		for (int i = 0; i < passwords.length; i++) {
			String password = passwords[i].trim();
			String encrypted = Cyfer.encrypt(password, "qcautomation@mem");
			encryptedMap.put(password, encrypted);
			saveData(filePath, password + "," + encrypted + "\n", true);
		}

	}

	/**
	 * Reading data from text file
	 * 
	 * @param filePath
	 *            Path of text file to read data
	 *
	 * @return String Text file data as String
	 * 
	 */
	public static String getStringData(String filePath) {

		// set buffer size
		byte[] contents = new byte[1024];

		// variable to hold byte index
		int bytesRead = 0;

		// variable to store file data
		String strFileData = null;

		File file = new File(filePath);

		try {
			// check file availability
			if (file.exists()) {

				// check if file is readable
				if (file.canRead()) {

					// file input stream creation
					FileInputStream fin = new FileInputStream(filePath);

					// buffer creation
					BufferedInputStream bin = new BufferedInputStream(fin);

					// read till end of file and store as string
					while ((bytesRead = bin.read(contents)) != -1) {

						strFileData = new String(contents, 0, bytesRead);
					}

					// close file
					fin.close();
					bin.close();

					System.out.println("Data is read from text file");

				} else {
					System.out.println("File is not readable");
				}
			} else {
				System.out.println("No such file exists");
			}

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found - " + e);

		} catch (IOException e) {
			System.out.println("Exception occurred in reading file - " + e);
		} catch (WebDriverException e) {
			System.out.println("Exception occurred while reading file - " + e);
		}

		return strFileData;
	}// end of readDataFromTxt

	/**
	 * Writing data in text file
	 * 
	 * @param filePath
	 *            Path of text file to write
	 *
	 * @param strText
	 *            Data to store in text file
	 * 
	 * @param appendFlag
	 *            Flag to clear or append the data in existing file.
	 *
	 */
	public static boolean saveData(String filePath, String strText, boolean appendFlag) {

		BufferedOutputStream bufferedOutput = null;

		try {

			File file = new File(filePath);

			// check file availability
			if (file.exists()) {

				// check if file is writable
				if (file.canWrite()) {

					// Output Buffer creation
					bufferedOutput = new BufferedOutputStream(new FileOutputStream(filePath, appendFlag));

					// writing string to file
					bufferedOutput.write(strText.getBytes());

					// close file
					bufferedOutput.close();

					System.out.println("Writing in text file is completed successfully");

					return true;

				} else {
					System.out.println("File is not writable");

					return false;
				}
			} else {
				System.out.println("No such file exists");

				return false;
			}
		} catch (WebDriverException e) {
			System.out.println("Exception occurred in writing in file" + e);

			return false;
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found" + e);
		} catch (IOException e) {
			System.out.println("Exception occurred in reading file" + e);
		}
		return false;

	}// end of writeDataToTxt

}
