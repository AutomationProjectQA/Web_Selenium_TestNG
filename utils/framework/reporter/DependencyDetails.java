package framework.reporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class DependencyDetails {
	Model pomModel;
	Logger log;

	public DependencyDetails(Logger log) {
		this.log = log;
		// get the pom reader
		MavenXpp3Reader pomReader = new MavenXpp3Reader();
		// get the pom model to get the details fetching
		try {
			pomModel = pomReader.read(new FileInputStream(new File("./pom.xml")));
		} catch (FileNotFoundException e) {
			log.error("Pom.xml is not available to read the dependency", e);
		} catch (IOException | XmlPullParserException e) {
			log.error("Error occured while reading the pom.xml for feaching the dependency details", e);
		}
	}

	/**
	 * To get the given dependency version based on the artifact id of it
	 * 
	 * @param artifactID
	 *        String
	 * @return String
	 */
	public String getDependencyVersion(String artifactID) {
		String depVersion = Strings.EMPTY;

		log.debug("Started getting version of dependency " + artifactID);

		Optional<Dependency> dependency = pomModel.getDependencies().stream()
				.filter((Dependency dep) -> dep.getArtifactId().equals(artifactID)).findFirst();

		if (dependency.isPresent())
			depVersion = dependency.get().getVersion();
		else
			depVersion = "Not Available";

		log.debug("Ended getting version of dependency " + artifactID + " version - " + depVersion);

		return depVersion;
	}

}
