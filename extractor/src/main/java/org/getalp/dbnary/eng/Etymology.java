package org.getalp.dbnary.eng;

import org.getalp.dbnary.wiki.WikiTool; 
import org.getalp.dbnary.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;          
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** 
 * @author pantaleo
 */
public class Etymology{
    static Logger log = LoggerFactory.getLogger(Etymology.class);
    
    public static List<String> bulletElements = Arrays.asList("COMMA", "", "LEMMA", "COLUMN");
    public static List<String> definitionElements = Arrays.asList("FROM", "", "LEMMA", "ABOVE", "COGNATE_WITH", "COMPOUND_OF", "UNCERTAIN", "COMMA", "YEAR", "AND", "PLUS", "DOT", "OR", "WITH", "STOP");
        	
    public static Pattern definitionPattern = Pattern.compile("(FROM )?(LANGUAGE LEMMA |LEMMA )(COMMA |DOT |OR )");

    public static Pattern compoundPattern = Pattern.compile("((COMPOUND_OF |FROM )(LANGUAGE )?(LEMMA )(PLUS |AND |WITH )(LANGUAGE )?(LEMMA ))|((LANGUAGE )?(LEMMA )(PLUS )(LANGUAGE )?(LEMMA ))");

    public static Pattern bulletPattern = Pattern.compile("(((LANGUAGE)|(LEMMA)) (COLUMN ))?((LEMMA)( COMMA )?)+");
    
    public static Sentence definition = new Sentence(definitionElements, definitionPattern); 
    public static Sentence bullet = new Sentence(bulletElements, bulletPattern); 
    
    public String lang;
    public String asString;
    public ArrayList<POE> asPOE;

    public Etymology(String etyString, String etyLang){
	asString = etyString;
	lang = etyLang;
	asPOE = new ArrayList<POE>();
    }
    
    public void cleanUpString(){
	asString = asString.trim();

	//REMOVE TEXT WITHIN HTML REFERENCE TAG
	asString = WikiTool.removeReferencesIn(asString);

	//REMOVE TEXT WITHIN TABLES
	for (Pair p : WikiTool.locateEnclosedString(asString, "{|", "|}")){
	    asString = asString.substring(0, p.start) + asString.substring(p.end, asString.length());
	}

	//REMOVE TEXT WITHIN PARENTHESES UNLESS PARENTHESES FALL INSIDE A WIKI LINK OR A WIKI TEMPLATE
	//locate templates {{}} and links [[]]
	ArrayList<Pair> templatesAndLinksLocations = WikiTool.locateEnclosedString(asString, "{{", "}}");
	templatesAndLinksLocations.addAll(WikiTool.locateEnclosedString(asString, "[[", "]]"));
	//locate parentheses ()
	ArrayList<Pair> parenthesesLocations = WikiTool.locateEnclosedString(asString, "(", ")");
	//ignore location of parentheses if they fall inside a link or a template
	int parenthesesLocationsLength = parenthesesLocations.size();
	for (int i = 0; i < parenthesesLocationsLength; i++) {
	    Pair p = parenthesesLocations.get(parenthesesLocationsLength - i - 1);
	    //check if parentheses are inside links [[  ()  ]]
	    if (! p.containedIn(templatesAndLinksLocations)){
		log.debug("Removing string {} in Etymology section of word {}", asString.substring(p.start, p.end));
		asString = asString.substring(0, p.start) + asString.substring(p.end, asString.length());
	    }
	}

	//ADD FINAL DOT
	if (asString != null && ! asString.trim().isEmpty() && !asString.trim().endsWith(".")){
	    //add final dot if etymology string doesn't end with a dot
	    asString += ".";
	}
    }
    
