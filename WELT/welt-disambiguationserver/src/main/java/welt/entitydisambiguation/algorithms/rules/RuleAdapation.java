package welt.entitydisambiguation.algorithms.rules;

import java.util.ArrayList;
import java.util.List;

import welt.entitydisambiguation.algorithms.SurfaceForm;
import welt.entitydisambiguation.knowledgebases.AbstractKnowledgeBase;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBDBpedia;
import welt.entitydisambiguation.knowledgebases.EntityCentricKBWikidata;

public class RuleAdapation {

	private List<AbstractRule> ruleChain;
	
	public RuleAdapation() {
		super();
		this.ruleChain = new ArrayList<AbstractRule>();
	}
	
	public void addNoCandidatesCheckPluralRule(AbstractKnowledgeBase eckb) {
		this.ruleChain.add(new NoCandidatesCheckPlural(eckb));
	}
	
	public void addNoCandidatesExpansionRule(AbstractKnowledgeBase eckb) {
		this.ruleChain.add(new NoCandidatesExpansionRules(eckb));
	}
	
	public void addUnambiguousToAmbiguousRule(EntityCentricKBWikidata eckb) {
		this.ruleChain.add(new UnambiguousToAmbiguousRule(eckb));
	}
	
	public void addPatternRule(EntityCentricKBWikidata eckb, String topic) {
		if (topic != null) {
			this.ruleChain.add(new PatternRule(eckb));
		}
	}
	
	public void addContextRule(EntityCentricKBWikidata eckb) {
		this.ruleChain.add(new ContextRule(eckb));
	}

	public void performRuleChainBeforeCandidateSelection(List<SurfaceForm> rep) {
		for (AbstractRule r : ruleChain) {
			r.applyRule(rep);
		}
	}
}
