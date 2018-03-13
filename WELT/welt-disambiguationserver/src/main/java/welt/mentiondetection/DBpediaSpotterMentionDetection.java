package welt.mentiondetection;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import welt.tools.ServiceQueries;

/**
 * 
 * @author Dimitar
 * 
 * Mention Detection provided by the DBpedia Spotter API. Before using this mention detection make sure
 * to run the server script and get a running instance of the DBpedia Spotter on your server. 
 *
 */
public class DBpediaSpotterMentionDetection implements MentionDetection {

	private String text;
	
	public DBpediaSpotterMentionDetection(String text) {
		this.text = text;
	}
	
	@Override
	public String detectMentions() {
		Header[] headers = {new BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")};
		return ServiceQueries.httpGetRequest("http://132.230.150.54:2222/rest/spot?text=" + text + "&spotter=Default", headers);
	}
	
}
