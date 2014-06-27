package org.getalp.dbnary.deu;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.LemonBasedRDFDataHandler;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.function.library.substr;

/**
 * 
 * @author suzan
 *
 */

public class WiktionaryDataHandler  extends LemonBasedRDFDataHandler{
	
	private final static String verbCat="Verb";
	private final static String nounCat="Substantiv";
	private final static String adjCat="Adjektiv";
	private final static String absAdjCat="Absolutadjektiv";
	private final static String partCat="Partizip";
	private final static String advCat="Adverb";
	private final static String worBindingCat="Wortverbindung";

	private final static String germanMorphoBegin = "{{Deutsch ";
	private final static String germanMorphoEnd = "}}";
	
	
	public WiktionaryDataHandler(String lang) {
		super(lang);
	}
	
	
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType){
		super.addPartOfSpeech(originalPOS,normalizedPOS,normalizedType);
		if (((normalizedType == wordEntryType) || (normalizedType == lexEntryType))) {
			String lexEntry=currentLexEntry();
			String page=wi.getTextOfPage(lexEntry);
			GermanExtractorWikiModel gewm= new GermanExtractorWikiModel(this, wi, new Locale("de"),"/${image}", "/${title}");
			gewm.addOtherForms(page, originalPOS);
		}
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
