package welt.entitydisambiguation.algorithms.collective.wikidata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import welt.entitydisambiguation.algorithms.AbstractDisambiguationAlgorithm;
import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.backend.AbstractDisambiguationTask;
import welt.entitydisambiguation.backend.DisambiguationTaskCollective;
import welt.entitydisambiguation.dpo.DisambiguatedEntity;
import welt.entitydisambiguation.dpo.EntityDisambiguationDPO;
import welt.entitydisambiguation.dpo.Response;
import welt.entitydisambiguation.knowledgebases.AbstractKnowledgeBase;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;
import welt.lucene.query.TermQuery;

/**
 * Collective Disambiguation Approach by Stefan Zwicklbauer
 * 
 * @author Dimitar
 * 
 */
public class CollectiveDisambiguationWikidataEntities extends AbstractDisambiguationAlgorithm {

	private final static Logger logger = LoggerFactory.getLogger(CollectiveDisambiguationWikidataEntities.class);
	
	private EntityCentricKBWikidata eckb;

	private DisambiguationTaskCollective task;

	public CollectiveDisambiguationWikidataEntities() {
		super();
	}

	@Override
	public boolean checkAndSetInputParameter(AbstractDisambiguationTask task) {
		AbstractKnowledgeBase kb = task.getKb();
		if (!(task instanceof DisambiguationTaskCollective)) {
			return false;
		} else if (!(kb instanceof EntityCentricKBWikidata)) {
			return false;
		}
		this.eckb = (EntityCentricKBWikidata) kb;
		this.task = (DisambiguationTaskCollective) task;
		return true;
	}

	@Override
	protected boolean preDisambiguation() {
		return true;
	}

	@Override
	public void processAlgorithm() {
//		AdditionalCandidateQuery aq = new AdditionalCandidateQuery(eckb);
		List<EntityDisambiguationDPO> entityList = task.getEntityToDisambiguate();
		Response[] responseArray = new Response[entityList.size()];

		List<SurfaceForm> collectiveRep = new LinkedList<SurfaceForm>();
		for (int i = 0; i < entityList.size(); i++) {
			EntityDisambiguationDPO dpo = entityList.get(i);
			dpo.setSelectedText(dpo.getSelectedText().replaceAll("’", "'"));
			Query query = createQuery(dpo.getSelectedText(), eckb);
			final IndexSearcher searcher = eckb.getSearcher();
			final IndexReader reader = searcher.getIndexReader();
			try {
				final TopDocs top = searcher.search(query, task.getReturnNr());
				final ScoreDoc[] score = top.scoreDocs;
				/*for(int j = 0; j < score.length; j++) {
					final Document doc = reader.document(score[j].doc);
					System.out.println(doc.get("Label")+" - "+doc.get("Mainlink") + " - " + doc.get("WikidataVertexDegree"));
				}*/
				// if there is no selected text
				if (dpo.getSelectedText().equalsIgnoreCase("") || dpo.getSelectedText() == null) {
					ArrayList<String> list = new ArrayList<String>();
					list.add("");
					SurfaceForm col = new SurfaceForm(dpo.getSelectedText(), dpo.getContext(), list, i,
							dpo.getStartPosition());
					collectiveRep.add(col);
				} else if (score.length == 1) {// only one entity coresponds to the given mention
					final Document doc = reader.document(score[0].doc);
					ArrayList<String> list = new ArrayList<String>();
					list.add(doc.get("Mainlink"));
					SurfaceForm surfaceForm = new SurfaceForm(dpo.getSelectedText(), dpo.getContext(), list, i,
							dpo.getStartPosition());
					surfaceForm.setInitial(true);
					int notInWikipediaNumeric = Integer.parseInt(doc.get("NotInWikipedia"));
					HashMap<String, Integer> notInWikipediaMap = surfaceForm.getNotInWikipediaMap();
					notInWikipediaMap.put(doc.get("Mainlink"), notInWikipediaNumeric);
					surfaceForm.setNotInWikipediaMap(notInWikipediaMap);
					collectiveRep.add(surfaceForm);
				} else if (score.length > 1) {// more entities correspond to the given mention
					ArrayList<String> list = new ArrayList<String>();
					SurfaceForm surfaceForm = new SurfaceForm(dpo.getSelectedText(), dpo.getContext(), list, i,
							dpo.getStartPosition());
					for (int j = 0; j < score.length; j++) {
						final Document doc = reader.document(score[j].doc);
						list.add(doc.get("Mainlink"));
						int notInWikipediaNumeric = Integer.parseInt(doc.get("NotInWikipedia"));
						HashMap<String, Integer> notInWikipediaMap = surfaceForm.getNotInWikipediaMap();
						notInWikipediaMap.put(doc.get("Mainlink"), notInWikipediaNumeric);
						surfaceForm.setNotInWikipediaMap(notInWikipediaMap);
					}
					// TODO check if sense prior is relevant
					//sensePriorDisambiguation(surfaceForm);
					collectiveRep.add(surfaceForm);

				}
//				else {
//					SurfaceForm sf = aq.checkAdditionalSurfaceForms(dpo, i);
//					collectiveRep.add(sf);
//				}

			} catch (final IOException e) {
				logger.error("JsonException in "+CollectiveDisambiguationWikidataEntities.class.getName(), e);
			}
		}

		// AlgorithmDriver solver = new CollectiveOnlyDriver(
		// responseArray, collectiveRep, eckb);
		
		printSurfaceForms(collectiveRep);
		CollectiveAndContextDriverWikidata solver = new CollectiveAndContextDriverWikidata(responseArray, collectiveRep, eckb, task.getMainTopic());
		solver.solve();

		solver.generateResult();
		List<Response> res = Arrays.asList(responseArray);
		task.setResponse(res);

		eckb.release();
	}

