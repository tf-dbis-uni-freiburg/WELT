package welt.entitydisambiguation.algorithms.collective.wikidata;

import java.util.LinkedList;
import java.util.List;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.algorithms.collective.CandidateReduction;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;

public class CandidateReductionWikidataW2V extends CandidateReduction {

	private int iterations;
	private boolean disambiguate;
	private EntityCentricKBWikidata eckb;
	private int reduceTo;
	
	CandidateReductionWikidataW2V(EntityCentricKBWikidata eckb, List<SurfaceForm> rep, int maxsurfaceformsperquery,
			int reduceTo, int iterations, boolean disambiguate, boolean alwaysAction) {
		super(rep, maxsurfaceformsperquery, alwaysAction);
		this.iterations = iterations;
		this.disambiguate = disambiguate;
		this.eckb = eckb;
		this.reduceTo = reduceTo;
	}

	@Override
	public List<SurfaceForm> miniSolve(List<SurfaceForm> rep) {
		List<SurfaceForm> result = new LinkedList<SurfaceForm>();
		Word2VecDisambiguator disambiguator = new Word2VecDisambiguator(eckb, rep, disambiguate, reduceTo, iterations);
		disambiguator.setup();
		disambiguator.solve();
		result.addAll(disambiguator.getRepresentation());
		return result;

	}

}
