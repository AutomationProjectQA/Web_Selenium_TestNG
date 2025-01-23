package execution.operations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import framework.Cyfer;
import framework.input.Configuration;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

public class UploadOperation extends CommonOperations {

	/**
	 * To upload the report to AXES portal
	 * 
	 */
	public boolean uploadReport(String reportPath) {
		Operations.log.debug("Started uploading the report");

		boolean isUploaded = false;

		// upload the report to the portal
		// get the portal config
		String CRUDreportPortalURL = Configuration.getProperty("CRUDreportPortalURL");

		// if portal url available then upload and send email based on config
		if (CRUDreportPortalURL != null && !CRUDreportPortalURL.trim().isEmpty()) {

			// store the report path for server
			String reportPortalFolder = Configuration.getProperty("reportPortalFolder")
					+ Configuration.getProperty("clientCode");

			// if uploaded email it out as per the configuration
			isUploaded = uploadReportToServer(CRUDreportPortalURL, reportPortalFolder, reportPath);

		} else {
			Operations.log.error("Report portal URL is not available");
		}

		Operations.log.debug("Ended uploading the report");

		return isUploaded;
	}

	/**
	 * To create report zip > upload it to server and delete the zip
	 * 
	 * @param apiURL           - API URL in the format 'http://host:port/endpoint'
	 * 
	 * @param serverFolderPath - Folder hierarchy in the reports folder on server
	 * 
	 * @param reportFolderPath - Path to zip file to be uploaded
	 * 
	 * @return boolean
	 */
	private boolean uploadReportToServer(String apiURL, String serverFolderPath, String reportFolderPath) {

		Operations.log.debug("Started uploading the report to server");

		boolean isUploaded = false;

		// zip file path
		String zipFilePath = reportFolderPath + ".zip";

		// create & upload the zip
		if (createZipFile(reportFolderPath, zipFilePath) && uploadZipFile(apiURL, serverFolderPath, zipFilePath)) {
			// delete
			deleteZipFile(zipFilePath);

			isUploaded = true;
		}

		Operations.log.debug("Ended uploading the report to server");

		return isUploaded;
	}

	/**
	 * Uploads the zip file from the local machine to the given folder on the server
	 * using multipart POST request.
	 * 
	 * @param apiURL           - API URL in the format 'http://host:port/endpoint'
	 * @param serverFolderPath - Folder hierarchy in the reports folder on server
	 * @param zipFilePath      - Path to zip file to be uploaded
	 * 
	 * 
	 * @return boolean
	 * 
	 */
	private boolean uploadZipFile(String apiURL, String serverFolderPath, String zipFilePath) {

		Operations.log.debug("Started uploading zip file");

		boolean isUploaded = false;

		try {

			// Initiate HttpClient object
			HttpClient httpclient = new DefaultHttpClient();

			// Initiate HttpPost object with apiURL in the format
			// 'http://host:port/endpoint'
			HttpPost httpPost = new HttpPost(apiURL);

			// Initiate MultiPartEntity object using
			MultipartEntity reqEntity = new MultipartEntity();

			// Add parts for zip file and foldername to MultiPartEntity object and set it to
			// request
			reqEntity.addPart("file", new FileBody(new File(zipFilePath), "application/x-zip-compressed"));
			reqEntity.addPart("reportDirectory", new StringBody(serverFolderPath));
			reqEntity.addPart("userID", new StringBody(System.getProperty("user.name")));
			httpPost.setEntity(reqEntity);

			// Execute POST request and get the response.
			isUploaded = sendRequest(httpPost);
		} catch (Exception e) {
			Operations.log.error("Failed to upload zip file", e);
		}

		Operations.log.debug("Ended uploading zip");

		return isUploaded;

	}

	/**
	 * 
	 * Send the request
	 * 
	 * @param httpPost request object
	 * @return request successful or not
	 */
	private boolean sendRequest(HttpPost httpPost) {

		boolean isResponseSend = false;

		try {
			Operations.log.debug("Started creating Email request");

			// attaching the certificate
			System.setProperty("javax.net.ssl.trustStore", Configuration.getProperty("certificatesTrustStorePath"));

			HttpClient httpclient = HttpClientBuilder.create().build();

			httpPost.setHeader("Authorization", "Bearer " + getToken());

			Operations.log.debug("Started execute post request and get the response");

			// Execute POST request and get the response.
			HttpResponse response = httpclient.execute(httpPost);

			Operations.log.debug("Ended execute post request and get the response");

			if (!response.getStatusLine().toString().contains("200")) {

				generateToken();

				// add token again
				httpPost.setHeader("Authorization", "Bearer " + getToken());

				// Execute POST request and get the response.
				response = httpclient.execute(httpPost);
			}

			// Response status for reference
			Operations.log.debug(response.getStatusLine().toString());

			isResponseSend = response.getStatusLine().toString().contains("200");

			Operations.log.debug("Ended creating Email request");
		} catch (Exception e) {
			Operations.log.error("Failed to send the request", e);
		}

		return isResponseSend;
	}

