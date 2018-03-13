package welt.word2vec;

import java.io.IOException;
import java.util.Set;

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

public class Word2VecJsonFormat {

	private final static Logger logger = LoggerFactory.getLogger(Word2VecJsonFormat.class);
	
	private String domain;
	private Set<String> data;

	public Set<String> getData() {
		return data;
	}

	public void setData(Set<String> data) {
		this.data = data;
	}
	
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public static JSONArray performquery(Object json, String serviceEndpoint) {
		/*final ObjectMapper mapper = new ObjectMapper();
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
		}*/
		return new JSONArray();
	}
}
