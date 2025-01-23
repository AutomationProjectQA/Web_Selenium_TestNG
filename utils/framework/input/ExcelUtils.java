package framework.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import base.BaseSuite;
import framework.LogUtil;

/**
 * 
 * Excel related functions to be used in Input
 *
 */
public class ExcelUtils {

	// for skipping the false execute iteration
	public static int executeIndex = 0;
	public static boolean executeAvailable = false;
	public static ArrayList<Integer> executeRowIds = new ArrayList<>();

	/**
	 * 
	 * To read a sheet for a given test for the iterations
	 * 
	 * @param fileName
	 *        excel file name which should be the same as suite name
	 * 
	 * @param sheetName
	 *        sheet name in excel file which should be same as test name
	 *        at @test annotation
	 * 
	 * @return returns two dimentional array containing iteration rows
	 */
	public static Object[][] importFromFile(File fileName, String sheetName) {
		Object[][] modelData = null;
		try {
			DataFormatter dataFormatter = new DataFormatter();

			InputStream file = new FileInputStream(fileName);

			// for reading xls & xlsx
			@SuppressWarnings("resource")
			Workbook workbook = WorkbookFactory.create(file);

			Sheet sheet = workbook.getSheet(sheetName);

			Iterator<Row> rowIterator = sheet.iterator();
			List<Object> columnNames = new ArrayList<Object>();

			Vector<Vector<Object>> tempData = new Vector<Vector<Object>>();
			while (rowIterator.hasNext()) {

				Row row = rowIterator.next();
				Iterator<Cell> cellIterator = row.cellIterator();

				// Treat first row in excel file as column names, column names
				// are assumed to be of type String

				if (row.getRowNum() == 0) {
					while (cellIterator.hasNext()) {
						Cell cell = cellIterator.next();
						columnNames.add(dataFormatter.formatCellValue(cell).trim());
					}
					// check if we have execute column for iteration
					if (columnNames.contains("execute")) {
						// set the flag
						executeAvailable = true;
						// get the column number
						executeIndex = columnNames.indexOf("execute");

					}
				} else {

					// get the value of execute cell and skip if false
					if (executeAvailable) {
						if (dataFormatter.formatCellValue(row.getCell(executeIndex)).trim().toLowerCase()
								.equals("false")) {
							continue;
						} else // store the row ids to use later to make column false from true
							executeRowIds.add(row.getRowNum());
					}

					Vector<Object> cellVals = new Vector<>();

					// go through cells based on columns given in first row to include null and
					// blank cells in data
					for (int i = 0; i < columnNames.size(); i++) {

						Cell cell = row.getCell(i);
						if (executeAvailable && (cell.getColumnIndex() == executeIndex)) {
							// skip the execute column
							continue;
						} else if (cell == null || cell.getCellType() == CellType.BLANK) {
							// This cell is empty
							cellVals.add("");
						} else {
							cellVals.add(dataFormatter.formatCellValue(cell).trim());
						}

					}

					tempData.add(cellVals);
				}
			}
			file.close();

			// if there is no iteration data
			// make execute column false as it affects next TC
			if (tempData.isEmpty())
				executeAvailable = false;

			modelData = to2DArray(tempData);

		} catch (IOException e) {
			BaseSuite.log.trace("Error encountered while reading the Excel for multiple data set", e);
		}

		return modelData;

	}

	/**
	 * 
	 * To write to a sheet for a given test for the iterations using execute column
	 * 
	 * @param fileName
	 *        excel file name which should be the same as suite name
	 * 
	 * @param sheetName
	 *        sheet name in excel file which should be same as test name
	 *        at @test annotation
	 * 
	 * @param rowID
	 *        row id of the iteration to change the execute flag
	 */
	public static void importToFile(File fileName, String sheetName, int rowID) {
		try {
			// get the sheet based on suite name/filename
			InputStream file = new FileInputStream(fileName);

			// get the file/workbook handle
			@SuppressWarnings("resource")
			Workbook workbook = WorkbookFactory.create(file); // will read xls & xlsx

			// get the sheet name based on the test name
			Sheet sheet = workbook.getSheet(sheetName);

			// get the row based on the id
			Row row = sheet.getRow(rowID);

			// take the execute column id and find the cell
			Cell cell = row.getCell(executeIndex);

			// change the value to false
			cell.setCellValue("false");

			// flush the data
			FileOutputStream fileOut = new FileOutputStream(fileName);

			// write this workbook to an Outputstream
			workbook.write(fileOut);
			fileOut.flush();
			fileOut.close();

			BaseSuite.log.debug("Changed execute value for row - " + rowID);
		} catch (IOException e) {
			BaseSuite.log.trace("Error encountered while reading the Excel for multiple data set", e);
		}
	}

	/**
	 * convert vector data into two dimension array object
	 * 
	 * @param v
	 *        vector object from excel
	 * 
	 * @return object[][]
	 * 
	 */
	public static Object[][] to2DArray(Vector<Vector<Object>> v) {
		BaseSuite.log.debug("Coverting read vector data into array object");
		Object[][] out = new Object[v.size()][0];
		for (int i = 0; i < out.length; i++) {
			out[i] = ((Vector<Object>) v.get(i)).toArray();
		}
		return out;
	}
}