	/**
	 * Read the token from txt file from configuration accessTokenFilePath
	 * 
	 * @return token string
	 */
	private String getToken() {
		String token = "";

		Operations.log.debug("Started getting access token");

		try {
			if (!new File(Configuration.getProperty("accessTokenFilePath")).exists())
				generateToken();
			token = new String(Files.readAllBytes(Paths.get(Configuration.getProperty("accessTokenFilePath"))));
		} catch (IOException e) {
			Operations.log.error("Failed to get Token from file", e);
		}

		Operations.log.debug("Ended getting access token");

		return token;
	}

	/**
	 * Generate the token and store to the access_token file in accessTokenFilePath
	 * directory
	 */
	private void generateToken() {
		try {
			Operations.log.debug("Started generating access token");

			// attaching the certificate
			System.setProperty("javax.net.ssl.trustStore", Configuration.getProperty("certificatesTrustStorePath"));

			// create json object for request
			JSONObject jsonObj = new JSONObject();

			// check if 'key' available to encrypt the password
			String encryptionKey = Configuration.getProperty("key");
			String password;
			if (encryptionKey != null && !encryptionKey.trim().isEmpty())
				password = Cyfer.decrypt(Configuration.getProperty("tokenPassword"), encryptionKey);
			else
				password = Configuration.getProperty("tokenPassword");

			jsonObj.put("username", Configuration.getProperty("tokenUserName"));
			jsonObj.put("password", password);

			// create entity
			StringEntity entity = new StringEntity(jsonObj.toString(), ContentType.APPLICATION_JSON);

			// send the request
			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(Configuration.getProperty("axesTokenGenerateAPI"));
			request.setEntity(entity);

			// set the header
			request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

			// fetch the response
			HttpResponse response = httpClient.execute(request);

			String content = EntityUtils.toString(response.getEntity());

			Operations.log.debug("First calling of generating token API response - " + content);

			// if response is not with 200 status resend request
			if (!response.getStatusLine().toString().contains("200")) {
				Operations.log.debug("First request is not with status 200, calling request again");
				response = httpClient.execute(request);
				content = EntityUtils.toString(response.getEntity());
				Operations.log.debug("Second calling of generating token API response - " + content);
			}

			// store access token
			String encodedToken = "";
			if (response.getStatusLine().toString().contains("200")) {
				Operations.log.debug("Generate token API responce before storing to file - " + content);

				// create the json object of content and fetch the token from it
				org.json.JSONObject jsonob = new org.json.JSONObject(content);

				encodedToken = jsonob.get("accessToken").toString();
				Operations.log.debug("Access token generated successfully");
			} else {
				Operations.log.error("Failed to get access token in second request too");
			}

			// write the token to the file
			FileWriter myWriter = new FileWriter(Configuration.getProperty("accessTokenFilePath"));
			myWriter.write(encodedToken);
			myWriter.close();

			Operations.log.debug("Ended generating access token");
		} catch (IOException e) {
			Operations.log.error("Failed while writing the token to file", e);
		}
	}

	/**
	 * Creates new zip file from the given folder.
	 * 
	 * @param folderPath - Path to the folder to be archived
	 * @param zipPath    - Path where the zip file is to be placed
	 * 
	 * @return boolean
	 */
	private boolean createZipFile(String folderPath, String zipPath) {

		Operations.log.debug("Started creating report zip file");

		boolean isZipped = false;

		try {

			// Initiate ZipFile object with the path/name of the zip file.
			ZipFile zipFile = new ZipFile(zipPath);

			// Initiate Zip Parameters which define various properties such
			// as compression method, etc.
			ZipParameters parameters = new ZipParameters();

			// set compression method to store compression
			parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);

			// Set the compression level
			parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

			// Add folder to the zip file
			zipFile.addFolder(folderPath, parameters);

			isZipped = true;
		} catch (Exception e) {
			Operations.log.error("Failed to create zip file", e);
		}

		Operations.log.debug("Ended creating report zip file");

		return isZipped;
	}

	/**
	 * To delete the zip file path given
	 * 
	 * @param zipPath
	 */
	private void deleteZipFile(String zipPath) {

		Operations.log.debug("Started deleting report zip file");

		try {

			// Initiate ZipFile object with the path/name of the zip file.
			File zipFile = new File(zipPath);

			// Initiate Zip Parameters which define various
			zipFile.delete();
		} catch (Exception e) {
			Operations.log.error("Failed to delete zip file", e);
		}

		Operations.log.debug("Ended deleting report zip file");
	}

}
