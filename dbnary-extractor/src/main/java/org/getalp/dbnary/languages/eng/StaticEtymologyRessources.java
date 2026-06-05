package org.getalp.dbnary.languages.eng;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;



public class StaticEtymologyRessources {

  static Logger log = LoggerFactory.getLogger(StaticEtymologyRessources.class);

  private static final HashMap<String, List<String>> mappings;
  static {
    HashMap<String, List<String>> tmp = new HashMap<>();
    tmp.put("FROM",
        new ArrayList<>(
            List.of("[Ff]rom", "[Bb]ack-formation (?:from)?", "[Aa]bbreviat(?:ion|ed)? (?:of|from)?", "[Cc]oined from", "[Bb]orrow(?:ing|ed)? (?:of|from)?",
                "[Cc]ontracted from", "[Aa]dopted from", "[Cc]alque(?: of)?", "[Ii]terative of", "[Ss]hort(?:ening|en|ened)? (?:form )?(?:of|from)?",
                "[Tt]hrough", "[Bb]lend of", "[Pp]articiple of", "[Aa]lteration of", "[Vv]ia", "[Dd]iminutive (?:form )?of", "[Uu]ltimately of",
                "[Vv]ariant of", "[Pp]lural of", "[Ff]orm of", "[Aa]phetic variation of", "\\<", "[Aa] \\[\\[calque\\]\\] of", "[Ff]ormed as")));

    tmp.put("TEMPLATE", Arrays.asList("\\{\\{"));

    tmp.put("LINK", List.of("\\[\\["));// removed (?:'') as this causes an error in
    // WiktinaryExtractor and function containedIn
    tmp.put("ABOVE", List.of("[Ss]ee above"));// this should precede cognateWith which matches
    // against "[Ss]ee"
    tmp.put("COGNATE_WITH", Arrays.asList("[Rr]elated(?: also)? to", "[Cc]ognate(?:s)? (?:include |with |to |including )?", "[Cc]ompare (?:also )?",
        "[Ww]hence (?:also )?", "(?:[Bb]elongs to the )?[Ss]ame family as ", "[Mm]ore at ", "[Aa]kin to ", "[Ss]ee(?:n)? (?:also )?"));// this should follow
    // abovePatternString
    // which matches against "[Ss]ee above"
    tmp.put("COMPOUND_OF", Arrays.asList("[Cc]ompound(?:ed)? (?:of|from) ", "[Mm]erg(?:ing |er )(?:of |with )?(?: earlier )?", "[Uu]niverbation of ",
        "[Ff]usion of ", "[Cc]orruption of "));
    tmp.put("UNCERTAIN", List.of("[Oo]rigin uncertain"));
    tmp.put("COMMA", List.of(","));
    tmp.put("YEAR", List.of("(?:[Aa].\\s*?[Cc].?|[Bb].?\\s*[Cc].?)?\\s*\\d++\\s*(?:[Aa].?\\s*[Cc].?|[Bb].?\\s*[Cc].?|th century|\\{\\{C\\.E\\.\\}\\})?"));
    tmp.put("AND", Arrays.asList("\\s+and\\s+", "with suffix "));
    tmp.put("PLUS", List.of("\\+"));
    tmp.put("DOT", Arrays.asList("\\.", ";"));
    tmp.put("OR", List.of("[^a-zA-Z0-9]or[^a-zA-Z0-9]"));
    tmp.put("WITH", List.of("[^a-zA-Z0-9]with[^a-zA-Z0-9]"));
    tmp.put("STOP", Arrays.asList("[Ss]uperseded", "[Dd]isplaced(?: native)?", "[Rr]eplaced", "[Mm]ode(?:l)?led on", "[Rr]eplacing", "[Cc]oined by",
        "equivalent to\\s*\\{\\{[^\\}]+\\}\\}"));// this icludes two types of patterns:
    // superseded and equivalent to
    tmp.put("COLON", List.of(":"));
    tmp.put("SLASH", List.of("/"));
    mappings = new HashMap<>(tmp);

  }

  String allowedKeys = String.join(", ", mappings.keySet());

