package welt.entitydisambiguation.algorithms.collective.wikidata;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBDBpedia;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;

class TimeNumberDisambiguation {

	private static final HashMap<String, String> TIMEANDNUMBERS = new HashMap<String, String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		{
			put("monday", "https://www.wikidata.org/entity/Q105");
			put("tuesday", "https://www.wikidata.org/entity/Q127");
			put("wednesday", "https://www.wikidata.org/entity/Q128");
			put("thursday", "https://www.wikidata.org/entity/Q129");
			put("friday", "https://www.wikidata.org/entity/Q130");
			put("saturday", "https://www.wikidata.org/entity/Q131");
			put("sunday", "https://www.wikidata.org/entity/Q132");
			put("one", "https://www.wikidata.org/entity/Q199");
			put("two", "https://www.wikidata.org/entity/Q200");
			put("three", "https://www.wikidata.org/entity/Q201");
			put("four", "https://www.wikidata.org/entity/Q202");
			put("five", "https://www.wikidata.org/entity/Q203");
			put("six", "https://www.wikidata.org/entity/Q23488");
			put("seven", "https://www.wikidata.org/entity/Q23350");
			put("eight", "https://www.wikidata.org/entity/Q23355");
			put("nine", "https://www.wikidata.org/entity/Q19108");
			put("ten", "https://www.wikidata.org/entity/Q23806");
			put("eleven", "https://www.wikidata.org/entity/Q37136");
			put("twelve", "https://www.wikidata.org/entity/Q36977");
			put("thirteen", "https://www.wikidata.org/entity/Q37141");
			put("fourteen", "https://www.wikidata.org/entity/Q38582");
			put("fifteen", "https://www.wikidata.org/entity/Q38701");
			put("sixteen", "https://www.wikidata.org/entity/Q40254");
			put("seventeen", "https://www.wikidata.org/entity/Q40118");
			put("eighteen", "https://www.wikidata.org/entity/Q38712");
			put("nineteen", "https://www.wikidata.org/entity/Q39850");
			put("twenty", "https://www.wikidata.org/entity/Q40292");
			put("thirty", "https://www.wikidata.org/entity/Q42817");
			put("forty", "https://www.wikidata.org/entity/Q42317");
			put("fifty", "https://www.wikidata.org/entity/Q712519");
			put("sixty", "https://www.wikidata.org/entity/Q79998");
			put("seventy", "https://www.wikidata.org/entity/Q712514");
			put("eighty", "https://www.wikidata.org/entity/Q712467");
			put("ninety", "https://www.wikidata.org/entity/Q239346");
			put("hundred", "https://www.wikidata.org/entity/Q37413");
			put("year", "https://www.wikidata.org/entity/Q577");
			put("years", "https://www.wikidata.org/entity/Q577");
			put("january", "https://www.wikidata.org/entity/Q108");
			put("february", "https://www.wikidata.org/entity/Q109");
			put("march", "https://www.wikidata.org/entity/Q110");
			put("april", "https://www.wikidata.org/entity/Q118");
			put("may", "https://www.wikidata.org/entity/Q119");
			put("june", "https://www.wikidata.org/entity/Q120");
			put("july", "https://www.wikidata.org/entity/Q121");
			put("august", "https://www.wikidata.org/entity/Q122");
			put("september", "https://www.wikidata.org/entity/Q123");
			put("october", "https://www.wikidata.org/entity/Q124");
			put("november", "https://www.wikidata.org/entity/Q125");
			put("december", "https://www.wikidata.org/entity/Q126");
			put("mile", "https://www.wikidata.org/entity/Q253276");
			put("miles", "https://www.wikidata.org/entity/Q253276");
			put("kilometre", "https://www.wikidata.org/entity/Q828224");
			put("kilometres", "https://www.wikidata.org/entity/Q828224");
			put("hour", "https://www.wikidata.org/entity/Q25235");
			put("hours", "https://www.wikidata.org/entity/Q25235");
			put("second", "https://www.wikidata.org/entity/Q11574");
			put("day", "https://www.wikidata.org/entity/Q573");
			put("days", "https://www.wikidata.org/entity/Q573");
			put("week", "https://www.wikidata.org/entity/Q23387");
			put("weeks", "https://www.wikidata.org/entity/Q23387");
		}
	};

	private EntityCentricKBWikidata eckb;

	public TimeNumberDisambiguation(EntityCentricKBWikidata eckb) {
		super();
		this.eckb = eckb;
	}

	void solve(List<SurfaceForm> reps) {
		for (SurfaceForm sf : reps) {
			String s = sf.getSurfaceForm().toLowerCase();
			String redirect = null;
			if (TIMEANDNUMBERS.containsKey(s)) {
				sf.setDisambiguatedEntity(TIMEANDNUMBERS.get(s));
			}/* TODO Re-add if needed also modigfy the links
				else if (isInteger(s, 10)) {
				String url = "https://www.wikidata.org/entity/" + s + "_(number)";
				if (isInIndex(url)) {
					sf.setDisambiguatedEntity(url);
				} else if ((redirect = getRedirect(url)) != null) {
					sf.setDisambiguatedEntity(redirect);
				}
			}*/
		}
	}

	private static boolean isInteger(String s, int radix) {
		if (s.isEmpty())
			return false;
		for (int i = 0; i < s.length(); i++) {
			if (i == 0 && s.charAt(i) == '-') {
				if (s.length() == 1)
					return false;
				else
					continue;
			}
			if (Character.digit(s.charAt(i), radix) < 0)
				return false;
		}
		return true;
	}

	private boolean isInIndex(String url) {
		IndexSearcher searcher = this.eckb.getSearcher();
		Query query = new TermQuery(new Term("Mainlink", url));
		try {
			TopDocs topdocs = searcher.search(query, 1);
			ScoreDoc[] scoredoc = topdocs.scoreDocs;
			if (scoredoc.length > 0) {
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	private String getRedirect(String uri) {
//		final Model model = DisambiguationMainService.getInstance().getDBpediaRedirects();
//		final String query = "SELECT ?label WHERE{ <" + uri
//				+ "> <http://dbpedia.org/ontology/wikiPageRedirects> ?label. }";
//		ResultSet results = null;
//		QueryExecution qexec = null;
//		String redirect = null;
//		try {
//			final com.hp.hpl.jena.query.Query cquery = QueryFactory.create(query);
//			qexec = QueryExecutionFactory.create(cquery, model);
//			results = qexec.execSelect();
//		} catch (final QueryException e) {
//			Logger.getRootLogger().error(e.getStackTrace());
//		} finally {
//			if (results.hasNext()) {
//				final QuerySolution sol = results.nextSolution();
//				redirect = sol.getResource("label").getURI();
//			}
//		}
		return null;
	}
}
