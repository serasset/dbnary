package org.getalp.dbnary.bul;

import info.bliki.wiki.filter.WikipediaParser;
import org.getalp.dbnary.DbnaryWikiModel;
import org.getalp.dbnary.WiktionaryDataHandler;
import org.getalp.dbnary.WiktionaryIndex;

import java.io.IOException;
import java.util.*;

public class BulgarianWikiModel extends DbnaryWikiModel {

    protected final static Set<String> bulgarianPOS = new TreeSet<String>();
    protected final static HashMap<String, String> nymMarkerToNymName;

    protected final String translationPattern = "";

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

    private WiktionaryDataHandler delegate;
    private boolean hasAPOS = false;

    public BulgarianWikiModel(WiktionaryDataHandler we, Locale locale, String imageBaseURL, String linkBaseURL) {
        this(we, (WiktionaryIndex) null, locale, imageBaseURL, linkBaseURL);
    }

    public BulgarianWikiModel(WiktionaryDataHandler we, WiktionaryIndex wi, Locale locale, String imageBaseURL, String linkBaseURL) {
        super(wi, locale, imageBaseURL, linkBaseURL);
        this.delegate = we;
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
                    String def = parameterMap.get(section).replace("# ", "");
                    def = def.replace("[0-9]+\\.", "").trim();
                    delegate.registerNewDefinition(def);
                } else if (section.contains("ПРЕВОД")) {
                    String[] translations = parameterMap.get(section).split("\\*");
                    for (String trans : translations) {
                        if (!trans.isEmpty()) {
                            String lang = BulgarianLangtoCode.triletterCode(trans.split(":")[0].trim().toLowerCase());
                            String translationBody = trans.split(":")[1];
                        }
                    }
                    //delegate.registerTranslation();
                }
            }
        } else {
            // Just ignore the other template calls (uncomment to expand the template calls).
            // super.substituteTemplateCall(templateName, parameterMap, writer);
        }
    }

    private String getPOS(String templateName) {
        for (String p : bulgarianPOS) {
            if (templateName.startsWith(p)) return p;
        }
        return null;
    }
}
