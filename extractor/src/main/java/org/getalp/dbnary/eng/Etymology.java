package org.getalp.dbnary.eng;

import org.getalp.dbnary.Pair;
import org.getalp.dbnary.wiki.WikiTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author pantaleo
 */
public class Etymology {
    static Logger log = LoggerFactory.getLogger(Etymology.class);

    //used by toOrPattern
    private static String eitherString(List<String> l) {
        StringBuilder toreturn = new StringBuilder();
        for (int i = 0; i < l.size(); i++) {
            toreturn.append(l.get(i));
            if (i < l.size() - 1) {
                toreturn.append("|");
            }
        }
        return toreturn.toString();
    }

    private static String eitherSymbol(List<String> l) {
        StringBuilder toreturn = new StringBuilder();
        toreturn.append("(");
        for (int i = 0; i < l.size(); i++) {
            toreturn.append(eitherString(mappings.get(l.get(i))));
            if (i < l.size() - 1) {
                toreturn.append(")|(");
            }
        }
        toreturn.append(")");
        return toreturn.toString();
    }

    private static final HashMap<String, List<String>> mappings;

    static {
        HashMap<String, List<String>> tmp = new HashMap<String, List<String>>();
        tmp.put("FROM", Arrays.asList("[Ff]rom", "[Bb]ack-formation (?:from)?", "[Aa]bbreviat(?:ion|ed)? (?:of|from)?", "[Cc]oined from", "[Bb]orrow(?:ing|ed)? (?:of|from)?", "[Cc]ontracted from", "[Aa]dopted from", "[Cc]alque(?: of)?", "[Ii]terative of", "[Ss]hort(?:hening|hen|hened)? (?:form )?(?:of|from)?", "[Tt]hrough", "[Pp]articiple of", "[Aa]lteration of", "[Vv]ia", "[Dd]iminutive (?:form )?of", "[Uu]ltimately of", "[Vv]ariant of", "[Pp]lural of", "[Ff]orm of", "[Aa]phetic variation of", "\\<", "[Aa] \\[\\[calque\\]\\] of", "[Ff]ormed as"));
        tmp.put("TEMPLATE", Arrays.asList("\\{\\{"));
        tmp.put("LINK", Arrays.asList("\\[\\["));//removed (?:'') as this causes an error in WiktinaryExtractor and function containedIn
        tmp.put("ABOVE", Arrays.asList("[Ss]ee above"));//this should precede cognateWith which matches against "[Ss]ee"
        tmp.put("COGNATE_WITH", Arrays.asList("[Rr]elated(?: also)? to", "[Cc]ognate(?:s)? (?:include |with |to |including )?", "[Cc]ompare (?:also )?", "[Ww]hence (?:also )?", "(?:[Bb]elongs to the )?[Ss]ame family as ", "[Mm]ore at ", "[Aa]kin to ", "[Ss]ee(?:n)? (?:also )?"));//this should follow abovePatternString which matches against "[Ss]ee above"
        tmp.put("COMPOUND_OF", Arrays.asList("[Cc]ompound(?:ed)? (?:of|from) ", "[Mm]erg(?:ing |er )(?:of |with )?(?: earlier )?", "[Uu]niverbation of ", "[Ff]usion of ", "[Cc]orruption of "));
        tmp.put("UNCERTAIN", Arrays.asList("[Oo]rigin uncertain"));
        tmp.put("COMMA", Arrays.asList(","));
        tmp.put("YEAR", Arrays.asList("(?:[Aa].\\s*?[Cc].?|[Bb].?\\s*[Cc].?)?\\s*\\d++\\s*(?:[Aa].?\\s*[Cc].?|[Bb].?\\s*[Cc].?|th century|\\{\\{C\\.E\\.\\}\\})?"));
        tmp.put("AND", Arrays.asList("\\s+and\\s+", "with suffix "));
        tmp.put("PLUS", Arrays.asList("\\+"));
        tmp.put("DOT", Arrays.asList("\\.", ";"));
        tmp.put("OR", Arrays.asList("[^a-zA-Z0-9]or[^a-zA-Z0-9]"));
        tmp.put("WITH", Arrays.asList("[^a-zA-Z0-9]with[^a-zA-Z0-9]"));
        tmp.put("STOP", Arrays.asList("[Ss]uperseded", "[Dd]isplaced(?: native)?", "[Rr]eplaced", "[Mm]ode(?:l)?led on", "[Rr]eplacing", "[Cc]oined by", "equivalent to\\s*\\{\\{[^\\}]+\\}\\}"));//this icludes two types of patterns: superseded and equivalent to
        tmp.put("COLON", Arrays.asList(":"));
        mappings = new HashMap(tmp);
    }

