package welt.lucene.features;

import java.util.Set;

/**
 * Interface to specify an external Lucene feature set for an entity-centric
 * knowledge base. External features are features not integrated in Apache
 * Lucene.
 * 
 * @author Stefan Zwicklbauer
 * @modifier Dimitar
 * 
 */
public interface IEntityCentricExtFeatures {

	public float getPriorOfDocument(final int docId);

	public float getSensePriorOfDocument(final String keyword, final int docId);
	
	public Set<String> getRelations(final String url);
	
	public int getHashedPrior(String sf, String uri);
}
