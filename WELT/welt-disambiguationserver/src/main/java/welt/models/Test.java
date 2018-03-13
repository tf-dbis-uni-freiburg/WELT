package welt.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Test {
	public static void main(String[] args) {
		String paragraph = "Washington, D.C., formally the District of Columbia and commonly referred to as Washington, the District, or simply D.C., is the capital of the United States. The signing of the Residence Act on July 16, 1790, approved the creation of a capital district located along the Potomac River on the country's East Coast. The U.S. Constitution provided for a federal district under the exclusive jurisdiction of the Congress and the District is therefore not a part of any U.S. state.";
		List<String> sentences = new ArrayList<String>();
	    List<String> tokens = new ArrayList<String>();
	    
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation annotation = pipeline.process(paragraph);
        for (CoreMap sentence : annotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            sentences.add(String.format("%d %d %s",
                    sentence.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
                    sentence.get(CoreAnnotations.CharacterOffsetEndAnnotation.class),
                    sentence.get(CoreAnnotations.TextAnnotation.class)));
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                tokens.add(String.format("%d %d %s %s",
                        token.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class),
                        token.get(CoreAnnotations.CharacterOffsetEndAnnotation.class),
                        token.get(CoreAnnotations.TextAnnotation.class),
                        token.get(CoreAnnotations.OriginalTextAnnotation.class)));
            }
        }
        
        for(String sentence : sentences) {
        	System.out.println(sentence);
        }
        
        for(String token : tokens) {
        	System.out.println(token);
        }
        
	}
}
