package org.getalp.dbnary.languages.deu;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.languages.deu.GermanInflectionData.Degree;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.PropertyObjectPair;
import org.getalp.dbnary.morphology.InflectionData;
import org.getalp.dbnary.tools.StringDistance;
import org.getalp.dbnary.wiki.WikiText;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 16/02/16.
 */
public class GermanMorphologyExtractor {

  private final WiktionaryPageSource wi;
  private final IWiktionaryDataHandler wdh;
  protected final GermanDeklinationExtractorWikiModel deklinationExtractor;
  protected final GermanKonjugationExtractorWikiModel konjugationExtractor;
  protected final GermanSubstantiveDeklinationExtractorWikiModel substantivDeklinationExtractor;

  private static HashSet<String> ignoredTemplates;

  // private boolean reflexiv=false;
  static {
    ignoredTemplates = new HashSet<>();
    ignoredTemplates.add("Absatz");
    ignoredTemplates.add("Hebr");
    ignoredTemplates.add("Internetquelle");
    ignoredTemplates.add("Lautschrift");
    ignoredTemplates.add("Lit-Duden: Rechtschreibung");
    ignoredTemplates.add("Lit-Stielau: Nataler Deutsch");
    ignoredTemplates.add("Ref-Grimm");
    ignoredTemplates.add("Ref-Kruenitz");
    ignoredTemplates.add("Ref-Länderverzeichnis");
    ignoredTemplates.add("Ref-OWID");
    ignoredTemplates.add("Schachbrett");
    ignoredTemplates.add("Wort des Jahres");
  }

  private Logger log = LoggerFactory.getLogger(GermanMorphologyExtractor.class);

  public GermanMorphologyExtractor(IWiktionaryDataHandler wdh, WiktionaryPageSource wi) {
    this.wdh = wdh;
    this.wi = wi;
    deklinationExtractor =
        new GermanDeklinationExtractorWikiModel(wdh, wi, new Locale("de"), "/${Bild}", "/${Titel}");
    konjugationExtractor =
        new GermanKonjugationExtractorWikiModel(wdh, wi, new Locale("de"), "/${Bild}", "/${Titel}");
    substantivDeklinationExtractor = new GermanSubstantiveDeklinationExtractorWikiModel(wdh, wi,
        new Locale("de"), "/${Bild}", "/${Titel}");
  }

  public void extractMorphologicalData(String wikiSourceText, String pageName) {
    WikiText wikiText = new WikiText(pageName, wikiSourceText);

    for (WikiText.Token t : wikiText.templatesOnUpperLevel()) {
      WikiText.Template wt = (WikiText.Template) t;
      String templateName = wt.getName().trim();
      if (templateName.startsWith("Vorlage:")) {
        templateName = templateName.substring(8);
      }
      if (ignoredTemplates.contains(templateName)) {
        continue;
      }

      if ("Deutsch Substantiv Übersicht".equals(templateName)
          || "Deutsch Toponym Übersicht".equals(templateName)
          || "Deutsch Nachname Übersicht".equals(templateName)) {
        extractMorphologicalSignature(wt);
        // TODO: extract the data from generated table, so that it is less fragile.
        extractFormsWithModel(wt.toString(), pageName, substantivDeklinationExtractor);
      } else if ("Deutsch Adjektiv Übersicht".equals(templateName)) {
        // DONE fetch and expand deklination page and parse all tables.
        // TODO: check if such template may be used on substantivs
        if (wdh.currentWiktionaryPos().equals("Substantiv")) {
          log.debug("Adjectiv ubersicht in noun : {} ", wdh.currentPagename());
        }
        // DONE: Extract comparative/Superlative from parametermap before fetching the full flexion
        // page.
        // extractAdjectiveDegree returns true(!) iff there are NO further forms ("Keine weiteren
        // Formen") therefore the Flexion: page is only consulted iff false is returned
        if (!extractAdjectiveDegree(wt.getParsedArgs())) {
          // Page name has changed to "Flexion:" String deklinationPageName = pageName + "
          // (Deklination)";
          String deklinationPageName = "Flexion:" + pageName;
          log.debug("German Adjectiv: try deklinationPageName: {}", deklinationPageName);
          extractFormsPageWithModel(deklinationPageName, pageName, deklinationExtractor);
        }
      } else if ("Deutsch Verb Übersicht".equals(templateName)
          || ("Verb-Tabelle".equals(templateName))) {
        // DONE get the link to the Konjugationnen page and extract data from the expanded tables
        // Page name has changed to "Flexion:" String conjugationPage = pageName + " (Konjugation)";
        String conjugationPage = "Flexion:" + pageName;
        extractFormsPageWithModel(conjugationPage, pageName, konjugationExtractor);
      } else if (templateName.equals("Deutsch adjektivische Deklination")) {
        extractFormsWithModel(wt.toString(), pageName, substantivDeklinationExtractor);
      } else if (templateName.startsWith("Deutsch adjektivische Deklination ")) {
        // Will expand to Deutsch adjektivische Deklination that will be caught afterwards.
        extractFormsWithModel(wt.toString(), pageName, substantivDeklinationExtractor);
      } else {
        log.debug("Morphology Extraction: Caught template call: {} --in-- {}", templateName,
            pageName);
        // Should I expand every other templates ?
      }
    }
  }

