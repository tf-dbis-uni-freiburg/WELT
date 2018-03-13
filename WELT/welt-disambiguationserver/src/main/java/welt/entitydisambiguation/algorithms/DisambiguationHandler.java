package welt.entitydisambiguation.algorithms;

import welt.entitydisambiguation.algorithms.collective.general.CollectiveDisambiguationGeneralEntities;
import welt.entitydisambiguation.algorithms.collective.wikidata.CollectiveDisambiguationWikidataEntities;
import welt.entitydisambiguation.backend.AbstractDisambiguationTask;
import welt.entitydisambiguation.backend.DisambiguationTaskSingle;
import welt.entitydisambiguation.dpo.EntityDisambiguationDPO;
import welt.entitydisambiguation.knowledgebases.KnowledgeBaseIdentifiers;

public class DisambiguationHandler {

	private static final DisambiguationHandler instance;

	static {
		try {
			instance = new DisambiguationHandler();
		} catch (Exception e) {
			throw new RuntimeException("An error occurred!", e);
		}
	}

	private DisambiguationHandler() {
		super();
	}

	public static DisambiguationHandler getInstance() {
		return instance;
	}

	public AbstractDisambiguationAlgorithm getAlgorithm(AbstractDisambiguationTask task) {
		AbstractDisambiguationAlgorithm algorithm = null;
		if (task instanceof DisambiguationTaskSingle) {
			DisambiguationTaskSingle t = (DisambiguationTaskSingle) task;
			EntityDisambiguationDPO dpo = t.getEntityToDisambiguate();
			if ((dpo.getSetting() != null
					&& (dpo.getSetting().equalsIgnoreCase("NoContext"))
					|| dpo.getContext() == null || dpo.getContext().equals("") || dpo
					.getContext().equals(" "))) {
				algorithm = new EntityCentricAlgorithmTableDefault();
			} else if ((dpo.getSetting() != null)
					&& (dpo.getSetting().equalsIgnoreCase("DocumentCentric"))) {
				//System.out.println("Document Centric Test");
				algorithm = new DocumentCentricAlgorithmDefault();
			} else {
				//System.out.println("Entity Centric Test");
				algorithm = new EntityCentricAlgorithmDefault();
			}
		} else {
			if (task.getKbIdentifier().equals(KnowledgeBaseIdentifiers.Biomed)) {
				//System.out.println("Nope Centric Test");
				algorithm = new CollectiveDisambiguationGeneralEntities();
			} else {
				//System.out.println("Nope 2 Centric Test");
				algorithm = new CollectiveDisambiguationWikidataEntities();
			}
		}
		return algorithm;
	}
}
