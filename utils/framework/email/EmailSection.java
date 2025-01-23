package framework.email;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import base.BaseSuite;
import framework.LogUtil;
import framework.email.constant.BeforeAfterData;
import framework.email.constant.FrameworkData;

public class EmailSection {

	private Logger log;

	public EmailSection() {
		// for framework utilize the BaseSuite initialize logger else create new for
		// using it separately
		log = BaseSuite.log == null ? new LogUtil(Strings.EMPTY, Strings.EMPTY).logger : BaseSuite.log;
	}

	/**
	 * To get the given html file content
	 * 
	 * @param filepath .html file path
	 * @return String
	 */
	private String htmlToString(String filepath) {
		String result = "";
		try {
			// read file
			StringBuilder html = new StringBuilder();
			// InputStream inputStream = getClass().getResourceAsStream(filepath);

			FileReader fr = new FileReader(filepath);
			BufferedReader br = new BufferedReader(fr);

			String val;

			// Reading the String till we get the null
			// string and appending to the string
			try {
				while ((val = br.readLine()) != null) {
					html.append(val);
				}
			} catch (IOException e) {
				log.error("IO error occured while getting the html from file " + filepath, e);
			}

			result = html.toString();

			log.debug("Retrieved html from file " + filepath);
		} catch (Exception e) {
			log.error("Error while getting/generating the html from file " + filepath, e);
		}
		return result;
	}

	/**
	 * 
	 * To create email template of beforeExecution.
	 * 
	 * Note: For the value of the map provide empty string in case of no data it
	 * will replace with 'NA'. If you do not want 'NA' then pass space(" ").
	 * 
	 * @param data This is EnumMap of the BeforeAfterExecution enum i.e.
	 *             EnumMap<BeforeAfterExecution,String> beforeExecution =new
	 *             EnumMap<>(BeforeAfterExecution.class)
	 * 
	 * @return String of beforeExecution template
	 */
	public String beforeExecution(EnumMap<BeforeAfterData, String> data) {
		// initiate variable
		String beforeExecutionHTML = "";

		try {
			ArrayList<BeforeAfterData> keys = new ArrayList<>();

			String htmlTemplate = htmlToString("/beforeExecution.html");

			if (!htmlTemplate.isEmpty()) {

				// replace empty value with 'NA'
				for (Map.Entry<BeforeAfterData, String> mapElement : data.entrySet()) {
					BeforeAfterData key = mapElement.getKey();
					if (data.get(key).isEmpty()) {
						data.put(key, "NA");
					}
					keys.add(key);
				}

				// replace string
				beforeExecutionHTML = htmlTemplate.replace(keys.get(0).toString(), data.get(keys.get(0)))
						.replace(keys.get(1).toString(), data.get(keys.get(1)))
						.replace(keys.get(2).toString(), data.get(keys.get(2)))
						.replace(keys.get(3).toString(), data.get(keys.get(3)))
						.replace(keys.get(4).toString(), data.get(keys.get(4)))
						.replace(keys.get(5).toString(), data.get(keys.get(5)));
			}

			log.debug("Generated the html template for beforeExecution with data - " + data);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - beforeExecution with data - "
					+ data, e);
		}

