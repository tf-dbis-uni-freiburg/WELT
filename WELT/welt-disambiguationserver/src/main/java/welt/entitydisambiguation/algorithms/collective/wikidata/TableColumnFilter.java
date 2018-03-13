package welt.entitydisambiguation.algorithms.collective.wikidata;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBDBpedia;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;
import welt.lucene.query.TermQuery;

public class TableColumnFilter {

	private EntityCentricKBWikidata eckb;
	private String topic;

	TableColumnFilter(EntityCentricKBWikidata eckb, String topic) {
		super();
		this.eckb = eckb;
		this.topic = topic;
	}

	public void filter(List<SurfaceForm> reps) {
		for (SurfaceForm sf : reps) {
			List<String> candidates = sf.getCandidates();
			if (candidates.size() > 0) {
				String s = performLuceneQuery(candidates, topic);
				if (s != null) {
					sf.setDisambiguatedEntity(s);
				}
			}
		}
	}

	private String performLuceneQuery(List<String> candidates, String topic) {
		String result = null;
		IndexSearcher searcher = eckb.getSearcher();
		IndexReader reader = searcher.getIndexReader();
		BooleanQuery candidateq = new BooleanQuery();
		for (String can : candidates) {
			candidateq.add(new TermQuery(new Term("Mainlink", can)), Occur.SHOULD);
		}
		BooleanQuery q = new BooleanQuery();
		q.add(candidateq, Occur.MUST);
		q.add(new TermQuery(new Term("LongDescription", topic)), Occur.MUST);
		TopDocs t = null;
		try {
			t = searcher.search(q, candidates.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (t != null) {
			ScoreDoc[] scoredocs = t.scoreDocs;
			if (scoredocs.length == 1) {
				try {
					result = reader.document(scoredocs[0].doc).get("Mainlink");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
}
