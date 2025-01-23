package execution.operations;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class API extends CommonOperations {

	/**
	 * Send request to get the response of grid status
	 * 
	 * @return string to get the json response
	 */
	public String sendRequestForStatus() {

		Operations.log.debug("Started sending status request api in grid");
		// generate the uri
		URI uri;
		String jsonResponse = "";
		try {
			uri = new URI(readJsonData("docker/grid/statusApi", ProcessStrings.sanityConfigJsonFile));

			// Execute GET request and get the response
			HttpGet httpget = new HttpGet(uri);

			HttpClient httpclient = HttpClientBuilder.create().build();

			// set the header
			httpget.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

			// Execute POST request and get the response.
			HttpResponse response = httpclient.execute(httpget);

			if (response.getStatusLine().toString().contains("200")) {
				jsonResponse = EntityUtils.toString(response.getEntity());
			}
		} catch (Exception e) {
			Operations.log.error("Error occured while sending status request api in grid", e);
		}
		Operations.log.debug("Ended sending status request api in grid");
		return jsonResponse;
	}

	/**
	 * Get the count of total nodes created in the grid
	 * 
	 * @return count of node
	 */
	public int getNodesCount() {
		Operations.log.debug("Started getting node count");

		String response = sendRequestForStatus();
		int nodeCount = -1;

		if (!response.isEmpty()) {
			try {
				JSONParser jsonParser = new JSONParser();
				JSONObject json = null;

				// get nodes array size from json values
				json = (JSONObject) jsonParser.parse(response);
				JSONObject value = (JSONObject) json.get("value");
				JSONArray nodes = (JSONArray) value.get("nodes");
				nodeCount = nodes.size();
			} catch (Exception e) {
				Operations.log.error("Error occured while parsing the json object response from grid to node count ",
						e);
			}
		}

		Operations.log.debug("Ended getting node count");
		return nodeCount;
	}

	/**
	 * Get the count of total session created in the grid
	 * 
	 * @return count of session
	 */
	public int getSessionCount() {
		Operations.log.debug("Started getting session count");

		String response = sendRequestForStatus();
		int sessionCount = -1;

		if (!response.isEmpty()) {
			try {
				JSONParser jsonParser = new JSONParser();
				JSONObject json = null;

				// get nodes array from json values
				json = (JSONObject) jsonParser.parse(response);
				JSONObject value = (JSONObject) json.get("value");
				JSONArray nodes = (JSONArray) value.get("nodes");

				for (Object nodeObj : nodes) {
					// get slots array from specific node
					JSONObject node = (JSONObject) nodeObj;
					JSONArray slots = (JSONArray) node.get("slots");

					for (Object slotObj : slots) {
						// get session from slot array
						JSONObject slot = (JSONObject) slotObj;
						sessionCount = slot.get("session") == null ? sessionCount : sessionCount + 1;
					}
				}
			} catch (Exception e) {
				Operations.log.error("Error occured while parsing the json object response from grid to session count ",
						e);
			}
		}

		Operations.log.debug("Ended getting session count");
		return sessionCount;
	}

	/**
	 * Get available nodes from API
	 * 
	 * @return list of string
	 */
	public List<String> getAvailableNodes() {
		Operations.log.debug("Started getting list of all the node created");

		String response = sendRequestForStatus();
		List<String> createdNodeList = new ArrayList<>();

		if (!response.isEmpty()) {
			try {
				JSONParser jsonParser = new JSONParser();
				JSONObject json = null;

				// get nodes array from json values
				json = (JSONObject) jsonParser.parse(response);
				JSONObject value = (JSONObject) json.get("value");
				JSONArray nodes = (JSONArray) value.get("nodes");

				for (Object nodeObj : nodes) {
					// get slots array from specific node
					JSONObject node = (JSONObject) nodeObj;
					JSONArray slots = (JSONArray) node.get("slots");

					for (Object slotObj : slots) {
						// get node name from applicationName
						JSONObject slot = (JSONObject) slotObj;
						JSONObject stereotype = (JSONObject) slot.get("stereotype");
						String nodeName = stereotype.get("nodename:applicationName").toString().replace("node_", "");
						createdNodeList.add(nodeName);
					}
				}
			} catch (Exception e) {
				Operations.log.error("Error occured while parsing the json object response from grid to node count ",
						e);
			}
		}

		Operations.log.debug("Ended getting list of all the node created");
		return createdNodeList;
	}
}
