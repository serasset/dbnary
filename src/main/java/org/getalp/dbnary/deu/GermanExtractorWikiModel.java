package org.getalp.dbnary.deu;

import java.security.acl.LastOwnerException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.iri.impl.ViolationCodeInfo.InSpec;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.fra.FrenchExtractorWikiModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.rdf.model.Resource;

public class GermanExtractorWikiModel extends DbnaryWikiModel {
	

	private final static String germanMorphoBegin = "{{Deutsch ";
	private final static String germanMorphoEnd = "}}";
	private final static String germanRegularVerbString=" regelmäßig";
	private static Pattern germanRegularVerbPattern;
	
	static{
		germanRegularVerbPattern= Pattern.compile(germanRegularVerbString);
	}
	
	private Logger log = LoggerFactory.getLogger(GermanExtractorWikiModel.class);
	
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

		Element tablesItem =(Element) tables.item(1);
		getTablesConj(tablesItem,11,0);
		
		
		tablesItem =(Element) tables.item(2);
		getTablesConj(tablesItem,2,1,3,2);
//		
		
		tablesItem =(Element) tables.item(3);
		
		Matcher m = germanRegularVerbPattern.matcher(conjugationTemplateCall);
		if(m.find()){
			getTablesConj(tablesItem,2,1);
			getTablesConj(tablesItem,11,1);
		}
		else{
			getTablesConj(tablesItem,3,2);
			getTablesConj(tablesItem,13,2);
		}
		
		
		
		
		
		

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
			s=s.replace("<br />","\n");
			if(s!=null){
				String[] tab = s.split("\n");
				for(String r : tab){
					if(r.indexOf("Hilfsverb")==-1 && r.indexOf("Bild")==-1 && r.indexOf("Weitere_Konjugationen")==-1){
        				int ind = r.indexOf("=");
						if(ind!=-1){
							String e=extractString(r, "=", "\n").substring(1);
							if(!originalPos.equals("Verb")){
								if (-1!=e.indexOf(" ")){
									e=extractString(e, " ", "\n");
									if(!e.isEmpty()){
										e=e.substring(1);
									}
								}
							}
//							System.out.println(e);
//							System.out.println(e.replace("[","").replace("]",""));
							e=e.replace("—","");
							if(!e.isEmpty()){
								wdh.registerOtherForm(e.replace("!",""));
							}
						}
			       		}
			        }
		        }
	        }
        }
	
	private void getTablesConj(Element tablesItem, int iBegin, int jBegin){
		int iEnd,jEnd;
		iEnd=iBegin+8;
		jEnd=jBegin+2;
		getTablesConj(tablesItem, iBegin, jBegin, iEnd, jEnd);
	}
	private String part="";
	
	private void getTablesConj(Element tablesItem, int iBegin, int jBegin, int iEnd, int jEnd){
		
		boolean change=false;	//this bool changes if the current verb is a phrasal verb
		
		NodeList someTRs = tablesItem.getElementsByTagName("tr");
		
		for(int i=iBegin;i<iEnd;i++){
			Element trItem= (Element)someTRs.item(i);
			if(trItem!=null){
				NodeList interrestingTDs = trItem.getElementsByTagName("td") ;
					for(int j=jBegin;j<jEnd;j++){
						Element tdItem=(Element)interrestingTDs.item(j);
						if(tdItem!=null){
							NodeList itemsList = tdItem.getChildNodes();
							for(int e=0; e<itemsList.getLength();e++){
								String form=itemsList.item(e).getTextContent().replace("—","");
								String name=itemsList.item(e).getNodeName();
								form=form.replace(" "," ");//remove insecable spaces
								form =removeUselessSpaces(form);
								if (name.equals("#text") && !form.isEmpty()) {
									// for verbs like ankommen : ich komme an
									if (!change && isPhrasalVerb(form) ) {
										part=(form.substring(form.lastIndexOf(" "))).replace(" ","").replace("\n","");
										if(!part.isEmpty()){
											change= true;
											iBegin=iBegin+1;
											iEnd=iEnd+1;
											jEnd=jEnd+2;
										}
									}
//									System.out.println("i : "+i+" j : "+j+"  form : "+form);
									addVerbForm(form, change);
								}
							}
						}
					}
			}
		}
	
	}
	
	private String removeUselessSpaces(String form){
		String res=form.replace("  "," ");
		while(!res.isEmpty() && res.charAt(0)==' '){
			res=res.substring(1);
		}
		while(!res.isEmpty() && res.length()!=0 && res.charAt(res.length()-1)==' '){
			res=res.substring(0,res.length()-1);
		}
		return res;
	}

	private boolean isPhrasalVerb(String form){
		return nbSpaceForm(form)>=2;
	}
	
	private int nbSpaceForm(String form){
		int nbsp=0;
		for(int i=1; i<form.length()-1;i++){
			if(form.charAt(i)==' '){
				nbsp++;
			}
		}
		return nbsp;
	}
	
	private void getTablesDeclination(Element tablesItem){
		
		if (null != tablesItem) {
			NodeList someTRs = tablesItem.getElementsByTagName("tr");
			if (null!=someTRs) {
				for (int i=0;i<someTRs.getLength();i++) {
					Element trItem= (Element)someTRs.item(i);
					if (null!=trItem) {
						NodeList someTD=trItem.getElementsByTagName("td");
						for (int j=0;j<someTD.getLength();j++) {
							if ((j%2)==1 && someTD.getLength()==8) {
								Element tdItem=(Element)someTD.item(j);
								if (null!=tdItem) {
									NodeList tdContent=tdItem.getChildNodes();
									for (int k=0;k<tdContent.getLength();k++) {
										String form=tdContent.item(k).getTextContent();
										if( !form.isEmpty() ) {
											addDeclinationForm(form);
										}
									}
								}
							}
						}
					}
				}
			}
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
	
	private void addDeclinationForm(String s){
		s=s.replace("\n"," ");
		String [] tab= s.split(" ");
		for (String r : tab) {
			if (!r.isEmpty()) {
				wdh.registerOtherForm(r);
			}
		}
	}
	
	//comp Verb
	private void addVerbForm(String s, boolean isPhrasal){
		s=(s.replace("\n","")).replace(",","");

		if (!s.isEmpty()) {
			int nbsp= nbSpaceForm(s);
			String res="";
			boolean imp=s.contains("!");
			if (!imp) {
				if(!isPhrasal){
//					System.out.println("non phrasal");
					if( nbsp==1) {
							res=s.substring(s.indexOf(" ")+1);
						
					}
					else if (nbsp==0) {
						res =s;
					}
				}
				else{
//					System.out.println("phrasal");
					//three words subject verb part
					if (nbsp==2) {
						res=s.substring(s.indexOf(" ")+1);
					}
					//two words subject verb or verb + part
					else if (nbsp==1) {
						if (s.substring(s.lastIndexOf(" ")+1).equals(part)) {
							res=s;
						}
						else{
							res=s.substring(s.indexOf(" ")+1);
						}
						
					}
					//only one word
					else if (nbsp==0 && !s.equals(part)) {
						res =s;
					}
				}
			}
			else{
				res=s.replace("!","");
			}
			if (!res.isEmpty()) {
				wdh.registerOtherForm(res);
//				System.out.println(res);
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
