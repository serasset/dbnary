package org.getalp.blexisma.wiktionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.getalp.blexisma.api.ConceptualVector;
import org.getalp.blexisma.api.ISO639_3;
import org.getalp.blexisma.api.SemanticDefinition;
import org.getalp.blexisma.api.SemanticDictionary;
import org.getalp.blexisma.api.SemanticNetwork;
import org.getalp.blexisma.api.Sense;
import org.getalp.blexisma.api.VectorialBase;
import org.getalp.blexisma.api.syntaxanalysis.MorphoProperties;

import static org.getalp.blexisma.api.syntaxanalysis.MorphoProperties.*;

public class WiktionaryBasedSemanticDictionary implements SemanticDictionary {
	private static final HashMap<String, List<MorphoProperties>> morphoProperties;
	private static final List<MorphoProperties> other = Arrays.asList(OTHER);
	static {
		morphoProperties = new HashMap<String, List<MorphoProperties>>(100);
        morphoProperties.put("#pos|-adj-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-/2", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-dém-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-excl-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-indéf-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-int-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-num-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adj-pos-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-adv-", Arrays.asList(ADVERB));
        morphoProperties.put("#pos|-adv-int-", Arrays.asList(ADVERB));
        morphoProperties.put("#pos|-adv-pron-", Arrays.asList(ADVERB));
        morphoProperties.put("#pos|-adv-rel-", Arrays.asList(ADVERB));
        morphoProperties.put("#pos|-aux-", Arrays.asList(VERB));
        morphoProperties.put("#pos|-loc-adj-", Arrays.asList(ADJECTIVE));
        morphoProperties.put("#pos|-loc-adv-", Arrays.asList(ADVERB));
        morphoProperties.put("#pos|-loc-nom-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-loc-verb-", Arrays.asList(VERB));
        morphoProperties.put("#pos|-nom-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-fam-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-ni-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-nu-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-nn-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-npl-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-pr-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-nom-sciences-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-onoma-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-prénom-", Arrays.asList(NOUN));
        morphoProperties.put("#pos|-verb-", Arrays.asList(VERB));
		morphoProperties.put("#pos|-verb-pr-", Arrays.asList(VERB));
		
		morphoProperties.put("#pos|Noun", Arrays.asList(NOUN));
		morphoProperties.put("#pos|Adjective", Arrays.asList(ADJECTIVE));
		morphoProperties.put("#pos|Adverb", Arrays.asList(ADVERB));
		morphoProperties.put("#pos|Verb", Arrays.asList(VERB));
		morphoProperties.put("#pos|Proper noun", Arrays.asList(NOUN));
		
		
       morphoProperties.put("#pos|Adjektiv", Arrays.asList(ADJECTIVE));
       morphoProperties.put("#pos|Adverb", Arrays.asList(ADVERB));
       morphoProperties.put("#pos|Eigenname", Arrays.asList(NOUN));
       morphoProperties.put("#pos|Erweiterter Infinitiv", Arrays.asList(VERB));
       morphoProperties.put("#pos|Fokuspartikel", Arrays.asList(OTHER));
       morphoProperties.put("#pos|Hilfsverb", Arrays.asList(VERB));
       morphoProperties.put("#pos|Interrogativadverb", Arrays.asList(ADVERB));
       morphoProperties.put("#pos|Konjugierte Form", Arrays.asList(VERB));
       morphoProperties.put("#pos|Nachname", Arrays.asList(NOUN));
       morphoProperties.put("#pos|Onomatopoetikum", Arrays.asList(NOUN));
       morphoProperties.put("#pos|Ortsnamen-Grundwort", Arrays.asList(NOUN));
       morphoProperties.put("#pos|Pronominaladverb", Arrays.asList(ADVERB));
       morphoProperties.put("#pos|Substantiv", Arrays.asList(NOUN));
       morphoProperties.put("#pos|Toponym", Arrays.asList(NOUN));
       morphoProperties.put("#pos|Verb", Arrays.asList(VERB));
       morphoProperties.put("#pos|Vorname", Arrays.asList(VERB));
       
		//	ADJECTIVE,ADVERB,NOUN,VERB,OTHER,MASCULINE,FEMININE,NEUTRAL,TRANSITIVE,INTRANSITIVE,DIRECTTRANSITIVE,SINGULAR, PLURAL;
	}
	
	private SemanticNetwork<String, String> wiktionaryNetwork;
	private VectorialBase vectorialBase;
	
	public WiktionaryBasedSemanticDictionary(VectorialBase vectorialBase, SemanticNetwork<String, String> wiktionaryNetwork) {
		this.wiktionaryNetwork = wiktionaryNetwork;
		this.vectorialBase = vectorialBase;
	}
	
	@Override
	public SemanticDefinition getDefinition(String txt, String lg) {
		String nodename = getNodeName(txt, lg);
		Collection<? extends SemanticNetwork<String,String>.Edge> edges = wiktionaryNetwork.getEdges(nodename);
		ArrayList<Sense> senses = new ArrayList<Sense>();
		
		for (SemanticNetwork<String,String>.Edge edge : edges) {
			if (edge.getRelation().equals(WiktionaryExtractor.DEF_RELATION)) {
				String def = edge.getDestination();
				ConceptualVector cv = vectorialBase.getVector(def);
				List<MorphoProperties> morph = getMorphoProperties(def);
				Sense s = new Sense(cv, morph);
				senses.add(s);
			}
		}
		ConceptualVector mcv = vectorialBase.getVector(nodename);
		return new SemanticDefinition(mcv, senses);
	}

	private static String getNodeName(String txt, String lg) {
		String lang = ISO639_3.sharedInstance.getIdCode(lg);
		return "#" + lang + "|" + txt;
	}

	@Override
	public ConceptualVector getVector(String txt, String lg) {
		return vectorialBase.getVector(getNodeName(txt, lg));
	}

	@Override
	public void setVector(String txt, String lg, ConceptualVector cv) {
		vectorialBase.addVector(getNodeName(txt, lg), cv);
	}

	
	private List<MorphoProperties> getMorphoProperties(String def) {
		Collection<? extends SemanticNetwork<String,String>.Edge> edges = wiktionaryNetwork.getEdges(def);
		for (SemanticNetwork<String, String>.Edge edge : edges) {
			if (edge.getRelation().equals(WiktionaryExtractor.POS_RELATION)) {
				List<MorphoProperties> morph = morphoProperties.get(edge.getDestination());
				return (null == morph) ? other : morph;
			}
		}
		return other;
	}

	public SemanticNetwork<String, String> getNetwork() {
		return wiktionaryNetwork;
	}

	public VectorialBase getBase() {
		return vectorialBase;
	}
	
	
}
