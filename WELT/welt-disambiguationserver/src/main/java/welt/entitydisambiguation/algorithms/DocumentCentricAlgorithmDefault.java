package welt.entitydisambiguation.algorithms;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.DefaultSimilarity;

import welt.entitydisambiguation.backend.AbstractDisambiguationTask;
import welt.entitydisambiguation.backend.DisambiguationMainService;
import welt.entitydisambiguation.backend.DisambiguationTaskSingle;
import welt.entitydisambiguation.dpo.DisambiguatedEntity;
import welt.entitydisambiguation.dpo.EntityDisambiguationDPO;
import welt.entitydisambiguation.dpo.Response;
import welt.entitydisambiguation.knowledgebases.AbstractKnowledgeBase;
import welt.entitydisambiguation.knowledgebases.DocumentCentricKnowledgeBaseDefault;
import welt.general.HelpfulMethods;
import welt.lucene.features.LuceneFeatures;
import welt.lucene.query.LearnToRankClause;
import welt.lucene.query.LearnToRankQuery;

/**
 * Algorithmus verallgemeinern sodass nicht nur Calbc funktioniert.
 * 
 * Allgemein optimieren. Stichwort HashMaps
 * 
 * @author quhfus
 * 
 */
public class DocumentCentricAlgorithmDefault extends AbstractDisambiguationAlgorithm {

	public static int CLASSIFICATIONDOCUMENTS = 101;

	public static String CONCEPTFIELD = "concept";

	private DocumentCentricKnowledgeBaseDefault dckb;

	private DisambiguationTaskSingle task;

	DocumentCentricAlgorithmDefault() {
		super();
	}

	@Override
	public boolean checkAndSetInputParameter(AbstractDisambiguationTask task) {
		AbstractKnowledgeBase kb = task.getKb();
		if (!(task instanceof DisambiguationTaskSingle)) {
			return false;
		} else if (!(kb instanceof DocumentCentricKnowledgeBaseDefault)) {
			return false;
		}
		this.dckb = (DocumentCentricKnowledgeBaseDefault) kb;
		this.task = (DisambiguationTaskSingle) task;
		return true;
	}

	@Override
	protected boolean preDisambiguation() {
		EntityDisambiguationDPO toDis = task.getEntityToDisambiguate();
		boolean res = true;
		final Pattern pattern = Pattern.compile("^\\d*[.,]?\\d*$");
		final String surfaceForms = toDis.getSelectedText();

			final String str = surfaceForms;
			final Matcher matcher = pattern.matcher(str);
			if (matcher.find()) {
				res = false;
			}
		if (!res) {
				final List<DisambiguatedEntity> disEntityList = new LinkedList<DisambiguatedEntity>();
				final DisambiguatedEntity disEntity = new DisambiguatedEntity();
				disEntity.setEntityUri("http://dbpedia.org/resource/Number");
				disEntityList.add(disEntity);
				Response response = new Response();
				response.setSelectedText(toDis.getSelectedText());
				response.setDisEntities(disEntityList);
				List<Response> resList = new LinkedList<Response>();
				resList.add(response);
				task.setResponse(resList);
			}
		
		return res;
	}

	@Override
	public void processAlgorithm()
			throws IllegalDisambiguationAlgorithmInputException {
		Query query = createQuery(task.getEntityToDisambiguate());
		final IndexSearcher searcher = dckb.getSearcher();
		final IndexReader reader = searcher.getIndexReader();
		HashMap<String, Integer> hashSaver = new HashMap<String, Integer>();
		EntityDisambiguationDPO dpo = task.getEntityToDisambiguate();

		try {
			TopDocs top = searcher.search(query, CLASSIFICATIONDOCUMENTS);
			ScoreDoc[] score = top.scoreDocs;

			for (int i = 0; i < score.length; i++) {
				Document doc = reader.document(score[i].doc);

				String str = doc.get(CONCEPTFIELD);
				String[] arr = createConceptArray(str);
				for (int j = 0; j < arr.length; j++) {
					if (hashSaver.get(arr[j]) != null) {
						Integer val = hashSaver.get(arr[j]);
						hashSaver.put(arr[j], ++val);
					} else {
						hashSaver.put(arr[j], 1);
					}
				}
			}
			List<Entry<String, Integer>> vals = HelpfulMethods
					.sortByValue(hashSaver);
			final List<DisambiguatedEntity> disList = new LinkedList<DisambiguatedEntity>();
			for (int i = 0; i < task.getReturnNr(); i++) {
				Entry<String, Integer> entry = vals.get(i);
				final DisambiguatedEntity entity = new DisambiguatedEntity();
				entity.setEntityUri(entry.getKey());
				disList.add(entity);
			}

			Response response = new Response();
			response.setSelectedText(dpo.getSelectedText());
			response.setDisEntities(disList);
			List<Response> resList = new LinkedList<Response>();
			resList.add(response);
			task.setResponse(resList);
		} catch (IOException e) {
			e.printStackTrace();
		}
		dckb.release();
	}

