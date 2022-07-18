package org.getalp.dbnary.languages.zho;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WiktionaryExtractor extends AbstractWiktionaryExtractor {
  private static final Logger log = LoggerFactory.getLogger(WiktionaryExtractor.class);
  protected final static String wikiSectionPatternString = "^={3,}\\s*([^=]*)\\s*={3,}\\s*$";
  protected final static String level2HeaderPatternString = "^==([^=].*[^=])==\\s*$";
  protected final static Pattern wikiSectionPattern;
  // private final static Pattern pronounciationPattern;
  private HashSet<String> unknownHeaders;
  private final int NODATA = 0;
  private final int TRADBLOCK = 1; // 翻譯
  private final int DEFBLOCK = 2; // 名詞
  private final int ORTHOALTBLOCK = 3;// isAlternant另一种表示
  private final int NYMBLOCK = 4;// 近義詞
  private final int PRONBLOCK = 5;// 讀音
  private final int RELBLOCK = 7;// 参考词汇
  private final int IGNOREPOS = 8;

  ChineseDefinitionExtractorWikiModel definitionExtractor;
  ChinesePronunciationExtractorWikiModel pronunciationExtractor;

  public WiktionaryExtractor(IWiktionaryDataHandler wdh) {
    super(wdh);
    // throw new RuntimeException("Chinese extractor is currently not functional.");
  }

  protected final static HashMap<String, String> posMarkers;
  protected final static HashMap<String, String> nymMarkerToNymName;

  static {
    posMarkers = new HashMap<String, String>(20);
    posMarkers.put("n.", "noun");
    posMarkers.put("名词", "noun");
    posMarkers.put("v.", "verb");
    posMarkers.put("动词", "verb");
    posMarkers.put("adj.", "adjective");
    posMarkers.put("形容词", "adjective");
    posMarkers.put("num.", "numeral");
    posMarkers.put("数词", "numeral");
    posMarkers.put("clas.", "chinese classifier");
    posMarkers.put("量词", "chinese classifier");
    posMarkers.put("pron.", "pronoun");
    posMarkers.put("代词", "pronoun");
    posMarkers.put("idiom.", "idiom");
    posMarkers.put("成语", "idiom");
    posMarkers.put("adv.", "adverb");
    posMarkers.put("副词", "adverb");
    posMarkers.put("prep.", "preposition");
    posMarkers.put("介词", "preposition");
    posMarkers.put("conj.", "conjunction");
    posMarkers.put("连词", "conjunction");
    posMarkers.put("conj.", "auxiliary");
    posMarkers.put("助词", "auxiliary");
    posMarkers.put("echo.", "echo");
    posMarkers.put("拟声词", "echo");
    posMarkers.put("int.", "interjection");
    posMarkers.put("叹词", "interjection");

    nymMarkerToNymName = new HashMap<String, String>(20);
    nymMarkerToNymName.put("Synonyms", "syn");
    nymMarkerToNymName.put("同义词", "syn");
    nymMarkerToNymName.put("Antonyms", "ant");
    nymMarkerToNymName.put("反义词", "ant");
    nymMarkerToNymName.put("Hyponyms", "hypo");
    nymMarkerToNymName.put("下位词", "hypo");
    nymMarkerToNymName.put("Hypernyms", "hyper");
    nymMarkerToNymName.put("上位词", "hyper");
    nymMarkerToNymName.put("Meronyms", "mero");
    nymMarkerToNymName.put("整体-部分", "mero");
  }
  protected final static Pattern level2HeaderPattern;
  static {
    level2HeaderPattern = Pattern.compile(level2HeaderPatternString, Pattern.MULTILINE);
    wikiSectionPattern = Pattern.compile(wikiSectionPatternString, Pattern.MULTILINE);
  }
  int state = NODATA;
  int translationBlockStart = -1;
  int definitionBlockStart = -1;
  int orthBlockStart = -1;
  private int nymBlockStart = -1;
  private int pronBlockStart = -1;
  private int relBlockStart = -1;
  private String currentNym = null;
  private HashSet<String> unkownHeaders;

  @Override
  public void setWiktionaryIndex(WiktionaryPageSource wi) {
    super.setWiktionaryIndex(wi);
    definitionExtractor = new ChineseDefinitionExtractorWikiModel(wdh, wi, new Locale("en"),
        "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
    pronunciationExtractor = new ChinesePronunciationExtractorWikiModel(wdh, wi, new Locale("en"),
        "--DO NOT USE IMAGE BASE URL FOR DEBUG--", "");
  }

  public void extractData() {
    Matcher filter = level2HeaderPattern.matcher(pageContent);
    unkownHeaders = new HashSet<String>();
    int chineseStart = -1;
    wdh.initializePageExtraction(getWiktionaryPageName());
    while (filter.find()) {
      if (-1 != chineseStart) {
        extractData(chineseStart, filter.start());
        chineseStart = -1;
      }
      if (isChineseHeader(filter)) {
        chineseStart = filter.end();
      }
    }
    if (-1 != chineseStart) {
      extractData(chineseStart, pageContent.length());
    }
    wdh.finalizePageExtraction();
  }

  private String getValidPOS(Matcher m) {
    String head = m.group(1).trim();
    return WiktionaryDataHandler.getValidPOS(head, getWiktionaryPageName());
  }

  void gotoNoData(Matcher m) {
    state = NODATA;
  }

  private boolean isChineseHeader(Matcher m) {
    if (m.group(1).trim().startsWith("{{zh")) {
      return true;
    }
    if (m.group(1).trim().startsWith("{{zho")) {
      return true;
    }
    return m.group(1).trim().startsWith("漢語");
  }

  void gotoDefBlock(Matcher m, String pos) {
    state = DEFBLOCK;
    definitionBlockStart = m.end();
    wdh.initializeLexicalEntry(pos);
  }

  void leaveDefBlock(Matcher m) {
    int end = computeRegionEnd(definitionBlockStart, m);
    extractDefinitions(definitionBlockStart, end);
    definitionBlockStart = -1;
  }

  void gotoTradAltBlock(Matcher m) {
    translationBlockStart = m.end();
    state = TRADBLOCK;
  }

  void leaveTradAltBlock(Matcher m) {
    extractTranslations(translationBlockStart, computeRegionEnd(translationBlockStart, m));
    translationBlockStart = -1;
  }

  // Alternate Spelling
  private boolean isAlternate(Matcher m) {
    String head = m.group(1).trim();
    return "另一种表示".equals(head);
  }

  void gotoOrthoAltBlock(Matcher m) {
    state = ORTHOALTBLOCK;
    orthBlockStart = m.end();
  }

  void leaveOrthoAltBlock(Matcher m) {
    extractOrthoAlt(orthBlockStart, computeRegionEnd(orthBlockStart, m));
    orthBlockStart = -1;
  }


  // Translation section
  private boolean isTranslation(Matcher m) {
    String head = m.group(1).trim();
    Matcher trans = WikiPatterns.macroPattern.matcher(head);
    if (trans.find()) {
      return (trans.group(1).equals("trans"));
    } else if ("翻譯".equals(head)) {
      return true;
    } else {
      return "翻译".equals(head);
    }
  }

  void gotoTradBlock(Matcher m) {
    translationBlockStart = m.end();
    state = TRADBLOCK;
  }


  // Nyms
  private boolean isNymHeader(Matcher m) {
    return (m.group(1).trim().equals("近義詞")) || (m.group(1).trim().equals("同義詞"))
        || (m.group(1).equals("相關詞"));
  }

  private void gotoNymBlock(Matcher m) {
    state = NYMBLOCK;
    nymBlockStart = m.end();
    Matcher nym = WikiPatterns.macroPattern.matcher(m.group(1).trim());
  }

  private void leaveNymBlock(Matcher m) {
    extractNyms(currentNym, nymBlockStart, computeRegionEnd(nymBlockStart, m));
    currentNym = null;
    nymBlockStart = -1;
  }

  // Related Words
  private boolean isRelatedHeader(Matcher m) {
    return (m.group(1).trim().equals("参考词汇") || m.group(1).trim().equals("參見"));
  }

  private void gotoRelBlock(Matcher m) {
    state = RELBLOCK;
    relBlockStart = m.end();
  }

  private void leaveRelBlock(Matcher m) {
    extractRelatedWords(relBlockStart, computeRegionEnd(relBlockStart, m));
    relBlockStart = -1;
  }

  // Pronounciation section
  private boolean isPronunciation(Matcher m) {
    String head = m.group(1).trim();
    Matcher pron = WikiPatterns.macroPattern.matcher(m.group(1).trim());
    Pattern pronHeadPattern = Pattern.compile("發音\\d?|讀音\\d?|发音\\d?|读音\\d?");
    Matcher pronHeadMatcher = pronHeadPattern.matcher(head);
    if (pron.find()) {
      return (pron.group(1).equals("pron"));
    } else {
      return pronHeadMatcher.matches();
    }
  }

  private void gotoPronBlock(Matcher m) {
    state = PRONBLOCK;
    pronBlockStart = m.end();
  }

  private void leavePronBlock(Matcher m) {
    extractPronTemplate(pronBlockStart, computeRegionEnd(pronBlockStart, m));
    pronBlockStart = -1;
  }

  private void gotoIgnorePos() {
    state = IGNOREPOS;
  }

  private void extractData(int startOffset, int endOffset) {
    wdh.initializeLanguageSection("zh");
    Matcher m = wikiSectionPattern.matcher(pageContent);
    m.region(startOffset, endOffset);
    gotoNoData(m);
    String pos = null;
    while (m.find()) {
      switch (state) {
        case NODATA:
          if (isTranslation(m)) {
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            gotoRelBlock(m);
          } else {
            log.trace("block named " + m.group(1) + " is ignored");
            gotoNoData(m);
          }
          break;

        case DEFBLOCK:
          // Iterate until we find a new section
          if (isTranslation(m)) {
            // leaveDefBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            // leaveDefBlock(m);
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            // leaveDefBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            // leaveDefBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            // leaveDefBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            // leaveDefBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            // leaveDefBlock(m);
            gotoNoData(m);
          } else {
            // leaveDefBlock(m);
            log.trace("block named " + m.group(1) + " is ignored");
            gotoNoData(m);
          }
          break;

        case TRADBLOCK:
          if (isTranslation(m)) {
            // leaveTradAltBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            // leaveTradAltBlock(m);
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            // leaveTradAltBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            // leaveTradAltBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            // leaveTradAltBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            // leaveTradAltBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            // leaveTradAltBlock(m);
            gotoNoData(m);
          } else {
            log.trace("block named " + m.group(1) + " is ignored");
            // leaveTradAltBlock(m);
            gotoNoData(m);
          }
          break;
        case ORTHOALTBLOCK:
          if (isTranslation(m)) {
            // leaveOrthoAltBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            // leaveOrthoAltBlock(m);
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            // leaveOrthoAltBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            // leaveOrthoAltBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            // leaveOrthoAltBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            // leaveOrthoAltBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            // leaveOrthoAltBlock(m);
            gotoNoData(m);
          } else {
            // leaveOrthoAltBlock(m);
            gotoNoData(m);
          }
          break;
        case NYMBLOCK:
          if (isTranslation(m)) {
            // leaveNymBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            // leaveNymBlock(m);
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            // leaveNymBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            // leaveNymBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            // leaveNymBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            // leaveNymBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            // leaveNymBlock(m);
            gotoNoData(m);
          } else {
            // leaveNymBlock(m);
            gotoNoData(m);
          }
          break;
        case PRONBLOCK:
          if (isTranslation(m)) {
            leavePronBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            leavePronBlock(m);
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            leavePronBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            leavePronBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            leavePronBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            leavePronBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            leavePronBlock(m);
            gotoNoData(m);
          } else {
            leavePronBlock(m);
            gotoNoData(m);
          }
          break;

        case RELBLOCK:
          if (isTranslation(m)) {
            // leaveRelBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m))) {
            // leaveRelBlock(m);
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            // leaveRelBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            // leaveRelBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            // leaveRelBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            // leaveRelBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            // leaveRelBlock(m);
            gotoNoData(m);
          } else {
            // leaveRelBlock(m);
            gotoNoData(m);
          }
          break;

        case IGNOREPOS:
          if (isTranslation(m)) {
          } else if (null != (pos = getValidPOS(m))) {
            if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
          } else if (isNymHeader(m)) {
          } else if (isPronunciation(m)) {
          } else if (isRelatedHeader(m)) {
          } else if (isChineseHeader(m)) {
          } else {
          }
          break;
        default:
          assert false : "Unexpected state while extracting translations from dictionary.";
      }
    }
    // Finalize the entry parsing
    switch (state) {
      case NODATA:
        break;
      case DEFBLOCK:
        // leaveDefBlock(m);
        break;
      case TRADBLOCK:
        // leaveTradAltBlock(m);
        break;
      case ORTHOALTBLOCK:
        // leaveOrthoAltBlock(m);
        break;
      case NYMBLOCK:
        // leaveNymBlock(m);
        break;
      case PRONBLOCK:
        leavePronBlock(m);
        break;
      case RELBLOCK:
        // leaveRelBlock(m);
        break;
      case IGNOREPOS:
        break;
      default:
        assert false
            : "Unexpected state while ending extraction of entry: " + getWiktionaryPageName();
    }
    wdh.finalizeLanguageSection();
  }

  private void extractRelatedWords(int startOffset, int endOffset) {
    String relCode = pageContent.substring(startOffset, endOffset);
    ChineseRelatedWordsExtractor dbnmodel = new ChineseRelatedWordsExtractor(this.wdh, this.wi);
    dbnmodel.parseRelatedWords(relCode);
  }


  private void extractPronTemplate(int startOffset, int endOffset) {
    String pronCode = pageContent.substring(startOffset, endOffset);
    pronunciationExtractor.setPageName(getWiktionaryPageName());
    pronunciationExtractor.parsePronunciation(pronCode);
  }

  @Override
  public void extractDefinition(String definition, int defLevel) {
    definitionExtractor.setPageName(this.getWiktionaryPageName());
    definitionExtractor.parseDefinition(definition, defLevel);
  }

  public void extractExample(String example) {
    definitionExtractor.setPageName(this.getWiktionaryPageName());
    definitionExtractor.parseExample(example);
  }

  private void extractTranslations(int startOffset, int endOffset) {
    String transCode = pageContent.substring(startOffset, endOffset);
    ChineseTranslationExtractor translationExtractor =
        new ChineseTranslationExtractor(this.wdh, glossFilter);
    translationExtractor.parseTranslationBlock(transCode);
  }

}
