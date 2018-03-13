package welt.entitydisambiguation.algorithms;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
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
import welt.entitydisambiguation.knowledgebases.EntityCentricKnowledgeBase;
import welt.lucene.features.LuceneFeatures;
import welt.lucene.query.LTRBooleanQuery;
import welt.lucene.query.LearnToRankClause;
import welt.lucene.query.LearnToRankQuery;

public class EntityCentricAlgorithmTableDefault extends AbstractDisambiguationAlgorithm {

	private EntityCentricKnowledgeBase eckb;
	private DisambiguationTaskSingle task;

	@Override
	public boolean checkAndSetInputParameter(AbstractDisambiguationTask task) {
		AbstractKnowledgeBase kb = task.getKb();
		if (!(task instanceof DisambiguationTaskSingle)) {
			return false;
		} else if (!(kb instanceof EntityCentricKnowledgeBase)) {
			return false;
		}
		this.eckb = (EntityCentricKnowledgeBase) kb;
		this.task = (DisambiguationTaskSingle) task;
		return true;
	}

	@Override
	public void processAlgorithm()
			throws IllegalDisambiguationAlgorithmInputException {
		Query query = createPhraseQuery(task.getEntityToDisambiguate(), eckb);
		final IndexSearcher searcher = eckb.getSearcher();
		final IndexReader reader = searcher.getIndexReader();
		EntityDisambiguationDPO dpo = task.getEntityToDisambiguate();
		try {
			TopDocs top = searcher.search(query, task.getReturnNr());
			ScoreDoc[] score = top.scoreDocs;
			if (score.length == 0) {
				query = createFuzzyQuery(task.getEntityToDisambiguate(), eckb);
				top = searcher.search(query, task.getReturnNr());
				score = top.scoreDocs;
			}

			final List<DisambiguatedEntity> disList = new LinkedList<DisambiguatedEntity>();
			final String[] entityMainLinks = new String[score.length];

			for (int j = 0; j < score.length; j++) {
				final DisambiguatedEntity entity = new DisambiguatedEntity();
				final Document doc = reader.document(score[j].doc);
				final String mainLink = doc.get("Mainlink");
				entity.setEntityUri(mainLink);
				disList.add(entity);
				entityMainLinks[j] = mainLink;

//				if (task.isRetrieveDocClasses()) {
//					entity.setDoc(doc);
//				}
			}

//			if (Properties.getInstance().getHBaseStorage()) {
//				task.getOutput().storeQuery(dpo.getDocumentId(),
//						dpo.getSelectedText(), dpo.getPosition(),
//						entityMainLinks, dpo.getContext());
//			}
			Response response = new Response();
			response.setSelectedText(dpo.getSelectedText());
			response.setDisEntities(disList);
			List<Response> resList = new LinkedList<Response>();
			resList.add(response);
			task.setResponse(resList);
		} catch (final IOException e) {
			Logger.getRootLogger().error(e.getStackTrace());
		}
		eckb.release();
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

	private Query createPhraseQuery(EntityDisambiguationDPO dpo,
			EntityCentricKnowledgeBase kb) {
		LearnToRankQuery query = new LearnToRankQuery();
		List<LearnToRankClause> features = new LinkedList<LearnToRankClause>();

		DefaultSimilarity defaultSim = new DefaultSimilarity();
		LTRBooleanQuery bq = new LTRBooleanQuery();
		bq.add(LuceneFeatures.queryLabelTerm(dpo.getSelectedText(),
				"UniqueLabelString", defaultSim), Occur.SHOULD);
		bq.add(LuceneFeatures.queryLabelTerm(dpo.getSelectedText(), "Label",
				defaultSim), Occur.SHOULD);

		// Feature 1
		features.add(query.add(bq, "Feature1", true));
		// Feature 2
		features.add(query.add(
				LuceneFeatures.querySensePrior(dpo.getSelectedText(),
						kb.getFeatureDefinition()), "Feature2", false));

		features.get(0).setWeight(1f);
		features.get(1).setWeight(1f);
		return query;
	}

	private Query createFuzzyQuery(EntityDisambiguationDPO dpo,
			EntityCentricKnowledgeBase kb) {
		LearnToRankQuery query = new LearnToRankQuery();
		List<LearnToRankClause> features = new LinkedList<LearnToRankClause>();
		DefaultSimilarity defaultSim = new DefaultSimilarity();

		// Feature 1
		features.add(query.add(LuceneFeatures.queryStringTerm(
				dpo.getSelectedText(), "Label", defaultSim, Occur.SHOULD,
				DisambiguationMainService.MAXCLAUSECOUNT), "Feature1", true));
		// Feature 2
		features.add(query.add(
				LuceneFeatures.querySensePrior(dpo.getSelectedText(),
						kb.getFeatureDefinition()), "Feature2", false));
		features.get(0).setWeight(0.0915161f);
		features.get(1).setWeight(0.350994f);
		return query;
	}
}
