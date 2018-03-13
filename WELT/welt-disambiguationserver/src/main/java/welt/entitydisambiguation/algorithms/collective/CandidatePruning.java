package welt.entitydisambiguation.algorithms.collective;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.knowledgebases.AbstractEntityCentricKBGeneral;
import welt.general.HelpfulMethods;

/**
 * 
 * @author Dimitar
 * 
 * Candidate pruning step.
 *
 */
public class CandidatePruning {

	private static final int NUMBEROFADDITIONALW2VENTITIES = 6;

	private static final int ENTITYTHRESHOLD = 6;

	private static final int MINIMUMSURFACEFORMS = 3;

	private static final float WORD2VECTHRESHOLD = 1.60f;

	private AbstractEntityCentricKBGeneral eckb;

	public CandidatePruning(AbstractEntityCentricKBGeneral eckb) {
		super();
		this.eckb = eckb;
	}

	public void prune(List<SurfaceForm> representations) {
		List<SurfaceForm> unambiguous = new LinkedList<SurfaceForm>();
		for (SurfaceForm surfaceForm : representations) {
			if (surfaceForm.getCandidates().size() == 1) {
				unambiguous.add(surfaceForm);
			}
		}

		List<String> list = new LinkedList<String>();
		for (SurfaceForm surfaceForm : representations) {
			if (representations.size() > 1 && surfaceForm.getCandidates().size() == 1 && surfaceForm.isInitial()) {
				list.add(surfaceForm.getCandidates().get(0));
			}
		}

		for (SurfaceForm c : representations) {
			List<String> candidates = c.getCandidates();
			// if number of candidates above entity amount threshold 
			if (candidates.size() > ENTITYTHRESHOLD) {
				Set<String> prunedCandidates = new HashSet<String>();

				// Sense Prior
				Map<String, Integer> map = new HashMap<String, Integer>();
				for (String candidate : candidates) {
					map.put(candidate, eckb.getFeatureDefinition().getHashedPrior(c.getSurfaceForm(), candidate));
				}
				@SuppressWarnings("deprecation")
				List<Map.Entry<String, Integer>> l = HelpfulMethods.sortByValue(map);
				for (int i = 0; i < ENTITYTHRESHOLD; ++i) {
					prunedCandidates.add(l.get(i).getKey());
					// System.out.println("SensePrior ADd: "+l.get(i).getKey()+"
					// "+l.get(i).getValue());
				}

				// Doc2Vec ContextSimilarity
				/*Map<String, Float> map_doc2vec = new HashMap<String, Float>();
				for (String candidate : candidates) {
					map_doc2vec.put(candidate, eckb.getDoc2VecSimilarity(c.getSurfaceForm(), c.getContext(), candidate));
				}
				@SuppressWarnings("deprecation")
				List<Map.Entry<String, Float>> l_doc2vec = HelpfulMethods.sortByValue(map_doc2vec);
				int added = 0;
				int counter = 0;
				while (counter < l_doc2vec.size() && added < 4) {
					String key = l_doc2vec.get(counter).getKey();
					if (!prunedCandidates.contains(key)) {
						prunedCandidates.add(key);
						added++;
					}
					counter++;
				}*/
//				for (int i = 0; i < ENTITYTHRESHOLD; ++i) {
//					prunedCandidates.add(l_doc2vec.get(i).getKey());
//				}

				// Check for very relevant Candidates via given Word2Vec
				// similarities
				if (list.size() >= MINIMUMSURFACEFORMS) {
					System.out.println("Above min surface forms");
					Set<String> w2vFormatStrings = new HashSet<String>();
					for (String can : candidates) {
						if (!prunedCandidates.contains(can)) {
							String query = this.eckb.generateWord2VecFormatString(list, can);
							w2vFormatStrings.add(query);
						}
					}

					Map<String, Float> similarityMap = this.eckb.getWord2VecSimilarities(w2vFormatStrings);
					Map<String, Integer> occmap = new HashMap<String, Integer>();
					for (String can : candidates) {
						if (!prunedCandidates.contains(can)) {
							String query = this.eckb.generateWord2VecFormatString(list, can);
							float val = similarityMap.get(query);
							System.out.println(can + " word2vec val:" + val);
							if (val > WORD2VECTHRESHOLD) {
								System.out.println("In map:" + can);
								occmap.put(can, eckb.getFeatureDefinition().getHashedPrior(c.getSurfaceForm(), can));
//								prunedCandidates.add(can);
							}
						}
					}
					@SuppressWarnings("deprecation")
					List<Map.Entry<String, Integer>> sortedl = HelpfulMethods.sortByValue(occmap);
					for (int i = 0; i < NUMBEROFADDITIONALW2VENTITIES; ++i) {
						if (i < sortedl.size()) {
							prunedCandidates.add(sortedl.get(i).getKey());
						}
					}
				}

				c.setCandidates(new ArrayList<String>(prunedCandidates));
			}
		}
	}
}
