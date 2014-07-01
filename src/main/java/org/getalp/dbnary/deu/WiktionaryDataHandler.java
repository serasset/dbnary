package org.getalp.dbnary.deu;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.w3c.dom.Document;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.function.library.substr;

/**
 * 
 * @author suzan
 *
 */

public class WiktionaryDataHandler  extends LemonBasedRDFDataHandler{
	
	private final static Pattern conjugationPattern;
	private final static Pattern declinationPattern;
	
	private final static String conjugationStringPattern ="\\{\\{Deutsch Verb";
	private final static String declinationStringPattern ="\\{\\{Deutsch";
	private final static String germanDeclinationSuffix =" (Deklination)";
	private final static String germanConjugationSuffix =" (Konjugation)";
	
	
	static{
		conjugationPattern = Pattern.compile(conjugationStringPattern);
		declinationPattern = Pattern.compile(declinationStringPattern);
	}
	
	public WiktionaryDataHandler(String lang) {
		super(lang);
	}
	
	
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType){
		super.addPartOfSpeech(originalPOS,normalizedPOS,normalizedType);
		if (((normalizedType == wordEntryType) || (normalizedType == lexEntryType))) {
			if(normalizedPOS==verbPOS){
				String conjugationPageContent = wi.getTextOfPage(currentLexEntry()+germanConjugationSuffix);
				GermanExtractorWikiModel gewm = new GermanExtractorWikiModel(this, wi, new Locale("de"), "/${Bild}", "/${title}");
				if(conjugationPageContent!=null){
					gewm.parseConjugation(conjugationPageContent);
				}
				else{
					gewm.addOtherDesi(wi.getTextOfPage(currentLexEntry()), originalPOS);
				}
				
			}
			else{
				if(!originalPOS.equals("Konjugierte Form")){
					String declinationPageContent = wi.getTextOfPage(currentLexEntry()+germanDeclinationSuffix);
					GermanExtractorWikiModel gewm = new GermanExtractorWikiModel(this, wi, new Locale("de"), "/${Bild}", "/${title}");
					if(declinationPageContent!=null){
						gewm.parseDeclination(declinationPageContent);
					}
					else{
						gewm.addOtherDesi(wi.getTextOfPage(currentLexEntry()), originalPOS);
					}
				}
			}
		}
	}

	public void registerOtherForm(String form){
		super.registerOtherForm(form.replace("\n", "").replace("[","").replace("]", ""));
		
	}
	
	
//	/**
//	 * 
//	 * @param normalizedPos
//	 * @return a String to find the beginning of the description
//	 */
//	private String getPosString(Resource normalizedPos){
//		String res=germanMorphoBegin;
//		if (normalizedPos==verbPOS){
//			res=germanMorphoBegin+verbCat;
//		} else if(normalizedPos==adjPOS){
//			res=germanMorphoBegin+adjCat;
//		} else if(normalizedPos==nounPOS){
//			res=germanMorphoBegin+nounCat;
//		} else if(normalizedPos==adverbPOS){
//			res=germanMorphoBegin+advCat;
//		}
//		return res;
//	}

	
}
