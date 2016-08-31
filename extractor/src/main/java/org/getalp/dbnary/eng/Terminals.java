package org.getalp.dbnary.eng;

import java.util.List; 
import java.util.Arrays;
import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Terminals{
    //used by toAlternativePatterns
    private static String either(List<String> l){
	StringBuilder toreturn = new StringBuilder();
	for (int i = 0; i < l.size(); i ++){
	    toreturn.append(l.get(i));
	    if (i < l.size() - 1){
		toreturn.append("|");
	    }
	}
	return toreturn.toString();
    }

    //used by the constructor
    private static Pattern toPossiblePatterns(List<String> l){
	StringBuilder toreturn = new StringBuilder();
	toreturn.append("(");
	for (int i = 0; i < l.size(); i ++){
	    toreturn.append(either(mappings.get(l.get(i))));
	    if (i < l.size() - 1){
		toreturn.append(")|(");
	    }
	}
	toreturn.append(")");
	return Pattern.compile(toreturn.toString());
    }

    private static final HashMap<String, List<String> > mappings;
    static {
	HashMap<String, List<String> > tmp = new HashMap<String, List<String> >();
	tmp.put("FROM", Arrays.asList("[Ff]rom", "[Bb]ack-formation (?:from)?", "[Aa]bbreviat(?:ion|ed)? (?:of|from)?", "[Cc]oined from", "[Bb]orrow(?:ing|ed)? (?:of|from)?", "[Cc]ontracted from", "[Aa]dopted from", "[Cc]alque(?: of)?", "[Ii]terative of", "[Ss]hort(?:hening|hen|hened)? (?:form )?(?:of|from)?", "[Tt]hrough", "[Pp]articiple of", "[Aa]lteration of", "[Vv]ia", "[Dd]iminutive (?:form )?of", "[Uu]ltimately of", "[Vv]ariant of", "[Pp]lural of", "[Ff]orm of", "[Aa]phetic variation of", "\\<"));
	tmp.put("", Arrays.asList("\\{\\{"));
	tmp.put("LEMMA", Arrays.asList("\\[\\["));//removed (?:'') as this causes an error in WiktionaryExtractor and function containedIn
	tmp.put("ABOVE", Arrays.asList("[Ss]ee above"));//this should precede cognateWith which matches against "[Ss]ee"
	tmp.put("COGNATE_WITH", Arrays.asList("[Rr]elated(?: also)? to", "[Cc]ognate(?:s)? (?:include |with |to |including )?", "[Cc]ompare (?:also )?", "[Ww]hence (?:also )?", "(?:[Bb]elongs to the )?[Ss]ame family as ", "[Mm]ore at ", "[Aa]kin to ", "[Ss]ee(?:n)? (?:also )?"));//this should follow abovePatternString which matches against "[Ss]ee above"
	tmp.put("COMPOUND_OF", Arrays.asList("[Cc]ompound(?:ed)? (?:of|from) ", "[Mm]erg(?:ing |er )(?:of |with )?(?: earlier )?", "[Uu]niverbation of "));
	tmp.put("UNCERTAIN", Arrays.asList("[Oo]rigin uncertain"));
	tmp.put("COMMA", Arrays.asList(","));
	tmp.put("YEAR", Arrays.asList("(?:[Aa].\\s*?[Cc].?|[Bb].?\\s*[Cc].?)?\\s*\\d++\\s*(?:[Aa].?\\s*[Cc].?|[Bb].?\\s*[Cc].?|th century|\\{\\{C\\.E\\.\\}\\})?"));
	tmp.put("AND", Arrays.asList("\\s+and\\s+", "with suffix "));
	tmp.put("PLUS", Arrays.asList("\\+"));
	tmp.put("DOT", Arrays.asList("\\.", ";"));
	tmp.put("OR", Arrays.asList("[^a-zA-Z0-9]or[^a-zA-Z0-9]"));
	tmp.put("WITH", Arrays.asList("[^a-zA-Z0-9]with[^a-zA-Z0-9]"));
        tmp.put("STOP", Arrays.asList("[Ss]uperseded", "[Dd]isplaced( native)?", "[Rr]eplaced", "[Mm]ode(?:l)?led on", "[Rr]eplacing", "equivalent to\\s*\\{\\{[^\\}]+\\}\\}"));//this are two types of patterns: superseded and equivalent to
	tmp.put("COLUMN", Arrays.asList(":"));
        mappings = new HashMap(tmp);
    }

    List<String> list;
    Pattern pattern;

    public Terminals(List<String> l){
        list = l;
	pattern = toPossiblePatterns(list);
    }
}
