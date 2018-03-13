package welt.tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * Class providing queries for different services. Integrated so far: DbPedia
 * Spotlight
 * 
 * @author Stefan Zwicklbauer
 * @modifier Dimitar Jovanov
 */
public class ServiceQueries {

	public static String httpPostRequest(String uri, AbstractHttpEntity entity,
			ArrayList<NameValuePair> postParameters, Header[] header) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost httpPost = new HttpPost(uri);
		httpPost.setHeaders(header);
		if(entity != null) { // for RAW queries
			httpPost.setEntity(entity);
		}else { // for post parameter queries
		    try {
				httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
			} catch (UnsupportedEncodingException e1) {
				System.out.println("Exception with encoded post parameters.");
			}
		}

		HttpResponse response;
		StringBuffer buffer = new StringBuffer();
		try {
			response = httpclient.execute(httpPost);
			HttpEntity ent = response.getEntity();

			buffer.append(EntityUtils.toString(ent));
			httpclient.getConnectionManager().shutdown();

		} catch (ClientProtocolException e) {
			Logger.getRootLogger().error("HTTPClient error", e);
		} catch (IOException e) {
			Logger.getRootLogger().error("HTTPClient error", e);
		}
		return buffer.toString();
	}
	
	public static String httpGetRequest(String uri, Header[] header) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(uri);
		if(header != null) {
			httpGet.setHeaders(header);
		}

		HttpResponse response;
		StringBuffer buffer = new StringBuffer();
		try {
			response = httpClient.execute(httpGet);
			HttpEntity ent = response.getEntity();

			buffer.append(EntityUtils.toString(ent));
			httpClient.getConnectionManager().shutdown();

		} catch (ClientProtocolException e) {
			Logger.getRootLogger().error("HTTPClient error", e);
		} catch (IOException e) {
			Logger.getRootLogger().error("HTTPClient error", e);
		}
		return buffer.toString();
	}
}
