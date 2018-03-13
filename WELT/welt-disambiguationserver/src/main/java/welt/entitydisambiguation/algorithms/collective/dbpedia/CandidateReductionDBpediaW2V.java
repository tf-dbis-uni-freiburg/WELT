package welt.entitydisambiguation.algorithms.collective.dbpedia;

import java.util.LinkedList;
import java.util.List;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.algorithms.collective.CandidateReduction;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBDBpedia;

public class CandidateReductionDBpediaW2V extends CandidateReduction {

	private int iterations;
	private boolean disambiguate;
	private EntityCentricKBDBpedia eckb;
	private int reduceTo;
	
	CandidateReductionDBpediaW2V(EntityCentricKBDBpedia eckb, List<SurfaceForm> rep, int maxsurfaceformsperquery,
			int reduceTo, int iterations, boolean disambiguate, boolean alwaysAction) {
		super(rep, maxsurfaceformsperquery, alwaysAction);
		this.iterations = iterations;
		this.disambiguate = disambiguate;
		this.eckb = eckb;
		this.reduceTo = reduceTo;
	}

	@Override
	public List<SurfaceForm> miniSolve(List<SurfaceForm> rep) {
		List<SurfaceForm> sol = new LinkedList<SurfaceForm>();
		Word2VecDisambiguator disambiguator = new Word2VecDisambiguator(eckb, rep, disambiguate, reduceTo, iterations);
		disambiguator.setup();
		disambiguator.solve();
		sol.addAll(disambiguator.getRepresentation());
		return sol;

	}

}