    public static List<String> bulletSymbolsList = Arrays.asList("COMMA", "TEMPLATE", "LINK", "COLON");
    public static List<String> definitionSymbolsList = Arrays.asList("FROM", "TEMPLATE", "LINK", "ABOVE", "COGNATE_WITH", "COMPOUND_OF", "UNCERTAIN", "COMMA", "YEAR", "AND", "PLUS", "DOT", "OR", "WITH", "STOP");

    public static Pattern bulletSymbolsListPattern = Pattern.compile(eitherSymbol(bulletSymbolsList));
    public static Pattern definitionSymbolsListPattern = Pattern.compile(eitherSymbol(definitionSymbolsList));

    public static Pattern definitionSymbolsPattern = Pattern.compile("(FROM )?(LANGUAGE LEMMA |LEMMA )(COMMA |DOT |OR )");
    public static Pattern compoundSymbolsPattern = Pattern.compile("((COMPOUND_OF |FROM )(LANGUAGE )?(LEMMA )(?:(PLUS |AND |WITH )(LANGUAGE )?(LEMMA ))+)|((LANGUAGE )?(LEMMA )(?:(PLUS )(LANGUAGE )?(LEMMA ))+)");
    //TODO: add ARROW and allow for situations like Italian: LEMMA LEMMA COMMA LEMMA
    public static Pattern bulletSymbolsPattern = Pattern.compile("(((LANGUAGE)|(LEMMA)) (COLON ))?((LEMMA)( COMMA )?)+");
    public static Pattern tableDerivedLemmasPattern = Pattern.compile("(LEMMA)(?: COMMA (LEMMA))*");

    public String lang;
    public String string;
    public ArrayList<Symbols> symbols;

    public Etymology(String s, String l) {
        string = s;
        lang = l;
        symbols = new ArrayList<Symbols>();
    }

    public void fromTableToSymbols() {
        string = WikiTool.removeReferencesIn(string);

        string = WikiTool.removeTextWithinParenthesesIn(string);
        string = string.trim();

        if (string == null || string.equals("")) {
            return;
        }

        toSymbols(bulletSymbolsList, bulletSymbolsListPattern);

        ArrayList<Symbols> lemmas = new ArrayList<>();
        Matcher m = tableDerivedLemmasPattern.matcher(toString(symbols));
        while (m.find()) {
            for (Symbols p : symbols) {
                if (p.values.get(0).equals("LEMMA")) {
                    lemmas.add(p);
                }
            }
            break;
        }
        symbols = lemmas;
    }