	private void printSurfaceForms(List<SurfaceForm> collectiveRep) {
		for(SurfaceForm sf : collectiveRep) {
			System.out.println(sf.getSurfaceForm());
		}
	}

	private void printResponses(Response[] responseArray) {
		for(Response r : responseArray) {
			System.out.println(r.getDocumentId());
			System.out.println(r.getSelectedText());
			for(DisambiguatedEntity e : r.getDisEntities()) {
				System.out.println(e.getEntityUri());
			}
		}
	}

	/**
	 * Methods exist three times - bad programming
	 * ToDo - Fix it
	 * 
	 * @param responseArray
	 * @param cols
	 */
	void generateResult(Response[] responseArray, List<SurfaceForm> cols) {
		for (int i = 0; i < responseArray.length; i++) {
			SurfaceForm r = search(i, cols);
			if (responseArray[i] == null && r != null && r.getCandidates().size() == 1) {
				Response res = new Response();
				List<DisambiguatedEntity> entList = new LinkedList<DisambiguatedEntity>();
				DisambiguatedEntity ent = new DisambiguatedEntity();
				ent.setEntityUri(r.getCandidates().get(0));
				entList.add(ent);
				res.setDisEntities(entList);
				res.setSelectedText(r.getSurfaceForm());
				responseArray[i] = res;
			}
		}
	}

	private SurfaceForm search(int qryNr, List<SurfaceForm> rep) {
		for (SurfaceForm r : rep) {
			if (r.getQueryNr() == qryNr) {
				return r;
			}
		}
		return null;
	}

	private void sensePriorDisambiguation(SurfaceForm surfaceForm) {
		if (surfaceForm.getCandidates().size() > 1) {
			List<String> s = surfaceForm.getCandidates();
			List<Candidate> canList = new LinkedList<Candidate>();
			for (String str : s) {
				canList.add(new Candidate(str, eckb.getFeatureDefinition().getHashedPrior(surfaceForm.getSurfaceForm(), str)));
			}

			Collections.sort(canList, Collections.reverseOrder());
			surfaceForm.setDisambiguatedEntity(canList.get(0).getCandidate());
		}
	}

	protected class Candidate implements Comparable<Candidate> {

		private String candidate;
		private double score;

		protected Candidate(String candidate, double score) {
			super();
			this.candidate = candidate;
			this.score = score;
		}

		@Override
		public int compareTo(Candidate o) {
			if (this.score < o.score) {
				return -1;
			} else if (this.score > o.score) {
				return 1;
			} else {
				return 0;
			}
		}

		protected String getCandidate() {
			return candidate;
		}

		protected double getScore() {
			return score;
		}

	}
	
	private Query createQuery(String sf, EntityCentricKBWikidata kb) {
		String surfaceform = sf;//.toLowerCase();
		TermQuery query = new TermQuery(new Term("UniqueLabel", surfaceform));

		return query;
	}

	public static void main(String args[]) {
		String s = "test . test ";
		Pattern regex = Pattern.compile(" ([,!?.])");
		Matcher regexMatcher = regex.matcher(s);
		StringBuffer buffer = new StringBuffer();
		while (regexMatcher.find())
			regexMatcher.appendReplacement(buffer, regexMatcher.group(1));
		regexMatcher.appendTail(buffer);
		System.out.println(buffer.toString());
	}
}
