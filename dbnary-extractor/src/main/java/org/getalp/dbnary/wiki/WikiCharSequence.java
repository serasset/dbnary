package org.getalp.dbnary.wiki;

import org.getalp.dbnary.tools.CharRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * A WikiCharSequence is a special character sequence that transforms a mediawiki code
 * according to a filter function.
 *
 * Default filter function is ...
 *
 * Created by serasset on 28/01/16.
 */
public class WikiCharSequence implements CharSequence {


    public static final CharRange LISTS_RANGE = new CharRange('\uE000', '\uE3FF'); // templates from U+E000 - U+E4FF
    public static final CharRange TEMPLATES_RANGE = new CharRange('\uE400', '\uE7FF'); // templates from U+E000 - U+E4FF
    public static final CharRange EXTERNAL_LINKS_RANGE = new CharRange('\uE800', '\uEBFF');
    public static final CharRange INTERNAL_LINKS_RANGE = new CharRange('\uEC00', '\uEFFF');
    public static final CharRange HEADERS_RANGE = new CharRange('\uF000', '\uF3FF');

    private final StringBuffer chars;
    private final WikiText.WikiContent content;

    private final Map<Character,WikiText.Token> characterTokenMap;

    private final Function<WikiText.Token, WikiSequenceFiltering.Action> filter;

    private int currentOffset = 0;
    private final int subSequenceStart;
    private final int subSequenceEnd;

    private char firstAvailableListChar = LISTS_RANGE.getStart();
    private char firstAvailableTemplateChar = TEMPLATES_RANGE.getStart();
    private char firstAvailableExternalLinkChar = EXTERNAL_LINKS_RANGE.getStart();
    private char firstAvailableInternalLinkChar = INTERNAL_LINKS_RANGE.getStart();
    private char firstAvailableHeaderChar = HEADERS_RANGE.getStart();

    public WikiCharSequence(String source) {
        this(new WikiText(source));
    }

    public WikiCharSequence(String source,
                            Function<WikiText.Token, WikiSequenceFiltering.Action> filter) {
        this(new WikiText(source), filter);
    }


    public WikiCharSequence(WikiText wt) {
        this(wt.content());
    }

    public WikiCharSequence(WikiText wt,
                            Function<WikiText.Token, WikiSequenceFiltering.Action> filter) {
        this(wt.content(), filter);
    }

    public WikiCharSequence(WikiText.WikiContent content) {
        this(   content,
                new StringBuffer(),
                new HashMap<>(),
                new ClassBasedSequenceFilter());
    }

    public WikiCharSequence(WikiText.WikiContent content,
                            Function<WikiText.Token, WikiSequenceFiltering.Action> filter) {
        this(   content,
                new StringBuffer(),
                new HashMap<>(),
                filter);
    }

    private WikiCharSequence(WikiText.WikiContent content,
                             StringBuffer chars,
                             Map<Character, WikiText.Token> characterTokenMap,
                             Function<WikiText.Token, WikiSequenceFiltering.Action> filter) {
        this.content = content;
        this.chars = chars;
        this.characterTokenMap = characterTokenMap;
        this.filter = filter;
        fillChars();
        this.subSequenceEnd = currentOffset;
        this.subSequenceStart = 0;
    }

    // Only used for sub sequences construction
    // Subsequences share the full sequence data but change their offset relative to subsequence bounds
    private WikiCharSequence(WikiCharSequence superSeq,
                             int subSequenceStart,
                             int subSequenceEnd) {
        this.content = superSeq.content;
        this.chars = superSeq.chars;
        this.characterTokenMap = superSeq.characterTokenMap;
        this.filter = superSeq.filter;
        this.subSequenceStart = subSequenceStart;
        this.subSequenceEnd = subSequenceEnd;
    }



