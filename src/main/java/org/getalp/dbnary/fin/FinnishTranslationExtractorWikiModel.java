package org.getalp.dbnary.fin;

import info.bliki.wiki.filter.WikipediaParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.getalp.blexisma.api.ISO639_3;
import org.getalp.dbnary.AbstractWiktionaryExtractor;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;
import org.getalp.dbnary.wiki.WikiPatterns;

public class FinnishTranslationExtractorWikiModel extends DbnaryWikiModel {
	
	private WiktionaryDataHandler delegate;
	
	public FinnishTranslationExtractorWikiModel(WiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
		this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
	}
	
	public FinnishTranslationExtractorWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
		super(wi, locale, imageBaseURL, linkBaseURL);
		this.delegate = we;
	}

	
	// TODO: handle entries where translations refer to the translations of another term (form: Kts. [[other entry]]).
	public void parseTranslationBlock(String block) {
		// Heuristics: if the translation block uses kohta macro, we assume that ALL translation data is available in the macro.
		if (block.contains("{{kohta")) {
			parseTranslationBlockWithBliki(block);
		} else {
			extractTranslations(block);
		}
	}
	
	public void parseTranslationBlockWithBliki(String block) {
		initialize();
		if (block == null) {
			return;
		}
		WikipediaParser.parse(block, this, true, null);
		initialize();
	}

	private static final HashSet<String> transMacroWithNotes = new HashSet<String>();
	static {
		transMacroWithNotes.add("xlatio");
		transMacroWithNotes.add("trad-");

	}
	private String currentGloss = null;

	@Override
	public void substituteTemplateCall(String templateName,
			Map<String, String> parameterMap, Appendable writer)
			throws IOException {
		if ("kohta".equals(templateName)) {
			// kohta macro contains a set of translations with no usage note.
			// Either: (1) arg 1 is the sens number and arg2 is the gloss, arg3 are translations and arg 4 is final
			// Or: arg1 is translations and arg 2 is final
			String gloss = "";
			String xans;
			if (null != parameterMap.get("3")) {
				// case (1)
				String n = (null == parameterMap.get("1")) ? null : parameterMap.get("1").trim();
				String g = (null == parameterMap.get("2")) ? null : parameterMap.get("2").trim();
				if (null != n && ! n.equals("")) gloss = n + "| ";
				if (null != g) gloss = gloss + g;
				
				xans = parameterMap.get("3");
				
				if (parameterMap.get("4") != null && ! (parameterMap.get("4").equals("loppu") || parameterMap.get("4").equals(""))) {
					System.err.println("Unexpected arg 4 value of kohta macro: " + parameterMap.get("4"));
				}
			} else {
				xans = parameterMap.get("1");
			}
			extractTranslations(xans, gloss);
			
		} else {
			 System.err.println("Called template: " + templateName + " while parsing translations of: " + this.getImageBaseURL());
			// Just ignore the other template calls (uncomment to expand the template calls).
			// super.substituteTemplateCall(templateName, parameterMap, writer);
		}
	}

	private String normalizeLang(String lang) {
		String normLangCode;
		if ((normLangCode = ISO639_3.sharedInstance.getIdCode(lang)) != null) {
			lang = normLangCode;
		}
		return lang;
	}
	
	
	protected final static String carPatternString;
	protected final static String macroOrLinkOrcarPatternString;


	static {
		// DONE: Validate the fact that links and macro should be on one line or may be on several...
		// DONE: for this, evaluate the difference in extraction !
		
		// les caractères visible 
		carPatternString=
				new StringBuilder().append("(.)")
				.toString();

		// TODO: We should suppress multiline xml comments even if macros or line are to be on a single line.
		macroOrLinkOrcarPatternString = new StringBuilder()
		.append("(?:")
		.append(WikiPatterns.macroPatternString)
		.append(")|(?:")
		.append(WikiPatterns.linkPatternString)
		.append(")|(?:")
		.append("(:*\\*)")
		.append(")|(?:")
		.append("(\\*:)")
		.append(")|(?:")
		.append(carPatternString)
		.append(")").toString();



	}
	protected final static Pattern carPattern;
	protected final static Pattern macroOrLinkOrcarPattern;


	static {
		carPattern = Pattern.compile(carPatternString);
		macroOrLinkOrcarPattern = Pattern.compile(macroOrLinkOrcarPatternString, Pattern.MULTILINE|Pattern.DOTALL);
		
	}

	public void extractTranslations(String block) {
		extractTranslations(block, null);
	}
	
	public void extractTranslations(String block, String gloss) {
		Matcher macroOrLinkOrcarMatcher = macroOrLinkOrcarPattern.matcher(block);
		final int INIT = 1;
		final int LANGUE = 2;
		final int TRAD = 3;

	
		
		int ETAT = INIT;

		String currentGlose = gloss;
		String lang=null, word= ""; 
		String usage = "";       
		String langname = "";
		String previousLang = null;
		
		while (macroOrLinkOrcarMatcher.find()) {

			String macro = macroOrLinkOrcarMatcher.group(1);
			String link = macroOrLinkOrcarMatcher.group(3);
			String star = macroOrLinkOrcarMatcher.group(5);
			String starcont = macroOrLinkOrcarMatcher.group(6);
			String character = macroOrLinkOrcarMatcher.group(7);

			switch (ETAT) {

			case INIT:
				if (macro!=null) {
					if (macro.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}

					} else if (macro.equalsIgnoreCase("ala")) {
						currentGlose = null;
					} else if (macro.equalsIgnoreCase("keski")) {
						//ignore
					}
				} else if(link!=null) {
					//System.err.println("Unexpected link while in INIT state.");
				} else if (starcont != null) {
					System.err.println("Unexpected point continuation while in INIT state.");
				} else if (star != null) {
					ETAT = LANGUE;
				} else if (character != null) {
					if (character.equals(":")) {
						//System.err.println("Skipping ':' while in INIT state.");
					} else if (character.equals("\n") || character.equals("\r")) {

					} else if (character.equals(",")) {
						//System.err.println("Skipping ',' while in INIT state.");
					} else {
						//System.err.println("Skipping " + g5 + " while in INIT state.");
					}
				}

				break;

			case LANGUE:

				if (macro!=null) {
					if (macro.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("ala")) {
						currentGlose = null;
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("keski")) {
						langname = ""; word = ""; usage = "";
						ETAT = INIT;
					} else {
						langname = macro;
						String l = ISO639_3.sharedInstance.getIdCode(langname);
						if (l != null) {
							langname = l;
						}
					}
				} else if(link!=null) {
					//System.err.println("Unexpected link while in LANGUE state.");
				} else if (starcont != null) {
					lang = previousLang;
					ETAT = TRAD;
				} else if (star != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (character != null) {
					if (character.equals(":")) {
						lang = langname.trim();
						lang=AbstractWiktionaryExtractor.supParenthese(lang);
						lang =SuomiLangToCode.triletterCode(lang);
						langname = "";
						ETAT = TRAD;
					} else if (character.equals("\n") || character.equals("\r")) {
						//System.err.println("Skipping newline while in LANGUE state.");
					} else if (character.equals(",")) {
						//System.err.println("Skipping ',' while in LANGUE state.");
					} else {
						langname = langname + character;
					}
				} 

				break ;
			case TRAD:
				if (macro!=null) {
					if (macro.equalsIgnoreCase("ylä"))  {
						if (macroOrLinkOrcarMatcher.group(2) != null) {
							currentGlose = macroOrLinkOrcarMatcher.group(2);
						} else {
							currentGlose = null;
						}
						//if (word != null && word.length() != 0) {
						//	if(lang!=null){
						//		delegate.registerTranslation(lang, currentGlose, usage, word);
						//	}
						//}
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("ala")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						currentGlose = null;
						langname = ""; word = ""; usage = ""; lang=null;
						ETAT = INIT;
					} else if (macro.equalsIgnoreCase("kski")) {
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						langname = ""; word = ""; usage = ""; lang = null;
						ETAT = INIT;
					} else {
						usage = usage + "{{" + macro + "}}";
					}
				} else if (link!=null) {
					word = word + " " + link;
				} else if (starcont != null) {
					System.err.println("Skipping '*:' while in LANGUE state.");
				} else if (star != null) {
					//System.err.println("Skipping '*' while in LANGUE state.");
				} else if (character != null) {
					if (character.equals("\n") || character.equals("\r")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						previousLang = lang;
						lang = null; 
						usage = "";
						word = "";
						ETAT = INIT;
					} else if (character.equals(",")) {
						usage = usage.trim();
						// System.err.println("Registering: " + word + ";" + lang + " (" + usage + ") " + currentGlose);
						if (word != null && word.length() != 0) {
							if(lang!=null){
								delegate.registerTranslation(lang, currentGlose, usage, word);
							}
						}
						usage = "";
						word = "";
					} else {
						usage = usage + character;
					}
				}
				break;
			default: 
				System.err.println("Unexpected state number:" + ETAT);
				break; 
			}

		}
	}  

}
