package org.getalp.dbnary.deu;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.PropertyResourcePair;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class GermanExtractorWikiModel extends DbnaryWikiModel {
	
	private final static String germanMorphoBegin = "{{Deutsch ";
	private final static String germanMorphoEnd = "}}";
	private final static String germanRegularVerbString="Deutsch Verb regelmäßig";
	private final static String germanNonRegularVerbString=" unregelmäßig";
	private static Pattern germanRegularVerbPattern;
	private static Pattern germanNonRegularVerbPattern;
	
	
	
	private  boolean isPhrasal=false;
	
	private HashSet<PropertyResourcePair> inflections;
	
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
		inflections= new HashSet<PropertyResourcePair>();
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
	private enum Genre {MASCULIN, FEMININ,NEUTRUM,NOTHING};
	private enum Cas {NOMINATIF,ACCUSATIF,DATIF, GENITIF,NOTHING};
	private enum Mode {PARTICIPS,IMPERATIV,INDICATIV,SUBJONCTIVE,NOTHING};
	private enum Tense {PRESENT,PAST,NOTHING};
	private enum  Degree {POSITIVE,COMPARATIVE,SUPERLATIVE,NOTHING};
	private enum Number {SINGULAR,PLURAL,NOTHING};
	private enum Person {FIRST,SECOND,THIRD,NOTHING};
	private Degree degree=Degree.NOTHING;
	private Mode mode=Mode.NOTHING;
	private Tense tense = Tense.NOTHING;
	private Number number=Number.NOTHING;
	private Cas cas=Cas.NOTHING;
	private Genre genre= Genre.NOTHING;
	private Person person = Person.NOTHING;
	
	private void intializeExclusiveInflectionInfo(){
		inflections=new HashSet<PropertyResourcePair>();
//		tense = Tense.NOTHING;
		number=Number.NOTHING;
		cas=Cas.NOTHING;
		genre= Genre.NOTHING;
		person = Person.NOTHING;
	}
	public void parseConjugation(String conjugationTemplateCall, String originalPos) {
		
		
		// Render the conjugation to html, while ignoring the example template
		Matcher mr = germanRegularVerbPattern.matcher(conjugationTemplateCall);
		Matcher mu=germanNonRegularVerbPattern.matcher(conjugationTemplateCall);
		//FIXME : check for reflexiv Verbs and adapt ConjTable
		
		Document doc = wikicodeToHtmlDOM(conjugationTemplateCall);
		
		
		
		if (null==doc) {
			return ;
		}

		
//		if(conjugationTemplateCall.indexOf("reflexiv")!=-1){
//			reflexiv=true;
//		}
		
		NodeList tables =doc.getElementsByTagName("table");
		
		Element tablesItem =(Element) tables.item(3);
		mode=Mode.NOTHING;
		if(mr.find()) {
//			System.out.println("regelmäßig");
			tense=Tense.PRESENT;
			getTablesConj(tablesItem,2,1);
			tense=Tense.PAST;
			getTablesConj(tablesItem,11,1);
		}
		else if (mu.find()) {
//			System.out.println("unregelmäßig");			
			tense=Tense.PRESENT;
			getTablesConj(tablesItem,3,2);
			tense=Tense.PAST;
			getTablesConj(tablesItem,13,2);
		}
		else{
//			System.out.println("anderen Type");
			tense=Tense.PRESENT;
			getTablesConj(tablesItem,3,2);
			tense=Tense.PAST;
			getTablesConj(tablesItem,13,2);
		}
		mode=Mode.PARTICIPS;
		
		
		tablesItem =(Element) tables.item(1);
		getTablesConj(tablesItem,11,0);
		
		mode=Mode.IMPERATIV;
		tablesItem =(Element) tables.item(2);
		getTablesConj(tablesItem,2,1);
//		getTablesConj(tablesItem,2,1,5,2);
		
		
		

	}
	
	public void parseDeclination(String declinationTemplateCall, String originalPos){
		Document doc = wikicodeToHtmlDOM(declinationTemplateCall);
		if (null==doc) {
			return ;
		}
		NodeList tables =doc.getElementsByTagName("table");
		for (int i=0;i<3;i++) {
			degree=Degree.values()[i];
			Element tablesItem=(Element) tables.item(i);
			getTablesDeclination(tablesItem);
		}
	}
	
	public void parseOtherForm(String page,String originalPos){
		if (null==originalPos) {
			wdh.addPartOfSpeech("");
			originalPos="";
		}
		if (null!=page) {
			if (!page.contains("\n")) {
				Document doc = wikicodeToHtmlDOM(page);
				if (null!= doc) {
					NodeList tables =doc.getElementsByTagName("table");
					for(int i=0;i<tables.getLength();i++){
						Element tablesItem=(Element) tables.item(i);
						getTablesOtherForm(tablesItem);
					}
				}
			} else {
				page=page.replaceAll("\\<.*\\>", "\n  =");
				HashMap<String,String> pag = new HashMap<String,String>();
				pag=(HashMap) (WikiTool.parseArgs(page.replace("{","").replace("}","")));
				for(String key : pag.keySet()){
					String r=pag.get(key);
					if (-1==r.indexOf("Bild") && -1==r.indexOf("Titel") && -1==r.indexOf("Konjugation") && (-1==r.indexOf("Deutsch") && -1!=r.indexOf(wdh.currentWiktionaryPos()))) {
						r=extractString(r, "=", "\n");
						if(!r.isEmpty()){
							if(key.equals("Hilfsverb")){
	////							inflections.add(e)
							} else {
							if (-1!=r.indexOf('(')) {
								addForm(r.replaceAll("!|\\(|\\)",""));
							}
							addForm(r.replaceAll("!|\\(.*\\)", ""));
							}
						}
					}
					
				}
				return;
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
		if (null!=tablesItem) {
			NodeList someTRs = tablesItem.getElementsByTagName("tr");
			for (int i=iBegin;i<iEnd;i++) {
				Element linesItem= (Element)someTRs.item(i);
				if (null!=linesItem) {
					NodeList interrestingTDs = linesItem.getElementsByTagName("td") ;
					for (int j=jBegin;j<jEnd;j++) {
						Element colsItem=(Element)interrestingTDs.item(j);
						if (null!=colsItem) {
							NodeList itemsList = colsItem.getChildNodes();
							for (int e=0; e<itemsList.getLength();e++) {
								intializeExclusiveInflectionInfo();
								String name=itemsList.item(e).getNodeName();
								if (name.equals("#text")) {
									String form=itemsList.item(e).getTextContent();
									form=removeUselessSpaces(form.replaceAll("\\<.*\\>", "").replace("—",""));//remove insecable spaces and </noinclude> markup
									if ( !form.isEmpty() && !form.contains("Pers.")) {
										// for verbs like ankommen : ich komme an
										if (!change && isPhrasalVerb(form) ) {
											part=extractPart(form);
											if (!part.isEmpty()) {
//													System.out.println("phrasal");
												change= true;
												isPhrasal=true;
												iBegin=iBegin+1;
												iEnd=iEnd+1;
												jEnd=jEnd+2;
											}
										}
										int tmp=change?(j-jBegin>=2?(j-jBegin)-2:j-jBegin):(j-jBegin);
										int nbr = (i-iBegin);

										if(mode==Mode.PARTICIPS){
											person=Person.NOTHING;
											if(j==jBegin){
												tense=Tense.PRESENT;
											} else {
												tense=Tense.PAST;
											}
										} else if(mode==Mode.IMPERATIV) {
											person=Person.NOTHING;
											tense=Tense.PRESENT;
											if(1>=nbr){
												number=Number.SINGULAR;
											} else {
												number=Number.PLURAL;
											}
										} else {
											person=Person.values()[nbr%3];
											number=Number.values()[(nbr/3)%2];
											if(0==tmp){
												mode=Mode.INDICATIV;
											} else if (1==tmp){
												mode=Mode.SUBJONCTIVE;
											}
										}
										
//										addVerbInfo(nbr,tmp, form);
	//									System.out.println("i : "+i+" j : "+j+"  form : "+form);
										form =(form.replace("\n","")).replace(",","");
										if (!form.replace(" ","").isEmpty()) {
											addInflectionsInfo();
											addVerbForm(form);
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
					intializeExclusiveInflectionInfo();
					Element trItem= (Element)someTRs.item(i);
					if (null!=trItem) {
						NodeList someTD=trItem.getElementsByTagName("td");//list of cols Elements
						for (int j=0;j<someTD.getLength();j++) {
							if (1==(j%2)) {
							Element tdItem=(Element)someTD.item(j);
								if (null!=tdItem) {
									NodeList tdContent=tdItem.getChildNodes();
									for (int k=0;k<tdContent.getLength();k++) {
										String form=tdContent.item(k).getTextContent();
										if (!form.isEmpty()) {
//											form=removeUselessSpaces(form.replaceAll("(\\<.*\\>|(\\(.*\\)))*(—|-|\\}|\\{)*", ""));
											form=removeUselessSpaces(form.replaceAll("(\\<.*\\>|(—|-|\\}|\\{))*", ""));
												if (0!=nbSpaceForm(form)) {
													form=extractPart(form);
												}
												if (!form.replace(" ","").isEmpty()) {
	//											System.out.println(form);
													cas=Cas.values()[i%4];
													if(j/2<3){
														genre=Genre.values()[j/2];
														number=Number.SINGULAR;
													} else {
														number =Number.PLURAL;
													}
													addInflectionsInfo();
													
													if (-1!=form.indexOf('(')) {
														addForm(form.replaceAll("!|\\(|\\)",""));
													}
													addForm(form.replaceAll("!|\\(.*\\)", ""));
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
					intializeExclusiveInflectionInfo();
					Element trItem= (Element)someTRs.item(i);
					if (null!=trItem) {
						NodeList someTD=trItem.getElementsByTagName("td");//list of cols Elements
						for (int j=1;j<someTD.getLength();j++) {
							Element tdItem=(Element)someTD.item(j);
								if (null!=tdItem) {
									NodeList tdContent=tdItem.getChildNodes();
									for (int k=0;k<tdContent.getLength();k++) {
										String form=tdContent.item(k).getTextContent();
										if (!form.isEmpty()) {
											if(j%2==1){
												number=Number.SINGULAR;
											}
											else {
												number= Number.PLURAL;
											}
											cas=Cas.values()[i%4];
											form=removeUselessSpaces(form.replaceAll("(\\<.*\\>|(\\(.*\\)))*(—|-|\\}|\\{)*", ""));
											if (0!=nbSpaceForm(form)) {
												form=form.substring(form.lastIndexOf(" ")+1);
											}
											
											addInflectionsInfo();
											if( !form.replace(" ","").isEmpty() ) {
//													System.out.println(form);
												addForm(form);
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
	
	
	private void addForm(String s){
		s=s.replace("]", "").replace("[","").replaceAll(".*\\) *","").replace("(","");
		wdh.registerInflection("deu", wdh.currentWiktionaryPos(), s, wdh.currentLexEntry(), 0, inflections);
//		wdh.registerOtherForm(s);
	}
	
	//comp Verb
	private void addVerbForm(String s){
		if (!s.isEmpty()) {
			int nbsp= nbSpaceForm(s);
			String res="";
			boolean imp=s.contains("!");
			
			if (!imp) {
				if (!isPhrasal) {
					//System.out.println("non phrasal");
					if (1==nbsp) {
							res=s.substring(s.indexOf(" ")+1);
						
					}
					else if (0==nbsp) {
						res =s;
					}
				}
				else{
					//System.out.println("phrasal");
					//three words subject verb part
					if (2==nbsp &&  part.equals(s.substring(s.lastIndexOf(" ")+1))) {
						res=s.substring(s.indexOf(" ")+1);
					}
					//two words subject verb or verb + part
					else if (1==nbsp) {
						
						if (s.substring(s.lastIndexOf(" ")+1).equals(part)) {
							res=s;
							
						}
						else{
							res=s.substring(s.indexOf(" ")+1);
						}
						
					}
					//only one word
					else if (0==nbsp && !s.equals(part)) {
						res =s;
					}
				}
			}
			else{
				if(0==nbsp || isPhrasal){
					res=s.replace("!","");
//					System.out.println(res);
				} else {
					res=s.substring(0, s.lastIndexOf(" "));
				}
			}
			if (!res.isEmpty()) {
				wdh.registerInflection("deu", wdh.currentWiktionaryPos(), res, wdh.currentLexEntry(), 1 , inflections);
//				wdh.registerOtherForm(res);
//				System.out.println("otherForm : "+res);
			}
		}
	}
	
/*	//return the form's table
	private String getForm(String s, String originalPos){

		String res=null,section=null;
				
		section=extractString(s, originalPos, "{{Wortart");
		res=extractString(s, germanMorphoBegin, germanMorphoEnd)+"}}";
		
		if(res.isEmpty()){
			res = extractString(s, "Tabelle", germanMorphoEnd);
		}
		
		return res;
	}
	*/
	
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
		if (ind <=start || ind >s.length()) {
			ind=s.length();
		}
		return ind;
	}

	//for the phrasal verb, extract the part without spaces : example extractPart("ich komme an")->an
	private String extractPart(String form){
		String res="";
		int i=form.length()-1;
		char cc=form.charAt(i);
		while (0<=i && ' '!=cc) {
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
		while (fin> debut && (' '==cdebut || ' '==cfin)) {
			if (' '==cdebut) {
				debut++;
				cdebut=res.charAt(debut);
			}
			if (' '==cfin) {
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
		return 2<=nbsp;
	}
	
	private int nbSpaceForm(String form){
		int nbsp=0;
		for(int i=1; i<form.length()-1;i++){
			if(' '==form.charAt(i)){
				nbsp++;
			}
		}
		return nbsp;
	}
	
	//otherway some phrasal verb don't have any inflected form
	public String getIncludeOnlyText(String rawWikiText) {
		return rawWikiText;
	}
	
	private void addInflectionsInfo(){
		switch(degree){
		case POSITIVE:
			inflections.add(new PropertyResourcePair(LexinfoOnt.degree, LexinfoOnt.positive));
			break;
		case COMPARATIVE:
			inflections.add(new PropertyResourcePair(LexinfoOnt.degree, LexinfoOnt.comparative));
			break;
		case SUPERLATIVE:
			inflections.add(new PropertyResourcePair(LexinfoOnt.degree, LexinfoOnt.superlative));
			break;	
		default:
			break;
		}
		switch(cas){
		case NOMINATIF:
			inflections.add(new PropertyResourcePair(LexinfoOnt.case_, LexinfoOnt.nominativeCase));
			break;
		case GENITIF:
			inflections.add(new PropertyResourcePair(LexinfoOnt.case_, LexinfoOnt.genitiveCase));
			break;
		case ACCUSATIF:
			inflections.add(new PropertyResourcePair(LexinfoOnt.case_, LexinfoOnt.accusativeCase));
			break;
		case DATIF:
			inflections.add(new PropertyResourcePair(LexinfoOnt.case_, LexinfoOnt.dativeCase));
		default:
			break;
		}
		switch(genre){
		case MASCULIN:
			inflections.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.masculine));
			break;
		case FEMININ:
			inflections.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.feminine));
			break;
		case NEUTRUM:
			inflections.add(new PropertyResourcePair(LexinfoOnt.gender, LexinfoOnt.neuter));
			break;
		default :
			break;
		}
		switch(number){
		case SINGULAR:
			inflections.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.singular));
			break;
		case PLURAL:
			inflections.add(new PropertyResourcePair(LexinfoOnt.number, LexinfoOnt.plural));
			break;
		default:
			break;
		}
		switch(tense){
		case PAST:
			inflections.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.past));
			break;
		case PRESENT:
			inflections.add(new PropertyResourcePair(LexinfoOnt.tense, LexinfoOnt.present));
			break;
		default:
			break;			
		}
		switch(mode){
		case IMPERATIV:
			inflections.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.imperative));
			break;
		case INDICATIV:
			inflections.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.indicative));
			break;
		case SUBJONCTIVE:
			inflections.add(new PropertyResourcePair(LexinfoOnt.mood, LexinfoOnt.subjunctive));
			break;
		case PARTICIPS:
			break;
		default:
			break;
		}
		switch(person) {
		case FIRST:
			inflections.add(new PropertyResourcePair(LexinfoOnt.person,LexinfoOnt.firstPersonForm));
			break;
		case SECOND:
			inflections.add(new PropertyResourcePair(LexinfoOnt.person,LexinfoOnt.secondPersonForm));
			break;
		case THIRD:
			inflections.add(new PropertyResourcePair(LexinfoOnt.person,LexinfoOnt.thirdPersonForm));
			break;
		default:
			break;
		}
	}
}