    private char allocateCharacterFor(WikiText.Token tok) {
        // Assign a new char to the token
        char ch;
        if (tok instanceof WikiText.Template) {
            ch = firstAvailableTemplateChar++;
            if (ch > TEMPLATES_RANGE.getEnd()) throw new RuntimeException("Too many templates in current WikiText");
        } else if (tok instanceof WikiText.ExternalLink) {
            ch = firstAvailableExternalLinkChar++;
            if (ch > EXTERNAL_LINKS_RANGE.getEnd()) throw new RuntimeException("Too many external links in current WikiText");
        } else if (tok instanceof WikiText.InternalLink) {
            ch = firstAvailableInternalLinkChar++;
            if (ch > INTERNAL_LINKS_RANGE.getEnd()) throw new RuntimeException("Too many internal links in current WikiText");
        } else if (tok instanceof WikiText.Heading) {
            ch = firstAvailableHeaderChar++;
            if (ch > HEADERS_RANGE.getEnd()) throw new RuntimeException("Too many headings in current WikiText");
        } else if (tok instanceof WikiText.Indentation) {
            ch = firstAvailableListChar++;
            if (ch > LISTS_RANGE.getEnd()) throw new RuntimeException("Too many headings in current WikiText");
        } else if (tok instanceof WikiText.ListItem) {
            ch = firstAvailableListChar++;
            if (ch > LISTS_RANGE.getEnd()) throw new RuntimeException("Too many headings in current WikiText");
        } else {// TODO: consider all possible tokens
            throw new RuntimeException("Annot allocate atomization char for unsupported token type.");
        }
        characterTokenMap.put(ch, tok);
        return ch;
    }

    private void fillChars() {
        fillChars(content);
    }

    private void fillChars(WikiText.WikiContent content) {
        fillChars(content.tokens());
    }

    private void fillChars(ArrayList<WikiText.Token> tokens) {
        for (WikiText.Token token : tokens) {
            fillChars(token);
        }
    }

    private void fillChars(String s) {
        chars.append(s);
        currentOffset += s.length();
    }

    private void fillChars(WikiText.Token token) {
        WikiSequenceFiltering.Action a = filter.apply(token);

        if (a instanceof WikiSequenceFiltering.OpenContentClose) {
            Function<WikiText.Token, ArrayList<WikiText.Token>> contentSelector = ((WikiSequenceFiltering.Content) a).getter;

            char o = allocateCharacterFor(token);
            fillChars("〔" + o); // LEFT TORTOISE SHELL BRACKET (\u3014)
            fillChars(contentSelector.apply(token));
            fillChars(o + "〕"); // RIGHT TORTOISE SHELL BRACKET (\u3015)

        } else if (a instanceof WikiSequenceFiltering.Content) {
            Function<WikiText.Token, ArrayList<WikiText.Token>> contentSelector = ((WikiSequenceFiltering.Content) a).getter;
            fillChars(contentSelector.apply(token));
        } else if (a instanceof WikiSequenceFiltering.KeepAsis) {
            chars.append(token.getFullContent(), token.offset.start, token.offset.end);
            currentOffset += (token.offset.end - token.offset.start);
        } else if (a instanceof WikiSequenceFiltering.Atomize) {
            char o = allocateCharacterFor(token);
            fillChars("" + o);
        } else if (a instanceof WikiSequenceFiltering.Void) {
            // Nothing...
        }

    }

    @Override
    public int length() {
        return subSequenceEnd - subSequenceStart;
    }

    @Override
    public char charAt(int index) {
        if (0 > index) throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", index));
        int realIndex = index + subSequenceStart;
        if (realIndex >= subSequenceEnd) throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", index));
        if (subSequenceEnd >0 && realIndex >= subSequenceEnd) throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", index));
        return chars.charAt(realIndex);
    }

    @Override
    public String toString() {
        return (subSequenceEnd == -1) ? chars.substring(subSequenceStart) : chars.substring(subSequenceStart, subSequenceEnd);
    }

    @Override
    public CharSequence subSequence(int beginIndex, int endIndex) {
        if (beginIndex < 0) {
            throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", beginIndex));
        }
        int subLen = endIndex - beginIndex;
        if (subLen < 0) {
            throw new IndexOutOfBoundsException(String.format("Invalid indexes : %d/%d", beginIndex, endIndex));
        }
        int realStart = beginIndex + subSequenceStart;
        int realEnd = endIndex + subSequenceStart;
        if (realStart > subSequenceEnd) throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", beginIndex));
        if (realEnd > subSequenceEnd) throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", endIndex));
        return new WikiCharSequence(this, realStart, realEnd);
    }

    public String getSourceContent(String s) {
        StringBuffer res = new StringBuffer();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.getType(c) == Character.PRIVATE_USE) {
                res.append(this.getToken(c).toString());
            } else {
                res.append(c);
            }
        }
        return res.toString();
    }

    public WikiText.Token getToken(String c) {
        if (c.length() != 1) throw new RuntimeException("A token name must be a single character.");
        return this.getToken(c.charAt(0));
    }

    public WikiText.Token getToken(char c) {
        return this.characterTokenMap.get(c);
    }
}
