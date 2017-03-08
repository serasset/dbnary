package org.getalp.dbnary.wiki;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by serasset on 05/03/17.
 */
public class WikiPattern {

    /**
     * Compiles the pattern using extended syntax fit to define patterns on wiki texts.
     *
     * <h3><a name="sum">Summary of regular-expression extension</a></h3>
     *
     * <table border="0" cellpadding="1" cellspacing="0"
     *  summary="Regular expression constructs, and what they match">
     *
     * <tr align="left">
     * <th align="left" id="construct">Construct</th>
     * <th align="left" id="matches">Matches</th>
     * </tr>
     *
     * <tr><th>&nbsp;</th></tr>
     * <tr align="left"><th colspan="2" id="classes">Character classes</th></tr>
     *
     *  <tr><td valign="top">{@code \p{Template}}</td>
     *     <td headers="matches">a full wiki template.</td></tr>
     *  <tr><td valign="top">{@code \p{Link}}</td>
     *     <td headers="matches">either an Internal or an External Link.</td></tr>
     *  <tr><td valign="top">{@code \p{ExternalLink}}</td>
     *     <td headers="matches">an External Link.</td></tr>
     *  <tr><td valign="top">{@code \p{InternalLink}}</td>
     *     <td headers="matches">an Internal Link (complete with its eventual suffix).</td></tr>
     *
     *
     * <tr><th>&nbsp;</th></tr>
     * <tr align="left"><th colspan="2" id="classes">Open/Close events</th></tr>
     *
     *  <tr><td valign="top"><tt>(_</tt><i>xxx</i><tt>_<tt></td>
     *     <td headers="matches">the opening of an event, where <i>xxx</i> is identifies
     *     the event (xxx is a sequence of characters, possibly empty). If present, xxx will represent a group name
     *     (take it into account when playing with group count.</td></tr>
     *  <tr><td valign="top"><tt>_</tt><i>xxx</i><tt>_)<tt></td>
     *     <td headers="matches">the closing of an event which was given name <i>xxx</i> on opening.
     *     If <i>xxx</i> is empty, matches any closing event.</td></tr>
     *
     * @param regex
     * @return a Pattern matching the given extended regex.
     */
    public static Pattern compile(String regex) {
        String correctedRegex = correctRegex(regex);
        return Pattern.compile(correctedRegex);
    }

    private static String reserveWikiPatternWords = new StringBuffer()
            .append("(?<")
            .append("TMPL")
            .append(">").append("\\\\(?<TMATCH>[pP])\\{Template\\}")
            .append(")|(?<")
            .append("RESERVED")
            .append(">").append("\\\\(?<RMATCH>[pP])\\{Reserved\\}")
            .append(")|(?<")
            .append("L")
            .append(">").append("\\\\(?<LMATCH>[pP])\\{Link\\}")
            .append(")|(?<")
            .append("IL")
            .append(">").append("\\\\(?<ILMATCH>[pP])\\{InternalLink\\}")
            .append(")|(?<")
            .append("EL")
            .append(">").append("\\\\(?<ELMATCH>[pP])\\{ExternalLink\\}")
            .append(")|(?<")
            .append("OPEN")
            .append(">").append("\\(_(?<ONAME>\\p{Alpha}\\p{Alnum}*)?_")
            .append(")|(?<")
            .append("CLOSE")
            .append(">").append("_(?<CNAME>\\p{Alpha}\\p{Alnum}*)?_\\)")
            //.append(")|(?<")
            //.append("EL")
            //.append(">").append("\\\\p\\{ExternalLink\\}")
            .append(")")
            .toString();

    private static final Pattern lexerPatern = Pattern.compile(reserveWikiPatternWords);

    private static final String TEMPLATES = WikiCharSequence.TEMPLATES_RANGE.toString();
    private static final String INTERNAL_LINKS = WikiCharSequence.INTERNAL_LINKS_RANGE.toString();
    private static final String EXTERNAL_LINKS = WikiCharSequence.EXTERNAL_LINKS_RANGE.toString();
    private static final String HEADERS = WikiCharSequence.HEADERS_RANGE.toString();
    private static final String LIST_ITEMS = WikiCharSequence.LISTS_RANGE.toString();
    private static final String LINKS = INTERNAL_LINKS + EXTERNAL_LINKS;
    private static final String RESERVED = TEMPLATES + LINKS + HEADERS + LIST_ITEMS ;


    private static String correctRegex(String regex) {
        StringBuffer correctedRegex = new StringBuffer(regex.length());

        Matcher lexer = lexerPatern.matcher(regex);
        while (lexer.find()) {
            String g;
            lexer.appendReplacement(correctedRegex, "");
            if (null != (g = lexer.group("TMPL"))) {
                String negation = "";
                if (lexer.group("TMATCH").equals("P")) negation = "^";
                correctedRegex.append(("[" + negation + TEMPLATES + "]"));
            } else if (null != (g = lexer.group("RESERVED"))) {
                String negation = "";
                if (lexer.group("RMATCH").equals("P")) negation = "^";
                correctedRegex.append(("[" + negation + RESERVED + "]"));
            } else if (null != (g = lexer.group("L"))) {
                String negation = "";
                if (lexer.group("LMATCH").equals("P")) negation = "^";
                correctedRegex.append(("[" + negation + LINKS + "]"));
            } else if (null != (g = lexer.group("IL"))) {
                String negation = "";
                if (lexer.group("ILMATCH").equals("P")) negation = "^";
                correctedRegex.append(("[" + negation + INTERNAL_LINKS + "]"));
            } else if (null != (g = lexer.group("EL"))) {
                String negation = "";
                if (lexer.group("ELMATCH").equals("P")) negation = "^";
                correctedRegex.append(("[" + negation + EXTERNAL_LINKS + "]"));
            } else if (null != (g = lexer.group("OPEN"))) {
                String oname = lexer.group("ONAME");
                if (oname != null && oname.length() > 0) {
                    correctedRegex.append(("〔(?<" + oname + ">[" + RESERVED + "])"));
                } else {
                    correctedRegex.append(("〔[" + RESERVED + "]"));
                }
            } else if (null != (g = lexer.group("CLOSE"))) {
                String cname = lexer.group("CNAME");
                if (cname != null && cname.length() > 0) {
                    correctedRegex.append(("\\k<" + cname + ">〕"));
                } else {
                    correctedRegex.append(("[" + RESERVED + "]〕"));
                }
            } else {
                assert false;
            }
        }
        lexer.appendTail(correctedRegex);

        return correctedRegex.toString();
    }


}
