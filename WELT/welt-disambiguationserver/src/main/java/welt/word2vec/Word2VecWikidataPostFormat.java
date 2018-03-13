package welt.word2vec;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import welt.entitydisambiguation.properties.Properties;
import welt.tools.ServiceQueries;

/**
 * 
 * @author Dimitar
 * 
 * Class that handles and maps the needed POST request to the Word2Vec service end-point.
 * To define the location of the end-point, reference @resources/disambiguation.properties
 *
 */
public class Word2VecWikidataPostFormat {

	private final static Logger logger = LoggerFactory.getLogger(Word2VecJsonFormat.class);
	
	private int queryNumber;
	private List<String> queries;

	public List<String> getData() {
		return this.queries;
	}

	public void setQueries(List<String> queries) {
		this.queries = queries;
	}
	
	public int getQueryNumber() {
		return queryNumber;
	}

	public void setQueryNumber(int queryNumber) {
		this.queryNumber = queryNumber;
	}
	
	public JSONArray performQuery(String serviceEndpoint) {
		StringBuilder getString = new StringBuilder();
		JSONArray result = null;
		ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
	    postParameters.add(new BasicNameValuePair("query_number", this.queryNumber+""));
		for(int i = 0; i < this.queryNumber; i++) {
			String entities = this.queries.get(i).replaceAll("\\|", ",");
		    postParameters.add(new BasicNameValuePair("query_"+i, entities));
		}
		Header[] headers = { new BasicHeader("Accept", "application/json"),
				new BasicHeader("Content-Type", "application/x-www-form-urlencoded") };
		//System.out.println(Properties.getInstance().getWord2VecService() + serviceEndpoint + getString);
		String resStr = ServiceQueries.httpPostRequest(
				(Properties.getInstance().getWord2VecService() + serviceEndpoint), null, 
					postParameters, headers);
		try {
			result = new JSONArray(resStr);
		} catch (JSONException e) {
			logger.error("JsonException in "+Word2VecJsonFormat.class.getName(), e);
		}
		return result;
	}

	/*public static JSONArray performquery(Object json, String serviceEndpoint) {
		final ObjectMapper mapper = new ObjectMapper();
		String jsonString = null;
		JSONArray result = null;
		try {
			jsonString = mapper.writeValueAsString(json);
			Header[] headers = { new BasicHeader("Accept", "application/json"),
					new BasicHeader("content-type", "application/json") };
			ByteArrayEntity ent = new ByteArrayEntity(jsonString.getBytes(),
					ContentType.create("application/json"));
			String resStr = ServiceQueries.httpPostRequest(
					(Properties.getInstance().getWord2VecService() + serviceEndpoint), ent, headers);
			JSONObject resultJSON = null;
			try {
				resultJSON = new JSONObject(resStr);
				result = resultJSON.getJSONArray("data");
			} catch (JSONException e) {
				logger.error("JsonException in "+Word2VecJsonFormat.class.getName(), e);
			}
		} catch (IOException e) {
			logger.error("JsonException in "+Word2VecJsonFormat.class.getName(), e);
		}
		return result;
	}*/
}
