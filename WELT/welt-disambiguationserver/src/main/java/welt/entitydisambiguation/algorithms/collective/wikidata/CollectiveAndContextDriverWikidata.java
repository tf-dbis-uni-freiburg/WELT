package welt.entitydisambiguation.algorithms.collective.wikidata;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.algorithms.collective.CandidatePruning;
import welt.entitydisambiguation.algorithms.rules.RuleAdapation;
import welt.entitydisambiguation.dpo.DisambiguatedEntity;
import welt.entitydisambiguation.dpo.Response;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;

/**
 * @author Dimitar
 */
class CollectiveAndContextDriverWikidata {

	static final int PREPROCESSINGCONTEXTSIZE = 200;

	private String topic;
	private Response[] currentResponse;
	private List<SurfaceForm> representation;
	private EntityCentricKBWikidata eckb;

	CollectiveAndContextDriverWikidata(Response[] res, List<SurfaceForm> representation, EntityCentricKBWikidata eckb, String topic) {
		super();
		this.topic = topic;

		/*System.out.println(res.length + " //// " + rep.size());
		if (res.length != rep.size()) {
			System.out.println("Size reset");
			res = new Response [rep.size()];
		}
		System.out.println(res.length + " //// " + rep.size());*/
		this.currentResponse = res;
		this.representation = representation;
		this.eckb = eckb;
		// TODO: Add if using doc2vec
		//this.eckb.precomputeDoc2VecSimilarities(rep, PREPROCESSINGCONTEXTSIZE);
	}

	void solve() {
		// First candidate pruning
		System.out.println("Candidate Pruning Begin");
		CandidatePruning pruning = new CandidatePruning(eckb);
		pruning.prune(this.representation);
		System.out.println("Candidate Pruning End");
		
		//topic filter which we don't have
		if (topic != null) {
			TableColumnFilter cf = new TableColumnFilter(eckb, topic);
			cf.filter(this.representation);
		}
		/*TimeNumberDisambiguation timenumberdis = new TimeNumberDisambiguation(eckb);
		timenumberdis.solve(rep);
		LocationDisambiguation locationDis = new LocationDisambiguation(eckb);
		locationDis.solve(rep);*/

		RuleAdapation rules = new RuleAdapation();
		rules.addNoCandidatesCheckPluralRule(eckb);
		rules.addNoCandidatesExpansionRule(eckb);
		rules.addUnambiguousToAmbiguousRule(eckb);
		rules.addPatternRule(eckb, topic);
		rules.addContextRule(eckb);
		rules.performRuleChainBeforeCandidateSelection(this.representation);

		System.out.println("Candidate Reduction Step1 Begin");
		CandidateReductionWikidataW2V w2vreduction = new CandidateReductionWikidataW2V(eckb, this.representation, 20, 5, 150, false, false);
		w2vreduction.solve();
		this.representation = w2vreduction.getRep();
		System.out.println("Candidate Reduction Step1 End");

		System.out.println("Candidate Reduction Step2 Begin");
		w2vreduction = new CandidateReductionWikidataW2V(eckb, this.representation, 45, 5, 250, true, true);
		w2vreduction.solve();
		this.representation = w2vreduction.getRep();
		System.out.println("Candidate Reduction Step2 End");
		System.out.println("Final Entity Disambiguation Begin");
		FinalEntityDisambiguation finalDis = new FinalEntityDisambiguation(eckb, this.representation);
		finalDis.setup();
		finalDis.solve();
		System.out.println("Final Entity Disambiguation End");
	}

	void generateResult() {
		ArrayList<Response> responses = new ArrayList<>();
		for (int i = 0; i < currentResponse.length; i++) {
			SurfaceForm surfaceForm = search(i);
			if (currentResponse[i] == null && surfaceForm != null && surfaceForm.getCandidates().size() == 1) {
				Response res = new Response();
				List<DisambiguatedEntity> entList = new LinkedList<DisambiguatedEntity>();
				DisambiguatedEntity entity = new DisambiguatedEntity();
				entity.setEntityUri(surfaceForm.getCandidates().get(0));
				entity.setNotInWikipedia(surfaceForm.getNotInWikipediaMap().get(surfaceForm.getCandidates().get(0)));
				entList.add(entity);
				res.setDisEntities(entList);
				res.setSelectedText(surfaceForm.getSurfaceForm());
				currentResponse[i] = res;
				responses.add(res);
			}

		}
		currentResponse = new Response [responses.size()];
		for(int i = 0; i < responses.size(); i++) {
			currentResponse[i] = responses.get(i);
		}
	}

	private SurfaceForm search(int qryNr) {
		for (SurfaceForm surfaceForm : representation) {
			if (surfaceForm.getQueryNr() == qryNr) {
				return surfaceForm;
			}
		}
		return null;
	}
}
