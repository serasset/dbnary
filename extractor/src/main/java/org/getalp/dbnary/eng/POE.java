/**
 *
 */
package org.getalp.dbnary.eng;

import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pantaleo
 */

/**
 * POE is a part of etymology
 * it has properties
 * .part an ArrayList&lt;String&gt;, e.g., {"LANGUAGE", "LEMMA"}
 * .args a Map&lt;String, String&gt;, e.g., {("1", "m"), ("lang","eng"), ("word1","door")}
 * .string, e.g., "m|en|door"
 */
public class POE {
    public ArrayList<String> part;
    public Map<String, String> args;
    public String string;
    static Logger log = LoggerFactory.getLogger(POE.class);

    /**
     * Constructor
     *
     * @param group a String specifying the "string" parameter of the object of class POE (e.g., "from")
     * @param index an integer specifying the Etymology pattern among possibleString
     */
    public POE(String group, int index){
	if (index==1 || index == 2){
	    log.debug("error in group index");
	}
	string = group;
	part = new ArrayList<String>(); 
	part.add(EtymologyPatterns.possibleString[index]);
    }
    //TODO: parse nested templates
    /**                                      
     * Constructor
     * 
     * @param group a String specifying the "string" parameter of the object of class POE (e.g., "from")
     * @param index an integer specifying the Etymology pattern among possibleString                                 
     */ 
    public POE(String group, String lang){
	string = group;
	part = new ArrayList<String>();

	String[] subs = string.split("\\|");
	if (subs.length > 1) {
	    log.debug("Ignoring unexpected argument {} in wiktionary link", string);
	    string = null;
	    args = null;
	    part = null;
	} else {
	    if (string.startsWith("Kanien'keh")) {
		string = "Kanienkehaka";
	    }
	    String[] substring = string.split(":");
	    if (substring.length == 1) { //it's a Wiktionary link to the English version of Wiktionary
		log.debug("Processing wiki link {} as {} word", string, lang);
		args = new HashMap<String, String>();
		args.put("1", "l");
		args.put("word1", cleanUp(substring[0]));
		args.put("lang", lang);
		part.add("LEMMA");
		string = "l|" + lang + "|" + substring[0];
	    } else {
		if (substring[0].length() == 0 || substring[0].equals("Image") || substring[0].equals("Category") || substring[0].equals("File") || substring[0].equals("Wikisaurus")) { //it's not a Wiktionary link eg:  [[:Category:English words derived from: load (noun)]]
		    log.debug("Ignoring unexpected argument {} in wiki link", string);
		    args = null;
		    string = null;
		    part = null;
		} else {
		    args = new HashMap<String, String>();
		    args.put("1", "l");
		    args.put("lang", substring[0]);
		    args.put("word1", cleanUp(substring[1]));
		    part.add("LEMMA");
		    log.debug("Processing wiki link {} as {} template \\{\\{{}\\}{}\\}\\}", string, "l|" + substring[0], substring[1]);
		    string = "l|" + substring[0] + "|" + substring[1];
		}
	    }
	}
	this.normalizeLang();
    }

    private void normalizeLang(){
	if (this.args != null) {
	    if (this.args.containsKey("lang")) {
		String languageCode = this.args.get("lang");
		languageCode = EnglishLangToCode.enNormalize(languageCode);
		if (languageCode != null) {
		    this.args.put("lang", languageCode);
		}//else leave it as it is
	    }
	} 
    }

    /**
     * Given a String, this function replaces some symbols
     * e.g., cleanUp("[[door]],[[dur]]") returns "door,dur"
     * e.g., cleanUp("o'hare") returns "o__hare"
     * e.g., cleanUp("*duhr") returns "_duhr"
     * e.g., cleanUp ("anti-particle") returns "anti__-particle"
     *
     * @param word an input String
     * @return a String where some characters have been replaced
     */
    public String cleanUp(String word) {
        word = word.replaceAll("\\[", "").replaceAll("\\]", "").trim().replaceAll("'", "__").replaceAll("\\*", "_").replaceAll("^-", "__-");
        return word;
    }

