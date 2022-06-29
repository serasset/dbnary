package org.getalp.dbnary.languages.zho;

import info.bliki.wiki.filter.WikipediaParser;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.function.library.leviathan.log;
import org.getalp.LangTools;
import org.getalp.dbnary.languages.AbstractGlossFilter;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.languages.jpn.JapaneseLangtoCode;
import org.getalp.dbnary.languages.jpn.JapaneseTranslationsExtractor;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiTool;
import org.getalp.iso639.ISO639_3;
import org.luaj.vm2.ast.Str;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChineseTranslationExtractorWikiModel extends ChineseDbnaryWikiModel {

  private final AbstractGlossFilter glossFilter;
  private IWiktionaryDataHandler delegate;
  private int rank = 1;

  private Logger log = LoggerFactory.getLogger(JapaneseTranslationsExtractor.class);

  public ChineseTranslationExtractorWikiModel(IWiktionaryDataHandler we, Locale locale,
                                              String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    this(we, (WiktionaryPageSource) null, locale, imageBaseURL, linkBaseURL, glossFilter);
  }

  public ChineseTranslationExtractorWikiModel(IWiktionaryDataHandler we, WiktionaryPageSource wi,
                                              Locale locale, String imageBaseURL, String linkBaseURL, AbstractGlossFilter glossFilter) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
    this.glossFilter = glossFilter;
  }

  public boolean isNormalType(String textContent){
    String normalTypeString = "(?:\\*.+\\uff1a?\\{\\{.+\\}\\})|(?:\\*.+:\\s\\{\\{.+\\}\\})";
    String abnormalTypeString = "(?:\\*.+\\uff1a?\\[\\[.+\\]\\])|(?:\\*.+:\\s\\[\\[.+\\]\\])";
    Pattern normalTypePattern = Pattern.compile(normalTypeString);
    Pattern abnormalTypePattern = Pattern.compile(abnormalTypeString);
    Matcher normalTypeMatcher = normalTypePattern.matcher(textContent);
    Matcher abnormalTypeMatcher = abnormalTypePattern.matcher(textContent);

    return (normalTypeMatcher.results().count()>=abnormalTypeMatcher.results().count());
  }

  public void parseTranslationBlock(String block) {
    initialize();
    this.rank = 1;
    if (block == null) {
      return;
    }
    //transblock type : *langage: {{XX|XX|XX}}
    if (isNormalType(block)) {
      WikipediaParser.parse(block, this, true, null);
    }
    //transblock type : *langage: [[]]
    else {
      extractTranslations(block);
    }
    initialize();
  }

  private Resource currentGloss = null;

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
                                     Appendable writer) throws IOException {
    if ("trad".equals(templateName)) {
      // Trad macro contains a set of translations with no usage note.
      String lang = normalizeLang(parameterMap.get("1"));
      for (Entry<String, String> kv : parameterMap.entrySet()) {
        if ("1".equals(kv.getKey())) {
          continue;
        }
        delegate.registerTranslation(lang, currentGloss, null, kv.getValue());
      }
    } else if ("xlatio".equals(templateName) || "trad-".equals(templateName)) {
      // xlatio and trad- macro contains a translation and a transcription.
      String lang = normalizeLang(parameterMap.get("1"));
      delegate.registerTranslation(lang, currentGloss, parameterMap.get("3"),
              parameterMap.get("2"));
    } else if ("t".equals(templateName) || "t+".equals(templateName)) {
      // t macro contains a translation, a transcription and an usage note.
      String lang = normalizeLang(parameterMap.get("1"));
      String usage = parameterMap.get("3");
      if (null == usage) {
        usage = "";
      }
      String transcription = parameterMap.get("tr");
      if (null == transcription) {
        transcription = parameterMap.get("4");
      }
      if (null == transcription) {
        transcription = "";
      }

      if (!transcription.equals("")) {
        usage = "(" + transcription + "), " + usage;
      }
      delegate.registerTranslation(lang, currentGloss, usage, parameterMap.get("2"));
    } else if ("tradini".equals(templateName)) {
      String g = parameterMap.get("1");
      if (null != g) {
        g = g.trim();
        currentGloss = delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
      } else {
        currentGloss = null;
      }
    } else if ("tradini-checar".equals(templateName)) {
      currentGloss = null;
    } else if ("tradmeio".equals(templateName)) {
      // nop
    } else if ("tradfim".equals(templateName)) {
      currentGloss = null;
    } else if (ISO639_3.sharedInstance.getIdCode(templateName) != null) {
      // This is a template for the name of a language, just ignore it...
    } else {

    }
  }

  private String normalizeLang(String lang) {
    String normLangCode;
    if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
      lang = normLangCode;
    }
    return lang;
  }

  //Extraction Method of transblock type : *langage: [[]]
  protected final static String carPatternString;

  protected final static String starChinesePatternString;
 
  protected final static String macroOrLinkOrcarPatternString;
  protected final int INIT = 1;
  protected final int LANGUE = 2;
  protected final int TRAD = 3;


  static {
    // les caractères visible
    carPatternString = new StringBuilder().append("(.)").toString();
    starChinesePatternString = new StringBuilder().append("\\*").append("([\\u4e00-\\u9fa5]+)").toString();
    

    // TODO: We should suppress multiline xml comments even if macros or line are to be on a single
    // line.
//    macroOrLinkOrcarPatternString =
//            new StringBuilder().append("(?:").append(WikiPatterns.macroPatternString).append(")|(?:")
//                    .append(WikiPatterns.linkPatternString).append(")|(?:").append("(:*\\*)")
//                    .append(")|(?:").append("^;([^:\\n\\r]*)") // Term definition
//                    .append(")|(?:").append(carPatternString).append(")").toString();
        macroOrLinkOrcarPatternString ="(?:\\{\\{([^\\}\\|]*)(?:\\|([^\\}]*))?\\}\\})|(?<=\\*)([\\u4e00-\\u9fa5]+)|(?:\\[\\[([^\\]\\|]*)(?:\\|([^\\]]*))?\\]\\])|(?:(:*\\*))|(?:^;([^:\\n\\r]*))|(?:(.))";
  }

  protected final static Pattern macroOrLinkOrcarPattern;
  protected final static Pattern carPattern;

  static {
    carPattern = Pattern.compile(carPatternString);
    macroOrLinkOrcarPattern =
            Pattern.compile(macroOrLinkOrcarPatternString, Pattern.DOTALL + Pattern.MULTILINE);
  }
  static HashSet<String> commonUsageMacros = new HashSet<>();
  static HashSet<String> fontMacros = new HashSet<>();

  static {
    commonUsageMacros.add("m");
    commonUsageMacros.add("f");
    commonUsageMacros.add("p");
    commonUsageMacros.add("s");
    commonUsageMacros.add("n");
    commonUsageMacros.add("c");

    fontMacros.add("Arab");
    fontMacros.add("ARchar");
    fontMacros.add("Unicode");
    fontMacros.add("FAchar");
    fontMacros.add("KOfont");
    fontMacros.add("THchar");
    fontMacros.add("URchar");
    fontMacros.add("ur-Arab");
    fontMacros.add("ku-Arab");
    fontMacros.add("Thai");
    fontMacros.add("KUchar");
    fontMacros.add("fa-Arab");
    fontMacros.add("IPAchar");
    fontMacros.add("HEchar");
    fontMacros.add("PSchar");
    fontMacros.add("RUchar");
    fontMacros.add("ug-Arab");
    fontMacros.add("ZHsim");
    fontMacros.add("sd-Arab");
    fontMacros.add("BNchar");

  }


  private void extractTranslations(String translations) {
    Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(translations);
    int ETAT = INIT;
    int rank = 1;

    Resource currentGloss = null;
    String lang = null, word = "";
    String usage = "";
    String langname = "";

    while (macroOrLinkOrcarMatcher.find()) {
      String macro = macroOrLinkOrcarMatcher.group(3) !=null? macroOrLinkOrcarMatcher.group(3):macroOrLinkOrcarMatcher.group(1); //显示语言种类 Ar 或者 中文
      String link = macroOrLinkOrcarMatcher.group(4);
      String star = macroOrLinkOrcarMatcher.group(6);
      String term = macroOrLinkOrcarMatcher.group(7);
      String car = macroOrLinkOrcarMatcher.group(8);

      switch (ETAT) {
        case INIT:
          if (macro != null) {
            if (macro.equalsIgnoreCase("翻译-顶") || macro.equalsIgnoreCase("翻譯-頂")) {
              if (macroOrLinkOrcarMatcher.group(2) != null) {
                String g = macroOrLinkOrcarMatcher.group(2);
                currentGloss =
                        delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
              } else {
                currentGloss = null;
              }

            } else if (macro.equalsIgnoreCase("翻译-底") || macro.equalsIgnoreCase("翻譯-底")) {
              currentGloss = null;
            } else if (macro.equalsIgnoreCase("翻译-中") || macro.equalsIgnoreCase("翻譯-中")) {
              // ignore
            } else {
              log.debug("Got {} macro while in INIT state. for page: {}", macro,
                      this.delegate.currentPagename());
            }
          } else if (link != null) {
            log.debug("Unexpected link {} while in INIT state. for page: {}", link,
                    this.delegate.currentPagename());
          } else if (star != null) {
            ETAT = LANGUE;
          } else if (term != null) {
            currentGloss =
                    delegate.createGlossResource(glossFilter.extractGlossStructure(term), rank++);
          } else if (car != null) {
            switch (car) {
              case ":":
                log.debug("Skipping ':' while in INIT state.");
                break;
              case "\n":
              case "\r":

                break;
              case ",":
                log.debug("Skipping ',' while in INIT state.");
                break;
              default:
              //  log.debug("Skipping {} while in INIT state.", car);
                break;
            }
          }

          break;

        case LANGUE:

          if (macro != null) {
            if (macro.equalsIgnoreCase("翻译-顶") || macro.equalsIgnoreCase("翻譯-頂")) {
              if (macroOrLinkOrcarMatcher.group(2) != null) {
                String g = macroOrLinkOrcarMatcher.group(2);
                currentGloss =
                        delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
              } else {
                currentGloss = null;
              }
              langname = "";
              word = "";
              usage = "";
              ETAT = INIT;
            } else if (macro.equalsIgnoreCase("翻译-底") || macro.equalsIgnoreCase("翻譯-底")) {
              currentGloss = null;
              langname = "";
              word = "";
              usage = "";
              ETAT = INIT;
            } else if (macro.equalsIgnoreCase("翻译-中") || macro.equalsIgnoreCase("翻譯-中")) {
              langname = "";
              word = "";
              usage = "";
              ETAT = INIT;
            } else {
              langname = LangTools.normalize(macro);
            }
          } else if (link != null) {
            // TODO: extract [[{{eng}}]] kind of links
            // TODO: some links come from *# bullet list used in a language.
            langname = extractLanguage(link);
          } else if (star != null) {
            // System.err.println("Skipping '*' while in LANGUE state.");
          } else if (term != null) {
            currentGloss =
                    delegate.createGlossResource(glossFilter.extractGlossStructure(term), rank++);
            langname = "";
            word = "";
            usage = "";
            ETAT = INIT;
          } else if (car != null) {
            if (car.equals(":")||car.equals("：")) {
              lang = langname.trim();
              lang = AbstractWiktionaryExtractor.stripParentheses(lang);
              lang = JapaneseLangtoCode.threeLettersCode(lang);
              if (null == lang) {
               // log.debug("Unknown language {} : {}", langname, this.delegate.currentPagename());
              }
              langname = "";
              ETAT = TRAD;
            } else if (car.equals("\n") || car.equals("\r")) {
              // System.err.println("Skipping newline while in LANGUE state.");
            } else if (car.equals(",")) {
              // System.err.println("Skipping ',' while in LANGUE state.");
            } else {
              langname = langname + car;
            }
          }

          break;
        case TRAD:
          if (macro != null) {
            if (macro.equalsIgnoreCase("翻译-顶") || macro.equalsIgnoreCase("翻譯-頂")) {
              if (macroOrLinkOrcarMatcher.group(2) != null) {
                String g = macroOrLinkOrcarMatcher.group(2);
                currentGloss =
                        delegate.createGlossResource(glossFilter.extractGlossStructure(g), rank++);
              } else {
                currentGloss = null;
              }
              // if (word != null && word.length() != 0) {
              // lang=stripParentheses(lang);
              // wdh.registerTranslation(lang, currentGloss, usage, word);
              // }
              langname = "";
              word = "";
              usage = "";
              lang = null;
              ETAT = INIT;
            } else if (macro.equalsIgnoreCase("翻译-底") || macro.equalsIgnoreCase("翻譯-底")) {
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  this.delegate.registerTranslation(lang, currentGloss, usage, word);
                }
              }
              currentGloss = null;
              langname = "";
              word = "";
              usage = "";
              lang = null;
              ETAT = INIT;
            } else if (macro.equalsIgnoreCase("翻译-中") || macro.equalsIgnoreCase("翻譯-中")) {
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  this.delegate.registerTranslation(lang, currentGloss, usage, word);
                }
              }
              langname = "";
              word = "";
              usage = "";
              lang = null;
              ETAT = INIT;
            } else if (macro.equals("朝鮮語訳")) {
              // Get the korean translation and romanization
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              word = argmap.get("word");
              argmap.remove("word");
              usage = argmap.toString();
              if (lang != null) {
                this.delegate.registerTranslation(lang, currentGloss, usage, word);
              }
              word = "";
              usage = "";
            } else if (macro.equals("ZHfont")) {
              // Switch for the Chinese fonts.
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              // Check if previous word has not been registered. TODO: Check when this arises.
              if (word != null && word.length() != 0) {
                // System.err.println("Word is not null when handling ZHfont macro in " +
                // this.delegate.currentLexEntry());
              }
              word = argmap.get("1");
              // usage note (equivalent in japanese chars ?)
              argmap.remove("1");
              if (!argmap.isEmpty()) {
                usage = argmap.toString();
              }
            } else if (macro.equals("zh-ts")) {
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              // Check if previous word has not been registered. TODO: Check when this arises.
              if (word != null && word.length() != 0) {
                // System.err.println("Word is not null when handling zh-ts macro in " +
                // this.delegate.currentLexEntry());
              }
              word = argmap.get("1");
              // TODO: Arg2 is the simplified chinese version
              argmap.remove("1");
              if (!argmap.isEmpty()) {
                usage = argmap.toString();
              }
            } else if (macro.equals("trans_link")) {
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              // if (null != word && word.length() != 0) System.err.println("Word is not null when
              // handling trans_link macro in " + this.delegate.currentLexEntry());
              word = argmap.get("2");
            } else if (macro.equals("t+") || macro.equals("t-") || macro.equals("t")
                    || macro.equals("tø") || macro.equals("trad")) {
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              if (null != word && word.length() != 0) {
              //  log.debug("Word is not null ({}) when handling t+- macro in {}", word,
                        this.delegate.currentPagename();
              }
              String l = argmap.get("1");
              if (null != l && (null != lang) && !lang.equals(LangTools.getCode(l))) {
                // System.err.println("Language in t+ macro does not map language in list in ");// +
                // this.delegate.currentLexEntry());
              }
              word = argmap.get("2");
              argmap.remove("1");
              argmap.remove("2");
              if (!argmap.isEmpty()) {
                usage = argmap.toString();
              }
            } else if (macro.equals("lang") || macro.equals("Lang")) {
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              if (null != word && word.length() != 0) {
               log.debug("Word is not null ({}) when handling lang macro in {}", word,
                        this.delegate.currentPagename());
              }
              String l = argmap.get("1");
              if (null != l && (null != lang) && !lang.equals(LangTools.getCode(l))) {
                // System.err.println("Language in lang macro does not map language in list in ");//
                // + this.delegate.currentLexEntry());
              }
              word = AbstractWiktionaryExtractor.cleanUpMarkup(argmap.get("2"), true);
              argmap.remove("1");
              argmap.remove("2");
              if (!argmap.isEmpty()) {
                usage = argmap.toString();
              }
            } else if (fontMacros.contains(macro)) {
              Map<String, String> argmap = WikiTool.parseArgs(macroOrLinkOrcarMatcher.group(2));
              // if (null != word && word.length() != 0) System.err.println("Word is not null when
              // handling a font macro in " + this.delegate.currentLexEntry());
              word = argmap.get("1");
            } else if (commonUsageMacros.contains(macro)) {
              usage = usage + "{{" + macro + "}}";
            } else if (macro.equals("sr-Latn") || macro.equals("sr-Cyrl")) {
              if (null != lang) {
                // System.err.println("Lang is " + lang + " while getting sr-Latn macro." +
                // this.delegate.currentLexEntry());
              }
              lang = "srp";
              usage = usage + " sc=" + macro;
            } else {
              // System.err.println("Got " + macro + " macro in usage. for page: "); // +
              // this.delegate.currentLexEntry());
              usage = usage + "{{" + macro + "}}";
            }
          } else if (link != null) {
            if (!isAnExternalLink(link)) {
              word = word + " " + ((macroOrLinkOrcarMatcher.group(4) == null) ? link
                      : macroOrLinkOrcarMatcher.group(4));
            }
          } else if (star != null) {
            // System.err.println("Skipping '*' while in LANGUE state.");
          } else if (term != null) {
            currentGloss =
                    delegate.createGlossResource(glossFilter.extractGlossStructure(term), rank++);
            langname = "";
            word = "";
            usage = "";
            lang = null;
            ETAT = INIT;
          } else if (car != null) {
            if (car.equals("\n") || car.equals("\r")) {
              usage = usage.trim();
              // System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " +
              // currentGloss);
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  this.delegate.registerTranslation(lang, currentGloss, usage, word);
                }
              }
              //存储后将所存的内容初始化
              lang = null;
              usage = "";
              word = "";
              ETAT = INIT;
            } else if (car.equals(",") || car.equals("、")||car.equals(";")) {
              usage = usage.trim();
              // System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " +
              // currentGloss);
              if (word != null && word.length() != 0) {
                if (lang != null) {
                  this.delegate.registerTranslation(lang, currentGloss, usage, word);
                }
              }
              usage = "";
              word = "";
            } else {
              usage = usage + car;
            }
          }
          break;
        default:
         // log.error("Unexpected state number {}", ETAT);
          break;
      }
    }
  }

  private String extractLanguage(String link) {
    Matcher m = WikiPatterns.macroPattern.matcher(link);
    String c;
    if (m.matches()) {
      c = LangTools.normalize(m.group(1));
    } else {
      c = JapaneseLangtoCode.threeLettersCode(link);
    }
    return (null == c) ? "" : c;
  }

  private boolean isAnExternalLink(String link) {
    // TODO Auto-generated method stub
    return link.startsWith(":");
  }

}