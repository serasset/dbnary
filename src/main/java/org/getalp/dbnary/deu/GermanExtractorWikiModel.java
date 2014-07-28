package org.getalp.dbnary.deu;

import java.awt.PageAttributes.OriginType;
import java.security.acl.LastOwnerException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class GermanExtractorWikiModel extends DbnaryWikiModel {
	
	private final static String germanMorphoBegin = "{{Deutsch ";
	private final static String germanMorphoEnd = "}}";
	private final static String germanRegularVerbString="Deutsch Verb regelmäßig";
	private final static String germanNonRegularVerbString=" unregelmäßig";
	private static Pattern germanRegularVerbPattern;
	private static Pattern germanNonRegularVerbPattern;
//	private boolean reflexiv=false;
	static{
		germanRegularVerbPattern= Pattern.compile(germanRegularVerbString);
		germanNonRegularVerbPattern= Pattern.compile(germanNonRegularVerbString);
	}
	
	private Logger log = LoggerFactory.getLogger(GermanExtractorWikiModel.class);
	
	private WiktionaryDataHandler wdh;
	
	public GermanExtractorWikiModel(WiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wdh, wi, locale, imageBaseURL, linkBaseURL);
		this.wdh=super.delegate;
	}
	
	public Document wikicodeToHtmlDOM (String wikicode, String regex) {
		String otherFormTemplateString=regex+"(\\||.*|\\(|\\)| |=|\n|\r)*"+germanMorphoEnd.replace("}", "\\}");
		Pattern otherFormPattern=Pattern.compile(otherFormTemplateString);
		Matcher m=otherFormPattern.matcher(wikicode);
		if(m.find()){
			return wikicodeToHtmlDOM(m.group(0));
		}
		else{
			return null;
		}
		
	}

	public void parseConjugation(String conjugationTemplateCall, String originalPos) {
		// Render the conjugation to html, while ignoring the example template
		Matcher mr = germanRegularVerbPattern.matcher(conjugationTemplateCall);
		Matcher mu=germanNonRegularVerbPattern.matcher(conjugationTemplateCall);
		
		
		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);
		
		
		
		if (doc == null){
			return ;
		}

		
