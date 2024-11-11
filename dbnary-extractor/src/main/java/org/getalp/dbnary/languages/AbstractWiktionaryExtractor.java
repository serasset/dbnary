package org.getalp.dbnary.languages;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.getalp.LangTools;
import org.getalp.dbnary.ExtractionFeature;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.IWiktionaryExtractor;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.enhancer.TranslationSourcesDisambiguator;
import org.getalp.dbnary.enhancer.evaluation.EvaluationStats;
import org.getalp.dbnary.enhancer.evaluation.TranslationGlossesStatsModule;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.iso639.ISO639_3;
import org.getalp.iso639.ISO639_3.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWiktionaryExtractor implements IWiktionaryExtractor {

  // TODO: Alter the extraction process by allowing multiple lines in a macro and evaluate the final
  // result
  // TODO: Determine how many nested macro are used in the different wiktionary languages.
  // These should be independent of the language
  private static final Logger log = LoggerFactory.getLogger(AbstractWiktionaryExtractor.class);

  protected String pageContent;
  protected IWiktionaryDataHandler wdh;
  protected ExpandAllWikiModel expander;

  protected String wiktionaryPageName;

  protected WiktionaryPageSource wi = null;

  public AbstractWiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super();
    this.wdh = wdh;
  }

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    this.wi = wi;
    expander = new ExpandAllWikiModel(this.wi, new Locale(wdh.getExtractedLanguage()), "/${image}",
        "/${title}");
  }

  protected String getWiktionaryPageName() {
    return wiktionaryPageName;
  }

  protected void setWiktionaryPageName(String wiktionaryPageName) {
    this.wiktionaryPageName = wiktionaryPageName;
    expander.setPageName(wiktionaryPageName);
  }
  // Suppression des commentaires XML d'un texte

  protected final static String debutOrfinDecomPatternString;

  static {
    debutOrfinDecomPatternString = "(?:" + "(<!--)" + ")|(?:" + "(-->)" + ")";
  }

  protected final static Pattern xmlCommentPattern;

  static {
    xmlCommentPattern = Pattern.compile(debutOrfinDecomPatternString, Pattern.DOTALL);
  }

  private static final int A = 0;
  private static final int B = 1;

  public static String removeXMLComments(String s) {
    if (null == s) {
      return null;
    }

    int ET = A;
    Matcher xmlCommentMatcher = xmlCommentPattern.matcher(s);

    int indexEnd = 0; // index du debut de la partie qui nous interesse
    int indexBegin = 0; // index de la fin de la partie qui nous interesse

    StringBuilder result = new StringBuilder(); // la nouvelles chaine de caracteres

    while (xmlCommentMatcher.find()) {
      String g1 = xmlCommentMatcher.group(1); // g1 =<!-- ou null
      String g2 = xmlCommentMatcher.group(2); // g2=-> ou null

      switch (ET) {
        case A:
          if (g1 != null) {
            // On a trouvé un debut de commentaire

            // On place la fin de la partie qui nous interesse
            indexEnd = xmlCommentMatcher.start(1);
            // on change d'etat
            ET = B;
            result.append(s, indexBegin, indexEnd);
          }
          break;
        case B:
          if (g2 != null) {
            // On a trouvé la fin du commentaire

            // on place le debut de le partie qui nous interesse
            indexBegin = xmlCommentMatcher.end(2);
            // on change d'etat
            ET = A;
          }
          break;

        default:
          System.err.println("Unexpected state number:" + ET);
          break;
      }

    }
    if (xmlCommentMatcher.hitEnd()) {
      switch (ET) {
        case A:
          result.append(s.substring(indexBegin));
          break;
        case B:
          break;

        default:
          System.err.println("Unexpected state number:" + ET);
          break;
      }
    }
    return result.toString();

  }


  // DONE: filter out pages that are in specific Namespaces (Wiktionary:, Categories:, ...)
  // TODO: take Redirect page into account as alternate spelling.
  // DONE: some xml comments may be in the string values. Remove them.
  public void extractData(String wiktionaryPageName, String pageContent) {
    // Entries containing the special char ":" are pages belonging to specific
    // namespaces.(Wiktionary:, Categories:, ...).
    // Such pages are simply ignored.
    if (filterOutPage(wiktionaryPageName)) {
      return;
    }
    this.setWiktionaryPageName(wiktionaryPageName);
    this.pageContent = removeXMLComments(pageContent);

    if (pageContent == null) {
      return;
    }
    try {
      log.trace("Extracting page '{}'", wiktionaryPageName);
      extractData();
    } catch (RuntimeException e) {
      System.err.println(
          "Caught RuntimeException while parsing entry [" + this.getWiktionaryPageName() + "]");
      log.trace("Caught RuntimeException while parsing entry [{}]. Stack trace follows",
          this.getWiktionaryPageName(), e);
      throw e;
    }
  }

  /**
   * @param pagename the name of the page
   * @return returns true iff the pagename should be ignored during extraction.
   */
  public boolean filterOutPage(String pagename) {
    return pagename.contains(":");
  }

  public abstract void extractData();

  static String defOrExamplePatternString = "(?:" + WikiPatterns.definitionPatternString + ")|(?:"
      + WikiPatterns.examplePatternString + ")";

  static Pattern defOrExamplePattern =
      Pattern.compile(defOrExamplePatternString, Pattern.MULTILINE);

  protected void extractDefinitions(int startOffset, int endOffset) {

    Matcher defOrExampleMatcher = defOrExamplePattern.matcher(this.pageContent);
    defOrExampleMatcher.region(startOffset, endOffset);
    while (defOrExampleMatcher.find()) {
      if (null != defOrExampleMatcher.group(1)) {
        extractDefinition(defOrExampleMatcher);
      } else if (null != defOrExampleMatcher.group(2)) {
        extractExample(defOrExampleMatcher);
      }
    }
  }

  /**
   * Extract and register a new wordsense in the current lexical entry. The definition is extracted
   * from a Matcher object where the group contains the list item's content and group(1) the
   * definition. The definition level is taken from the matcher object
   *
   * @param definitionMatcher the pattern matcher containing the definition
   * @return the resource representing the new word sense
   */
  public Resource extractDefinition(Matcher definitionMatcher) {
    // Remove at least the leading spaces from definition as they are meaningless (just a separator
    // from the list item) and may be interpreted as pre content when taken out of context.
    String definition = definitionMatcher.group(1).trim();
    int defLevel = 1;
    if (definitionMatcher.group().charAt(1) == '#') {
      defLevel = 2;
    }
    return extractDefinition(definition, defLevel);
  }

  /**
   * Extract and register a new wordsense in the current lexical entry.
   * 
   * @param definition the definition string
   * @param defLevel the level at which the word sense is defined (sub-senses vs senses)
   * @return the resource representing the new word sense
   */
  public Resource extractDefinition(String definition, int defLevel) {
    String def = cleanUpMarkup(definition);
    if (!def.isEmpty()) {
      return wdh.registerNewDefinition(def, defLevel);
    }
    return null;
  }

  public static String cleanUpMarkup(String group) {
    return cleanUpMarkup(group, false);
  }

  public void extractExample(Matcher definitionMatcher) {
    String example = definitionMatcher.group(2);
    extractExample(example);
  }

  protected static final Map<String, String> NON_STANDARD_LANGUAGE_MAPPINGS = new HashMap<>();

  /**
   * Standardize a wiktionary language code into a "valid" language code. As language editions use
   * codes that may differ from ISO-639-3. Sometimes these codes are referring to languages that are
   * not represented in the iso standard and there are some that may lead to invalid turtle dumps
   * (either because IRI become malformed, but also because the language tag of string values is
   * invalid.
   * <p>
   * In this common implementation, we only consider ISO language codes.
   * <p>
   * Language extractor may refine this method or just add new language to the
   * NON_STANDARD_LANGUAGE_MAPPINGS map.
   *
   * @param language the language code to be checked
   * @return the String representing the standardized representation for the language (usable as a
   *         language tag in RDF) or null if language is invalid
   */
  protected String validateAndStandardizeLanguageCode(String language) {
    Lang languageObject = ISO639_3.sharedInstance.getLang(language);
    if (languageObject != null) {
      return languageObject.getId();
    }
    return NON_STANDARD_LANGUAGE_MAPPINGS.get(language);
  }

  public Resource extractExample(String example) {
    String ex = expander.expandAll(example, null);
    if (ex != null && !ex.isEmpty()) {
      return wdh.registerExample(ex, null);
    }
    return null;
  }


  // Some utility methods that should be common to all languages
  // DONE: (priority: top) keep annotated lemma (#{lemma}#) in definitions.
  // DONE: handle ''...'' and '''...'''.
  // DONE: suppress affixes that follow links, like: e in [[français]]e.
  // DONE: Extract lemma AND OCCURENCE of links in non human readable form

  /**
   * cleans up the wiktionary markup from a string in the following manner: <br>
   * str is the string to be cleaned up. the result depends on the value of humanReadable.
   * Wiktionary macros are always discarded. xml/xhtml comments are always discarded. Wiktionary
   * links are modified depending on the value of humanReadable. e.g. str = "{{a Macro}} will be
   * [[discard]]ed and [[feed|fed]] to the [[void]]." if humanReadable is true, it will produce:
   * "will be discarded and fed to the void." if humanReadable is false, it will produce: "will be
   * #{discard|discarded}# and #{feed|fed}# to the #{void|void}#."
   *
   * @param str is the String to be cleaned up
   * @param humanReadable a boolean
   * @return a String
   */
  public static String cleanUpMarkup(String str, boolean humanReadable) {
    Matcher m = WikiPatterns.macroOrLinkPattern.matcher(str);
    StringBuilder sb = new StringBuilder(str.length());
    String leftGroup, rightGroup;
    while (m.find()) {
      if (m.group(1) != null) {
        // It's a macro, ignore it for now
        m.appendReplacement(sb, "");
      } else if ((leftGroup = m.group(3)) != null) {
        // It's a link, only keep the alternate string if present.
        rightGroup = m.group(4);
        String replacement;
        if (rightGroup == null && humanReadable) {
          replacement = leftGroup;
        } else if (humanReadable) {
          replacement = rightGroup;
        } else {
          replacement = "#{" + leftGroup + "|" + ((rightGroup == null) ? leftGroup : rightGroup);
        }
        // Discard stupidly encoded morphological affixes.
        if (!humanReadable) { // && str.length() > m.end() &&
          // Character.isLetter(str.charAt(m.end()))
          int i = m.end();
          StringBuilder affix = new StringBuilder();
          while (i < str.length() && Character.isLetter(str.charAt(i))) {
            affix.append(str.charAt(i));
            i++;
          }
          replacement = replacement + affix;
          replacement = replacement + "}#";
          replacement = Matcher.quoteReplacement(replacement);
          m.appendReplacement(sb, replacement);
          // Start over the match after discarded affix
          str = str.substring(i);
          m.reset(str);
        } else {
          replacement = Matcher.quoteReplacement(replacement);
          m.appendReplacement(sb, replacement);
        }
      } else {
        m.appendReplacement(sb, "");
      }
    }
    m.appendTail(sb);
    // normalize whitespaces
    int l = 0;
    int i = 0;
    boolean previousCharIsASpace = true;
    while (i != sb.length()) {
      if (Character.isSpaceChar(sb.charAt(i))) {
        if (!previousCharIsASpace) {
          previousCharIsASpace = true;
          sb.setCharAt(l, ' ');
          l++;
        }
      } else {
        previousCharIsASpace = false;
        sb.setCharAt(l, sb.charAt(i));
        l++;
      }
      i++;
    }
    if (l > 0 && sb.charAt(l - 1) == ' ') {
      l--;
    }
    sb.setLength(l);
    return sb.toString();
  }

  private static final String definitionMarkupString = "#\\{([^\\|]*)\\|([^\\}]*)\\}\\#";
  private static final Pattern definitionMarkup = Pattern.compile(definitionMarkupString);

  public static String convertToHumanReadableForm(String def) {
    Matcher m = definitionMarkup.matcher(def);
    StringBuilder sb = new StringBuilder(def.length());
    while (m.find()) {
      m.appendReplacement(sb, m.group(2));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  // TODO: dissociates entry parsing and structure building in 2 classes.
  // So that we will factorize the matching code.
  protected void extractOrthoAlt(int startOffset, int endOffset) {
    Matcher bulletListMatcher = WikiPatterns.bulletListPattern.matcher(this.pageContent);
    bulletListMatcher.region(startOffset, endOffset);
    while (bulletListMatcher.find()) {
      String alt = cleanUpMarkup(bulletListMatcher.group(1), true);
      if (!alt.isEmpty()) {
        wdh.registerAlternateSpelling(alt);
      }
    }
  }

  // TODO: There are entries where Files, Fichier or Image Links are inside the entry and not at the
  // end of it...
  // links.group(1).equalsIgnoreCase("Image") ||
  // links.group(1).equalsIgnoreCase("File") ||
  // links.group(1).equalsIgnoreCase("Fichier")
  protected int computeRegionEnd(int blockStart, Matcher m) {
    if (m.hitEnd()) {
      // Take out categories, files and interwiki links.
      Matcher links = WikiPatterns.categoryOrInterwikiLinkPattern.matcher(pageContent);
      links.region(blockStart, m.regionEnd());
      while (links.find()) {
        // TODO: use localized versions of the namespaces
        if (links.group(2).equals(this.wiktionaryPageName)
            || links.group(1).equalsIgnoreCase("Catégorie")
            || links.group(1).equalsIgnoreCase("Category")
            || links.group(1).equalsIgnoreCase("Kategorie")
            || links.group(1).equalsIgnoreCase("Annexe")
            || LangTools.getCode(links.group(1)) != null) {
          return links.start();
        } else if (links.group(1) != null) {
          // System.out.println("--- In: " + this.wiktionaryPageName + " --->");
          // System.out.println(links.group());
        }
      }
      return m.regionEnd();
    } else {
      return m.start();
    }
  }


  // TODO: Some nyms can be placed in sublists and lists (hence with ** or ***). In this case, we
  // currently extract the additional stars.
  protected void extractNyms(String synRelation, int startOffset, int endOffset) {

    // Extract all links
    Matcher linkMatcher = WikiPatterns.linkPattern.matcher(this.pageContent);
    linkMatcher.region(startOffset, endOffset);
    while (linkMatcher.find()) {
      // TODO: remove debug specific treatment for nym extraction and take a better heuristic
      // It's a link, only keep the alternate string if present.
      String leftGroup = linkMatcher.group(1);
      if (leftGroup != null && !leftGroup.equals("") && !leftGroup.startsWith("Wikisaurus:")
          && !leftGroup.startsWith("Catégorie:") && !leftGroup.startsWith("#")) {
        wdh.registerNymRelation(leftGroup, synRelation);
      }
    }
  }

  // FIXME this doesn't handle nested parentheses. Is it correct?
  // Should be fixed now --pantaleo
  public static String stripParentheses(String s) {
    final int A = 0;
    final int B = 1;

    int ET = A;
    String resultat = "";
    int debut = 0;
    int fin = 0; // la fin de partie qui nous inter
    int i = 0;
    int numberOfParentheses = 0;

    while (i != s.length()) {
      switch (ET) {
        case A:
          if (s.charAt(i) == '(') {
            numberOfParentheses++;
            // On a trouvé un debut de parenthese

            // On place la fin de la partie qui nous interesse
            fin = i;
            // on change d'etat
            ET = B;
            resultat = resultat + s.substring(debut, fin);
          }
          break;
        case B:
          if (s.charAt(i) == ')') {
            numberOfParentheses = numberOfParentheses - 1;
            if (numberOfParentheses == 0) {
              // On a trouvé la fin du commentaire

              // on place le debut se le partie qui nous interesse
              debut = i + 1;
              // on change d'etat
              ET = A;
            }
          } else if (s.charAt(i) == '(') {
            numberOfParentheses++;
          }
          break;

        default:
          System.err.println("Unexpected state number:" + ET);
          break;
      }

      // On passe au caractère suivant ;
      i = i + 1;

    }
    if (i == s.length()) {
      switch (ET) {
        case A:
          resultat = resultat + s.substring(debut);
          break;
        case B:
          break;

        default:
          System.err.println("Unexpected state number:" + ET);
          break;
      }
    }
    return resultat;
  }

  private AbstractGlossFilter createGlossFilter() {
    AbstractGlossFilter f = null;
    String cname = this.getClass().getCanonicalName();
    int dpos = cname.lastIndexOf('.');
    String pack = cname.substring(0, dpos);
    try {
      Class<?> wec = Class.forName(pack + ".GlossFilter");
      f = (AbstractGlossFilter) wec.getConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      System.err
          .println("No gloss filter found for " + cname + " reverting to  DefaultGlossFilter");
      f = new DefaultGlossFilter();
    } catch (InstantiationException e) {
      System.err.println("Could not instanciate gloss filter.");
    } catch (IllegalAccessException e) {
      System.err.println("Illegal access to gloss filter.");
    } catch (IllegalArgumentException e) {
      System.err.println("Illegal argument passed to gloss filter's constructor.");
      e.printStackTrace(System.err);
    } catch (SecurityException e) {
      System.err.println("Security exception while instanciating gloss filter.");
      e.printStackTrace(System.err);
    } catch (InvocationTargetException e) {
      System.err.println("InvocationTargetException exception while instanciating gloss filter. ");
      e.printStackTrace(System.err);
    } catch (NoSuchMethodException e) {
      System.err.println("No appropriate constructor when instanciating gloss filter.");
    }
    return f;
  }

  @Override
  public void postProcessData(String dumpFileVersion) {
    postProcessModel(wdh.getEndolexFeatureBox(ExtractionFeature.ENHANCEMENT),
        wdh.getEndolexFeatureBox(ExtractionFeature.MAIN), dumpFileVersion);
    postProcessModel(wdh.getExolexFeatureBox(ExtractionFeature.ENHANCEMENT),
        wdh.getExolexFeatureBox(ExtractionFeature.MAIN), dumpFileVersion);
  }

  public void postProcessModel(Model enhancementModel, Model sourceModel, String dumpFileVersion) {
    if (null == enhancementModel) {
      return;
    }
    TranslationGlossesStatsModule stats = new TranslationGlossesStatsModule();
    EvaluationStats evaluator = new EvaluationStats();
    TranslationSourcesDisambiguator disambiguator =
        new TranslationSourcesDisambiguator(0.1, 0.9, 0.05, true, stats, evaluator);
    // TODO: getCurrentEntryLanguage may be incorrect in DataHandler refinements...
    disambiguator.processTranslations(sourceModel, enhancementModel, wdh.getExtractedLanguage());

    // add stats results in the Stats box
    for (String l : stats.getStatsMap().keySet()) {
      wdh.buildDatacubeObservations(l, stats.getStatsMap().get(l),
          evaluator.getConfidenceMap().get(l), dumpFileVersion);
    }
  }

  @Override
  public void computeStatistics(String dumpVersion) {
    wdh.computeStatistics(wdh.getEndolexFeatureBox(ExtractionFeature.STATISTICS),
        wdh.getEndolexFeatureBox(ExtractionFeature.MAIN), dumpVersion);
    wdh.computeStatistics(wdh.getExolexFeatureBox(ExtractionFeature.ENHANCEMENT),
        wdh.getExolexFeatureBox(ExtractionFeature.MAIN), dumpVersion);
  }

  @Override
  public void populateMetadata(String dumpFilename, String extractorVersion) {
    // LIME is global to endolexicon and exolexicon
    wdh.populateMetadata(wdh.getEndolexFeatureBox(ExtractionFeature.LIME),
        wdh.getEndolexFeatureBox(ExtractionFeature.MAIN), dumpFilename, extractorVersion, false);
    wdh.populateMetadata(wdh.getExolexFeatureBox(ExtractionFeature.LIME),
        wdh.getExolexFeatureBox(ExtractionFeature.MAIN), dumpFilename, extractorVersion, true);
  }

}