    public void fromDefinitionToSymbols() {
        if (string == null || string.equals("")) {
            return;
        }

        //REMOVE TEXT WITHIN HTML REFERENCE TAG
        string = WikiTool.removeReferencesIn(string);
        //REMOVE TEXT WITHIN TABLES
        string = WikiTool.removeTablesIn(string);

        //REMOVE TEXT WITHIN PARENTHESES UNLESS PARENTHESES FALL INSIDE A WIKI LINK OR A WIKI TEMPLATE
        string = WikiTool.removeTextWithinParenthesesIn(string);
        string = string.trim();

        if (string == null || string.equals("")) {
            return;
        } else {
            if (!string.endsWith(".")) {
                //add final dot if etymology string doesn't end with a dot
                string += ".";
            }
        }


        toSymbols(definitionSymbolsList, definitionSymbolsListPattern);

        parseEtyl();

        parseCompound();

        //find where list of cognates or OR statements start
        //e.g, if toString(symbols) == "FROM LEMMA COMMA FROM LEMMA COMMA COGNATE_WITH LEMMA COMMA" registers 6, the index of "COGNATE_WITH" or
        //e.g., if toString(symbols) == "FROM LEMMA OR LEMMA" it registers 2, the index of "OR",
        //remove any element of the input ArrayList<Symbols> after that index.
        for (int j = 0; j < symbols.size(); j++) {
            if (symbols.get(j).values.size() > 0) {
                if (symbols.get(j).values.get(0).equals("COGNATE_WITH") || symbols.get(j).values.get(0).equals("OR")) {
                    symbols.subList(j, symbols.size()).clear();
                    break;
                }
            }
        }

        ArrayList<Pair> m = findMatch(symbols, definitionSymbolsPattern);
        if (m.size() == 0) {
            return;//there is no match to the definitionSymbolsPattern
        }

        //remove any Symbols after "DOT" or "AND" that follow a definitionSymbolsPattern
        for (int j = m.get(0).end + 1; j < symbols.size(); j++) {
            for (String b : symbols.get(j).values) {
                if (b.equals("DOT") || b.equals("AND")) {
                    symbols.subList(j, symbols.size()).clear();
                    j = symbols.size();//break j loop
                    break;//break symbols loop
                }
            }
        }
        //remove any Symbols that preceeds the first match to the definitionSymbolsPattern
        symbols.subList(0, m.get(0).start).clear();
    }

    //TODO: handle * [[crisismanager]] {{g|m}}    
    public void fromBulletToSymbols() {
        //REPLACE LANGUAGE STRING WITH LANGUAGE _ETYL TEMPLATE
        parseLanguage();
	//* &rarr; Italian: {{l|it|baruffare}}
	//Sardinian: [[pobulu]], [[poburu]], [[populu]] -> {{_etyl|en|sc}}: [[pobulu]], [[poburu]], [[populu]]  
	//{{_etyl|eng|sc}}: [[pobulu]], [[poburu]], [[populu]]-> LANGUAGE COLON LEMMA COMMA LEMMA COMMA LEMMA
        toSymbols(bulletSymbolsList, bulletSymbolsListPattern);

        parseEtyl();//[[pobulu]] -> {{m|lang=sc|word1=pobulu}}

        //REPLACE SENSE TEMPLATE
        //case "{{sense|kill}} {{l|en|top oneself}}" -> {{l|en|top oneself|sense=kill}}
        parseSense();

        ArrayList<Symbols> lemmas = new ArrayList<>();
        Matcher m = bulletSymbolsPattern.matcher(toString(symbols));
        while (m.find()) {
            //case LANGUAGE COLON LEMMA COMMA LEMMA, e.g.:
            //case "Sardinian: [[pobulu]], [[poburu]], [[populu]]"
            //and case "[[Asturian]]: {{l|ast|águila}}"
            if (m.group(2) != null && m.group(5) != null) {
                if (m.group(2).equals("LANGUAGE") && m.group(5).equals("COLON ")) {
                    String language = null;
                    for (Symbols b : symbols) {
                        if (b.values.get(0).equals("LANGUAGE")) {
                            language = b.args.get("lang");
                        }
                        if (language != null && b.values.get(0).equals("LEMMA")) {
                            b.args.put("lang", language);
                            lemmas.add(b);
                        }
                    }
                } else if (m.group(2).equals("LEMMA")) {//case "{{ja-r|武威|ぶい}}: [[military]] [[power]]"
		    //TODO: distinguish "[[color]], [[colour]]" and "[[military]] [[power]]" 
                    for (Symbols b : symbols) {
                        if (b.values.get(0).equals("LEMMA")) {
                            lemmas.add(b);
                            break;
                        }
                    }
                    //case "[[color]], [[colour]]"
                }
            } else if (m.group(2) == null && m.group(5) == null) {//case "[[color]], [[colour]]"
                if (m.group(7).equals("LEMMA")) {
		    //TODO: distinguish "[[color]], [[colour]]" and "[[military]] [[power]]"
                    for (Symbols b : symbols) {
                        if (b.values.get(0).equals("LEMMA")) {
                            lemmas.add(b);
                        }
                    }
                }
            }
        }
        symbols = lemmas;
    }

