package org.getalp.dbnary.languages.eng;

import info.bliki.extensions.scribunto.engine.ScribuntoEngine;
import info.bliki.extensions.scribunto.engine.lua.CompiledScriptCache;
import info.bliki.extensions.scribunto.engine.lua.ScribuntoLuaEngine;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.getalp.dbnary.LexinfoOnt;
import org.getalp.dbnary.OliaOnt;
import org.getalp.dbnary.OntolexOnt;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.morphology.InflectedFormSet;
import org.getalp.dbnary.morphology.InflectionData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnglishMorphologyExtractorWikiModel extends EnglishWikiModel {

  private final Logger log = LoggerFactory.getLogger(EnglishMorphologyExtractorWikiModel.class);

  private final WiktionaryDataHandler delegate;


  public EnglishMorphologyExtractorWikiModel(WiktionaryDataHandler we, Locale locale,
      String imageBaseURL, String linkBaseURL) {
    this(we, null, locale, imageBaseURL, linkBaseURL);
  }

  public EnglishMorphologyExtractorWikiModel(WiktionaryDataHandler we, WiktionaryPageSource wi,
      Locale locale, String imageBaseURL, String linkBaseURL) {
    super(wi, locale, imageBaseURL, linkBaseURL);
    this.delegate = we;
  }

  public void parseMorphology(String morphTemplate) {
    // Render the pronunciation template to plain text then extract the computed pronunciation
    log.trace("Parsing morphology : ||| {} ||| in {}", morphTemplate.toString(),
        delegate.currentPagename());
    String expandedMorph = null;
    expandedMorph = expandWikiCode(morphTemplate);
    InflectedFormSet forms;
    if (morphTemplate.contains("{{en-")) {
      forms = parseEnTemplatesHTML(expandedMorph);
    } else if (morphTemplate.contains("{{head")) {
      forms = parseHeadHTML(expandedMorph);
    } else {
      log.debug("MORPH: unknown form of morph template call");
      forms = new InflectedFormSet();
    }
    // FOR NOUNS
    // TODO: scan forms to which one is missing (the canonical form) and specify number on
    // the canonical form
    // TODO: add countable feature on the entry if plural is present
    for (Entry<InflectionData, Set<String>> form : forms) {
      delegate.registerInflection(form.getKey(), form.getValue());
    }
  }

  private InflectedFormSet parseHeadHTML(String htmlCode) {
    Document doc = Jsoup.parse(htmlCode);

    InflectedFormSet forms = new InflectedFormSet();
    Elements elts = doc.select("strong.headword, i, b>a");
    String currentInfl = "";
    for (Element elt : elts) {
      if (elt.tagName().equalsIgnoreCase("i")) {
        String flex = elt.text().trim();
        if (!flex.equals("or") && !flex.equals("and")) {
          currentInfl = flex;
        }
      } else if (elt.tagName().equalsIgnoreCase("a")) {

        String href = elt.attr("href").trim();
        if (!href.startsWith("/Appendix:Glossary#")) {
          inflectionFromClass(currentInfl, elt.text(), forms);
        }
      } else if (elt.hasClass("headword")) {
        if (!getPageName().equals(elt.text())) {
          log.debug("MORPH: headword `{}` is not pagename `{}`", elt.text(), getPageName());
        }
      }
    }
    return forms;
  }

  public InflectedFormSet parseEnTemplatesHTML(String htmlCode) {
    Document doc = Jsoup.parse(htmlCode);

    InflectedFormSet forms = new InflectedFormSet();

    String note = null;
    Elements elts =
        doc.select("strong.headword, b.form-of, a, span.qualifier-content, span.ib-content");
    for (Element elt : elts) {
      if (elt.tagName().equalsIgnoreCase("a")) {
        String href = elt.attr("href").trim();
        if (href.startsWith("/Appendix:Glossary#")) {
          String concept = href.substring(1 + href.indexOf('#'));
          if (concept.equals("countable")) {
            delegate.countable();
          } else if (concept.equals("uncountable")) {
            delegate.uncountable();
          } else {
            if (!"comparative".equals(concept) && !"superlative".equals(concept)
                && !"comparable".equals(concept))
              log.trace("MORPH: Ignoring glossary term `{}` in `{}`", concept, getPageName());
          }
        }
      } else if (elt.hasClass("headword")) {
        String text = elt.text().trim();
        if (!getPageName().equals(text)) {
          delegate.addWrittenRep(text);
          log.debug("MORPH: headword `{}` is not pagename `{}`", text, getPageName());
        }
      } else if (elt.hasClass("qualifier-content") || elt.hasClass("ib-content")) {
        note = elt.text().trim();
      } else {
        InflectedFormSet newForms = new InflectedFormSet();
        addInflexions(elt, elt.text(), newForms);
        if (null != note) {
          final String finalNote = note;
          newForms.forEach(e -> ((EnglishInflectionData) e.getKey()).note(finalNote));
          // Assume a note only concerns the following form
          note = null;
        }
        newForms.forEach(e -> forms.add(e.getKey(), e.getValue()));
      }
    }
    return forms;
  }


  private static final Supplier<EnglishInflectionData> plural =
      () -> new EnglishInflectionData().plural();
  private static final Supplier<EnglishInflectionData> singular =
      () -> new EnglishInflectionData().singular();
  private static final Supplier<EnglishInflectionData> feminine =
      () -> new EnglishInflectionData().feminine();
  private static final Supplier<EnglishInflectionData> masculine =
      () -> new EnglishInflectionData().masculine();
  private static final Supplier<EnglishInflectionData> comparative =
      () -> new EnglishInflectionData().comparative();
  private static final Supplier<EnglishInflectionData> superlative =
      () -> new EnglishInflectionData().superlative();
  private static final Supplier<EnglishInflectionData> pres3Sg =
      () -> new EnglishInflectionData().presentTense().thirdPerson().singular();
  private static final Supplier<EnglishInflectionData> presPtc =
      () -> new EnglishInflectionData().presentTense().participle();
  private static final Supplier<EnglishInflectionData> past =
      () -> new EnglishInflectionData().pastTense();
  private static final Supplier<EnglishInflectionData> pastPtc =
      () -> new EnglishInflectionData().pastTense().participle();

  private static final Map<String, BiConsumer<InflectedFormSet, String>> inflectionDecoder =
      new HashMap<>();

  static BiConsumer<InflectedFormSet, String> ppt = (forms, text) -> {
    forms.add(pastPtc.get(), text);
    forms.add(past.get(), text);
  };

  static {
    inflectionDecoder.put("comparative-form-of",
        (forms, text) -> forms.add(comparative.get(), text));
    inflectionDecoder.put("comparative", (forms, text) -> forms.add(comparative.get(), text));
    inflectionDecoder.put("superlative-form-of",
        (forms, text) -> forms.add(superlative.get(), text));
    inflectionDecoder.put("superlative", (forms, text) -> forms.add(superlative.get(), text));
    inflectionDecoder.put("p-form-of", (forms, text) -> forms.add(plural.get(), text));
    inflectionDecoder.put("plural", (forms, text) -> forms.add(plural.get(), text));
    inflectionDecoder.put("singular", (forms, text) -> forms.add(singular.get(), text));
    inflectionDecoder.put("feminine", (forms, text) -> forms.add(feminine.get(), text));
    inflectionDecoder.put("masculine", (forms, text) -> forms.add(masculine.get(), text));
    inflectionDecoder.put("3|s|pres-form-of", (forms, text) -> forms.add(pres3Sg.get(), text));
    inflectionDecoder.put("pres|ptcp-form-of", (forms, text) -> forms.add(presPtc.get(), text));
    inflectionDecoder.put("past|ptcp-form-of", (forms, text) -> forms.add(pastPtc.get(), text));
    inflectionDecoder.put("past-form-of", (forms, text) -> forms.add(past.get(), text));
    inflectionDecoder.put("past|and|past|ptcp-form-of", ppt);
    inflectionDecoder.put("simple past and past participle", ppt);

    // Do nothing actions
    inflectionDecoder.put("Latn", (forms, text) -> {
    });
    inflectionDecoder.put("lang-en", (forms, text) -> {
    });
    inflectionDecoder.put("form-of", (forms, text) -> {
    });
  }

  private void addInflexions(Element elt, String text, InflectedFormSet forms) {
    for (String clazz : elt.classNames()) {
      clazz = clazz.trim();
      inflectionFromClass(clazz, text, forms);
    }
  }

  private void inflectionFromClass(String clazz, String text, InflectedFormSet forms) {
    BiConsumer<InflectedFormSet, String> action = inflectionDecoder.get(clazz);
    if (null != action) {
      action.accept(forms, text);
    } else {
      log.debug("MORPH: unknown class `{}` for value `{}` in `{}`", clazz, text, getPageName());
    }
  }

  @Override
  public void substituteTemplateCall(String templateName, Map<String, String> parameterMap,
      Appendable writer) throws IOException {
    super.substituteTemplateCall(templateName, parameterMap, writer);
  }

  private CompiledScriptCache compiledScriptCache = new CompiledScriptCache();
  private ScribuntoEngine fScribuntoEngine = null;

  @Override
  public ScribuntoEngine createScribuntoEngine() {
    if (null == fScribuntoEngine)
      fScribuntoEngine = new ScribuntoLuaEngine(this, compiledScriptCache, log.isDebugEnabled());
    return fScribuntoEngine;
  }
}
