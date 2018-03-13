package welt.mentiondetection;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import welt.models.Mention;
import welt.tools.ServiceQueries;

/**
 * 
 * @author Dimitar
 * 
 * Mention Detection class that provides support for the FOX mention detection.
 * For more info: http://fox-demo.aksw.org/#!/home
 *
 */
public class FoxMentionDetection implements MentionDetection {

	private String text;
	private ArrayList<Mention> mentions;
	
	public FoxMentionDetection(String text) {
		try {
			this.text = URLDecoder.decode(text, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			this.text = "";
		}
		this.mentions = new ArrayList<>();
	}
	
	@Override
	public String detectMentions() {
		Header[] headers = {new BasicHeader("Content-Type", "application/json;charset=UTF-8")};
		ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		StringEntity entity = null;
		String json = null;
	    try {
			JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("type", "text");
            jsonObject.accumulate("task", "ner");
            jsonObject.accumulate("input", text);
            jsonObject.accumulate("output", "JSON-LD");
            jsonObject.accumulate("nif", 0);
            jsonObject.accumulate("foxlight", "OFF");
            jsonObject.accumulate("defaults", 0);
            jsonObject.accumulate("state", "sending");
            jsonObject.accumulate("lang", "en");
            json = jsonObject.toString();
			entity = new StringEntity(json, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported Exception:" + e.getMessage());
		} catch (JSONException e) {
			System.out.println("JSON Exception:" + e.getMessage());
		}
		String jsonResponse = ServiceQueries.httpPostRequest("http://fox-demo.aksw.org/fox", entity, postParameters, headers);
	    // JSON parsing
		try {
			JSONObject jsonObject = new JSONObject(jsonResponse);
			JSONArray array = jsonObject.getJSONArray("@graph");
			for(int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				if(object.getString("@type").equals("nif:Phrase")) {
					String text = object.getString("anchorOf");
					int offset = Integer.parseInt(object.getString("beginIndex"));
					this.mentions.add(new Mention(text, offset));
				}
			}
		} catch (JSONException e) {
			System.out.println("Result JSON Exception:" + e.getMessage());
		}
		
		// sort mentions by offset
		Collections.sort(this.mentions);
		
		// XML building
		StringBuilder xmlBuilder = new StringBuilder("<annotation text=\""+StringEscapeUtils.escapeXml(text)+"\">");

		for(Mention mention : this.mentions) {
			xmlBuilder.append("<surfaceForm name=\""+StringEscapeUtils.escapeXml(mention.getMentionText())+"\" offset=\""+mention.getOffset()+"\"/>");
		}
		xmlBuilder.append("</annotation>");
		
		return xmlBuilder.toString();
	}
	
}
