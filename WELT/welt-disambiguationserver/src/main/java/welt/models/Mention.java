package welt.models;

/**
 * POJO class used for the Mention Detection process.
 * 
 * @author Dimitar
 *
 */
public class Mention implements Comparable<Mention> {
	private String mentionText;
	private int offset;
	
	public Mention(String mentionText, int offset) {
		super();
		this.mentionText = mentionText;
		this.offset = offset;
	}

	public String getMentionText() {
		return mentionText;
	}

	public void setMentionText(String mentionText) {
		this.mentionText = mentionText;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public int compareTo(Mention mention) {
		return this.offset > mention.offset ? 1 : this.offset < mention.offset ? -1 : 0;
	}
	
}
