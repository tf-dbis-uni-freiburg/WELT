package welt.entitydisambiguation.knowledgebases;

import java.util.List;

import org.apache.lucene.search.similarities.BM25Similarity;;

/**
 * 
 * @author Dimitar
 * 
 * Class used for the handling of Wikidata entities.
 *
 */
public class EntityCentricKBWikidata extends AbstractEntityCentricKBGeneral {

	public EntityCentricKBWikidata(String uri, boolean dynamic) {
		super(uri, dynamic);
	}

	public EntityCentricKBWikidata(String uri, boolean dynamic, BM25Similarity sim) {
		super(uri, dynamic, sim);
	}

	/**
	 * Takes a set of wikidata entities as well as a target entity and generates
	 * one string that fits into the word2vec query format used in this class.
	 * The source entities are concatenated and should be compared with the
	 * target entity.
	 *
	 * @param source
	 *            a set of source entities
	 * @param target
	 *            the target entity.
	 * @return String in appropriate word2vec query format
	 */
	@Override
	public String generateWord2VecFormatString(String source, String target) {
		String s = source.replaceAll("http://www.wikidata.org/entity/", "");
		String t = target.replaceAll("http://www.wikidata.org/entity/", "");
		int c = s.compareToIgnoreCase(t);
		String res = "";
		if (c < 0) {
			res = s + "|" + t;
		} else if (c == 0) {
			res = s + "|" + t;
		} else {
			res = t + "|" + s;
		}
		return res;
	}

	/**
	 * Takes a set of wikidata entities as well as a target entity and generates
	 * one string that fits into the word2vec query format used in this class.
	 * The source entities are concatenated and should be compared with the
	 * target entity.
	 *
	 * @param source
	 *            a set of source entities
	 * @param target
	 *            the target entity.
	 * @return String in appropriate word2vec query format
	 */
	@Override
	public String generateWord2VecFormatString(List<String> source, String target) {
		StringBuilder builder = new StringBuilder();
		for (String s : source) {
			s = s.replaceAll("http://www.wikidata.org/entity/", "");
			builder.append(s);
			builder.append("|");
		}
		String src = builder.toString();
		src = src.substring(0, src.length() - 1);
		String t = target.replaceAll("http://www.wikidata.org/entity/", "");
		return src + "|" + t;
	}
	
	@Override
	protected String generateDomainName() {
		return "Wikidata";
	}
	
	@Override
	protected String kbName() {
		return "Wikidata KB";
	}
}