    public void toSymbols(List<String> l, Pattern p) {
        if (string == null || string.trim().isEmpty()) {
            return;
        }

        ArrayList<Pair> templatesLocations = WikiTool.locateEnclosedString(string, "{{", "}}");
        ArrayList<Pair> linksLocations = WikiTool.locateEnclosedString(string, "[[", "]]");

        //match against regex pattern of symbols p
        Matcher m = p.matcher(string);
        while (m.find()) {
            for (int i = 0; i < m.groupCount(); i++) {
                if (m.group(i + 1) != null) {
                    //check if match is contained in template or link
                    boolean check = false;
                    //check if match is contained in a template (or is a template)
                    Pair match = new Pair(m.start(), m.end());
                    for (Pair template : templatesLocations) {
                        if (match.containedIn(template)) {//match is contained in a template
                            check = true;
                            if (l.get(i).equals("TEMPLATE")) {//match is a template
				System.out.format("temp: %s\n", string.substring(template.start + 2, template.end - 2));
                                Symbols b = new Symbols(string.substring(template.start + 2, template.end - 2), lang, l.get(i));
                                if (b.values != null && b.args != null) {
                                    if (b.values.get(0).equals("STOP")) {
                                        return;
                                    }
                                    symbols.add(b);
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
                                if (l.get(i).equals("LINK")) {//match is a link
				    System.out.format("link: %s\n", string.substring(link.start + 2, link.end - 2));
                                    Symbols b = new Symbols(string.substring(link.start + 2, link.end - 2), lang, l.get(i));
                                    if (b.values != null && b.args != null) {
                                        symbols.add(b);
                                    }
                                    break;
                                }//else ignore match
                            }
                        }
                    }
                    if (check == false) {//if match is neither contained in a template nor in a link
                        Symbols b = new Symbols(m.group(i + 1), lang, l.get(i));
                        if (b.values != null) {
                            if (b.values.get(0).equals("STOP")) {
                                return;
                            }
                            symbols.add(b);
                        }
                    }
                }
            }
        }
    }

    //when a LEMMA is preceded by a LANGUAGE etyl template
    //set language of LEMMA to language of etyl template
    private void parseEtyl() {
        String etylLang = null;
        int etylIndex = -1;
        for (int i = 0; i < symbols.size(); i++) {
            if (symbols.get(i).values != null && symbols.get(i).args != null) {
                if (symbols.get(i).args.get("1").equals("etyl") || symbols.get(i).args.get("1").equals("_etyl")) {
                    etylLang = symbols.get(i).args.get("lang");
                    etylIndex = i;
                }
                if (etylIndex != -1 && i == etylIndex + 1) {
                    symbols.get(i).args.put("lang", etylLang);
                }
            }
        }
    }

    //TODO: this cannot parse correctly
    //{{ja-r|宮古島|^みやこじま|[[w:Miyako Island|Miyako Island]]; [[w:Miyakojima, Okinawa|Miyakojima, Okinawa]]}}
    //{{ja-r|宮古諸島|^みやこしょとう|[[w:Miyako Islands|Miyako Islands]]}}
    private void parseLanguage() {
        String[] subs = string.split(":");

        if (subs.length == 2) {
            ArrayList<Pair> linksLocations = WikiTool.locateEnclosedString(subs[0], "[[", "]]");
            if (linksLocations.size() == 0) {//PARSE case "Sardinian: [[pobulu]], [[poburu]], [[populu]]"
		//also PARSE case "→ Georgian: {{l|ka|ყავა|gloss=coffee}}"
                String bulletLang = EnglishLangToCode.threeLettersCode(subs[0].replace("→","").trim());

                if (bulletLang != null) {
                    string = "{{_etyl|" + bulletLang + "|" + lang + "}} : " + subs[1].trim();
                }
            } else if (linksLocations.size() == 1) {//PARSE case "[[Asturian]]: {{l|ast|águila}}"
                String bulletLang = EnglishLangToCode.threeLettersCode(subs[0].substring(2, subs[0].length() - 2));

                if (bulletLang != null) {
                    string = "{{_etyl|" + bulletLang + "|" + lang + "}} : " + subs[1].trim();
                }
            }
        } else if (subs.length > 2) {
            log.debug("Ignoring bullet {}", string);
            string = "";
        }
    }

    public void parseSense() {
        int aSize = symbols.size();

        if (aSize > 1) {
            for (int i = 0; i < aSize; i++) {
                if (symbols.get(i).values.get(0).equals("SENSE")) {
                    String sense = symbols.get(i).args.get("2");
                    if (i + 1 < aSize && symbols.get(i + 1).values.get(0).equals("LEMMA")) {
                        Symbols tmp = symbols.get(i + 1);
                        tmp.args.put("sense", sense);
                        symbols.set(i, tmp);
                        symbols.remove(i + 1);
                        aSize--;
                    } else if (i - 1 >= 0 && symbols.get(i - 1).values.get(0).equals("LEMMA")) {
                        Symbols tmp = symbols.get(i - 1);
                        tmp.args.put("sense", sense);
                        symbols.set(i, tmp);
                        symbols.remove(i - 1);
                        aSize--;
                        i--;
                    }
                }
            }
        }
        return;
    }

    /**
     * @return a String (e.g., "FROM LEMMA OR LEMMA") concatenating the property "symbols" of each element of ArrayList<Symbols>
     */
    public static String toString(ArrayList<Symbols> a) {
        StringBuilder s = new StringBuilder();
        for (Symbols b : a) {
            for (String values : b.values) {
                s.append(values);
                s.append(" ");
            }
        }
        return s.toString();
    }

    public ArrayList<Pair> findMatch(ArrayList<Symbols> a, Pattern p) {
        ArrayList<Pair> toreturn = new ArrayList<Pair>();
        if (a == null || a.size() == 0) {
            return toreturn;
        }
        ArrayList<Integer> arrayListInteger = new ArrayList<Integer>();
        ArrayList<Integer> arrayListPosition = new ArrayList<Integer>();
        int c = 0;
        arrayListPosition.add(c);
        for (int i = 0; i < a.size(); i++) {
            Symbols b = a.get(i);
            for (int j = 0; j < b.values.size(); j++) {
                arrayListInteger.add(i);
                c += b.values.get(j).length() + 1;
                arrayListPosition.add(c);
            }
        }

        Matcher m = p.matcher(toString(a));
        while (m.find()) {
            int start = -1, end = -1;
            for (int i = 0; i < arrayListPosition.size() - 1; i++) {
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
     * with a single "LEMMA" Symbol of type compound|lang1|word1|lang2|word2
     */
    public void parseCompound() {
        //iterate over all matches to a compound pattern
        //starting from the last
        ArrayList<Pair> match = findMatch(symbols, compoundSymbolsPattern);
        if (match == null || match.size() == 0) {
            return;
        }
        for (int i = match.size() - 1; i >= 0; i--) {
            Pair m = match.get(i);
            ArrayList<Symbols> a = new ArrayList<Symbols>();
            for (int k = m.start; k < m.end + 1; k++) {
                Symbols b = symbols.get(k);
                for (String values : b.values) {
                    if (values.equals("LEMMA") && b.args != null) {
                        a.add(b);
                        break;
                    }
                }
            }
            Symbols b = new Symbols(a);
            if (b.string != null) {
                Symbols f = new Symbols("from", lang, "FROM");
                symbols.set(m.start, f);
                symbols.set(m.start + 1, b);
                symbols.subList(m.start + 2, m.end + 1).clear();
            } else {
                symbols.subList(m.start, m.end + 1).clear();
                break;
            }
        }
    }
}
