package welt.models;

public class EntityNIF {
	private int start;
	private int end;
	private String uri;
	private String text;
	
	public EntityNIF(int start, int end, String uri, String text) {
		super();
		this.start = start;
		this.end = end;
		this.uri = uri;
		this.text = text;
	}
	
	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}
	
	public String getUri() {
		return uri;
	}
	
	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
}
