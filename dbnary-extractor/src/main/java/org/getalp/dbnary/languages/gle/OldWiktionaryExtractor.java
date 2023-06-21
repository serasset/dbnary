package org.getalp.dbnary.languages.gle;

import info.bliki.wiki.filter.PlainTextConverter;
import org.getalp.dbnary.api.IWiktionaryDataHandler;
import org.getalp.dbnary.api.WiktionaryPageSource;
import org.getalp.dbnary.bliki.ExpandAllWikiModel;
import org.getalp.dbnary.languages.AbstractWiktionaryExtractor;
import org.getalp.dbnary.wiki.WikiText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OldWiktionaryExtractor extends AbstractWiktionaryExtractor {
    public OldWiktionaryExtractor(IWiktionaryDataHandler wdh) {
        super(wdh);
    }

    private final Logger log = LoggerFactory.getLogger(OldWiktionaryExtractor.class);

    private static int count = 0;
    private static int TOTAL = 3076;
    private static int FINISH = 2015;
    private static int MISS_GENDER = 541;
    protected ExpandAllWikiModel model;
    private static final String URL = "https://ga.wiktionary.org/wiki/";


    @Override
    public void setWiktionaryIndex(WiktionaryPageSource wi) {
        super.setWiktionaryIndex(wi);
        System.out.println("Page source : " + wi);
        model = new ExpandAllWikiModel(wi, new Locale("ga"),
                "/${image}", "/${title}");
    }

    @Override
    protected void setWiktionaryPageName(String wiktionaryPageName) {
        super.setWiktionaryPageName(wiktionaryPageName);
        //  System.out.println("Setting name : " + wiktionaryPageName);
        model.setPageName(wiktionaryPageName);
    }

    @Override
    public void extractData() {
        wdh.initializePageExtraction(getWiktionaryPageName());

        WikiText page = new WikiText(getWiktionaryPageName(), pageContent);
        WikiText.WikiDocument doc = page.asStructuredDocument();

        // System.out.println(doc.getContent().wikiTokens().get(0));
/*
        if (pageContent.contains("{{ucf|")) {
            System.out.println(URL + getWiktionaryPageName());
        }
        // ArrayList<ResultTemplate> templates = findSimpleTemplate(pageContent);*/


        firstRoot(doc.getContent().tokens());
        if (getWiktionaryPageName().equals("chance"))
            System.out.println("Count : " + count);

        wdh.finalizePageExtraction();
    }


    public void firstRoot(final List<WikiText.Token> tokens) {
        String current_language;
        int cursor = 0;


        if (tokens.size() < 4) {
            // System.out.println("ERROR ! No data detected !");
            return;
        }
        ResultTemplate temp = null;
        if (pageContent.contains("__NOTOC__") || pageContent.contains("{{-sym-}}"))
            return;

        while (cursor < tokens.size() && (temp = findFirstTemplate(tokens.get(cursor).getText())) == null || (temp != null) && !(temp.name.matches("-[a-z][a-z]-") || !temp.name.matches("-[a-z][a-z][a-z]-")))
            cursor++;

        if (temp == null)
            return;

        if (tokens.get(cursor).getText().startsWith("{{tábla peiriadach") || tokens.get(cursor).getText().startsWith("{{vicipéid")
            || tokens.get(cursor).getText().startsWith("{{rós an chompáis-"))
            cursor += 2;

        // System.out.println("FIRST : " + tokens.get(cursor).getText() + " --> " + URL + getWiktionaryPageName());

        temp = findFirstTemplate(tokens.get(cursor).getText());
        if (temp != null && (temp.name.equals("t") || temp.name.matches("-[a-z][a-z]-") || temp.name.matches("-[a-z][a-z][a-z]-"))) {
            if (temp.name.equals("t"))
                current_language = temp.getArgs()[0];
            else
                current_language = temp.name.substring(1, temp.name.length() - 1);

            cursor += 2;

         /*   if (!current_language.equals("ga")) {
              //  count++;
                return;
            }*/

            wdh.initializeLanguageSection("ga");

            while (cursor < tokens.size() && !tokens.get(cursor).getText().equals("{{-fuaim-}}") && !tokens.get(cursor).getText().equals("{{-pron-}}") && !tokens.get(cursor).getText().equals("{{-phon-}}")) {
                cursor++;
                //System.out.println("SKIPPED : " + URL + getWiktionaryPageName());
            }

            if (tokens.size() > cursor && (tokens.get(cursor).getText().equals("{{-fuaim-}}") || tokens.get(cursor).getText().equals("{{-pron-}}") || tokens.get(cursor).getText().equals("{{-phon-}}"))) {
                ArrayList<ResultTemplate> templates;
                cursor += 2;

                if (tokens.size() > 3 && (templates = findSimpleTemplate(tokens.get(cursor).getText())).size() != 0 && templates.get(0).name.equals("IPA")) {
                    // System.out.println("Prononciation " + current_language + " -> " + templates.get(0).getArgs()[0]);
                    // System.out.println(current_language + "-pron");
                    if (!templates.get(0).getArgs()[0].equals("//"))
                        wdh.registerPronunciation(templates.get(0).getArgs()[0], current_language + "-pron");

                    cursor += 2;
                    //    System.out.println(tokens.get(cursor));
                   /* if(getWiktionaryPageName().equals("Apfel"))
                        System.out.println(tokens.get(cursor).getText());*/
                    while ((temp = findFirstTemplate(tokens.get(cursor).getText())) == null || temp.name.equals("audio"))
                        cursor++;
                    String wordType = findFirstTemplate(tokens.get(cursor).getText()).name;
                    wdh.initializeLexicalEntry(wordType);
                    //System.out.println(wordType);
                    cursor += 2;

                    if (tokens.get(cursor).getText().startsWith("{{ucf"))
                        cursor += 2;

                    if (tokens.get(cursor).getText().equals("{{pn}}") || tokens.get(cursor).getText().equals("{{en-noun}}")) // skip {{pn}}
                        cursor += 2;

                    // System.out.println("skipped : " + tokens.get(cursor - 2).getText() + " current : " + tokens.get(cursor).getText());
                    if (tokens.get(cursor).getText().equals("{{fir}}") || tokens.get(cursor).getText().equals("{{fir|}}") // skip {{fir}}
                        || tokens.get(cursor).getText().equals("{{bain}}")
                    ) {
                        //  System.out.println(tokens.get(cursor).getText() + " ->  " + URL + getWiktionaryPageName());
                        cursor += 2;
                    }

                    if (!tokens.get(cursor).getText().startsWith("#")) { // TODO GENDER AND COMPLEX TEMPLATE NAME
                        // System.out.println(tokens.get(cursor).getText() + " --> " + url());
                        while (cursor < tokens.size() && (temp = findFirstTemplate(tokens.get(cursor).getText())) == null)
                            cursor++;
                        complexTemplateExtractor(temp);
                        cursor += 2;
                    }

                    if (cursor >= tokens.size())
                        return;


                    if (tokens.get(cursor).getText().startsWith("#")) {
                        //       count++;
                        //     System.out.println(tokens.get(cursor).getText() + " -> " + URL + getWiktionaryPageName());
                        StringBuilder def = new StringBuilder();
                        while (cursor < tokens.size() && tokens.get(cursor).getText().startsWith("#")) {

                            String rended = null;
                            try {
                                rended = model.render(new PlainTextConverter(), tokens.get(cursor).getText()).trim();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            System.out.println(tokens.get(cursor).getText() + " --> " + rended + " --> " + url());
                            cursor += 2;

                            if (cursor < tokens.size())
                                if (tokens.get(cursor).getText().startsWith("#*")) {
                                    def.append(" ");
                                //    System.out.println(" Points : " + url());
                                } else if (tokens.get(cursor).getText().startsWith("#")) {
                                    //            System.out.println(" ---> Parsed : " + def);
                                 //   System.out.println("Multiple url : " + url());
                                    wdh.registerNewDefinition(def.toString());
                                    def = new StringBuilder();
                                }
                        }

                        //   count++;
                        wdh.registerNewDefinition(def.toString());
                        //    System.out.println(" ---> Parsed : " + def);
                        if (tokens.size() <= cursor) {
                            return;
                        }

                        if (tokens.get(cursor).getText().equals("{{-aistr-}}")) {
                            //   System.out.println(tokens.get(cursor) + " ---> " + URL + getWiktionaryPageName());
                           /* cursor += 2;
                            if (findFirstTemplate(tokens.get(cursor).getText()).name.equals("("))
                                cursor += 2;*/


                            while (cursor < tokens.size() && (temp = findFirstTemplate(tokens.get(cursor).getText())) != null && !temp.name.equals(")")) {
                                cursor += 2;
                                if (!temp.name.equals("aistr"))
                                    continue;
                                //   wdh.registerTranslation(LangTools.normalize(temp.getArgs()[0]), null /*TODO implement glossary*/,null/*TODO implement usage*/, temp.getArgs()[1]);
                                if (temp.getArgs().length < 2)
                                    continue;
                                //     System.out.println("Translation " + getWiktionaryPageName() + " --" + LangTools.normalize(temp.getArgs()[0]) + "--> " + temp.getArgs()[1]);
                            }

                        }

                     /*   if (tokens.get(cursor).getText().equals("{{-fuaim-}}")) {
                            System.out.println(tokens.get(cursor) + " ---> " + URL + getWiktionaryPageName());
                        }*/
                    }
                }

            }
        } else {
            //  count++;
            // System.out.println(tokens.get(cursor).getText() + " --> " + URL + getWiktionaryPageName());
        }
    }

    public static ResultTemplate findFirstTemplate(final String contentPage) {
        ArrayList<ResultTemplate> templates = findSimpleTemplate(contentPage);
        return templates.isEmpty() ? null : templates.get(0);
    }


    public static ArrayList<ResultTemplate> findSimpleTemplate(final String contentPage) {
        ArrayList<ResultTemplate> simpleTemplates = new ArrayList<>();

        String current;

        int deep = 0;
        int cursor = 0;
        int start = 0;
        while (cursor + 1 < contentPage.length()) {
            current = contentPage.substring(cursor, cursor + 2);
            if (current.equals("{{")) {
                deep++;
                if (deep == 1)
                    start = cursor;
            } else if (current.equals("}}")) {
                deep--;
                if (deep == 0)
                    simpleTemplates.add(toTemplateResult(contentPage.substring(start, ++(cursor) + 1)));
                else if (deep < 0)
                    deep = 0;
            }
            cursor++;
        }

        return simpleTemplates;
    }

    public void complexTemplateExtractor(final ResultTemplate template) {
        if (template == null) {
            System.err.println("TEMPLATE NULL ! --> " + url());
            return;
        }
        String genitifSingulier = null;
        String genitifPluriel = null;
        String gender = null;
        String pluriels = null;
        String singulier = null;
        String presentativeSingular = null;
        switch (template.name) {
            case "ainm 1":
                genitifSingulier = template.getArgs()[0];
                gender = "f";
                if (template.getArgs().length > 1)
                    pluriels = template.getArgs()[1];
                singulier = getArgName(template.getArgs(), "au");
                break;
            case "ainm 2":
                genitifSingulier = template.getArgs()[0];
                if (template.getArgs().length > 1)
                    pluriels = template.getArgs()[1];
                gender = getArgName(template.getArgs(), "i");
                if (gender == null)
                    gender = "b";
                presentativeSingular = getArgName(template.getArgs(), "t");
                singulier = getArgName(template.getArgs(), "au");
                break;

            case "ainm 3":
                gender = template.getArgs()[0];
                genitifSingulier = template.getArgs()[1];
                if (template.getArgs().length > 2)
                    pluriels = template.getArgs()[2];
                if (template.getArgs().length > 3)
                    genitifPluriel = template.getArgs()[3];
                singulier = getArgName(template.getArgs(), "au");
                break;
            case "ainm 4":
                gender = template.getArgs()[0];
                pluriels = template.getArgs()[1];
                singulier = getArgName(template.getArgs(), "au");
                genitifSingulier = getArgName(template.getArgs(), "gu");
                break;
            case "ainm 5":
                gender = template.getArgs()[0];
                genitifSingulier = template.getArgs()[1];
                if (template.getArgs().length > 2)
                    pluriels = template.getArgs()[2];
                if (template.getArgs().length > 3)
                    genitifPluriel = template.getArgs()[3].equals("") ? null : genitifSingulier;
                singulier = getArgName(template.getArgs(), "au");
                break;
            default:
                //  System.err.println("Unhandled template : " + template.name + " : " + template.content + " ---> " + url());
                break;
        }
        if (pluriels != null && pluriels.equals("a"))
            pluriels = genitifSingulier + "a";
        if (singulier == null)
            singulier = getWiktionaryPageName();

    }

    public static ResultLink findFirstLink(final String contentPage) {
        ArrayList<ResultLink> links = findSimpleLink(contentPage);
        return links.isEmpty() ? null : links.get(0);
    }

    public static ArrayList<ResultLink> findSimpleLink(final String contentPage) {
        ArrayList<ResultLink> simpleTemplates = new ArrayList<>();

        String current;

        int deep = 0;
        int cursor = 0;
        int start = 0;
        while (cursor + 1 < contentPage.length()) {
            current = contentPage.substring(cursor, cursor + 2);
            if (current.equals("[[")) {
                deep++;
                if (deep == 1)
                    start = cursor;
            } else if (current.equals("]]")) {
                deep--;
                if (deep == 0)
                    simpleTemplates.add(toResultLink(contentPage.substring(start, ++(cursor) + 1)));
                else if (deep < 0)
                    deep = 0;
            }
            cursor++;
        }

        return simpleTemplates;
    }


    public static ResultLink toResultLink(final String content) {
        if (content.contains("Catagóir"))
            return new ResultLink("", "");

        String[] argsList = content.substring(2, content.length() - 2).split("\\|");
        if (argsList.length == 1)
            return new ResultLink(argsList[0], argsList[0]);
        else
            return new ResultLink(argsList[0], argsList[1]);

    }

    public static class ResultLink {
        final String linkedWork;
        final String toDisplay;

        public ResultLink(final String linkedWork, final String toDisplay) {
            this.linkedWork = linkedWork;
            this.toDisplay = toDisplay;
        }
    }

    public static ResultTemplate toTemplateResult(final String content) {
        if (content.contains("|"))
            return new ResultTemplate(content.substring(2, content.indexOf("|")), content);

        return new ResultTemplate(content.substring(2, content.length() - 2), content);
    }


    public static String trucChelou(final String content) {
        ArrayList<ResultTemplate> templates = findSimpleTemplate(content);
        StringBuilder str = new StringBuilder();

        for (ResultTemplate temp : templates) {
            switch (temp.name) {
                case "+ai":
                    str.append(" agus ainmneach iolra ");
                    break;
                case "+gi":
                    str.append(" agus ginideach iolra ");
                    break;
                case "+ái":
                    str.append(" agus áinsíoch iolra ");
                    break;
                case "+ti":
                    str.append(" agus tabharthach iolra ");
                    break;
                default:
                    break;
            }
        }

        return str.toString();
    }


    public static class ResultTemplate {
        public String name;
        public String content;

        public ResultTemplate(final String name, final String content) {
            this.name = name;
            this.content = content;
        }

        public String[] getArgs() {
            return content.substring(2 + name.length() + 1, content.length() - 2).split("\\|");
        }

        @Override
        public String toString() {
            return "ResultTemplate{" +
                   "name='" + name + '\'' +
                   ", content='" + content + '\'' +
                   '}';
        }
    }

    public static String getArgName(final String[] args, final String argName) {
        for (final String arg : args)
            if (arg.startsWith(argName + "="))
                return arg.substring(argName.length());

        return null;
    }

    public String url() {
        return URL + getWiktionaryPageName();
    }

}
