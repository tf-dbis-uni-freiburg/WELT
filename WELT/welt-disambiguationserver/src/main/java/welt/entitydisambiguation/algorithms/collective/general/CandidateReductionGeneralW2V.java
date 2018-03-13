package welt.entitydisambiguation.algorithms.collective.general;

import java.util.LinkedList;
import java.util.List;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.algorithms.collective.CandidateReduction;
import welt.entitydisambiguation.knowledgebases.AbstractEntityCentricKBGeneral;

public class CandidateReductionGeneralW2V extends CandidateReduction {

	private int iterations;
	private boolean disambiguate;
	private AbstractEntityCentricKBGeneral eckb;
	private int reduceTo;
	
	public CandidateReductionGeneralW2V(AbstractEntityCentricKBGeneral eckb, List<SurfaceForm> rep, int maxsurfaceformsperquery,
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
		Word2VecDisambiguatorGeneral disambiguator = new Word2VecDisambiguatorGeneral(eckb, rep, disambiguate, reduceTo,
				iterations);
		disambiguator.setup();
		disambiguator.solve();
		sol.addAll(disambiguator.getRepresentation());
		return sol;

	}
}
