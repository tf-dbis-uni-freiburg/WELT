package welt.entitydisambiguation.algorithms.rules;

import java.util.List;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.knowledgebases.AbstractKnowledgeBase;

abstract class AbstractRule {

	protected AbstractKnowledgeBase eckb;
	
	AbstractRule(AbstractKnowledgeBase eckb) {
		super();
		this.eckb = eckb;
	}
	
	abstract boolean applyRule(List<SurfaceForm> rep);
	
}
