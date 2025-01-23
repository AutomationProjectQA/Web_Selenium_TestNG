package framework.reporter;

/**
 * For using it for exitTest()
 *
 */
public enum ResultType {

	FAIL, WARNING;

	public boolean compareTo(String input) {
		return (this.name().equalsIgnoreCase(input) ? true : false);
	}
}
