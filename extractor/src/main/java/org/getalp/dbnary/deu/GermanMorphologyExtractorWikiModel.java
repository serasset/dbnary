package org.getalp.dbnary.deu;

import info.bliki.wiki.filter.PlainTextConverter;
import org.getalp.dbnary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import static org.getalp.dbnary.deu.GermanInflectionData.Cas.*;
import static org.getalp.dbnary.deu.GermanInflectionData.GNumber.*;


public class GermanMorphologyExtractorWikiModel extends GermanDBnaryWikiModel {

	private final static String germanRegularVerbString="Deutsch Verb regelmäßig";
	private final static String germanNonRegularVerbString=" unregelmäßig";

	private  boolean isPhrasal=false;

	private static HashSet<String> ignoredTemplates;

//	private boolean reflexiv=false;
	static {
		ignoredTemplates = new HashSet<String>();
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

	private Logger log = LoggerFactory.getLogger(GermanMorphologyExtractorWikiModel.class);

	private IWiktionaryDataHandler wdh;
	protected GermanDeklinationExtractorWikiModel deklinationExtractor;
	protected GermanKonjugationExtractorWikiModel konjugationExtractor;

	public GermanMorphologyExtractorWikiModel(IWiktionaryDataHandler wdh, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		deklinationExtractor = new GermanDeklinationExtractorWikiModel(wdh, wi, new Locale("de"), "/${Bild}", "/${Titel}");
		konjugationExtractor = new GermanKonjugationExtractorWikiModel(wdh, wi, new Locale("de"), "/${Bild}", "/${Titel}");
		this.wdh=wdh;
	}

	private int isOtherForm=0;

	public void parseInflectedForms(String page, String normalizedPOS){
		System.out.println("page : "+page);
	}
	

	public void parseOtherForm(String page,String originalPos) {
		// Render the definition to plain text, while ignoring the example template
		render(new PlainTextConverter(), page).trim();
	}

	@Override
	public void substituteTemplateCall(String templateName,
									   Map<String, String> parameterMap, Appendable writer)
			throws IOException {
		if (ignoredTemplates.contains(templateName)) {
			; // NOP
		} else if ("Deutsch Substantiv Übersicht".equals(templateName)) {
			// TODO extract directly the data from the template call
			extractSubstantiveForms(parameterMap);
		} else if ("Deutsch Adjektiv Übersicht".equals(templateName)) {
			// TODO fetch and expand deklination page and parse all tables.
			String deklinationPageName = this.getPageName() + " (Deklination)";
			extractAdjectiveForms(deklinationPageName);
		} else if ("Deutsch Verb Übersicht".equals(templateName) || ("Verb-Tabelle".equals(templateName))) {
			// TODO get the link to the Konjugationnen page and extract data from the expanded tables
			String conjugationPage = this.getPageName() + " (Konjugation)";
			extractVerbForms(conjugationPage);
		} else {
			log.debug("Morphology Extraction: Caught template call: {} --in-- {}", templateName, this.getPageName());
			// super.substituteTemplateCall(templateName, parameterMap, writer);
		}
	}

	private void extractAdjectiveForms(String deklinationPageName) {
		String deklinationPageContent = wi.getTextOfPage(deklinationPageName);
		if (null == deklinationPageContent) return;
		if(!deklinationPageContent.contains("Deutsch")) return;

		deklinationExtractor.setPageName(this.getPageName());
		deklinationExtractor.parseTables(deklinationPageContent);
	}

	private void extractSubstantiveForms(Map<String, String> parameterMap) {
		// {{Deutsch Substantiv Übersicht
		// |Nominativ Singular=das Zyanid
		//		|Nominativ Plural=die Zyanide
		//		|Genitiv Singular=des Zyanids
		//		|Genitiv Plural=der Zyanide
		//		|Dativ Singular=dem Zyanid
		//		|Dativ Plural=den Zyaniden
		//		|Akkusativ Singular=das Zyanid
		//		|Akkusativ Plural=die Zyanide
		for (Map.Entry<String, String> e : parameterMap.entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			GermanInflectionData inflection = new GermanInflectionData();

			// TODO: pass if key is an image or non morphological parameter
			if (key.contains("Bild") || key.matches("\\d+")) continue;


			if (key.contains("Singular")) {
				inflection.number = SINGULAR;
			} else if (key.contains("Plural")) {
				inflection.number = PLURAL;
			} else {
				log.debug("no plural, neither singular in Substantiv Ubersicht: {} | {}", key, wdh.currentLexEntry());
			}

			if (key.contains("Nominativ")) {
				inflection.cas = NOMINATIF;
			} else if (key.contains("Genitiv")) {
				inflection.cas = GENITIF;
			} else if (key.contains("Dativ")) {
				inflection.cas = DATIF;
			} else if (key.contains("Akkusativ")) {
				inflection.cas = ACCUSATIF;
			} else {
				log.debug("no known case in Substantiv Ubersicht: {} | {}", key, wdh.currentLexEntry());
			}

			value = value.replaceAll("<(?:/)?small>", "");
			for (String form : value.split("<br(?: */)?>")) {
				addForm(inflection.toPropertyObjectMap(), form);
			}

		}
	}

	private void extractVerbForms(String conjugationPage) {
		String konjugationPageContent = wi.getTextOfPage(conjugationPage);
		if (null == konjugationPageContent) return;
		if(!konjugationPageContent.contains("Deutsch")) return;

		konjugationExtractor.setPageName(this.getPageName());
		konjugationExtractor.parseTables(konjugationPageContent);
	}


	private void addForm(HashSet<PropertyObjectPair> infl, String s) {
		s=s.replace("]", "").replace("[","").replaceAll(".*\\) *","").replace("(","").trim();
		if (s.length() == 0 || s.equals("—") || s.equals("-")) return;

		wdh.registerInflection("deu", wdh.currentWiktionaryPos(), s, wdh.currentLexEntry(), 1, infl);
	}

}
