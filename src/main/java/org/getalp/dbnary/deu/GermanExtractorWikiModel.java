package org.getalp.dbnary.deu;

import java.util.Locale;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.fra.FrenchExtractorWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.rdf.model.Resource;

public class GermanExtractorWikiModel extends DbnaryWikiModel {
	

	private final static String germanMorphoBegin = "{{Deutsch ";
	private final static String germanMorphoEnd = "}}";
	
	
	private Logger log = LoggerFactory.getLogger(FrenchExtractorWikiModel.class);
	
	private WiktionaryDataHandler wdh;
	
	public GermanExtractorWikiModel(WiktionaryDataHandler wdh, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(wdh, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public 	GermanExtractorWikiModel(WiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.wdh = wdh;
//		setPageName(wdh.currentLexEntry());
	}
	
	public void addOtherForms(String page,String originalPos){
		String s = extractForms(page, originalPos);
		String[] tab = s.split("\n");
		for(String r : tab){
			int ind = r.indexOf("=");
			if(ind!=-1){
				wdh.registerOtherForm(r.substring(ind+1).replace("[","").replace("]",""));
			}
		}
	}
	
	
	private String extractForms(String s, String originalPos){

		String res=null,section=null;
				
		section=extractString(s, originalPos, "{{Wortart");
		res=extractString(section, germanMorphoBegin, germanMorphoEnd);
		
		if(res.isEmpty()){
			res = extractString(section, "Tabelle", germanMorphoEnd);
		}
		
		return res;
	}

	private String extractString(String s, String start, String end){
		String res;
		int startIndex,endIndex;
		startIndex=getIndexOf(s, start, 0);
		endIndex=getIndexOf(s, end, startIndex);
		res=s.substring(startIndex, endIndex);
		return res;
	}
	
	/**
	 * 
	 * @param s text where I look for regex
	 * @param pattern
	 * @return where pattern starts
	 */
	private int getIndexOf(String s, String pattern, int start){
		int ind = s.indexOf(pattern, start);
		if(ind==-1 || ind <=start || ind >s.length()){
			ind=s.length();
		}
		return ind;
	}
	
	
	
}