  private static ArrayList<String> cases = new ArrayList<>();
  private static ArrayList<String> numbers = new ArrayList<>();
  private static ArrayList<String> substTmplKeys = new ArrayList<>();
  static {
    cases.add("Nominativ");
    cases.add("Genitiv");
    cases.add("Dativ");
    cases.add("Akkusativ");
    numbers.add("Singular");
    numbers.add("Plural");

    for (String c : cases) {
      for (String num : numbers) {
        String k = c + " " + num;
        substTmplKeys.add(k);
      }
    }
  }


  private static final Pattern dashes = Pattern.compile("[\u2010-\u2015]+");

  private void extractMorphologicalSignature(Template wt) {



    // Analyse declinations and compute the regular deltas
    StringBuilder signature = new StringBuilder();
    signature.append("/");
    Map<String, String> args = wt.getParsedArgs();
    for (String k : substTmplKeys) {
      String arg = args.get(k);
      signature.setLength(signature.length() - 1);
      signature.append("|");
      if (arg != null) {
        // there is a general pattern
        arg = arg.trim();
        addFormSignature(arg, signature);
        signature.append("/");
        arg = args.get(k + "*");
        if (null != arg) {
          // there is an additional form
          arg = arg.trim();
          addFormSignature(arg, signature);
          signature.append("/");
        }
        arg = args.get(k + "**");
        if (null != arg) {
          // there is an additional form
          arg = arg.trim();
          addFormSignature(arg, signature);
          signature.append("/");
        }
      } else {
        // We have multiple patterns (depending on Genus)
        // Should we try to factorise it or not ?
        signature.append("YYYYYY");
      }
    }
    signature.setLength(signature.length() - 1);
    // TODO: treat defective cases (kein plural, etc.)

    log.debug("SUBSTANTIVE MORPHOLOGY @ {} >SIGNATURE: {}", wdh.currentPagename(),
        signature.toString());
  }

  private void addFormSignature(String arg, StringBuilder signature) {
    char c; // dashes are used for defective entries
    if (arg.length() == 1 && ('-' == (c = arg.charAt(0)) || ('\u2010' <= c && c <= '\u2015'))) {
      signature.append("X");
    } else {
      signature.append(StringDistance.suffixChange(wdh.currentPagename(), arg.trim()));
    }

  }

  private boolean extractAdjectiveDegree(Map<String, String> parameterMap) {
    boolean noOtherForms = false;

    for (Map.Entry<String, String> e : parameterMap.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      GermanInflectionData inflection = new GermanInflectionData();

      if (key.contains("Bild") || key.matches("\\d+")) {
        continue;
      }
      if (key.equalsIgnoreCase("keine weiteren Formen")) {
        noOtherForms = true;
        continue;
      }

      if (key.contains("Positiv")) {
        inflection.degree = Degree.POSITIVE;
      } else if (key.contains("Komparativ")) {
        inflection.degree = Degree.COMPARATIVE;
      } else if (key.contains("Superlativ")) {
        inflection.degree = Degree.SUPERLATIVE;
      } else {
        log.debug("no known degree, neither singular in Substantiv Ubersicht: {} | {}", key,
            wdh.currentPagename());
      }

      value = value.replaceAll("<(?:/)?small>", "");
      for (String form : value.split("(?:<br(?: */)?>)|(?:,\\s*)")) {
        addForm(inflection.toPropertyObjectMap(), form.trim());
      }
    }
    return noOtherForms;
  }

  private void addForm(HashSet<PropertyObjectPair> infl, String s) {
    if (s.length() == 0 || s.equals("—") || s.equals("-")) {
      return;
    }
    wdh.registerInflection("deu", wdh.currentWiktionaryPos(), s, wdh.currentPagename(), 1, infl);
  }

  private void extractFormsPageWithModel(String formsPageName, String pageName,
      GermanTableExtractorWikiModel model) {
    String subPageContent = wi.getTextOfPageWithRedirects(formsPageName);
    if (null == subPageContent) {
      log.debug("extractFormsPageWithModel: subPageContent is null : {} / {}", formsPageName,
          pageName);
      return;
    }
    if (!subPageContent.contains("Deutsch")) {
      log.debug("extractFormsPageWithModel: page does not contain \"Deutsch\": {} / {}",
          formsPageName, pageName);
      return;
    }

    extractFormsWithModel(subPageContent, pageName, model);
  }

  private void extractFormsWithModel(String wikiCode, String pageName,
      GermanTableExtractorWikiModel model) {
    model.setPageName(pageName);
    InflectedFormSet forms = model.parseTables(wikiCode);
    registerAllForms(forms);
  }

  private void registerAllForms(InflectedFormSet forms) {
    for (Entry<InflectionData, Set<String>> form : forms) {
      wdh.registerInflection(form.getKey(), form.getValue());
    }
  }

}