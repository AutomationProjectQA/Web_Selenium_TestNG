package framework.reporter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Description {

	/**
     * To create HTML from the description, mandatory format
     * (1) For Pre-condition : [1. Add your condition here]<br/>
     * (2) For Test Case Description : (1. Add your description here)<br/>
     * (3) For Steps : 1. Add your steps here. >> Add  your expected result here. <br/>
     * 
     * @param String description
     * @return String html
     */
	public String generate(String description) {

		// regular expressions
		String regexSteps = "^[0-9]+.+>>.*";
		String regexPreCondition = "^\\[[0-9]+.+\\]$";
		String regexForDescription = "^\\(+.+\\)$";
		String regexForNumber = "^[0-9]{1,2}";
		String regexForBR = "<br[ \\/]*>";
		String regexSqBracket = "[\\[\\]]";
		String regexRoundBracket = "[\\(\\)]";

		// required html
		String finalTableDescription = "<table class='desc-table'><tr class='header'><th class='testDescription'> Test Case Description </th></tr>%s</table>";
		String finalTablePreCondition = "<table class='desc-table'><tr class='header'><th class='no'>No.</th><th> Pre Requisites </th></tr>%s</table>";
		String finalTableSteps = "<table class='desc-table'><tr class='header'><th class='no'>No.</th><th class='steps'>Step Description</th><th>Step Expected</th></tr>%s</table>";
		String descriptions = "<tr><td class='testDescription'> %s </td></tr>";
		String preConditions = "<tr><td class='no'>%n</td><td> %s </td></tr>";
		String steps = "<tr><td class='no'>%n</td><td>%s</td><td>%e</td></tr>";
		String count = "<div class='count'><div id='stepsCount' class='authors'> <span id='%s' class='author text-white'> Steps Count - %s </span></div></div>";
		String readCountCombineElement = "<div class='after-desc-table'><div id='stepsCount' class='authors'> <span id='%s' class='author text-white'> Steps Count - %s </span></div><button class='btnReadMoreLessClass' id='btnReadMoreLessId'onclick='clickReadButton(this)'>Read More</button></div>";
		int noOfSteps = 0;

		// table data to be entered
		String descriptionTableData = "";
		String conditionsTableData = "";
		String stepsTableData = "";
		String finalResult = "";

		// other string
		String emptyWithSpace = " ";

		// split with br first
		String[] list = description.split(regexForBR);

		// iterate through each sentence and split and enter in the table data
		for (String scenario : list) {

			scenario = scenario.trim();
			boolean isPrecondition = Pattern.matches(regexPreCondition, scenario);
			boolean isStep = Pattern.matches(regexSteps, scenario);
			boolean isDescription = Pattern.matches(regexForDescription, scenario);

			String no = "";
			String step = "";
			String expectedResult = "";

			if (isDescription || (isPrecondition || isStep)) {

				if (isPrecondition) {

					// split the String and get no and step
					scenario = scenario.replaceAll(regexSqBracket, "");

					Pattern p = Pattern.compile(regexForNumber);
					Matcher m = p.matcher(scenario);
					if (m.find()) {
						no = m.group(0);
					}
					step = scenario.replaceAll(regexForNumber, "").replace(".", "").trim();

					// store the result in precondition data
					conditionsTableData = conditionsTableData
							+ preConditions.replaceAll("%n", no).replaceAll("%s", step);
				}

				if (isStep) {

					noOfSteps++;
					// split the and get no , step and expected result
					String[] stepDetail = scenario.split(">>");

					if (stepDetail.length <= 1) {
						scenario = scenario + emptyWithSpace;
						stepDetail = scenario.split(">>");
					}

					Pattern p = Pattern.compile(regexForNumber);
					Matcher m = p.matcher(stepDetail[0]);
					if (m.find()) {
						no = m.group(0);
					}
					step = stepDetail[0].replaceAll(regexForNumber, "").replace(".", "").trim();
					expectedResult = stepDetail[1];

					// store result in table data
					stepsTableData = stepsTableData
							+ steps.replaceAll("%n", no).replaceAll("%s", step).replaceAll("%e", expectedResult);
				}

				if (isDescription) {

					// split the String and get no and step
					scenario = scenario.replaceAll(regexRoundBracket, "");

					step = scenario.replace(".", "").trim();

					// store the result in description table data
					descriptionTableData = descriptionTableData + descriptions.replaceAll("%s", step);

				}
			}
			// if out of regular expression
			else {
				return description;
			}
		}

		// if there is no precondition and no description then add only steps table.
		if (conditionsTableData.isEmpty() && descriptionTableData.isEmpty()) {
			finalResult = finalTableSteps.replaceAll("%s", stepsTableData);
		}
		// if all things precondition, description and steps are present
		else if ((!conditionsTableData.isEmpty()) && (!descriptionTableData.isEmpty())) {
			finalResult = finalTableDescription.replaceAll("%s", descriptionTableData)
					+ finalTablePreCondition.replaceAll("%s", conditionsTableData)
					+ finalTableSteps.replaceAll("%s", stepsTableData);
		}
		// if there is any one of precondition or description is present
		else {
			if (conditionsTableData.isEmpty()) {
				finalResult = finalTableDescription.replaceAll("%s", descriptionTableData)
						+ finalTableSteps.replaceAll("%s", stepsTableData);
			}
			if (descriptionTableData.isEmpty()) {
				finalResult = finalTablePreCondition.replaceAll("%s", conditionsTableData)
						+ finalTableSteps.replaceAll("%s", stepsTableData);
			}
		}

		String readAndCount;
		if (noOfSteps > 5) {
			readAndCount = readCountCombineElement.replaceAll("%s", String.valueOf(noOfSteps));
		} else {
			readAndCount = count.replaceAll("%s", String.valueOf(noOfSteps));
		}
		return finalResult + readAndCount;
	}

}