	private String[] createConceptArray(String str) {
		List<String> lst = new LinkedList<String>();
		str = str.trim();
		String[] arr = str.split(" ");
		for (int i = 0; i < arr.length; i++) {
			if (!arr[i].equalsIgnoreCase("") && analyseConcept(arr[i])) {
				lst.add(generateID(arr[i].toUpperCase()));
			}
		}
		String[] result = new String[lst.size()];
		lst.toArray(result);
		return result;
	}

	private String generateID(String line) {
		String[] splitter = line.split(":");

		String link = "";
		if (splitter[1].equalsIgnoreCase("uniprot")
				&& !splitter[2].equalsIgnoreCase("") && splitter[2] != null) {
			link = "UN_" + splitter[2];
		} else if (splitter[1].equalsIgnoreCase("entrezgene")
				&& !splitter[2].equalsIgnoreCase("") && splitter[2] != null) {
			link = "NC_" + splitter[2];
		} else if (splitter[1].equalsIgnoreCase("umls")
				&& !splitter[2].equalsIgnoreCase("") && splitter[2] != null) {
			link = "LI_" + splitter[2];
		} else if (splitter[1].equalsIgnoreCase("ncbi")
				&& !splitter[2].equalsIgnoreCase("") && splitter[2] != null) {
			link = "NC_" + splitter[2];
		} else if (splitter[1].equalsIgnoreCase("disease")
				&& !splitter[2].equalsIgnoreCase("") && splitter[2] != null) {
			link = "LI_" + splitter[2];
		}
		return link;
	}

	private boolean analyseConcept(String str) {
		String[] arr = str.split(":");
		if (arr.length < 3) {
			return false;
		}
		if (arr[2] == null || arr[2].equalsIgnoreCase("")) {
			return false;
		}
		return true;
	}

	private Query createQuery(EntityDisambiguationDPO dpo) {
		LearnToRankQuery query = new LearnToRankQuery();
		List<LearnToRankClause> features = new LinkedList<LearnToRankClause>();
		DefaultSimilarity defaultSim = new DefaultSimilarity();

		// Feature 1
		features.add(query.add(LuceneFeatures.queryLabelTerm(
				dpo.getSelectedText(), "title", defaultSim), "Feature1",
				true));
		// Feature 2
		features.add(query.add(LuceneFeatures.queryLabelTerm(
				dpo.getSelectedText(), "abstract", defaultSim), "Feature2",
				true));
		// Feature 3
		features.add(query.add(LuceneFeatures.queryStringTerm(dpo.getContext(),
				"title", defaultSim, Occur.SHOULD,
				DisambiguationMainService.MAXCLAUSECOUNT), "Feature3", false));
		// Feature 4
		features.add(query.add(LuceneFeatures.queryStringTerm(dpo.getContext(),
				"abstract", defaultSim, Occur.SHOULD,
				DisambiguationMainService.MAXCLAUSECOUNT), "Feature4", false));

		features.get(0).setWeight(0.0056836f);
		features.get(1).setWeight(0.0305069f);
		features.get(2).setWeight(0.117543f);
		features.get(3).setWeight(0.365259f);
		return query;
	}
}
