package org.getalp.dbnary.deu;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.DbnaryModel;
import org.getalp.dbnary.LemonBasedRDFDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.WiktionaryIndexerException;
import org.getalp.dbnary.wiki.ExpandAllWikiModel;
import org.getalp.dbnary.wiki.WikiTool;

import com.hp.hpl.jena.rdf.model.Resource;

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

	private final static String germanMorphoBegin = "\\{\\{Deutsch ";
	private final static String germanMorphoEnd = "\\}\\}";
	
	
	public WiktionaryDataHandler(String lang) {
		super(lang);
	}
	
	
	public void addPartOfSpeech(String originalPOS, Resource normalizedPOS, Resource normalizedType){
		super.addPartOfSpeech(originalPOS,normalizedPOS,normalizedType);
		
		if (((normalizedType == wordEntryType) || (normalizedType == lexEntryType))) {
			System.out.println("cat : "+originalPOS);
			String lexEntry=currentLexEntry();
			String page=wi.getTextOfPage(lexEntry);
			String formes=extractForms(page,normalizedPOS);
			if (formes!=null) {
				//printing the forms
				//System.out.println(formes);
			}
		}
	}
	
	public String extractForms(String s, Resource normalizedPos){
		String catWord=getPosString(normalizedPos);
		
		if (catWord!=null) {
			Pattern patternBegin= Pattern.compile(catWord);
			Pattern patternEnd = Pattern.compile(germanMorphoEnd);
			Matcher m = patternBegin.matcher(s);
			int start,end;
			
			if (m.find()) {
			start=m.end();
			m = patternEnd.matcher(s);
				if (m.find(start)) {
					end=m.start();
					return s.substring(start, end);
				}
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param normalizedPos
	 * @return a String to find the beginning of the description
	 */
	private String getPosString(Resource normalizedPos){
		String res;
		if (normalizedPos==verbPOS){	
			res=germanMorphoBegin+verbCat;
		} else if(normalizedPos==adjPOS){
			res=germanMorphoBegin+adjCat;
		} else if(normalizedPos==nounPOS){
			res=germanMorphoBegin+nounCat;
		} else if(normalizedPos==adverbPOS){
			res=germanMorphoBegin+advCat;
		} else {
			res=null;
		}
		return res;
	}
	
}
