package framework.logging;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.Strings;

/*
 * Custom pattern for logs to be used in the PatternLayout
 * 
 * https://www.baeldung.com/log4j2-plugins
 */
@Plugin(name = "CustomPatternLayout", category = PatternConverter.CATEGORY)
@ConverterKeys({ "CustomPatternLayout" })
public class CustomPatternLayout extends LogEventPatternConverter {

	protected CustomPatternLayout(String name, String style) {
		super(name, style);
	}

	public static CustomPatternLayout newInstance(String[] options) {
		return new CustomPatternLayout("CustomPatternLayout", Strings.EMPTY);
	}

	@Override
	public void format(LogEvent event, StringBuilder toAppendTo) {
		// get the event
		toAppendTo.append(event.getLevel() + " ");

		// get the time
		toAppendTo.append(new SimpleDateFormat("HH:mm:ss:SSSS").format(event.getTimeMillis()) + " ");

		// get the logger
		String loggerName = event.getLoggerName();
		loggerName = loggerName.trim().length() == 0 ? "root" : loggerName;
		toAppendTo.append(loggerName + " ");

		// get the file: line
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

		// evaluate for re run based on the '' method which re-execute again
		boolean reRunTrace = Arrays.asList(stackTraceElements).stream().anyMatch(
				(StackTraceElement s) -> s.getMethodName().toString().contains("generateAndExecuteTestNGXML"));

		List<StackTraceElement> trace = Arrays.asList(stackTraceElements).stream()
				.filter((StackTraceElement s) -> s.getClassName().toString().contains("base.")
						|| s.getClassName().toString().contains("corelibrary.")
						|| s.getClassName().toString().contains("pages.")
						|| s.getClassName().toString().contains("suite.")
						|| s.getClassName().toString().contains("execution."))
				.collect(Collectors.toList());

		// for re run remove the last two stack
		if (reRunTrace & trace.size() > 1) {
			// remove 'generateAndExecuteTestNGXML' trace
			OptionalInt generateAndExecuteTestNGXMLTraceIndexOpt = IntStream.range(0, trace.size())
					.filter(i -> trace.get(i).getMethodName().equals("generateAndExecuteTestNGXML")).findFirst();
			if (generateAndExecuteTestNGXMLTraceIndexOpt.isPresent()) {
				trace.remove(generateAndExecuteTestNGXMLTraceIndexOpt.getAsInt());
				// remove 'tearDown' trace
				OptionalInt tearDownOpt = IntStream.range(0, trace.size())
						.filter(i -> trace.get(i).getMethodName().equals("tearDown")).findFirst();
				if (tearDownOpt.isPresent())
					trace.remove(tearDownOpt.getAsInt());
			}

		}

		// for showing the proper place of project file with line from trace of the log
		// event on console
		StackTraceElement fileLine;
		if (trace.size() == 1)
			fileLine = trace.get(0);
		else if (trace.size() == 2)
			fileLine = trace.get(1);
		else // this is important for project log
			fileLine = trace.get(trace.size() - 2);

		/*
		 * For console create link for file
		 * https://stackoverflow.com/questions/30928171/create-a-hyperlink-to-a-project-
		 * file-in-console-output
		 */
		String fileLineString = "(" + fileLine.getFileName() + ":" + fileLine.getLineNumber() + ") - "
				+ fileLine.getMethodName() + "()";
		toAppendTo.append(fileLineString);

		// get the message
		String message = (String) event.getMessage().getFormattedMessage();
		toAppendTo.append(" - " + message);

		toAppendTo.append("\r\n");
	}

}
