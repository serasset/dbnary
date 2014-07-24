package org.getalp.dbnary.deu;

import java.util.Locale;
import java.util.regex.Pattern;

import org.getalp.dbnary.LemonBasedRDFDataHandler;

import com.hp.hpl.jena.rdf.model.Resource;

import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.LemonOnt;

/**
 * 
 * @author suzan
 *
 */

public class WiktionaryDataHandler  extends LemonBasedRDFDataHandler{
	
//	//for some modal verbs
//	posAndTypeValueMap.put("Hilfsverb", new PosAndType(LexinfoOnt.verb, LemonOnt.LexicalEntry));
	private final static Pattern conjugationPattern;
	private final static Pattern declinationPattern;
	private final static Pattern basedFormPattern;
	
	private final static String conjugationStringPattern ="\\{\\{Deutsch Verb";
	private final static String declinationStringPattern ="\\{\\{Deutsch";
	private final static String germanDeclinationSuffix =" (Deklination)";
	private final static String germanConjugationSuffix =" (Konjugation)";
	private final static String basedFormPrefix="\\{\\{Grundformverweis\\|(.*)\\}\\}";
	
	static{
		basedFormPattern=Pattern.compile(basedFormPrefix);
		conjugationPattern = Pattern.compile(conjugationStringPattern);
		declinationPattern = Pattern.compile(declinationStringPattern);
	}
	
	public WiktionaryDataHandler(String lang) {
		super(lang);
	}
	
	
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType){
		super.addPartOfSpeech(originalPOS,normalizedPOS,normalizedType);
		
		GermanExtractorWikiModel gewm = new GermanExtractorWikiModel(this, wi, new Locale("de"), "/${Bild}", "/${Titel}");
		
		//problem with deklination...
		if (((normalizedType == LemonOnt.Word) || (normalizedType == LemonOnt.LexicalEntry))) {
			if(normalizedPOS==LexinfoOnt.verb){
				String conjugationPageContent = wi.getTextOfPage(currentLexEntry()+germanConjugationSuffix);
				if(conjugationPageContent!=null){
					gewm.parseConjugation(conjugationPageContent, originalPOS);
				}
				else{
					gewm.parseOtherForm(wi.getTextOfPage(currentLexEntry()), originalPOS);
				}
				
			}
			else{
				
//				if(!originalPOS.equals("Konjugierte Form") && !originalPOS.equals("Deklinierte Form")){
					String declinationPageContent = wi.getTextOfPage(currentLexEntry()+germanDeclinationSuffix);
					if(declinationPageContent!=null){
						gewm.parseDeclination(declinationPageContent, originalPOS);
					}
					else{
						gewm.parseOtherForm(wi.getTextOfPage(currentLexEntry()), originalPOS);
					}
//				}
//				else{
//					//add the infinitiv form if the current lex Entry is inflected
//					Matcher m = basedFormPattern.matcher(wi.getTextOfPage(currentLexEntry()));
//					if(m.find()){
//						this.registerOtherForm(m.group(1));
//						String pageContent = lexEntryToPage(m.group(1));
//						if(null!= pageContent){
//							if(originalPOS.equals("Konjugierte Form")){
//								gewm.parseConjugation(pageContent);
//							}
//							else{
//								gewm.parseDeclination(pageContent);
//							}
//						}
//					}
//				}
			}
		}
	}
	
	
	private String lexEntryToPage(String lexEntry){
		int i=0;
		String[] suffix={germanConjugationSuffix,germanDeclinationSuffix,""};
		String pageContent = null;

			while(null==pageContent && i< suffix.length){
				pageContent=wi.getTextOfPage(lexEntry+suffix[i]);
				i++;
			}
		return pageContent;
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
//		if (normalizedPos==LexinfoOnt.verb){
//			res=germanMorphoBegin+verbCat;
//		} else if(normalizedPos==LexinfoOnt.adjective){
//			res=germanMorphoBegin+adjCat;
//		} else if(normalizedPos==LexinfoOnt.noun){
//			res=germanMorphoBegin+nounCat;
//		} else if(normalizedPos==LexinfoOnt.adverb){
//			res=germanMorphoBegin+advCat;
//		}
//		return res;
//	}

	
}
