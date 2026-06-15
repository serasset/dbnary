package org.getalp.dbnary.languages.fra;

import org.getalp.dbnary.languages.eng.Etymology;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiTool;
import org.getalp.iso639.ISO639_3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/*
 * cette class permet d'avoir une structure pour l'étymologie
 */
public class FrenchEtymology {


  private static final Logger log = LoggerFactory.getLogger(FrenchEtymology.class);
  static HashMap<String, List<String>> tmp = new HashMap<>();

  // mapping des mots , faudra l'enrichir
  static {

    tmp.put("INHERITANCE", Arrays.asList("[Dd]e", "[Dd]u", "[Dd]epuis", "[iI]ssue d[eu]","[Vv]enant d[eu]", "[Vv]ient d[eu]"));
    tmp.put("BORROWING", Arrays.asList("[Ee]mprunté à", "[Cc]alqué d[eu]" , "[Ff]ormé d[eu]"));
    tmp.put("COGNATE", List.of("[Cc]ognat(e)?"));
    tmp.put("FORM",Arrays.asList("[Ff]orm(e)?"));
    tmp.put("PHONETIC",Arrays.asList("[Pp]rononciation"));
  }

  public WikiText etymology;
  public String pageName;
  public Iterator<WikiText.Token> etymologyIterator;
  List<FSymbols> linkedTemplates = null;

  public FrenchEtymology(WikiText etymology, String pageName) {
    this.etymology = etymology;
    System.out.println("etymology : " + etymology.toString());
    etymologyIterator = etymology.templates().iterator();
    this.pageName = pageName;
  }

  public void extractEtymology() {
    List<String> list = new ArrayList<>();
    for (WikiText.Token t : etymology.tokens()) {
      Arrays.stream(getLinkedTemplatesAsString(t)).iterator().forEachRemaining(list::add);
    }

    linkedTemplates = cleanFSymbols(list.stream().map(this::getFSymbols).collect(Collectors.toList()));
    System.out.println("final linkedTemplates :");
    System.out.println(
        linkedTemplates.stream().map(fs -> fs.relationShip + ' ' + fs.templates.toString() + " size " + fs.templates.size()).collect(Collectors.joining("\n")));
  }


  /**
   *
   * @param t a WikiText.Token is a part of the etymology description of a word , usually each token contians a type of information ,
   * @return an array of strings, each string contains a template and the informations related to that template
   */

  private String[] getLinkedTemplatesAsString(WikiText.Token t) {
    return WikiTool.splitUnlessInTemplateOrLink(t.toString(),',').toArray(String[]::new);
  }


  /*
   * This method extracts the FSymbols from the given text ,
   */


  private FSymbols getFSymbols(String text) {

    String regex = "(\\{\\{[^}]+}})" + // templates
        "|(''{2}[^']+''{2})" + // italique
        "|([A-Za-zÀ-ÿ’]+)";

    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(text);

    List<String> fragment = new ArrayList<>();

    while (m.find()) {
      String t = m.group();

      // enlever italique wiki
      t = t.replaceAll("^''|''$", "");

      fragment.add(t);
    }



    FSymbols fsymbols = new FSymbols("none");


    // faut faire une recherche


    fragment = fragment.stream().toList();
    int indiceFrangment = 0;

    // while fragment not finished and keyWord not found : continue
    while (indiceFrangment < fragment.size() && !exists(tmp, fragment.get(indiceFrangment))) {
      indiceFrangment++;
    }
    // if keyWord found : set relationShip and add templates
    if (indiceFrangment < fragment.size()) {
      fsymbols.setRelationShip(getKeyFromValue(tmp, fragment.get(indiceFrangment)));
      for (int i = indiceFrangment + 1; i < fragment.size(); i++) {
        fsymbols.addTemplates(new WikiText(fragment.get(i)).templates());
      }
      // if keyWord not found : set relationShip to none ,all none relationships get removed
    } else {
      fsymbols = new FSymbols("none");
    }

    return fsymbols;
  }

  /**
   * Check if the value matches any of the regex patterns in the map.
   *
   * @param tmp The map containing regex patterns.
   * @param value The value to check.
   * @return true if the value matches any of the regex patterns, false otherwise.
   */
  private boolean exists(Map<String, List<String>> tmp, String value) {
    for (List<String> patterns : tmp.values()) {
      for (String regex : patterns) {
        if (value.matches(regex)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Get the key from the map that matches the given value.
   *
   * @param tmp The map containing regex patterns.
   * @param value The value to match.
   * @return The key that matches the value or null if no match is found.
   */
  private String getKeyFromValue(Map<String, List<String>> tmp, String value) {
    return tmp.entrySet().stream().filter(entry -> entry.getValue().stream().anyMatch(value::matches)).map(Map.Entry::getKey).findFirst().orElse(null);
  }


  private boolean isValid(FSymbols fs) {
    return !"none".equals(fs.relationShip) && (fs.templates != null && !fs.templates.isEmpty());
  }

  /**
   * Clean the list of FSymbols by removing those with an empty relationShip.
   *
   * @param fsymbolsList The list of FSymbols to clean.
   * @return The cleaned list of FSymbols.
   */
  private List<FSymbols> cleanFSymbols(List<FSymbols> fsymbolsList) {
    return fsymbolsList.stream().filter(this::isValid).collect(Collectors.toList());
  }


  public String getWordFromTemplateList(List<WikiText.Template> templates){
    switch (templates.size()) {
      case 1:
        return getTemplateWord(templates.getFirst());

      case 2:
        if ((templates.getFirst().getArgs().containsKey("mot") || templates.getFirst().getArgs().containsKey("dif"))
            && templates.getFirst().getName().equals("étyl")) {
          return getTemplateWord(templates.getFirst());
        }
        return getTemplateWord(templates.get(1));
      default:
        return "no word found";
    }
  }

  public String getTemplateWord(WikiText.Template t)  {
    switch (t.getName()) {
      case "étyl":
        try {
          return t.getArg("3").toString();
        } catch (NullPointerException e) {
          try {
            return t.getArg("mot").toString();
          } catch (NullPointerException e2) {
            return "no word found";
          }

        }



      case "recons":
        try {
          return t.getArg("1").toString();
        } catch (NullPointerException e) {
          return e.toString();
        }



      case "lien":
        return t.getArg("1").toString();
        default:
        return "null-word";


    }

  }

  public String getLanguageFromTemplateList(List<WikiText.Template> templates) {
    switch (templates.size()) {
      case 1:
        return getTemplateLanguage(templates.getFirst());
      case 2:
        if ((templates.getFirst().getArgs().containsKey("mot") || templates.getFirst().getArgs().containsKey("dif"))
            && templates.getFirst().getName().equals("étyl")) {
          return getTemplateLanguage(templates.getFirst());


        }
      default:
        return getTemplateLanguage(templates.get(1));

    }
  }


  public String getTemplateLanguage(WikiText.Template t) {

    switch (t.getName()) {
      case "étyl":
        try {
          return ISO639_3.sharedInstance.getIdCode(t.getArg("1").toString());
        } catch (NullPointerException e) {
          return e.toString();
        }

      case "recons":
        try {
          return ISO639_3.sharedInstance.getIdCode(t.getArg("lang").toString());
        } catch (NullPointerException e) {
          return "null_lang";
        }
      default:
        return "template non recon";
    }
  }

}