    public void toPOE(Sentence sentence){
	if (asString == null || asString.trim().isEmpty()){
	    return; //return null
	}
	String etylLang = null;
	int etylIndex = - 1;
	ArrayList<Pair> templatesLocations = WikiTool.locateEnclosedString(asString, "{{", "}}");
	ArrayList<Pair> linksLocations = WikiTool.locateEnclosedString(asString, "[[", "]]");

	//match against regex
	//System.out.format("sentence = %s\n", sentence.elementsPattern);
	Matcher m = sentence.elementsPattern.matcher(asString);
	while (m.find()) {
	    //System.out.format("group = %s\n", m.group());
	    for (int i = 0; i < m.groupCount(); i ++) {
		//System.out.format("group %s = %s\n", i, m.group(i + 1));
		if (m.group(i + 1) != null) {
		    //check if match is contained in template or link
		    boolean check = false;
		    //check if match is contained in a template (or is a template)
		    Pair match = new Pair(m.start(), m.end());
		    for (Pair template : templatesLocations) {
			if (match.containedIn(template)) {//match is contained in a template
			    check = true;
			    if (i == 1) {//match is a template
				POE poe = new POE(asString.substring(template.start + 2, template.end - 2), lang, sentence.elements.get(i));
				//System.out.format("template=%s, element=%s, part=%s\n", asString.substring(template.start + 2, template.end - 2), sentence.elements.get(i), poe.part);
				if (poe.part != null && poe.args != null) {
				    if (poe.args.get("1").equals("etyl") || poe.args.get("1").equals("_etyl")){
					etylLang = poe.args.get("lang");
					etylIndex = asPOE.size();
				    }
				    //when a LEMMA is preceded by a LANGUAGE etyl template
				    //set language of LEMMA to language of etyl template
				    if (etylIndex != -1 && asPOE.size() == etylIndex + 1){
					poe.args.put("lang", etylLang);
					System.out.format("etylang=%s\n", etylLang);
				    }
				    if (poe.part.equals("STOP")){
					return;
				    } else {
					asPOE.add(poe);
					//System.out.format("poe part of template =%s\n", poe.part);
				    }
				}
				break;
			    }//else ignore match
			}
		    }

		    //change match by adding "+ 2 (- 1 as above)" to its start to check both:
		    //*   if match "''[[" is contained in link "[[...]]"
		    //*   if match "[[" is contained in link "[[...]]"
		    //check if match is contained in a link (or is a link)
		    if (check == false) {//if match is not contained in a template
			for (Pair link : linksLocations) {
			    if (match.containedIn(link)) {
				check = true;
				if (i == 2) {//match is a link
				    POE poe = new POE(asString.substring(link.start + 2, link.end - 2), lang, sentence.elements.get(i));
				    if (poe.part != null && poe.args != null) {
					if (etylIndex != -1 && asPOE.size() == etylIndex + 1){
					    poe.args.put("lang", etylLang);
					}
					asPOE.add(poe);
				    }
				    break;
				}//else ignore match
			    }
			}
		    }
		    if (check == false) {//if match is neither contained in a template nor in a link
			POE poe = new POE(m.group(i + 1), lang, sentence.elements.get(i));
			if (poe.part != null) {
			    if (poe.part.equals("STOP")){
				return;
			    } else {
				asPOE.add(poe);
			    }
			}
		    }
		}
	    }
	}
    }

    public void replaceSense(){
	int aSize = asPOE.size();

	if (aSize > 1){
	    for (int i = 0; i < aSize; i ++){
		if (asPOE.get(i).part.get(0).equals("SENSE")){
		    String sense = asPOE.get(i).args.get("2");
		    if (i + 1 < aSize && asPOE.get(i + 1).part.get(0).equals("LEMMA")){
			POE tmp = asPOE.get(i + 1);
			tmp.args.put("sense", sense);
			asPOE.set(i, tmp);
			asPOE.remove(i + 1);
			aSize --;
		    } else if (i - 1 >= 0 && asPOE.get(i - 1).part.get(0).equals("LEMMA")){
			POE tmp = asPOE.get(i - 1);
			tmp.args.put("sense", sense);
			asPOE.set(i, tmp);
			asPOE.remove(i - 1);
			aSize --;
			i --;
		    }
		}
	    }
	}
	return;
    }

    public void replaceLanguage(){
	//REPLACE LANGUAGE STRING WITH LANGUAGE _ETYL TEMPLATE
	//case "Sardinian: [[pobulu]], [[poburu]], [[populu]]"
	//case "[[Asturian]]: {{l|ast|águila}}"
	String[] subs = asString.split(":");
	if (subs.length == 2) {
	    ArrayList<Pair> linksLocations = WikiTool.locateEnclosedString(subs[0], "[[", "]]");

	    if (linksLocations.size() == 0){//PARSE case "Sardinian: [[pobulu]], [[poburu]], [[populu]]"
		String bulletLang = EnglishLangToCode.threeLettersCode(subs[0].trim());
		if (bulletLang != null){
		    asString = "{{_etyl|" + bulletLang + "|" + lang + "}} : " + subs[1].trim();
		}
	    } else if (linksLocations.size() == 1){//PARSE case "[[Asturian]]: {{l|ast|águila}}"
		String bulletLang = EnglishLangToCode.threeLettersCode(subs[0].substring(2, subs[0].length() - 2));
		if (bulletLang != null){
		    asString = "{{_etyl|" + bulletLang + "|" + lang + "}} : " + subs[1].trim();
		}
	    }
	} else if (subs.length > 2){
	    log.debug("Ignoring bullet {}", asString);
	    asString = "";
	}
    }

    /** 
     * @return a String (e.g., "FROM LEMMA OR LEMMA") concatenating the property "part" of each element of ArrayListPOE
     */
    public static String toString(ArrayList<POE> a){
	StringBuilder s = new StringBuilder();
	for (POE poe : a){
	    for (String part : poe.part){
		s.append(part);
		s.append(" ");
	    }
	}
	return s.toString();
    }
    
