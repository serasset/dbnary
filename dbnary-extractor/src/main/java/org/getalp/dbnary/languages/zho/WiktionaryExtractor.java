package org.getalp.dbnary.languages.zho;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.jena.rdf.model.Resource;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.wiki.WikiPatterns;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.*;
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

  private final static HashSet<String> nymMarkerSet = new HashSet<>();
  protected final static HashMap<String, String> nymMarkerToNymName;

  static {
    nymMarkerSet.add("反義詞");
    nymMarkerSet.add("反義字");
    nymMarkerSet.add("同義字");
    nymMarkerSet.add("同義詞");
    nymMarkerSet.add("近義詞");
    nymMarkerSet.add("近義字");
    nymMarkerSet.add("Synonyms");
    nymMarkerSet.add("相关词汇");
    nymMarkerSet.add("相關詞彙");
    nymMarkerSet.add("相關詞");
    nymMarkerSet.add("相近詞彙");
    nymMarkerSet.add("近义词");
    nymMarkerSet.add("反义词");
    nymMarkerToNymName = new HashMap<String, String>(20);
    nymMarkerToNymName.put("反義詞", "ant");
    nymMarkerToNymName.put("反義字", "ant");
    nymMarkerToNymName.put("反义词", "ant");
    nymMarkerToNymName.put("同義字", "syn");
    nymMarkerToNymName.put("同義詞", "syn");
    nymMarkerToNymName.put("近義詞", "syn");
    nymMarkerToNymName.put("近義字", "syn");
    nymMarkerToNymName.put("Synonyms", "syn");
    nymMarkerToNymName.put("近义词", "syn");
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
    String head = m.group(1).trim();
    return head.startsWith("{{zh") || head.startsWith("{{zho") || head.startsWith("漢語")
        || head.startsWith("汉语") || head.startsWith("官話") || head.startsWith("粵語");
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
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
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
            leaveDefBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            leaveDefBlock(m);
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            leaveDefBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            leaveDefBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            leaveDefBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            leaveDefBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            leaveDefBlock(m);
            gotoNoData(m);
          } else {
            leaveDefBlock(m);
            log.trace("block named " + m.group(1) + " is ignored");
            gotoNoData(m);
          }
          break;
        case TRADBLOCK:
          if (isTranslation(m)) {
            leaveTradAltBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            leaveTradAltBlock(m);
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            leaveTradAltBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            leaveTradAltBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            leaveTradAltBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            leaveTradAltBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            leaveTradAltBlock(m);
            gotoNoData(m);
          } else {
            log.trace("block named " + m.group(1) + " is ignored");
            leaveTradAltBlock(m);
            gotoNoData(m);
          }
          break;
        case ORTHOALTBLOCK:
          if (isTranslation(m)) {
            leaveOrthoAltBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            leaveOrthoAltBlock(m);
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            leaveOrthoAltBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            leaveOrthoAltBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            leaveOrthoAltBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            leaveOrthoAltBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            leaveOrthoAltBlock(m);
            gotoNoData(m);
          } else {
            leaveOrthoAltBlock(m);
            gotoNoData(m);
          }
          break;
        case NYMBLOCK:
          if (isTranslation(m)) {
            leaveNymBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            leaveNymBlock(m);
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            leaveNymBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            leaveNymBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            leaveNymBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            leaveNymBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            leaveNymBlock(m);
            gotoNoData(m);
          } else {
            leaveNymBlock(m);
            gotoNoData(m);
          }
          break;
        case PRONBLOCK:
          if (isTranslation(m)) {
            // leavePronBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            leavePronBlock(m);
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
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
            leaveRelBlock(m);
            gotoTradBlock(m);
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            leaveRelBlock(m);
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
              gotoIgnorePos();
            } else {
              gotoDefBlock(m, pos);
            }
          } else if (isAlternate(m)) {
            leaveRelBlock(m);
            gotoOrthoAltBlock(m);
          } else if (isNymHeader(m)) {
            leaveRelBlock(m);
            gotoNymBlock(m);
          } else if (isPronunciation(m)) {
            leaveRelBlock(m);
            gotoPronBlock(m);
          } else if (isRelatedHeader(m)) {
            leaveRelBlock(m);
            gotoRelBlock(m);
          } else if (isChineseHeader(m)) {
            leaveRelBlock(m);
            gotoNoData(m);
          } else {
            leaveRelBlock(m);
            gotoNoData(m);
          }
          break;
        case IGNOREPOS:
          if (isTranslation(m)) {
          } else if (null != (pos = getValidPOS(m)) || "释义".equals(m.group(1).trim())) {
            if (null == pos) {
              gotoDefBlock(m, "");
            } else if (pos.length() == 0) {
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
        leaveDefBlock(m);
        break;
      case TRADBLOCK:
        leaveTradAltBlock(m);
        break;
      case ORTHOALTBLOCK:
        leaveOrthoAltBlock(m);
        break;
      case NYMBLOCK:
        leaveNymBlock(m);
        break;
      case PRONBLOCK:
        leavePronBlock(m);
        break;
      case RELBLOCK:
        leaveRelBlock(m);
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
  public Resource extractDefinition(String definition, int defLevel) {
    definitionExtractor.setPageName(this.getWiktionaryPageName());
    definitionExtractor.parseDefinition(definition, defLevel);
    return null;
  }

  public Resource extractExample(String example) {
    definitionExtractor.setPageName(this.getWiktionaryPageName());
    definitionExtractor.parseExample(example);
    return null;
  }

  private void extractTranslations(int startOffset, int endOffset) {
    String transCode = pageContent.substring(startOffset, endOffset);
    ChineseTranslationExtractor translationExtractor = new ChineseTranslationExtractor(this.wdh);
    translationExtractor.parseTranslationBlock(transCode);
  }

  private void removeIrrelevantToken(List<Token> tokens, int location) {
    if (tokens.get(location).getText().equals(" ") || tokens.get(location).getText().equals(": ")
        || tokens.get(location).getText().equals("：")) {
      tokens.remove(location);
    }
  }

  protected void extractNyms(String currentNym, int startOffset, int endOffset) {
    String nymBlock = pageContent.substring(startOffset, endOffset);
    WikiText text = new WikiText(nymBlock);
    // 正常情况下，显示相关词汇
    if (currentNym == null) {
      for (Token t : text.wikiTokens()) {
        if (t instanceof ListItem) {
          WikiContent listContent = t.asListItem().getContent();
          List<Token> tokens = listContent.tokens();
          removeIrrelevantToken(tokens, 0);
          if (tokens.size() == 0)
            return;
          else if (tokens.size() == 1 && tokens.get(0) instanceof Text) { // situation 1 :
                                                                          // *近義詞：[[標記]]｜[[標誌]]｜[[象徵]]
            String nymMarker = tokens.get(0).getText().split("：")[0];
            nymMarker = nymMarker.substring(0, nymMarker.length());
            if (nymMarkerSet.contains(nymMarker)) {
              currentNym = nymMarkerToNymName.get(nymMarker);
              if (tokens.get(0).getText().split("：").length > 1) {
                String nymTex = tokens.get(0).getText().split("：")[1];
                wdh.registerNymRelation(nymTex, currentNym);
              }
            } else {
              log.debug("Extract nym: can't find the nymMarker: " + nymMarker);
            }
          } else {
            Token firstToken = tokens.get(0);
            String nymMarker = firstToken.getText().split("：")[0];
            if (nymMarkerSet.contains(nymMarker)) {
              currentNym = nymMarkerToNymName.get(nymMarker);
              tokens.remove(0);
              for (Token tokenInList : tokens) {
                if (tokenInList instanceof InternalLink) {
                  String nymText = tokenInList.asInternalLink().getTargetText();
                  wdh.registerNymRelation(nymText, currentNym);
                }
              }
            } else {
              log.trace("Extract nym: can't find the nymMarker: " + nymMarker);
            }
          }
        }
      }
    }
    // 其他情况,显示近义词和反义词
    else {
      for (Token t : text.wikiTokens()) {
        if (t instanceof ListItem) {
          WikiContent listContent = t.asListItem().getContent();
          List<Token> tokens = listContent.tokens();
          removeIrrelevantToken(tokens, 0);
          for (Token token : tokens) {
            if (token instanceof Template) {
              if (((Template) token).getName().equals("zh-l")) {
                wdh.registerNymRelation(((Template) token).getArg("1").toString(), currentNym);
              }
            }
          }
        } else if (t instanceof Template) {
          if (((Template) t).getParsedArg("1") != null)
            wdh.registerNymRelation(((Template) t).getArg("1").toString(), currentNym);
        }
      }
    }
  }
}