//		if(conjugationTemplateCall.indexOf("reflexiv")!=-1){
//			reflexiv=true;
//		}
		
		NodeList tables =doc.getElementsByTagName("table");
		
		Element tablesItem =(Element) tables.item(1);
		getTablesConj(tablesItem,11,0);
		
		
		tablesItem =(Element) tables.item(2);
		getTablesConj(tablesItem,2,1,3,2);
		
		
		tablesItem =(Element) tables.item(3);
		
		
		if(mr.find()){
//			System.out.println("regelmäßig");
			getTablesConj(tablesItem,2,1);
			getTablesConj(tablesItem,11,1);
		}
		else if (mu.find()){
//			System.out.println("unregelmäßig");			
			getTablesConj(tablesItem,3,2);
			getTablesConj(tablesItem,13,2);
		}
		else{
//			System.out.println("anderen Type");
			getTablesConj(tablesItem,3,2);
			getTablesConj(tablesItem,13,2);
		}

	}
	
	public void parseDeclination(String declinationTemplateCall, String originalPos){
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
		if(null==originalPos){
			wdh.addPartOfSpeech("");
			originalPos="";
		}
		if(page!=null){
				if(!page.contains("\n")){
					Document doc = wikicodeToHtmlDOM(page);
					if(null!= doc){
						NodeList tables =doc.getElementsByTagName("table");
						for(int i=0;i<tables.getLength();i++){
							Element tablesItem=(Element) tables.item(i);
							getTablesOtherForm(tablesItem);
						}
					}
				}
			else{
				//here we can Use a Map<String,String> and parseArgs from Wikitool
				page=page.replaceAll("\\<.*\\>", "\n  =");
				page=page.replace(" "," ");
					String[] tab = page.split("\n");
					for(String r : tab){
						if(!r.isEmpty()){
							if(r.indexOf("Hilfsverb")==-1 && r.indexOf("Bild")==-1 && r.indexOf("Titel")==-1 && r.indexOf("Weitere_Konjugationen")==-1){
			    				int ind = r.indexOf("=");
								if(ind!=-1){
									String e=extractString(r, "=", "\n");
									e=removeUselessSpaces(e);
									if(!e.isEmpty()){
											e=e.substring(1);
										if(!originalPos.equals("Verb")){
											if (-1!=e.indexOf(" ")){
												e=extractString(e," ", "\n");
												if(!e.isEmpty()){
													e=e.substring(1);
												}
											}
										}
										e=e.replace("—","");
										if(!e.isEmpty()){
											if(e.indexOf('(')!=-1){
												addDeclinationForm(e.replaceAll("!|\\(|\\)",""));
//												System.out.println(e.replaceAll("!|\\(|\\)",""));
											}
//											System.out.println(e.replaceAll("!|\\(.*\\)", ""));
											addDeclinationForm(e.replaceAll("!|\\(.*\\)", ""));
										}
									}
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
		if(null!=tablesItem){
			NodeList someTRs = tablesItem.getElementsByTagName("tr");
			
			for(int i=iBegin;i<iEnd;i++){
				Element linesItem= (Element)someTRs.item(i);
				if(linesItem!=null){
					NodeList interrestingTDs = linesItem.getElementsByTagName("td") ;
						for(int j=jBegin;j<jEnd;j++){
							Element colsItem=(Element)interrestingTDs.item(j);
							if(colsItem!=null){
								NodeList itemsList = colsItem.getChildNodes();
								for(int e=0; e<itemsList.getLength();e++){
									String name=itemsList.item(e).getNodeName();
									if (name.equals("#text")) {
										String form=itemsList.item(e).getTextContent();
										form=removeUselessSpaces(form.replaceAll("\\<.*\\>", "").replace("—",""));//remove insecable spaces and </noinclude> markup
										if( !form.isEmpty() && !form.contains("Pers.")) {
											// for verbs like ankommen : ich komme an
											if (!change && isPhrasalVerb(form) ) {
												part=extractPart(form);
												if(!part.isEmpty()){
													change= true;
													iBegin=iBegin+1;
													iEnd=iEnd+1;
													jEnd=jEnd+2;
												}
											}
		//									System.out.println("i : "+i+" j : "+j+"  form : "+form);
											form =(form.replace("\n","")).replace(",","");
											if(!form.replace(" ","").isEmpty()){
												addVerbForm(form, change);
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
	
		
	private void getTablesDeclination(Element tablesItem){
		
		if (null != tablesItem) {
			NodeList someTRs = tablesItem.getElementsByTagName("tr");//list of line Elements
			if (null!=someTRs) {
				for (int i=0;i<someTRs.getLength();i++) {
					Element trItem= (Element)someTRs.item(i);
					if (null!=trItem) {
						NodeList someTD=trItem.getElementsByTagName("td");//list of cols Elements
						for (int j=0;j<someTD.getLength();j++) {
							if((j%2)==1){
							Element tdItem=(Element)someTD.item(j);
								if (null!=tdItem) {
									NodeList tdContent=tdItem.getChildNodes();
									for (int k=0;k<tdContent.getLength();k++) {
										String form=tdContent.item(k).getTextContent();
										if(!form.isEmpty()){
											form=removeUselessSpaces(form.replaceAll("(\\<.*\\>|(\\(.*\\)))*(—|-|\\}|\\{)*", ""));
												if(nbSpaceForm(form)!=0){
													form=extractPart(form);
												}
												if( !form.replace(" ","").isEmpty() ) {
	//											System.out.println(form);
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
	}
	
	private void getTablesOtherForm(Element tablesItem){
		if (null != tablesItem) {
			NodeList someTRs = tablesItem.getElementsByTagName("tr");//list of line Elements
			if (null!=someTRs) {
				for (int i=2;i<someTRs.getLength();i++) {
					Element trItem= (Element)someTRs.item(i);
					if (null!=trItem) {
						NodeList someTD=trItem.getElementsByTagName("td");//list of cols Elements
						for (int j=1;j<someTD.getLength();j++) {
							Element tdItem=(Element)someTD.item(j);
								if (null!=tdItem) {
									NodeList tdContent=tdItem.getChildNodes();
									for (int k=0;k<tdContent.getLength();k++) {
										String form=tdContent.item(k).getTextContent();
										if(!form.isEmpty()){
											form=removeUselessSpaces(form.replaceAll("(\\<.*\\>|(\\(.*\\)))*(—|-|\\}|\\{)*", ""));
												if(nbSpaceForm(form)!=0){
													form=form.substring(form.lastIndexOf(" ")+1);
												}
												if( !form.replace(" ","").isEmpty() ) {
//												System.out.println(form);
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
	
	
	private void addDeclinationForm(String s){
//		System.out.println(s);
		s=s.replace("]", "").replace("[","");
		wdh.registerOtherForm(s);
	}
	
	//comp Verb
	private void addVerbForm(String s, boolean isPhrasal){
		if (!s.isEmpty()) {
			int nbsp= nbSpaceForm(s);
			String res="";
			boolean imp=s.contains("!");
			if (!imp) {
				if(!isPhrasal){
					//System.out.println("non phrasal");
					if( nbsp==1) {
							res=s.substring(s.indexOf(" ")+1);
						
					}
					else if (nbsp==0) {
						res =s;
					}
				}
				else{
					//System.out.println("phrasal");
					//three words subject verb part
					if (nbsp==2 &&  part.equals(s.substring(s.lastIndexOf(" ")+1))) {
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
	
	//return the form's table
	private String getForm(String s, String originalPos){

		String res=null,section=null;
				
		section=extractString(s, originalPos, "{{Wortart");
		res=extractString(s, germanMorphoBegin, germanMorphoEnd)+"}}";
		
		if(res.isEmpty()){
			res = extractString(s, "Tabelle", germanMorphoEnd);
		}
		
		return res;
	}
	
	
	//extract a String in s between start and end
	private String extractString(String s, String start, String end){
		String res;
		int startIndex,endIndex;
		startIndex=getIndexOf(s, start, 0);
		endIndex=getIndexOf(s, end, startIndex);
		res=s.substring(startIndex, endIndex);
		return res;
	}
	
	//return the index of pattern in s after start
	private int getIndexOf(String s, String pattern, int start){
		int ind = s.indexOf(pattern, start);
		if(ind <=start || ind >s.length()){
			ind=s.length();
		}
		return ind;
	}

	//for the phrasal verb, extract the part without spaces : example extractPart("ich komme an")->an
	private String extractPart(String form){
		String res="";
		int i=form.length()-1;
		char cc=form.charAt(i);
		while(i>=0 && ' '!=cc){
			res=cc+res;
			i--;
			cc=form.charAt(i);
		}
		return res;
	}

	//remove spaces before the first form's character and after the last form's character
	//and the unsecable spaces
	private String removeUselessSpaces(String form){
		form =form.replace(" "," ").replace("&nbsp;"," ").replace("\t"," ");//replace unsecable spaces
		String res=form.replace("  "," ");
		if(!res.isEmpty()){
		int debut=0,fin=res.length()-1;
		char cdebut=res.charAt(debut),cfin=res.charAt(fin);
		while(fin> debut && (cdebut==' ' || cfin==' ')){
			if(cdebut == ' '){
				debut++;
				cdebut=res.charAt(debut);
			}
			if(cfin==' '){
				fin--;
				cfin=res.charAt(fin);
			}
		}
		res = res.substring(debut,fin+1);
		}
		return res;
	}

	//return if the form given in parameter is a phrasal verb
	private boolean isPhrasalVerb(String form){
		int nbsp=nbSpaceForm(form);
//		return ((!reflexiv && nbsp>=2) || (reflexiv && nbsp>=3));
		return nbsp>=2;
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
	
	//otherway some phrasal verb don't have any inflected form
	public String getIncludeOnlyText(String rawWikiText) {
		return rawWikiText;
	}
	

	
	
}
