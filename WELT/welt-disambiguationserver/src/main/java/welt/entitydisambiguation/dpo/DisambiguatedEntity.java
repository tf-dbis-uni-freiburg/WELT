package welt.entitydisambiguation.dpo;


/**
 * Class representing a disambiguated entity consisting of the entity mention
 * (the text), the identified URI, a value representing the confidence for the
 * decision, and a entity description. Class is a POJO for automatic
 * (de-)serialization. TODO may not be complete (e.g. relevant terms may be
 * added)
 * 
 * @author zwicklbauer
 * 
 * @modifier Dimitar Jovanov
 * 
 */
public class DisambiguatedEntity {

	private String entityUri;
	private int notInWikipedia;

	public DisambiguatedEntity() {
		super();
		this.entityUri = "";
	}

	public DisambiguatedEntity(final String text, final String entityUri,
			final double confidence, final String description) {
		this.entityUri = entityUri;
	}

	public String getEntityUri() {
		return this.entityUri;
	}

	public void setEntityUri(final String entityUri) {
		this.entityUri = entityUri;
	}

	public int getNotInWikipedia() {
		return notInWikipedia;
	}

	public void setNotInWikipedia(int notInWikipedia) {
		this.notInWikipedia = notInWikipedia;
	}
}
