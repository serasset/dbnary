package org.getalp.dbnary.pol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.LemonOnt;
import org.getalp.dbnary.LexinfoOnt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class WiktionaryDataHandler extends LemonBasedRDFDataHandler {

	private Logger log = LoggerFactory.getLogger(WiktionaryDataHandler.class);
	
	private Map<String,Resource> currentWordsenses = new HashMap<String,Resource>();
	
	class DecodedPOS {
		class PropValPair {
			Resource prop, val;
			public PropValPair(Resource p, Resource v) {this.prop = p; this.val = v;}
		}
		String simplePOSName;
		Resource lexinfoPOS;
		Resource entryType;
		
		ArrayList<PropValPair> additionalProps = new ArrayList<PropValPair>();

		public DecodedPOS(String sn, Resource pos, Resource type) {
			this.simplePOSName = sn;
			this.lexinfoPOS = pos;
			this.entryType = type;
		}
		
		public void addAnnotation(Resource prop, Resource val) {
			additionalProps.add(new PropValPair(prop, val));
		}
	}
	
	public WiktionaryDataHandler(String lang) {
		super(lang);
	}

	@Override
	public void initializeEntryExtraction(String wiktionaryPageName) {
        super.initializeEntryExtraction(wiktionaryPageName);
        
        currentWordsenses.clear();
    }
	

	@Override
	public void registerNewDefinition(String def, String senseNumber) {
		super.registerNewDefinition(def, senseNumber);
		if (null != this.currentLexEntry) {
			currentWordsenses.put(senseNumber, this.currentSense);
		}
	}
	
	public void registerNymRelation(String target, String synRelation, String senseNum) {
		Resource ws = currentWordsenses.get(senseNum);
		registerNymRelationToEntity(target, synRelation, ws);
    }
	
	private DecodedPOS decodePOS(String group) {
		String orig = new String(group);
		if (group.contains("forma")) return null;
		
		if (group.startsWith("rzeczownik")
			|| group.startsWith("przymiotnik")
			|| group.startsWith("czasownik")
			|| group.startsWith("przysłówek")
			|| group.startsWith("fraza")
			|| group.startsWith("związek frazeologiczny"))
			group = group.split("''|\\(|<")[0];
		
		DecodedPOS dpos = computeDecodedPOS(group);
		if (dpos != null) {
			if (group.contains("rodzaj żeński/męski") || group.contains("rodzaj męski/żeński") 
					|| group.contains("rodzaj męski lub żeński") || group.contains("rodzaj żeński, męski")) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.feminine);
				group = group.replace("rodzaj żeński/męski", "");
				group = group.replace("rodzaj męski/żeński", "");
			}
			if (group.contains("rodzaj żeński") || group.contains("rodzaju żeńskiego") || group.contains("lub żeński") || group.contains("i żeński")) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.feminine);
				group = group.replace("rodzaj żeński", "");
				group = group.replace("rodzaju żeńskiego", "");
				group = group.replace("lub żeński", "");
				group = group.replace("i żeński", "");
			}
			if (group.contains("rodzaj nijaki") || group.contains("rodzaju nijakiego") || group.contains("lub nijaki")) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.neuter);
				group = group.replace("rodzaj nijaki", "");
				group = group.replace("rodzaju nijakiego", "");
				group = group.replace("lub nijaki", "");
			}
			if (group.contains("rodzaj męski") || group.contains("rodzaju męskiego") || group.contains("lub męski") || group.contains(", męski")) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
				group = group.replace("rodzaj męski", "");
				group = group.replace("rodzaju męskiego", "");
				group = group.replace("lub męski", "");
			}
			if (group.contains("rodzaj męskorzeczowy") || group.contains("rodzaju męskorzeczowego") || group.contains("lub męskorzeczowy") ) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
				dpos.addAnnotation(LexinfoOnt.animacy, DbnaryModel.inanimate);
				group = group.replace("rodzaj męskorzeczowy", "");
				group = group.replace("rodzaju męskorzeczowego", "");
				group = group.replace("lub męskorzeczowy", "");
			}
			if (group.contains("rodzaj męskozwierzęcy") || group.contains("lub męskozwierzęcy")) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
				dpos.addAnnotation(LexinfoOnt.animacy, DbnaryModel.animate);
				group = group.replace("rodzaj męskozwierzęcy", "");
				group = group.replace("lub męskozwierzęcy", "");
			}
			if (group.contains("rodzaj męskoosobowy") || group.contains("lub męskoosobowy")) {
				dpos.addAnnotation(LexinfoOnt.gender, LexinfoOnt.masculine);
				dpos.addAnnotation(LexinfoOnt.animacy, DbnaryModel.animate);
				dpos.addAnnotation(LexinfoOnt.referentType, LexinfoOnt.personal);
				group = group.replace("rodzaj męskoosobowy", "");
				group = group.replace("lub męskoosobowy", "");
			}
			if (group.contains("rodzaj niemęskoosobowy")) {
				// What is this ? non-masculine ?

				group = group.replace("rodzaj niemęskoosobowy", "");
			} 
			if (group.contains("nieżywotny")) {
				dpos.addAnnotation(LexinfoOnt.animacy, DbnaryModel.inanimate);
				group = group.replace("nieżywotny", "");
			}
			if (group.contains("lub męskorzeczowy")) {
				dpos.addAnnotation(LexinfoOnt.animacy, DbnaryModel.inanimate);
				group = group.replace("lub męskorzeczowy", "");
			}
			if (group.contains("liczebnik")) {
				dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.numeral);
				group = group.replace("liczebnik", "");
			} 
			
			if (group.contains("nieprzechodnia") || group.contains("nieprzechodni")) {
				dpos.addAnnotation(LemonOnt.synBehavior, LexinfoOnt.IntransitiveFrame);
				group = group.replace("nieprzechodnia", "");
				group = group.replace("nieprzechodni", "");
			}
			if (group.contains("przechodnia") || group.contains("przechodni")) {
				dpos.addAnnotation(LemonOnt.synBehavior, LexinfoOnt.TransitiveFrame);
				group = group.replace("przechodnia", "");
				group = group.replace("przechodni", "");
			}
			
			if (group.contains("niedokonany") || group.contains("niedokonana")) {
				dpos.addAnnotation(LexinfoOnt.aspect, LexinfoOnt.imperfective);
				group = group.replace("niedokonany", "");
				group = group.replace("niedokonana", "");
			}
			if (group.contains("dokonany") || group.contains("dokonana")) {
				dpos.addAnnotation(LexinfoOnt.aspect, LexinfoOnt.perfective);
				group = group.replace("dokonany", "");
				group = group.replace("dokonana", "");
			}
			if (group.contains("zwrotny")) {
				dpos.addAnnotation(LemonOnt.synBehavior, LexinfoOnt.ReflexiveFrame);
				group = group.replace("zwrotny", "");
			}
			if (group.contains("liczba mnoga")) {
				dpos.addAnnotation(LexinfoOnt.number, LexinfoOnt.plural);
				group = group.replace("liczba mnoga", "");
			}
			if (group.contains("zbiorowy")) {
				dpos.addAnnotation(LexinfoOnt.number, LexinfoOnt.collective);
				group = group.replace("zbiorowy", "");
			}
			
			if (group.contains("w funkcji rzeczownika")) {
				dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.noun);
				group = group.replace("w funkcji rzeczownika", "");
			}
			
			if (group.contains("w użycu przysłówkowym")) {
				dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adverb);
				group = group.replace("w użycu przysłówkowym", "");
			} 
			if (group.contains("w użyciu przymiotnikowym")) {
				dpos.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adjective);
				group = group.replace("w użyciu przymiotnikowym", "");
			} 
			
			
			
			
			group = group.replace("nazwa własna", "");
			group = group.replace("fraza rzeczownikowa", "");
			group = group.replace("fraza przymiotnikowa", "");
			group = group.replace("fraza czasownikowa", "");
			group = group.replace("fraza przysłówekowa", "");
			group = group.replace("rzeczownik", "");
			group = group.replace("przymiotnik dzierżawczy", "");
			group = group.replace("przymiotnik", "");
			group = group.replace("czasownik modalny", "");
			group = group.replace("czasownik ułomny", "");
			group = group.replace("czasownik", "");
			group = group.replace("przysłówek", "");
			group = group.replace("związek frazeologiczny", "");
			group = group.replace("{{przysłowie polskie}}", "");
			group = group.replace("skrótowiec", "");
			
			group = group.replaceAll("[\\.,\\?i]", "");
			group = group.trim();
			
			// TODO: check if there are remaining elements in pos.
			if (group.length() > 0) log.debug("Did not parse {} in MorphoSyntactic info \"{}\"", group, orig );
		}
		
		return dpos;
	}
	
	
	private DecodedPOS computeDecodedPOS(String group) {
		if (group.startsWith("rzeczownik")) {
			if (group.contains("nazwa własna"))
				//TODO: remove dependency to DBnaryModel
				return new DecodedPOS("rzeczownik_nazwa_własna", LexinfoOnt.properNoun, LemonOnt.Word);
			else
				return new DecodedPOS("rzeczownik", LexinfoOnt.noun, LemonOnt.Word);
		} else if (group.startsWith("przymiotnik dzierżawczy")) {
			DecodedPOS res = new DecodedPOS("przymiotnik_dzierżawczy", LexinfoOnt.possessiveAdjective, LemonOnt.Word);
			res.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.adjective);
			return res;
		} else if (group.startsWith("przymiotnik")) {
			return new DecodedPOS("przymiotnik", LexinfoOnt.adjective, LemonOnt.Word);
		} else if (group.startsWith("czasownik modalny")) {
			DecodedPOS res = new DecodedPOS("czasownik_modalny", LexinfoOnt.modal, LemonOnt.Word);
			res.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.verb);
			return res;
		} else if (group.startsWith("czasownik ułomny")) {
			DecodedPOS res = new DecodedPOS("czasownik_modalny", LexinfoOnt.modal, LemonOnt.Word);
			res.addAnnotation(LexinfoOnt.partOfSpeech, LexinfoOnt.deficientVerb);
			return res;
		} else if (group.startsWith("czasownik")) {
			return new DecodedPOS("czasownik", LexinfoOnt.verb, LemonOnt.Word);
		} else if (group.startsWith("przysłówek")) {
			return new DecodedPOS("przysłówek", LexinfoOnt.adverb, LemonOnt.Word);
		} else if (group.startsWith("fraza")) {
			if (group.contains("rzeczownikowa")) {
				if (group.contains("nazwa własna"))
					return new DecodedPOS("rzeczownik_nazwa_własna", LexinfoOnt.properNoun, LemonOnt.Phrase);
				else
					return new DecodedPOS("rzeczownik", LexinfoOnt.noun, LemonOnt.Phrase);
			} else if (group.contains("przymiotnikowa")) {
				return new DecodedPOS("przymiotnik", LexinfoOnt.adjective, LemonOnt.Phrase);
			} else if (group.contains("czasownikowa")) {
				return new DecodedPOS("czasownik", LexinfoOnt.verb, LemonOnt.Phrase);
			} else if (group.contains("przysłówekowa") || group.contains("przysłówkowa")) {
				return new DecodedPOS("przysłówek", LexinfoOnt.adverb, LemonOnt.Phrase);
			} else if (group.contains("wykrzyknikowa")) {
				return new DecodedPOS("wykrzyknik", LexinfoOnt.interjection, LemonOnt.Phrase);
			}
		} else if (group.startsWith("związek frazeologiczny")) {
			return new DecodedPOS("związek frazeologiczny", LexinfoOnt.idiom, LemonOnt.Phrase);
		} else if (group.startsWith("{{przysłowie polskie")) {
			return new DecodedPOS("przysłowie", LexinfoOnt.proverb, LemonOnt.Phrase);
		} else if (group.startsWith("skrótowiec")) {
			return new DecodedPOS("skrótowiec", LexinfoOnt.acronym, LemonOnt.Word);
		} else if (group.startsWith("skrót")) {
			return new DecodedPOS("skrót", LexinfoOnt.abbreviation, LemonOnt.Word);
		} else if (group.startsWith("zaimek")) {
			if (group.contains("pytajny")) {
				return new DecodedPOS("zaimek_pytajny", LexinfoOnt.interrogativePronoun, LexinfoOnt.Pronoun);
			} else if (group.contains("nieokreślony")) {
				return new DecodedPOS("zaimek_nieokreślony", LexinfoOnt.indefinitePronoun, LexinfoOnt.Pronoun);
			} else if (group.contains("osobowy")) {
				return new DecodedPOS("zaimek_osobowy", LexinfoOnt.personalPronoun, LexinfoOnt.Pronoun);
			} else {
				// TODO add other type of pronouns
				return new DecodedPOS("zaimek", LexinfoOnt.pronoun, LexinfoOnt.Pronoun);
			}
		} else if (group.startsWith("wykrzyknik")) {
			return new DecodedPOS("wykrzyknik", LexinfoOnt.interjection, LexinfoOnt.Interjection);
		} else if (group.startsWith("partykuła")) {
			return new DecodedPOS("partykuła", LexinfoOnt.particle, LexinfoOnt.Particle);
		} else if (group.startsWith("spójnik")) {
			return new DecodedPOS("spójnik", LexinfoOnt.conjunction, LexinfoOnt.Conjunction);
		} else if (group.startsWith("przyimek")) {
			return new DecodedPOS("przyimek", LexinfoOnt.preposition, LexinfoOnt.Preposition);
		} else if (group.startsWith("liczebnik")) {
			if (group.contains("porządkowy")) {
				return new DecodedPOS("liczebnik_porządkowy", LexinfoOnt.ordinalAdjective, LemonOnt.Word);
			} else if (group.contains("mnożny")) {
				return new DecodedPOS("liczebnik_mnożny", LexinfoOnt.multiplicativeNumeral, LexinfoOnt.Numeral);
			} else if (group.contains("główny")) {
				return new DecodedPOS("liczebnik_główny", LexinfoOnt.cardinalNumeral, LexinfoOnt.Numeral);
			} else if (group.contains("zbiorowy")) {
				return new DecodedPOS("liczebnik_zbiorowy", LexinfoOnt.collective, LexinfoOnt.Numeral);
			} else if (group.contains("nieokreślony")) {
				return new DecodedPOS("liczebnik_nieokreślony", LexinfoOnt.indefiniteCardinalNumeral, LexinfoOnt.Numeral);
			} else if (group.contains("ułamkowy")) {
				return new DecodedPOS("liczebnik_ułamkowy", LexinfoOnt.numeralFraction, LexinfoOnt.Numeral);
			} else {
				// TODO add other type of pronouns
				return new DecodedPOS("liczebnik", LexinfoOnt.numeral, LexinfoOnt.Numeral);
			}
		}
		
		return null;
	}

	@Override
	public void addPartOfSpeech(String pos) {
    	// TODO: normalize POS and register a new lexical entry using abbreviated pos id.
		// TODO: extract simplified POS then register all category informations
		// DONE: register the normalized POS.
		DecodedPOS dpos = decodePOS(pos);
		
		if (dpos != null)
			super.addPartOfSpeech(dpos.simplePOSName, dpos.lexinfoPOS, dpos.entryType);
		else {
			this.voidPartOfSpeech();
			log.debug("Could not register a POS for {}", pos);
		}
	}

	public void voidPartOfSpeech() {
		// DONE: create a LexicalEntry for this part of speech only and attach info to it.
		currentWiktionaryPos = null;
		currentLexinfoPos = null;

		currentEncodedPageName = null;
		currentLexEntry = null;
		
		currentSense = null;
	}

	public boolean posIsValid() {
		return currentWiktionaryPos != null;
	}
	
}
