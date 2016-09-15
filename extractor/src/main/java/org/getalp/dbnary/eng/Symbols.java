/**
 *
 */
package org.getalp.dbnary.eng;

import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pantaleo
 */

/**
 * TerminalSymbols is a part of etymology
 * it has properties
 * .asTerminalSymbols an ArrayList&lt;String&gt;, e.g., {"LANGUAGE", "LEMMA"}
 * .args a Map&lt;String, String&gt;, e.g., {("1", "m"), ("lang", "eng"), ("word1", "door")}
 * .string, e.g., "m|en|door"
 */
public class Symbols {
    public ArrayList<String> values;
    public Map<String, String> args;
    public String string;//needed only for debugging purposes
    static Logger log = LoggerFactory.getLogger(Symbols.class);

    public Symbols(String group, String lang, String s){
	if (s.equals("TEMPLATE")){
	    parseTemplate(group, lang);
	} else if (s.equals("LINK")){
	    parseLink(group, lang);
	} else {
	    parseOther(group, s);
	}
    }

    public void parseOther(String group, String value){
	string = group;
	args = null;
	values = new ArrayList<String>();
	values.add(value);
    }
    
    public Symbols(ArrayList<Symbols> a){
	for (Symbols b : a){
	    if (b.args.get("lang") == null){
		log.debug("no lang in template {}", b);
		string = null;
		args = null;
		values = null;
		return;
	    }
	}
	StringBuilder compound = new StringBuilder();
	compound.append("_compound");
	for (int i = 0; i < a.size(); i ++){
	    if (a.get(i).args.get("lang") == null){
		log.debug("no lang in template {}", a.get(i).string);
		string = null;
		args = null;
		values = null;
		return;
	    }
	    if (a.get(i).args.get("word1") == null){
		log.debug("no lemma in template {}", a.get(i).string);
		string = null;
		args = null;
		values = null;
		return;
	    }
	    compound.append("|lang" + Integer.toString(i + 1) + "=" + a.get(i).args.get("lang"));
	    compound.append("|word" + Integer.toString(i + 1) + "=" + a.get(i).args.get("word1"));
	}
	string = compound.toString();
	values = new ArrayList<String>();
					     
	values.add("LEMMA");
	args = WikiTool.parseArgs(string);
    }
    
