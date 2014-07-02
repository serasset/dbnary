package org.getalp.dbnary.deu;

import java.util.Locale;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.fra.FrenchExtractorWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.rdf.model.Resource;

public class GermanExtractorWikiModel extends DbnaryWikiModel {
	

	private final static String germanMorphoBegin = "{{Deutsch ";
	private final static String germanMorphoEnd = "}}";
	
	
	private Logger log = LoggerFactory.getLogger(FrenchExtractorWikiModel.class);
	
	private WiktionaryDataHandler wdh;
	
	public GermanExtractorWikiModel(WiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wdh, wi, locale, imageBaseURL, linkBaseURL);
		this.wdh=super.delegate;
	}
	
	public void parseConjugation(String conjugationTemplateCall) {
		// Render the conjugation to html, while ignoring the example template
		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);
		if (doc == null){
			return ;
		}
		
		NodeList tables =doc.getElementsByTagName("table");

		
		Element tablesItem =(Element) tables.item(3);
		getTablesConj(tablesItem, 3, 9, 2, 4);
		getTablesConj(tablesItem, 13, 19, 2, 4);
//		
		tablesItem =(Element) tables.item(2);
		getTablesConj(tablesItem, 2, 4, 1, 2);
		
		tablesItem =(Element) tables.item(1);
		getTablesConj(tablesItem,11 ,12 , 0, 2);

	}
	
	public void parseDeclination(String declinationTemplateCall){
		Document doc = wikicodeToHtmlDOM(declinationTemplateCall);
		if (doc == null){
			return ;
		}
		
		NodeList tables =doc.getElementsByTagName("table");
		for(int i=0;i<3;i++){
			Element tablesItem=(Element) tables.item(i);
			getTablesDeclination(tablesItem);
		}
		
		
	}
	
	public void parseOtherForm(String page,String originalPos){
		if(page!=null){
			String s = getForm(page, originalPos);
			if(s!=null){
				String[] tab = s.split("\n");
				for(String r : tab){
					if(r.indexOf("Hilfsverb")==-1 && r.indexOf("Weitere_Konjugationen")==-1){
						int ind = r.indexOf("=");
						if(ind!=-1){
//							System.out.println(r.substring(ind+1).replace("[","").replace("]",""));
							wdh.registerOtherForm(r.substring(ind+1));
						}
					}
				}
			}
		}
	}
	
	private void getTablesConj(Element tablesItem, int lbegin, int lend, int cbegin, int cend){
		for(int i=cbegin;i!=cend;i++){
			for(int j=lbegin;j!=lend;j++){
				NodeList interrestingTDs = ((Element)tablesItem.getElementsByTagName("tr").item(j)).getElementsByTagName("td");
				String form=interrestingTDs.item(i).getTextContent();
				if(!form.equals("—")){
					addForm(form);
				}
			}
		}
		
	
	}

	
	private void getTablesDeclination(Element tablesItem){
		int i=0;
		Element someTRs = ((Element)tablesItem.getElementsByTagName("tr").item(i));
		NodeList interrestingTDs;
		while(someTRs!=null){
			interrestingTDs=someTRs.getElementsByTagName("td");
			int l=interrestingTDs.getLength();
			if(l==8){
				for(int j=0;j<l;j=j+1){
					if((j%2)==1){
						wdh.registerOtherForm(interrestingTDs.item(j).getTextContent());
					}
				}
			}
			i=i+1;
			someTRs = ((Element)tablesItem.getElementsByTagName("tr").item(i));
			l=interrestingTDs.getLength();
		}
	}
	
	private String getForm(String s, String originalPos){

		String res=null,section=null;
				
		section=extractString(s, originalPos, "{{Wortart");
		res=extractString(section, germanMorphoBegin, germanMorphoEnd);
		
		if(res.isEmpty()){
			res = extractString(section, "Tabelle", germanMorphoEnd);
		}
		
		return res;
	}
	
	
	
	private void addForm(String s){
		int ind;
		s=s.replace(","," ").replace("\n"," ");
		String[] tab=s.split(" ");
		for(int i=0;i<tab.length;i++){
			//for normal Verb forms
			if(i%2==1){
				wdh.registerOtherForm(tab[i]);
//				System.out.println(tab[i]);
			}
			//for imperativs
			else if((ind=tab[i].indexOf("!"))!=-1){
				for(String r : tab[i].split("!")){
					wdh.registerOtherForm(r);
//					System.out.println(r);
				}
			}
			//for past and present particips 
			else if(tab.length==1){
				wdh.registerOtherForm(tab[i]);
			}
		}
			
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