  public static List<String> bulletSymbolsList = Arrays.asList("COMMA", "TEMPLATE", "LINK", "COLON");
  public static List<String> definitionSymbolsList = Arrays.asList("FROM", "TEMPLATE", "LINK", "ABOVE", "COGNATE_WITH", "COMPOUND_OF", "UNCERTAIN", "COMMA",
      "YEAR", "AND", "PLUS", "DOT", "OR", "WITH", "STOP", "SLASH");

  public static Pattern bulletSymbolsListPattern = Pattern.compile(eitherSymbol(bulletSymbolsList));
  public static Pattern definitionSymbolsListPattern = Pattern.compile(eitherSymbol(definitionSymbolsList));

  public static Pattern definitionSymbolsPattern = Pattern.compile("(FROM )?(LANGUAGE LEMMA |LEMMA )(COMMA |SLASH |DOT |OR )");
  public static Pattern compoundSymbolsPattern = Pattern.compile(
      "((COMPOUND_OF |FROM )(LANGUAGE )?(LEMMA (COMMA LEMMA )*)(?:(PLUS |AND |WITH )(LANGUAGE )?(LEMMA (COMMA LEMMA )*))+)|((LANGUAGE )?(LEMMA (COMMA LEMMA )*)(?:(PLUS )(LANGUAGE )?(LEMMA (COMMA LEMMA )*))+)");
  // TODO: add ARROW and allow for situations like Italian: LEMMA LEMMA COMMA LEMMA
  public static Pattern bulletSymbolsPattern = Pattern.compile("((((LEMMA )(COMMA )?)+)|(LANGUAGE ))(COLON ((LEMMA)( COMMA )?)+)?");
  public static Pattern tableDerivedLemmasPattern = Pattern.compile("(LEMMA)(?: COMMA (LEMMA))*");
  public static Pattern multipleBorrowingSymbolsPattern =
      Pattern.compile("(FROM )?(LANGUAGE LEMMA |LEMMA )((COMMA (LANGUAGE LEMMA |LEMMA ))+)?(AND (LANGUAGE LEMMA |LEMMA ))?DOT ");


  protected static String eitherSymbol(List<String> l) {
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


  protected static String eitherString(List<String> l) {
    StringBuilder toreturn = new StringBuilder();
    for (int i = 0; i < l.size(); i++) {
      toreturn.append(l.get(i));
      if (i < l.size() - 1) {
        toreturn.append("|");
      }
    }
    return toreturn.toString();
  }

  /**
   *
   * @param key must be a value that exists in the mapping
   * @param value the expression that is synonem to the mapping example : comes from ---> FROM
   *        LANGUAGE LEMMA
   *
   *
   */
  protected void addMapping(String key, String value) {
    if (!mappings.containsKey(key)) {
      System.err.println("WARNING: key " + key + " not found in mappings" + allowedKeys);

      throw new IllegalArgumentException("key " + key + " not found in mappings" + " " + allowedKeys);
    }
    List<String> list = mappings.get(key);
    // the Mappings are not supposed to contain duplicates
    if (list.contains(value)) {
      System.err.println("WARNING: value " + value + " already in mappings" + allowedKeys);
    }


    else if (list != null) {
      try{
        String transformedValue=transformToRegularExpression(value);
        list.add(transformedValue);
      }catch (IllegalArgumentException e){
        log.error(e.getMessage());
      }


    }
    // rare case if the list returns null , but might be useful in the future
    else {
      mappings.put(key, new ArrayList<>(Arrays.asList(value)));
    }
  }




  private String transformToRegularExpression(String value) throws IllegalArgumentException{
    value=value.stripLeading();
    value=value.stripTrailing();
    if(value.isEmpty()){
      throw new IllegalArgumentException("value is empty");
    }

      String firstChar=value.substring(0,1);

    if(!firstChar.matches("[a-zA-Z]")){
      throw new IllegalArgumentException("value does not start with a letter");
    }

      return value.replaceFirst(firstChar,"["+firstChar.toUpperCase() +firstChar.toLowerCase()+ "]");


  }


}


