package org.getalp.dbnary.eng;

import java.util.List; 
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sentence{
    private static String concatenateRegexElements(List<String> l){
	StringBuilder toreturn = new StringBuilder();
	for (int i = 0; i < l.size(); i++){
	    toreturn.append(l.get(i));
	    if (i < l.size() - 1){
		toreturn.append("|");
	    }
	}
	return toreturn.toString();
    }

    private static String concatenateRegex(List<String> l){
	StringBuilder toreturn = new StringBuilder();
	toreturn.append("(");
	for (int i = 0; i < l.size(); i++){
	    toreturn.append(regexElements.get(l.get(i)));
	    if (i < l.size() - 1){
		toreturn.append("|");
	    }
	}
	toreturn.append(")");
	return toreturn.toString();
    }

    private static final HashMap<String, String> regexElements;
    static {
	HashMap<String, String> tmp = new HashMap<String, String>();
	tmp.put("FROM", concatenateRegexElements(Arrays.asList("[Ff]rom", "[Bb]ack-formation (?:from)?", "[Aa]bbreviat(?:ion|ed)? (?:of|from)?", "[Cc]oined from", "[Bb]orrow(?:ing|ed)? (?:of|from)?", "[Cc]ontracted from", "[Aa]dopted from", "[Cc]alque(?: of)?", "[Ii]terative of", "[Ss]hort(?:hening|hen|hened)? (?:form )?(?:of|from)?", "[Tt]hrough", "[Pp]articiple of", "[Aa]lteration of", "[Vv]ia", "[Dd]iminutive (?:form )?of", "[Uu]ltimately of", "[Vv]ariant of", "[Pp]lural of", "[Ff]orm of", "[Aa]phetic variation of", "\\<")));
	tmp.put("", concatenateRegexElements(Arrays.asList("\\{\\{")));
	tmp.put("LEMMA", concatenateRegexElements(Arrays.asList("\\[\\[")));//removed (?:'') as this causes an error in WiktionaryExtractor and function containedIn
	tmp.put("ABOVE", concatenateRegexElements(Arrays.asList("[Ss]ee above")));//this should precede cognateWith which matches against "[Ss]ee"
	tmp.put("COGNATE_WITH", concatenateRegexElements(Arrays.asList("[Rr]elated(?: also)? to", "[Cc]ognate(?:s)? (?:include |with |to |including )?", "[Cc]ompare (?:also )?", "[Ww]hence (?:also )?", "(?:[Bb]elongs to the )?[Ss]ame family as ", "[Mm]ore at ", "[Aa]kin to ", "[Ss]ee(?:n)? (?:also )?")));//this should follow abovePatternString which matches against "[Ss]ee above"
	tmp.put("COMPOUND_OF", concatenateRegexElements(Arrays.asList("[Cc]ompound(?:ed)? (?:of|from) ", "[Mm]erg(?:ing |er )(?:of |with )?(?: earlier )?", "[Uu]niverbation of ")));
	tmp.put("UNCERTAIN", concatenateRegexElements(Arrays.asList("[Oo]rigin uncertain")));
	tmp.put("COMMA", concatenateRegexElements(Arrays.asList(",")));
	tmp.put("YEAR", concatenateRegexElements(Arrays.asList("(?:[Aa].\\s*?[Cc].?|[Bb].?\\s*[Cc].?)?\\s*\\d++\\s*(?:[Aa].?\\s*[Cc].?|[Bb].?\\s*[Cc].?|th century|\\{\\{C\\.E\\.\\}\\})?")));
	tmp.put("AND", concatenateRegexElements(Arrays.asList("\\s+and\\s+", "with suffix ")));
	tmp.put("PLUS", concatenateRegexElements(Arrays.asList("\\+")));
	tmp.put("DOT", concatenateRegexElements(Arrays.asList("\\.", ";")));
	tmp.put("OR", concatenateRegexElements(Arrays.asList("[^a-zA-Z0-9]or[^a-zA-Z0-9]")));
	tmp.put("WITH", concatenateRegexElements(Arrays.asList("[^a-zA-Z0-9]with[^a-zA-Z0-9]")));
	tmp.put("STOP", concatenateRegexElements(Arrays.asList("[Ss]uperseded", "[Dd]isplaced( native)?", "[Rr]eplaced", "[Mm]ode(?:l)?led on", "[Rr]eplacing", "equivalent to\\s*\\{\\{[^\\}]+\\}\\}")));//this are two types of patterns: superseded and equivalent to
	tmp.put("COLUMN", concatenateRegexElements(Arrays.asList(":")));
	regexElements = new HashMap(tmp);
    }

    List<String> elements;
    Pattern elementsPattern;
    Pattern pattern;

    public Sentence(List<String> l, Pattern p){
        elements = l;
	elementsPattern = Pattern.compile(concatenateRegex(elements));
	pattern = p;
    }
}
