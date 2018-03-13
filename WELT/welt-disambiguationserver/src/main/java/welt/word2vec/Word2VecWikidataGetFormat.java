package welt.word2vec;

import java.io.IOException;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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
public class Word2VecWikidataGetFormat {

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
		getString.append("?query_number=").append(this.queryNumber).append("&");
		for(int i = 0; i < this.queryNumber; i++) {
			String entities = this.queries.get(i).replaceAll("\\|", ",");
			getString.append("query_").append(i).append("=").append(entities);
			if(i != this.queryNumber-1) {//leave out & on the last element
				getString.append("&");
			}
		}
		Header[] headers = { new BasicHeader("Accept", "application/json"),
				new BasicHeader("content-type", "application/json") };
		System.out.println(Properties.getInstance().getWord2VecService() + serviceEndpoint + getString);
		String resStr = ServiceQueries.httpGetRequest(
				(Properties.getInstance().getWord2VecService() + serviceEndpoint + getString), headers);
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