    public ArrayList<Pair> findMatch(ArrayList<POE> a, Pattern p) {
	ArrayList<Pair> toreturn = new ArrayList<Pair>();
	if (a == null || a.size() == 0){
	    return toreturn;
	}
        ArrayList<Integer> arrayListInteger = new ArrayList<Integer>();
	ArrayList<Integer> arrayListPosition = new ArrayList<Integer>();
	int c = 0;
	arrayListPosition.add(c);
	for (int i = 0; i < a.size(); i ++) {
	    POE poe = a.get(i);
	    for (int j = 0; j < poe.part.size(); j ++) {
		arrayListInteger.add(i);
		c += poe.part.get(j).length() + 1;
		arrayListPosition.add(c);
	    }
	}
	Matcher m = p.matcher(toString(a));
	while (m.find()) {
	    int start = -1, end = -1;
	    for (int i = 0; i < arrayListPosition.size() - 1; i ++) {
		if (arrayListPosition.get(i) == m.start()) {
		    start = arrayListInteger.get(i);
		} else if (arrayListPosition.get(i + 1) == m.end()) {
		    end = arrayListInteger.get(i);
		}
	    }
	    if (start < 0 || end < 0) {
		log.debug("Error: start or end of match are not available\n");
	    }
	    toreturn.add(new Pair(start, end));
	}
	return toreturn;
    }

    /** 
     * This function is used to replace "COMPOUND_OF LEMMA AND LEMMA" and equivalents 
     * with a single "LEMMA" POE of type compound|lang|word1|word2 
     */
    public void replaceCompound(){
	//iterate over all matches to a compound pattern
	//starting from the last
	ArrayList<Pair> match = findMatch(asPOE, compoundPattern);
	if (match == null || match.size() == 0){
	    return;
	}
	for (int i = match.size() - 1; i >= 0; i --) {
	    Pair m = match.get(i);
	    ArrayList<POE> p = new ArrayList<POE>();
	    for (int k = m.start; k < m.end + 1; k ++) {
		POE poe = asPOE.get(k);
		for (String part : poe.part){
		    if (part.equals("LEMMA") && poe.args != null) {
			p.add(poe);
			break;
		    }
		}
	    }
	    asPOE.set(m.start, mergePOE(p)); 
	    asPOE.subList(m.start + 1, m.end + 1).clear();
	}
    }

    private POE mergePOE(ArrayList<POE> p){
	if (p.size() == 2){ 
            StringBuilder compound = new StringBuilder();
	    compound.setLength(0);//clear StringBuilder
	    if (p.get(0).args.get("lang") == null || p.get(1).args.get("lang") == null){
	        log.debug("no lang in templates {} and {}", p.get(0).string, p.get(1).string);
	        return null;
	    }
	    compound.append("_compound|" + p.get(0).args.get("lang") + "|" + p.get(0).args.get("word1") + "|");
	    compound.append(p.get(1).args.get("word1"));
	    return new POE(compound.toString(), lang, "");
	} else {
	    log.debug("compound can only be created from two POEs; skipping templates");
	    return null;
	}
    }
    
    /** 
     * This function finds where list of cognates or OR statements start,
     * e.g, if toString(asPOE) == "FROM LEMMA COMMA FROM LEMMA COMMA COGNATE_WITH LEMMA COMMA"
     * it registers 6, the index of "COGNATE_WITH" or e.g., 
     * if toString(asPOE) == "FROM LEMMA OR LEMMA" it registers 2, the index of "OR",
     * and removes any element of the input ArrayList<POE> after that index.
     * also it removes any POE that preceeds the first match to the etymologyPattern
     */
    public void cleanUpPOE(){
	//remove any POE that follows "COGNATE_WITH" or "OR"
	for (int j = 0; j < asPOE.size(); j ++) {
	    if (asPOE.get(j).part.size() > 0) {
		//System.out.format("string=%s, part = %s\n", asPOE.get(j).string, asPOE.get(j).part.get(0));
		if (asPOE.get(j).part.get(0).equals("COGNATE_WITH") || asPOE.get(j).part.get(0).equals("OR")) {
		    asPOE.subList(j, asPOE.size()).clear();
		    break;
		}
	    }
	}

	ArrayList<Pair> match = findMatch(asPOE, definitionPattern);
	if (match.size() == 0) {
	    return;//there is no match to the definitionPattern
	}
	//System.out.format("match=%s\n", match.get(0).start); 		
	//remove any POE after "DOT" or "AND" that follow a definitionPattern
	for (int j = match.get(0).end + 1; j < asPOE.size(); j ++){
	    for (String part : asPOE.get(j).part){
		if (part.equals("DOT") || part.equals("AND")) {
		    asPOE.subList(j, asPOE.size()).clear();
		    j = asPOE.size();//break j loop
		    break;//break part loop
		}
	    }
	}
	//remove any POE that preceeds the first match to the definitionPattern
	asPOE.subList(0, match.get(0).start).clear();
	return;
    }
}