    //template
    /**
     * Constructor for a template POE
     * Usage: POE("m|en|door")
     * e.g.: POE("cog|fr|orgue", 1) returns an object POE with POE.string="cog|fr|orgue", POE.part="COGNATE_WITH", POE.args={("1", "cog"), ("lang", "fra"), ("word1", "orgue"}
     *
     * @param group a String specifying the "string" parameter of the object of class POE (e.g., "m|en|door")
     */
    public POE(String group) {
        string = group;
        part = new ArrayList<String>();
            args = WikiTool.parseArgs(group);
	    if (args.get("1").equals("sense")){
		log.debug("Found sense template {}", string);
		part.add("SENSE");
	    } else if (args.get("1").equals("cog") || args.get("1").equals("cognate")) {//e.g.:       {{cog|fr|orgue}}
                part.add("COGNATE_WITH");
                if (args.get("3") != null) {
                    part.add("LEMMA");
                }
            } else if (args.get("1").equals("etymtwin")) {//e.g.:    {{etymtwin|lang=en}} {{m|en|foo}}
                part.add("COGNATE_WITH");
            } else if (args.get("1").equals("bor") || args.get("1").equals("borrowing")){
		if (args.get("lang") != null){
		    if (args.get("3") != null) {
		        args.put("lang", args.get("3"));
		        args.remove("3");
		    }
		    if (args.get("4") != null) {
		        part.add("LEMMA");
		        args.put("word1", cleanUp(args.get("4")));
		        args.remove("4");
		    }
		} else {
		    args.put("lang", args.get("2"));
		    args.remove("2");
		    if (args.get("3") != null) {
			part.add("LEMMA");
			args.put("word1", cleanUp(args.get("3")));
			args.remove("3");
		    }     
		}
	    } else if (args.get("1").equals("inh") || args.get("1").equals("inherited") || args.get("1").equals("der") || args.get("1").equals("derived") || args.get("1").equals("loan")) {//1=language, 2=language, (3=term), (4|alt=alternative), (tr=translation),(pos=) || 1=language, 2=language, (3=term)   || //1=language, 2=language, (3=term), (4|alt=alternative), (tr=translation),(pos=)
                if (args.get("lang") != null) {
                    if (args.get("3") != null) {
                        part.add("LEMMA");
                        args.put("word1", cleanUp(args.get("3")));
                        args.remove("3");
                    }
                    if (args.get("2") != null) {
                        args.put("lang", args.get("2"));
                        args.remove("2");
                    }
                } else {
                    if (args.get("3") != null) {
                        args.put("lang", args.get("3"));
                        args.remove("3");
                    }
                    if (args.get("4") != null) {
                        args.put("word1", cleanUp(args.get("4")));
                        args.remove("4");
                        part.add("LEMMA");
                    } else {
                        part.add("LANGUAGE");
                    }
                    if (args.get("5") != null) {
                        args.put("alt", args.get("5"));
                        args.remove("5");
                    }
                    if (args.get("6") != null) {
                        args.put("gloss1", args.get("6"));
                        args.remove("6");
                    } 
                }
            } else if (args.get("1").equals("calque")){
		args.put("lang", args.get("etyl lang"));
		args.remove("etyl lang");
		args.put("word1", args.get("etyl term"));
		args.remove("etyl term");
		part.add("FROM");
		part.add("LEMMA");
		args.put("gloss1", args.get("etyl t"));
		args.remove("etyl t");
	    } else if (args.get("1").equals("compound") || args.get("1").equals("blend")) {
                if (args.get("lang") == null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                    if (args.get("3") != null) {//(1=language - can be empty) 2=first word 3=third word
                        part.add("LEMMA");
                        args.put("word1", cleanUp(args.get("3")));
                        args.remove("3");
                    }
                    for (int kk = 4; kk < 10; kk++) {
                        if (args.get(Integer.toString(kk)) != null) {
                            args.put("word" + Integer.toString(kk - 2), cleanUp(args.get(Integer.toString(kk))));
                            args.remove(Integer.toString(kk));
                        } else {
                            break;
                        }
                    }
                } else {
                    if (args.get("2") != null) {//(1=language - can be empty) 2=first word 3=third word
                        part.add("LEMMA");
                        args.put("word1", cleanUp(args.get("2")));
                        args.remove("2");
                    }
                    for (int kk = 3; kk < 10; kk++) {
                        if (args.get(Integer.toString(kk)) != null) {
                            args.put("word" + Integer.toString(kk - 1), cleanUp(args.get(Integer.toString(kk))));
                            args.remove(Integer.toString(kk));
                        } else {
                            break;
                        }
                    }
                }
            } else if (args.get("1").equals("etycomp")) { //e.g.: {{etycomp|lang1=de|inf1=|case1=|word1=dumm|trans1=dumb|lang2=|inf2=|case2=|word2=Kopf|trans2=head}} All parameters except word1= can be omitted.
                args.put("1", "compound");
                part.add("LEMMA");
                for (int kk = 1; kk < 10; kk++) {
                    if (args.get("word" + Integer.toString(kk)) != null) {
                        args.put("word" + Integer.toString(kk), cleanUp(args.get("word" + Integer.toString(kk))));
                    }
                }
            } else if (args.get("1").equals("vi-etym-sino")) {
		args.put("lang", "zh");//check this
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                if (args.get("3") != null) {
                    args.put("gloss1", args.get("3"));
                    args.remove("3");
                }
                if (args.get("4") != null) {
                    args.put("word2", cleanUp(args.get("4")));
                    args.remove("4");
                }
                if (args.get("5") != null) {
                    args.put("gloss2", args.get("5"));
                    args.remove("5");
                }
                if (args.get("6") != null) {
                    args.put("word3", cleanUp(args.get("6")));
                    args.remove("6");
                }
                if (args.get("7") != null) {
                    args.put("gloss3", args.get("7"));
                    args.remove("7");
                }        
                part.add("LEMMA");
            } else if (args.get("1").equals("abbreviation of")) {
                part.add("FROM");
                part.add("LEMMA");
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                if (args.get("4") != null) {
                    args.put("gloss1", args.get("4"));
                    args.remove("4");
                }
            } else if (args.get("1").equals("back-form") || args.get("1").equals("named-after")) { //1=term, (2=display form)
                part.add("FROM");
                part.add("LEMMA");
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
            } else if (args.get("1").equals("fi-form of")){
		part.add("FROM");
		part.add("LEMMA");
		args.put("word1", args.get("2"));
		args.put("lang", "fin");
		args.remove("2");
	    } else if (args.get("1").equals("m") || args.get("1").equals("mention") || args.get("1").equals("l") || args.get("1").equals("link")) {
                if (args.get("2") != null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                }
                if (args.get("3") != null) {
                    if (args.get("3").equals("")) {
                        args.remove("3");
                        if (args.get("4") != null) {
                            part.add("LEMMA");
                            args.put("word1", cleanUp(args.get("4")));
                            args.remove("4");
                        }
                    } else {
                        part.add("LEMMA");
                        args.put("word1", cleanUp(args.get("3")));
                        args.remove("3");
                        if (args.get("4") != null) {
                            args.put("alt", args.get("4"));
                            args.remove("4");
                        }
                        if (args.get("5") != null) {
                            args.put("gloss1", args.get("5"));
                            args.remove("5");
                        }
                    }
                }
	    } else if (args.get("1").equals("affix") || args.get("1").equals("confix") || args.get("1").equals("prefix") || args.get("1").equals("suffix")) {
                part.add("LEMMA");
                if (args.get("lang") == null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                    for (int kk = 3; kk < 10; kk++) {
                        if (args.get(Integer.toString(kk)) != null) {
                            args.put("word" + Integer.toString(kk - 2), cleanUp(args.get(Integer.toString(kk))));
                            args.remove(Integer.toString(kk));
                        } else {
                            break;
                        }
                    }
                } else {
                    for (int kk = 2; kk < 9; kk++) {
                        if (args.get(Integer.toString(kk)) != null) {
                            args.put("word" + Integer.toString(kk - 1), cleanUp(args.get(Integer.toString(kk))));
                            args.remove(Integer.toString(kk));
                        } else {
                            break;
                        }
                    }
                }
            } else if (args.get("1").equals("infix") || args.get("1").equals("circumfix") || args.get("1").equals("clipping") || args.get("1").equals("hu-prefix") || args.get("1").equals("hu-suffix")) {
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                if (args.get("3") != null) {
                    args.put("word2", cleanUp(args.get("3")));
                    args.remove("3");
                    if (args.get("4") != null) {
                        args.put("word3", cleanUp(args.get("4")));
                        args.remove("4");
                    }
                }
		args.put("lang", "hun");
                part.add("LEMMA");
            } else if (args.get("1").equals("term")){//e.g.: {{term|de-|di-|away}}
		if (args.get("lang") == null){
		    args.put("lang", "en");
		}
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                part.add("LEMMA");
            } else if (args.get("1").equals("etyl")) {
                part.add("LANGUAGE");
                if (args.get("2") != null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                }
            } else if (args.get("1").equals("etystub") || args.get("1").equals("rfe")) {
                part.add("EMPTY");
            } else if (args.get("1").equals("-er")) {
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                args.put("word2", cleanUp(args.get("1")));
                args.put("1", "agent noun ending in -er");
                args.put("lang", "en");
                part.add("LEMMA");
            } else if (args.get("1").equals("-or")) {
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                args.put("word2", cleanUp(args.get("1")));
                args.put("1", "agent noun ending in -or");
                args.put("lang", "en");
                part.add("LEMMA");
            } else {
                log.debug("Ignoring template {} in either Etymology or Derived terms or Descendants section", string);
                args.clear();
                string = null;
                part = null;
                //part.add("ERROR");
            }        
	this.normalizeLang(); 
    }

}