		return beforeExecutionHTML;
	}

	/**
	 * 
	 * To create email template of AfterExecution.
	 * 
	 * Note: For the value of the map provide empty string in case of no data it
	 * will replace with 'NA'. If you do not want 'NA' then pass space(" ").
	 * 
	 * @param data This is EnumMap of the BeforeAfterExecution enum i.e.
	 *             EnumMap<BeforeAfterExecution,String> afterExecution =new
	 *             EnumMap<>(BeforeAfterExecution.class)
	 * 
	 * @return String of after execution template
	 */
	public String afterExecution(EnumMap<BeforeAfterData, String> data) {
		// initiate variable
		String afterExecutionHTML = "";

		try {
			// array list of BeforeAfterExecution enum
			ArrayList<BeforeAfterData> keys = new ArrayList<>();

			// read file
			String htmlTemplate = htmlToString("/afterExecution.html");

			if (!htmlTemplate.isEmpty()) {

				// replace empty string with 'NA'
				for (Map.Entry<BeforeAfterData, String> mapElement : data.entrySet()) {

					BeforeAfterData key = mapElement.getKey();
					if (data.get(key).isEmpty()) {
						data.put(key, "NA");
					}
					keys.add(key);
				}

				afterExecutionHTML = htmlTemplate.replace(keys.get(0).toString(), data.get(keys.get(0)))
						.replace(keys.get(1).toString(), data.get(keys.get(1)))
						.replace(keys.get(2).toString(), data.get(keys.get(2)))
						.replace(keys.get(3).toString(), data.get(keys.get(3)))
						.replace(keys.get(4).toString(), data.get(keys.get(4)))
						.replace(keys.get(5).toString(), data.get(keys.get(5)));
			}

			log.debug("Generated the html template for beforeExecution with data - " + data);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - afterExecution with data - "
					+ data, e);
		}

		return afterExecutionHTML;
	}

	/**
	 * 
	 * To create email template of the framework.
	 * 
	 * Note: For the value of the map provide empty string in case of no data it
	 * will replace with 'NA'. If you do not want 'NA' then pass space(" ").
	 * 
	 * @param data This is EnumMap of Framework enum i.e.EnumMap<Framework, String>
	 *             data = new EnumMap<>(Framework.class)
	 * 
	 * @return String of framework email template.
	 */
	public String frameworkDefault(EnumMap<FrameworkData, String> data) {
		// initiate variable
		String frameworkDefaultHTML = "";

		try {
			// array list of framework enum
			ArrayList<FrameworkData> keys = new ArrayList<>();

			String htmlTemplate = htmlToString("framework.html");

			if (!htmlTemplate.isEmpty()) {

				// replace empty string with 'NA'
				for (Map.Entry<FrameworkData, String> mapElement : data.entrySet()) {

					FrameworkData key = mapElement.getKey();
					if (data.get(key).isEmpty()) {
						data.put(key, "NA");
					}
					keys.add(key);
				}

				// replace string and get email template
				frameworkDefaultHTML = htmlTemplate.replace(keys.get(0).toString(), data.get(keys.get(0)))
						.replace(keys.get(1).toString(), data.get(keys.get(1)))
						.replace(keys.get(2).toString(), data.get(keys.get(2)))
						.replace(keys.get(3).toString(), data.get(keys.get(3)))
						.replace(keys.get(4).toString(), data.get(keys.get(4)))
						.replace(keys.get(5).toString(), data.get(keys.get(5)));
			}

			log.debug("Generated the html template for frameworkDefault with data - " + data);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - frameworkDefault with data - "
					+ data, e);
		}

		return frameworkDefaultHTML;
	}

	/**
	 * 
	 * TO create email template of beforeExecution without data.
	 *
	 * @param text String of text which user want to send in email.
	 * 
	 * @return String of beforeExecution without data email template.
	 */
	public String beforeExecutionWithoutData(String text) {
		// initiate variable
		String beforeExecutionWithoutDataHTML = "";

		try {
			// read file
			String htmlTemplate = htmlToString("beforeExecutionWithoutData.html");

			if (!htmlTemplate.isEmpty()) {

				beforeExecutionWithoutDataHTML = htmlTemplate.replace("text", text);
			}

			log.debug("Generated the html template for beforeExecutionWithoutData with text - " + text);
		} catch (Exception e) {
			log.error(
					"Error occured while getting/generating html for email section - beforeExecutionWithoutData with text - "
							+ text,
					e);
		}

		return beforeExecutionWithoutDataHTML;
	}

	/**
	 * 
	 * TO create email template of afterExecution without data.
	 *
	 * @param text String of text which user want to send in email.
	 * 
	 * @return String of afterExecution without data email template.
	 */
	public String afterExecutionWithoutData(String text) {
		// initiate variable
		String afterExecutionWithoutDataHTML = "";

		try {

			// read file
			String htmlTemplate = htmlToString("/afterExecutionWithoutData.html");

			// readTxtFile("afterExecutionWithoutData.txt");

			if (!htmlTemplate.isEmpty()) {

				afterExecutionWithoutDataHTML = htmlTemplate.replace("text", text);

			}

			log.debug("Generated the html template for afterExecutionWithoutData with text - " + text);
		} catch (Exception e) {
			log.error(
					"Error occured while getting/generating html for email section - afterExecutionWithoutData with text - "
							+ text,
					e);
		}

		return afterExecutionWithoutDataHTML;
	}

	/**
	 * 
	 * To create email template of simple text.
	 * 
	 * @param text String of text which user want to send in email.
	 * 
	 * @return String of text email template.
	 */
	public String plainText(String text) {
		// initiate variable
		String plainTextHTML = "";

		try {
			// read file
			String htmlTemplate = htmlToString("/text.html");

			if (!htmlTemplate.isEmpty()) {

				plainTextHTML = htmlTemplate.replace("text", text);
			}

			log.debug("Generated the html template for plainText with text - " + text);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - plainText with text - " + text,
					e);
		}

		return plainTextHTML;
	}

	/**
	 * 
	 * TO create email template of button.
	 * 
	 * @param url String of the url.
	 * 
	 * @return String of button email template.
	 */
	public String button(String url) {
		// initiate variable
		String buttonHTML = "";

		try {
			// read file
			String htmlTemplate = htmlToString("/button.html");

			if (!htmlTemplate.isEmpty()) {

				buttonHTML = htmlTemplate.replace("report url", url);
			}

			log.debug("Generated the html template for button with utl - " + url);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - button with url - " + url, e);
		}

		return buttonHTML;
	}

	/**
	 * 
	 * To create email template of heading.
	 * 
	 * @param heading String of the text which user want to add in heading
	 * 
	 * @return String of heading email template.
	 */
	public String heading(String heading) {
		// initiate variable
		String headingHTML = "";

		try {
			// read file
			String htmlTemplate = htmlToString("/heading.html");

			if (!htmlTemplate.isEmpty()) {

				headingHTML = htmlTemplate.replace("heading", heading);
			}

			log.debug("Generated the html template for heading with text - " + heading);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - heading with text - " + heading,
					e);
		}
		return headingHTML;
	}

	/**
	 * This method is used to make ArrayList of LinkedHashMap from two array(one is
	 * column name array and second is table data array).
	 * 
	 * @param columns array of column name
	 * @param data    array of table data if there is no data then pass empty string
	 * 
	 * @return ArrayList of LinkedHashMap
	 */
	public ArrayList<LinkedHashMap<String, String>> makeList(String[] columns, String[] data) {
		// make array list of linked hash map
		ArrayList<LinkedHashMap<String, String>> table = new ArrayList<>();

		try {
			for (int i = 0; i < data.length;) {
				// make linked hash map
				LinkedHashMap<String, String> mapData = new LinkedHashMap<>();

				for (int j = 0; j < columns.length; j++) {
					mapData.put(columns[j], data[i++]);
				}

				// add hash map in array list
				table.add(mapData);
			}

			log.debug("Transformed given columns - " + columns + " with data - " + data
					+ "to the ArrayList<LinkedHashMap<String, String>>");
		} catch (Exception e) {
			log.error("Error occured while making list for columns - " + columns + " with data - " + data, e);
		}

		return table;
	}

	/**
	 * This method is used to make ArrayList of LinkedHashMap from one 2D array(In
	 * this 2D array first array element must be column name).
	 * 
	 * @param data 2D array in which first array element is column name and reset of
	 *             other are table data. If there is not data then pass empty
	 *             string.
	 * 
	 * @return ArrayList of LinkedHashMap
	 */
	public ArrayList<LinkedHashMap<String, String>> makeList(String[][] data) {
		// make array list of linked hash map
		ArrayList<LinkedHashMap<String, String>> table = new ArrayList<>();

		try {
			for (int i = 1; i < data.length; i++) {

				// make linked hash map
				LinkedHashMap<String, String> mapData = new LinkedHashMap<>();

				for (int j = 0; j < data[0].length; j++) {
					mapData.put(data[0][j], data[i][j]);
				}
				// add hash map in array list
				table.add(mapData);
			}

			log.debug("Transformed given 2d array data - " + data + "to the ArrayList<LinkedHashMap<String, String>>");
		} catch (Exception e) {
			log.error("Error occured while making list from 2d array data - " + data, e);
		}

		return table;
	}

	/**
	 * 
	 * To create email template of list. Take LinkedHashMap with key and value for
	 * list.
	 *
	 * Note: Use LinkedHashMap i.e. LinkedHashMap<String, String> listData = new
	 * LinkedHashMap<>() If you want to add link than pass link text and link
	 * as("link text $ link") in value. If you want to add list in value then pass
	 * '$' separated elements in string.
	 * 
	 * @param data Linked hash map with key and value of list
	 * 
	 * @return String of list email template
	 */
	public String list(LinkedHashMap<String, String> data) {
		// initiate variable
		String listHTML = "";
		String listValue = "";
		String row = "";

		try {
			// read file
			String htmlTemplate = htmlToString("/list.html");

			if (!htmlTemplate.isEmpty()) {

				listValue = listValue + "<ul style='float: left; margin-top:0px;margin-bottom:0px;'>";

				for (Map.Entry<String, String> mapElement : data.entrySet()) {
					// get key
					String key = mapElement.getKey();

					if (data.get(key).contains("$")) {

						// create list of all the elements which are separated by '$'
						ArrayList<String> listData = new ArrayList<String>(Arrays.asList(data.get(key).split("\\$")));

						row = "";

						// add link
						if (listData.get(1).contains(":") && listData.get(1).contains("http")) {
							row = row + "<a style='color:#7b24ac ;' href='" + listData.get(1) + "'>" + listData.get(0)
									+ "</a>";
						} else {
							row = row + "<br/><ul>";

							// make list
							for (String element : listData) {
								if (!element.trim().isEmpty()) {
									row = row + "<li style='text-align:left;'>" + element + "</li>";
								}
							}
							row = row + "</ul>";
						}
					} else {
						row = data.get(key);
					}
					// make list of key and value
					listValue = listValue + "<li> <p>" + key + ": " + "<b>" + row + "</b></p></li>";
				}
				listValue = listValue + "</ul><br/>";

				listHTML = listHTML + htmlTemplate.replace("keyCol", listValue);
			}

			log.debug("Generated the html template for list with data - " + data);
		} catch (Exception e) {
			log.error("Error occured while getting/generating html for email section - list with data - " + data, e);
		}

		return listHTML;
	}

	/**
	 * 
	 * To create email template of table. If there is not data in particular cell
	 * then it will pass 'NA'. If you do not want 'NA' then pass space(" ").
	 * 
	 * Note: If you want to add link in particular cell than pass link text and link
	 * as("link text $ link"). If you want to add list in particular cell then pass
	 * '$' separated elements in string.
	 * 
	 * @param tableData ArrayList of the linkedHashMap(In LinkedHashMap keys are the
	 *                  column name and values are the data)
	 * @param fullWidth This is a optional boolean parameter.If you want table width
	 *                  '100%' then make this parameter true.
	 * 
	 * @return String of table email template
	 */
	public String table(ArrayList<LinkedHashMap<String, String>> tableData, boolean... fullWidth) {

		// initiate variable
		String finalHtmlTemplate = "";
		String tableHead = "";
		String htmlTemplate = "";
		// read file
		String template = htmlToString("/table.html");

		// set the the width of the table according to parameter passed
		if (fullWidth.length > 0 && fullWidth[0]) {

			// if fullWidth if true the set the width to 100%
			htmlTemplate = template.replace("widthValue", "100%");
		} else {
			htmlTemplate = template.replace("widthValue", "auto");
		}

		if (!htmlTemplate.isEmpty()) {

			// get column name from the list
			ArrayList<String> keyList = new ArrayList<String>(tableData.get(0).keySet());

			// create table head
			tableHead = tableHead + "<tr>";

			for (String i : keyList) {
				tableHead = tableHead + "<th>" + i + "</th> ";
			}
			tableHead = tableHead + "</tr>";

			// for get the rows
			String tableRow = "";

			// for making the table rows
			for (int i = 0; i < tableData.size(); i++) {

				// store single row data in the map
				LinkedHashMap<String, String> rows = tableData.get(i);

				tableRow = tableRow + "<tr> ";
				for (String key : keyList) {

					// check for the empty cell and replace with 'NA'
					if (rows.get(key).isEmpty()) {
						rows.replace(key, "NA");
					}

					String row = rows.get(key);

					// check if the cell data contains list and make list of the data
					if (rows.get(key).contains("$")) {

						// create list of all the elements which are separated by '$'
						ArrayList<String> listData = new ArrayList<String>(Arrays.asList(rows.get(key).split("\\$")));

						row = "";

						// add link in the table
						if (listData.get(1).contains(":") && listData.get(1).contains("http")) {
							row = row + "<a style='color:#7b24ac ;' href='" + listData.get(1) + "'>" + listData.get(0)
									+ "</a>";
						} else {
							row = row + "<ul>";

							for (String element : listData) {
								if (!element.trim().isEmpty()) {
									row = row + "<li style='text-align:left;'>" + element + "</li>";
								}
							}
							row = row + "</ul>";
						}
					}
					tableRow = tableRow + "<td style='max-width: 700px; line-height: 1.5'>" + row + "</td> ";
				}
				tableRow = tableRow + "</tr>";
			}

			finalHtmlTemplate = finalHtmlTemplate + htmlTemplate.replace("column", tableHead).replace("rows", tableRow);
		}
		return finalHtmlTemplate;
	}

}
