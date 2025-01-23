package framework.logging;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.util.Transform;
import org.apache.logging.log4j.util.Strings;

/**
 * Ref: For custom layout:
 * https://stackoverflow.com/questions/44005200/log4j2-custom-layout For getting
 * location info:
 * https://github.com/apache/logging-log4j2/blob/42aa6aeb54a2d179b0271c09b450ca3d18c3a7a8/log4j-core/src/main/java/org/apache/logging/log4j/core/layout/HtmlLayout.java#L239
 *
 */
@Plugin(name = "CustomHTMLLayout", elementType = Layout.ELEMENT_TYPE, printObject = true, category = Node.CATEGORY, deferChildren = true)
public class CustomHTMLLayout extends AbstractStringLayout {

	protected CustomHTMLLayout(Charset charset) {
		super(charset);
	}

	@Override
	public String toSerializable(LogEvent event) {
		StringBuffer sbuf = new StringBuffer();

		sbuf.append("<tr>");

		// time
		sbuf.append("<td>");
		sbuf.append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss:SSSS").format(event.getTimeMillis()));
		sbuf.append("</td>" + Strings.LINE_SEPARATOR);

		// level
		Level logLevel = event.getLevel();
		sbuf.append("<td class='" + logLevel + "'>");
		sbuf.append(logLevel);
		sbuf.append("</td>" + Strings.LINE_SEPARATOR);

		// Logger
		sbuf.append("<td>");
		String loggerName = event.getLoggerName();
		loggerName = loggerName.trim().length() == 0 ? "root" : loggerName;
		sbuf.append(loggerName);
		sbuf.append("</td>" + Strings.LINE_SEPARATOR);

		// for getting code details
		final StackTraceElement element = event.getSource();

		// File : Line
		String firstStack = "";
		String logStackDescription = Strings.EMPTY;
		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

		// evaluate for re run based on the '' method which re-execute again
		boolean reRunTrace = Arrays.asList(stackTraceElements).stream().anyMatch(
				(StackTraceElement s) -> s.getMethodName().toString().contains("generateAndExecuteTestNGXML"));

		List<StackTraceElement> trace = Arrays.asList(stackTraceElements).stream()
				.filter((StackTraceElement s) -> s.getClassName().toString().contains("base.")
						|| s.getClassName().toString().contains("corelibrary.")
						|| s.getClassName().toString().contains("pages.")
						|| s.getClassName().toString().contains("suite."))
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

		// look through to create the sequence to reach logger
		for (int i = trace.size() - 1; i > -1; i--) {
			StackTraceElement stackTraceElement = trace.get(i);
			String methodWithLine = stackTraceElement.getMethodName() + "():" + stackTraceElement.getLineNumber();
			;
			String currentStack = stackTraceElement.getClassName() + "." + methodWithLine;
			logStackDescription += currentStack;

			if (i == trace.size() - 1)
				firstStack = methodWithLine;

			if (i > 0)
				logStackDescription += " >> ";
		}

		String finalStack = element.getClassName() + "." + element.getMethodName() + "()" + ":"
				+ element.getLineNumber();

		if (!logStackDescription.contains(finalStack))
			logStackDescription += " >> " + finalStack;

		// create HTML link for show casing the alert with stack of the statement for
		// understanding the place of it
		String fileLineStack = "<a href=\"javascript:void(0);\" style=\"font-weight: bold; color: black !important;\" onclick=\"alert('"
				+ logStackDescription + "')\" title=\"" + logStackDescription + "\">" + firstStack + "</a>";

		sbuf.append("<td>");
		sbuf.append(fileLineStack);
		sbuf.append("</td>");

		// for log message
		sbuf.append("<td>");
		String message = (String) event.getMessage().getFormattedMessage();
		sbuf.append(message); // for before stacktrace given user message

		// for stack-trace in the logs
		if (event.getThrown() != null) {
			Throwable error = event.getThrown();

			sbuf.append("<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : medium");
			sbuf.append(";\" colspan=\"5\">");
			appendThrowableAsHtml(error, sbuf);
		}
		sbuf.append("</td>" + Strings.LINE_SEPARATOR);

		sbuf.append("</tr>");

		return sbuf.toString();
	}

	@PluginFactory
	public static CustomHTMLLayout createLayout(
			@PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset) {
		return new CustomHTMLLayout(charset);
	}

	@Override
	public byte[] getHeader() {
		return ("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n"
				+ "<html>\n" + "<head>\n" + "<meta charset=\"UTF-8\"/>\n" + "<title>Automation Execution Logs</title>\n"
				+ "<style type=\"text/css\">\n"
				+ "td[class] {font-weight: bold;}  .DEBUG {color: green;} .INFO {color: blue;}\n .WARN { color: orange;}\n .ERROR {color: red; }\n <!--\n"
				+ "body, table {font-family:arial,sans-serif; font-size: medium\n"
				+ "; table-layout: auto; width: 100%; }th {background: #336699; color: #FFFFFF; text-align: center; }td {word-wrap: break-word}\n"
				+ "-->\n" + "</style>\n" + "</head>\n" + "<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">\n"
				+ "<hr size=\"1\" noshade=\"noshade\">\n" + "<br>\n"
				+ "<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">\n"
				+ "<tr>\n" + "<th>Time</th>\n" + "<th>Level</th>\n" + "<th>Logger</th>\n" + "<th>File:Line</th>\n"
				+ "<th>Message</th>\n" + "</tr>").getBytes();
	}

	@Override
	public byte[] getFooter() {
		return ("</tbody>\n </table>\n </body>\n </html>").getBytes();
	}

	private static final String TRACE_PREFIX = "<br />&nbsp;&nbsp;&nbsp;&nbsp;";

	private void appendThrowableAsHtml(final Throwable throwable, final StringBuffer sbuf) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		try {
			throwable.printStackTrace(pw);
		} catch (final RuntimeException ex) {
			// Ignore the exception.
		}
		pw.flush();
		final LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
		final ArrayList<String> lines = new ArrayList<>();
		try {
			String line = reader.readLine();
			while (line != null) {
				lines.add(line);
				line = reader.readLine();
			}
		} catch (final IOException ex) {
			if (ex instanceof InterruptedIOException) {
				Thread.currentThread().interrupt();
			}
			lines.add(ex.toString());
		}
		boolean first = true;
		for (final String line : lines) {
			if (!first) {
				sbuf.append(TRACE_PREFIX);
			} else {
				first = false;
			}
			sbuf.append(Transform.escapeHtmlTags(line));
			sbuf.append(Strings.LINE_SEPARATOR);
		}
	}
}
