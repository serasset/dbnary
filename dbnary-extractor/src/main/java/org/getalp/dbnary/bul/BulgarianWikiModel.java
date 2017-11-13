package org.getalp.dbnary.bul;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.AbstractGlossFilter;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.IWiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BulgarianWikiModel extends DbnaryWikiModel {

  private Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);

  protected final static Set<String> bulgarianPOS = new TreeSet<String>();
  protected final static HashMap<String, String> nymMarkerToNymName;

  protected final static String translationExpression = "\\s?\\*?\\s?.*\\s?:\\s*.*";
  protected final static Pattern translationPattern = Pattern
      .compile(BulgarianWikiModel.translationExpression);
  protected final static String glossExpression = "(\\]|\\})(\\]|\\})[^\\]\\[\\}\\{\\:\\n]*((\\[|\\{)(\\[|\\{)|$)";
  static final Pattern glossPattern = Pattern.compile(glossExpression);
  protected final static String translationLangExpression = "\\s*\\*\\s*[^\\:]*";
  protected final static Pattern translationLangPattern = Pattern
      .compile(BulgarianWikiModel.translationLangExpression);
  //protected final static String translationBodyExpression = "(\\[\\[[^\\]]+\\]\\]\\s?\\(?[^\\)\\[\\,]*\\)?\\)?)";
  //protected final static String translationBodyExpression = "(\\[\\[[^\\]]+\\]\\]\\s?\\(?[^\\)\\[]+\\)?;?)";
  protected final static String translationBodyExpression = "([^:\\*]*(\\[|\\{)*)$";
  protected final static Pattern translationBodyPattern = Pattern
      .compile(BulgarianWikiModel.translationBodyExpression);

  static {

    bulgarianPOS.add("Съществително нарицателно име"); // Common Noun
    bulgarianPOS.add("Съществително собствено име"); // Proper Noun
    bulgarianPOS.add("Прилагателно име"); // Adjective
    bulgarianPOS.add("Глагол"); // Verb
    bulgarianPOS.add("Наречие"); //  Adverb
    bulgarianPOS.add("Частица"); // Particle
    bulgarianPOS.add("Числително име"); //Ordinal
    bulgarianPOS.add("Предлог"); // Preposition
    bulgarianPOS.add("междуметие"); // Interjection
    bulgarianPOS.add("съюз"); // Conjunction
  }

  static {

    nymMarkerToNymName = new HashMap<String, String>(20);
    nymMarkerToNymName.put("Синоними", "syn"); //
    nymMarkerToNymName.put("Антоними", "ant"); //

    nymMarkerToNymName.put("Гипонимы", "hypo");
    nymMarkerToNymName.put("Хипоними", "hyper");
    nymMarkerToNymName.put("Мероним", "mero");
    nymMarkerToNymName.put("Холоним", "holo");
  }

  protected final static String nymExpression = "(\\[\\[[^\\]]*\\]\\])";
  protected final static Pattern nymPattern = Pattern.compile(BulgarianWikiModel.nymExpression);
  static final Pattern linkPattern = Pattern.compile("\\[\\[([^\\]]*)\\]\\]");
  static final Pattern macroPattern = Pattern.compile("\\{\\{([^\\}]*)\\}\\}");
  static final Pattern parens = Pattern.compile("\\(([^\\)]*)\\)");
  private IWiktionaryDataHandler delegate;
  private boolean hasAPOS = false;

  private DefinitionsWikiModel expander;
  Set<String> templates = null;
  private AbstractGlossFilter glossFilter;

  public BulgarianWikiModel(IWiktionaryDataHandler wdh, Locale locale, String imageBaseURL,
      String linkBaseURL, AbstractGlossFilter glossFilter) {
    this(wdh, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL, glossFilter);
  }

  public BulgarianWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale,
      String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = wdh;
    if (log.isDebugEnabled()) {
      templates = new HashSet<String>();
    }
    this.glossFilter = glossFilter;
    this.expander = new DefinitionsWikiModel(wi, this.fLocale, this.getImageBaseURL(),
        this.getWikiBaseURL(), templates);
  }

  @Override
  public void setPageName(String pageTitle) {
    super.setPageName(pageTitle);
    this.expander.setPageName(pageTitle);
  }

  public boolean parseBulgarianBlock(String block) {
    initialize();
    if (block == null) {
      return false;
    }
    WikipediaParser.parse(block, this, true, null);
    initialize();
    boolean r = hasAPOS;
    hasAPOS = false;
    return r;
  }

  @Override
  public void substituteTemplateCall(String templateName,
      Map<String, String> parameterMap, Appendable writer)
      throws IOException {
    String pos = getPOS(templateName);
    if (null != pos) {
      hasAPOS = true;
      delegate.addPartOfSpeech(pos);

      for (String section : parameterMap.keySet()) {
        if (section.contains("ЗНАЧЕНИЕ")) {
          extractDefinitions(parameterMap.get(section));
        } else if (section.contains("ПРЕВОД")) {
          String sectionContent = parameterMap.get(section)
              .replaceAll("\\[\\[:[^:]*:[^\\|]*\\|\\s*\\(?[^\\)\\]]*\\)?\\s*\\]\\]", "");
          sectionContent = sectionContent.replaceAll("\\[\\[\\s*\\]\\]", "");
          // if (sectionContent.contains("\n# ")) log.debug("Translation with sens number in {}", this.getPageName());
          // TODO: use a shared instance of the translation parser.
          TranslationsParser tp = new TranslationsParser();
          tp.extractTranslations(sectionContent, delegate, glossFilter);

          // extractTranslationsFromRawWikiCode(sectionContent)
          //delegate.registerTranslation();
        } else if (section.contains("ID")) { // ID, same as page name for Bulgarian
        } else if (section.contains("РОД")) { //Gender
        } else if (section.contains("ТИП")) { // Type
        } else if (section.contains("ИЗРАЗИ")) { // Examples
        } else if (section.contains("ЕТИМОЛОГИЯ")) { // Etymology
        } else if (section.contains("ПРОИЗВОДНИ ДУМИ")) { // Derived Terms
        } else if (section.contains("ДРУГИ")) { // Related Words
        } else {
          for (String rt : nymMarkerToNymName.keySet()) {
            String body = parameterMap.get(section);
            if (section.toLowerCase().contains(rt.toLowerCase()) && !body.isEmpty()) {
              Matcher nymMatcher = nymPattern.matcher(body);
              while (nymMatcher.find()) {
                String name = nymMatcher.group().replaceAll("\\[", "").replaceAll("\\]", "");
                // System.err.println(name);
                delegate.registerNymRelation(name, nymMarkerToNymName.get(rt));
              }
            }
          }
        }
      }
    } else if ("п".equals(templateName)) {
      String trad = parameterMap.get("2");
      if (null != trad) {
        writer.append(trad);
      }
    } else {
      // Just ignore the other template calls (uncomment to expand the template calls).
      // super.substituteTemplateCall(templateName, parameterMap, writer);
      appendTemplateCall(templateName, parameterMap, writer);
    }
  }


  private void appendTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) {
    try {
      writer.append("{{").append(templateName);
      for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
        // gwtWiki provides a parameter map that is sorted by insertion time
        // and unnamed parameters are inserted in the expected order.
        String key = entry.getKey();
        String val = entry.getValue();
        if (key.matches("\\d+")) {
          writer.append("|").append(val);
        } else {
          writer.append("|").append(key).append("=").append(val);
        }
      }
      writer.append("}}");
    } catch (IOException e) {
      // NOP
    }
  }

  private void extractDefinitions(String defSection) {
    String protectedDefs = defSection.replaceAll("(?m)^(#{1,2})", "__$1__");
    String def = expander.expandAll(protectedDefs);
    def = def.replaceAll("(?m)^__(#{1,2})__", "$1");
    Matcher definitionMatcher = WikiPatterns.definitionPattern.matcher(def);
    while (definitionMatcher.find()) {
      if (definitionMatcher.group(1).startsWith("Значението на думата все още не е въведено")) {
        continue;
      }
      delegate.registerNewDefinition(definitionMatcher.group(1));
    }
  }

  private String getPOS(String templateName) {
    for (String p : bulgarianPOS) {
      if (templateName.startsWith(p)) {
        return p;
      }
    }
    return null;
  }


  public void displayUsedTemplates() {
    if (templates != null && templates.size() > 0) {
      log.debug("in {}: template used : {}", this.delegate.currentLexEntry(), templates.toString());
    }
  }
}