    public void parseTemplate(String group, String lang){
	string = group;
	values = new ArrayList<String>();
            args = WikiTool.parseArgs(string);
	    if (args.get("1").equals("rel-top") && args.get("2").equals("cognates")){
		values.add("STOP");
	    } else if (args.get("1").equals("rel-top") && args.get("2").equals("detailed etymology")){
		values.add("STOP"); 
	    } else if (args.get("1").equals("ja-r")){
		args.put("lang", "jpn");//Japanese
		args.put("word1", args.get("2"));
		values.add("LEMMA");
	    } else if (args.get("1").equals("Han compound")){
		for (int kk = 2; kk < 12; kk++) {
		    if (args.get(Integer.toString(kk)) != null) {
			args.put("word" + Integer.toString(kk - 2), args.get(Integer.toString(kk)));
			args.remove(Integer.toString(kk));
		    } else {
			break;
		    }
		}
		args.put("lang", "Hani");//??
		values.add("FROM");
		values.add("LEMMA");    
	    } else if (args.get("1").equals("SI link")){
		args.put("lang", lang);
		args.put("word1", args.get("2"));
		values.add("LEMMA");
	    } else if (args.get("1").equals("etymtree")){
		if (args.get("3") != null && args.get("4") != null){
		    args.put("lang", args.get("3"));
		    args.remove("3");
		    args.put("word1", args.get("4"));
		    args.remove("4"); 
		    values.add("ETYMTREE");
		} else if (args.get("2")!= null){
		    args.put("lang", args.get("2"));
		    args.remove("2");
		    values.add("ETYMTREE");     
		}
	    } else if (args.get("1").equals("Han simp")){
		if (args.get("2") != null){
		    args.put("lang", "Hani");//??  
		    args.put("word1", args.get("2"));
		    args.remove("2");
		    values.add("FROM");
		    values.add("LEMMA"); 
		} else {
		    args.clear();
		    values = null;
		    log.debug("Skipping Han simp template {}", string);
		}
	    } else if (args.get("1").equals("sense")){
		log.debug("Found sense template {}", string);
	        values.add("SENSE");
	    } else if (args.get("1").equals("jbo-etym")){//TODO: this is incorrect!!! 
		int counter = 1;
		ArrayList<String> tt = new ArrayList<>();
		for (String kk : args.keySet()){
		    tt.add(kk);
		}
		args.put("lang", "jbo");
		for (String k : tt){		    
		   if (args.get(k) != null){
		      if (k.endsWith("_t")){
			  k = k.substring(0, k.length() - 2);
			  args.put("lang" + Integer.toString(counter), "k");
			  args.put("word" + counter, args.get(k + "_t"));
			  args.remove(k + "_t");
		    	  String tr = args.get(k + "_tr");
		    	  if (tr != null){
			      args.put("tr" + counter, tr);
			      args.remove(k + "_tr");
		    	  }
		    	  if (args.get(k) != null){
			      args.remove(k);
		    	  }
		          counter ++;
		      }
		   }
		}
		values.add("FROM"); 
		values.add("LEMMA"); 
	    } else if (args.get("1").equals("cog") || args.get("1").equals("cognate")) {//e.g.:       {{cog|fr|orgue}}
		if (args.get("2") != null && args.get("3") != null && ! args.get("3").equals("-")) {
		    values.add("LANGUAGE");
		    values.add("LEMMA"); 
		    args.put("lang", args.get("2"));
		    args.put("word1", cleanUp(args.get("3")));
                } else {
		    args.clear();
		    values = null;
		}
            } else if (args.get("1").equals("etymtwin")) {//e.g.:    {{etymtwin|lang=en}} {{m|en|foo}}
                values.add("COGNATE_WITH");
            } else if (args.get("1").equals("bor") || args.get("1").equals("borrowing")){
	        values.add("FROM");
		if (args.get("lang") != null){
		    if (args.get("2") != null) {
		        args.put("lang", args.get("2"));
		        args.remove("2");
		    }
		    if (args.get("3") != null) {
		        values.add("LEMMA");
		        args.put("word1", cleanUp(args.get("3")));
		        args.remove("3");
		    }
		} else {
		    args.put("lang", args.get("3"));
		    args.remove("3");
		    if (args.get("4") != null) {
			values.add("LEMMA");
			args.put("word1", cleanUp(args.get("4")));
			args.remove("4");
		    }     
		}
	    } else if (args.get("1").equals("inh") || args.get("1").equals("inherited") || args.get("1").equals("der") || args.get("1").equals("derived") || args.get("1").equals("loan")) {//1=language, 2=language, (3=term), (4|alt=alternative), (tr=translation),(pos=) || 1=language, 2=language, (3=term)   || //1=language, 2=language, (3=term), (4|alt=alternative), (tr=translation),(pos=)
                if (args.get("lang") != null) {
                    if (args.get("3") != null) {
                        values.add("LEMMA");
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
                        values.add("LEMMA");
                    } else {
                        values.add("LANGUAGE");
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
		values.add("FROM");
	        values.add("LEMMA");
		args.put("gloss1", args.get("etyl t"));
		args.remove("etyl t");
	    } else if (args.get("1").equals("compound") || args.get("1").equals("blend")) {
		if (args.get("1").equals("compound")){
		    args.put("1", "_compound");
		} else if (args.get("1").equals("blend")){
		    args.put("1", "_blend");
		}
                if (args.get("lang") == null) {
                    if (args.get("3") != null) {//(1=language - can be empty) 2=first word 3=third word
                        values.add("LEMMA");
                        args.put("word1", cleanUp(args.get("3")));
                        args.remove("3");
		    }    
		    if (args.get("2") != null){
			args.put("lang1", args.get("2"));
			args.remove("2");
                    } else {
			args.put("lang1", lang);
		    }
                    for (int kk = 4; kk < 12; kk++) {
                        if (args.get(Integer.toString(kk)) != null) {
                            args.put("word" + Integer.toString(kk - 2), cleanUp(args.get(Integer.toString(kk))));
                            args.remove(Integer.toString(kk));
			    args.put("lang" + Integer.toString(kk - 2), args.get("lang1"));
                        } else {
                            break;
                        }
                    }
                } else {
                    if (args.get("2") != null) {//(1=language - can be empty) 2=first word 3=third word
                        values.add("LEMMA");
                        args.put("word1", cleanUp(args.get("2")));
                        args.remove("2");
			args.put("lang1", args.get("lang"));
			args.remove("lang");
                    }
                    for (int kk = 3; kk < 12; kk++) {
                        if (args.get(Integer.toString(kk)) != null) {
                            args.put("word" + Integer.toString(kk - 1), cleanUp(args.get(Integer.toString(kk))));
                            args.remove(Integer.toString(kk));
			    args.put("lang" + Integer.toString(kk - 1), args.get("lang1"));
                        } else {
                            break;
                        }
                    }
                }
            } else if (args.get("1").equals("etycomp")) { //e.g.: {{etycomp|lang1=de|inf1=|case1=|word1=dumm|trans1=dumb|lang2=|inf2=|case2=|word2=Kopf|trans2=head}} All parameters except word1= can be omitted.
                args.put("1", "compound");
                values.add("LEMMA");
                for (int kk = 1; kk < 12; kk++) {
                    if (args.get("word" + Integer.toString(kk)) != null) {
                        args.put("word" + Integer.toString(kk), cleanUp(args.get("word" + Integer.toString(kk))));
                    }
                }
            } else if (args.get("1").equals("vi-l") || args.get("1").equals("vi-link")){
		args.put("lang", "vi");
		args.put("word1", args.get("2"));
		args.remove("2");
		if (args.get("4") != null){
		    args.put("synonim", args.get("4"));
		    args.remove("4");
		}
		if (args.get("3") != null){
		    args.put("gloss", args.get("3"));
		    args.remove("3"); 
		}
		values.add("LEMMA");
	    } else if (args.get("1").equals("zh-l")){
	        args.put("lang", "zh");
		if (args.get("2") != null){
		    args.put("word1", args.get("2"));
		    values.add("LEMMA");
		    args.remove("2");
		} else {
		    args.clear();
		    values = null;
		}
	    } else if (args.get("1").equals("vi-etym-sino")) {
		args.put("lang", "zh");//check this
		if (args.get("2") != null){
                    args.put("word1", cleanUp(args.get("2")));
                    args.remove("2");
		    values.add("LEMMA");
		} else {
		    values = null;
		    args.clear();
		}
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
            } else if (args.get("1").equals("abbreviation of")) {
                values.add("FROM");
                values.add("LEMMA");
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                if (args.get("4") != null) {
                    args.put("gloss1", args.get("4"));
                    args.remove("4");
                }
            } else if (args.get("1").equals("back-form") || args.get("1").equals("named-after")) { //1=term, (2=display form)
		if (args.get("lang")==null){//how to deal with {{back-form|ilu|lang=io|gloss=he, him|nodot=yes}}, {{l|io|elu|gloss=she, her}} and {{l|io|olu|gloss=it}}.
		    if (args.get("2")!= null){
			args.put("lang", args.get("2"));
			args.remove("2");
			values.add("FROM"); 
		    } else {
			args.put("lang", "en");
			log.debug("Warning: no language specified for lemma {} in back-formation template, using English", string);
		    }
		    if (args.get("3")!= null){
		        args.put("word1", cleanUp(args.get("3")));
		        args.remove("3");
			values.add("LEMMA");
		    }
		} else {
		    if (args.get("2")!= null){ 
                        args.put("word1", cleanUp(args.get("2")));
                        args.remove("2");
		        values.add("FROM");
		        values.add("LEMMA");
		    }
		}
            } else if (args.get("1").equals("lang")){
		if (args.get("2")!= null){
		    args.put("lang", args.get("2"));
		}
		if (args.get("3")!= null){
		    args.put("word1", cleanUp(args.get("3")));
		    values.add("LEMMA");  
		} else {
		    args.put("1", "_etyl");
		    values.add("LANGUAGE");
		}
	    } else if (args.get("1").equals("fi-form of")){
		values.add("FROM");
		values.add("LEMMA");
		args.put("word1", args.get("2"));
		args.put("lang", "fin");
		args.remove("2");
	    } else if (args.get("1").equals("m") || args.get("1").equals("mention") || args.get("1").equals("l") || args.get("1").equals("link") || args.get("1").equals("_m")) {
                if (args.get("2") != null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                } 
                if (args.get("3") != null) {
                    values.add("LEMMA");
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
                } else {
		    args.clear();
		    values = null;
		}
	    } else if (args.get("1").equals("affix") || args.get("1").equals("confix") || args.get("1").equals("prefix") || args.get("1").equals("suffix")) {
                values.add("LEMMA");
                if (args.get("lang") == null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                    for (int kk = 3; kk < 12; kk++) {
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
                values.add("LEMMA");
            } else if (args.get("1").equals("term")){//e.g.: {{term|de-|di-|away}}
		if (args.get("lang") == null){
		    args.put("lang", "en");
		}
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                values.add("LEMMA");
            } else if (args.get("1").equals("etyl") || args.get("1").equals("_etyl")) {
                values.add("LANGUAGE");
                if (args.get("2") != null) {
                    args.put("lang", args.get("2"));
                    args.remove("2");
                }
            } else if (args.get("1").equals("etystub") || args.get("1").equals("rfe") || args.get(
"1").equals("unk.")) {
                values.add("EMPTY");
            } else if (args.get("1").equals("-er")) {
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                args.put("word2", cleanUp(args.get("1")));
                args.put("1", "agent noun ending in -er");
                args.put("lang", "en");
                values.add("LEMMA");
            } else if (args.get("1").equals("-or")) {
                args.put("word1", cleanUp(args.get("2")));
                args.remove("2");
                args.put("word2", cleanUp(args.get("1")));
                args.put("1", "agent noun ending in -or");
                args.put("lang", "en");
                values.add("LEMMA");
            } else {
                log.debug("Ignoring template {} in either Etymology or Derived terms or Descendants section", string);
                args.clear();
                string = null;
                values = null;
                //values.add("ERROR");
            }        
	this.normalizeLang();
    }
    
    public void parseLink(String group, String lang){
	string = group;
	values = new ArrayList<String>();
            if (string.startsWith("Kanien'keh")) {
	        string = "Kanienkehaka";
            }
            String[] subs = string.split("\\|");
            String[] substring = subs[0].split(":");
            String[] subsubs = subs[0].split("\\#");

            if (substring[0].length() == 0 || substring[0].equals("w") || substring[0].equals("Image") || substring[0].equals("Category") || substring[0].equals("File") || substring[0].equals("Wikisaurus")) { //it's not a Wiktionary link eg:  [[:Category:English words derived from: load (noun)]]
	        log.debug("Ignoring unexpected argument {} in wiki link", string);
                args = null;
                string = null;
                values = null;
            } else {
                args = new HashMap<String, String>();
                args.put("1", "l");
                if (subsubs.length > 1){
	            lang = EnglishLangToCode.threeLettersCode(subsubs[1].trim());
	            if (lang != null){
	                args.put("word1", cleanUp(subsubs[0]));
	                args.put("lang", lang);
	                values.add("LEMMA");
	                string = "l|" + lang + "|" + cleanUp(args.get("word1"));
	            } else {
	                log.debug("Ignoring unexpected argument {} in wiki link", string);
	                args = null;
	                values = null;
	                string = null;
	            }
                } else {
	            if (substring.length == 1) { //it's a Wiktionary link to the English version of Wiktionary
	                args.put("lang", lang);
	                args.put("word1", cleanUp(substring[0]));
	            } else {
	                args.put("lang", substring[0]);
	                args.put("word1", cleanUp(substring[1]));
	            }
	            values.add("LEMMA");
	            string = "l|" + args.get("lang") + "|" + args.get("word1");
	            log.debug("Processing wiki link {} as {} word {}", string, args.get("lang"), args.get("word1"));
                }
            }
            this.normalizeLang();
    }
    
    private void normalizeLang(){
	if (this.args != null) {
	    for (String key : this.args.keySet()){
		if (key.startsWith("lang")){
		    String languageCode = EnglishLangToCode.threeLettersCode(this.args.get(key));
		    if (languageCode != null) {
			this.args.put(key, languageCode);
		    }
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
    }
