package org.getalp.dbnary.wiki;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.getalp.dbnary.tools.CharRange;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.Action;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.Atomize;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.Content;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.KeepAsis;
import org.getalp.dbnary.wiki.WikiSequenceFiltering.OpenContentClose;
import org.getalp.dbnary.wiki.WikiText.ExternalLink;
import org.getalp.dbnary.wiki.WikiText.Heading;
import org.getalp.dbnary.wiki.WikiText.IndentedItem;
import org.getalp.dbnary.wiki.WikiText.InternalLink;
import org.getalp.dbnary.wiki.WikiText.Template;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;

/**
 * A WikiCharSequence is a special character sequence that transforms a mediawiki code according to
 * a filter function.
 * <p>
 * The character sequence will reflect a cut in the wikitext tree. The cut is specified by a
 * filter function that takes nodes and decide if
 * <ul>
 *   <li>the node source is to be added to the sequence</li>
 *   <li>the node is to be ignored</li>
 *   <li>the node is to be added as an atomic character (from unicode private use blocks)</li>
 *   <li>the node is to be added as a sequence beginning/ending with specific open/close
 *   characters and containing a cut of its descendants forest</li>
 * </ul>.
 * </p><p>
 * Default filter function is ...
 * </p><p>
 * Created by serasset on 28/01/16.
 */
public class WikiCharSequence implements CharSequence, Cloneable {

  // Private use ranges : U+E000..U+F8FF, U+F0000..U+FFFFD and U+100000..U+10FFFD.
  public static final CharRange LISTS_RANGE = new CharRange(0xF0000, 0xF3FFF);
  public static final CharRange TEMPLATES_RANGE = new CharRange(0xF4000, 0xF7FFF);
  // External and internal links should remain contiguous AND in this block order as this property
  // allows for a better detection of general links
  public static final CharRange EXTERNAL_LINKS_RANGE = new CharRange(0xF8000, 0xFBFFF);
  public static final CharRange INTERNAL_LINKS_RANGE = new CharRange(0xFC000, 0xFFFFD);
  public static final CharRange HEADERS_RANGE = new CharRange('\uE000', '\uF8FF');

  private final StringBuffer chars;
  private final WikiContent content;

  private final Map<Integer, Token> characterTokenMap;

  private final Function<Token, Action> filter;

  private int currentOffset = 0;
  private final int subSequenceStart;
  private final int subSequenceEnd;

  private int firstAvailableListChar = LISTS_RANGE.getStart();
  private int firstAvailableTemplateChar = TEMPLATES_RANGE.getStart();
  private int firstAvailableExternalLinkChar = EXTERNAL_LINKS_RANGE.getStart();
  private int firstAvailableInternalLinkChar = INTERNAL_LINKS_RANGE.getStart();
  private int firstAvailableHeaderChar = HEADERS_RANGE.getStart();

  // public WikiCharSequence(String source) {
  // this(new WikiText(source));
  // }

  // public WikiCharSequence(String source, Function<Token, Action> filter) {
  // this(new WikiText(source), filter);
  // }

  public WikiCharSequence(WikiText wt) {
    this(wt.content());
  }

  public WikiCharSequence(WikiText wt, Function<Token, Action> filter) {
    this(wt.content(), filter);
  }

  public WikiCharSequence(WikiContent content) {
    this(content, new StringBuffer(), new HashMap<>(), new ClassBasedSequenceFilter());
  }

  public WikiCharSequence(WikiContent content, Function<Token, Action> filter) {
    this(content, new StringBuffer(), new HashMap<>(), filter);
  }

  private WikiCharSequence(WikiContent content, StringBuffer chars,
      Map<Integer, Token> characterTokenMap, Function<Token, Action> filter) {
    this.content = content;
    this.chars = chars;
    this.characterTokenMap = characterTokenMap;
    this.filter = filter;
    fillChars();
    assert this.chars.length() == currentOffset;
    this.subSequenceEnd = currentOffset;
    this.subSequenceStart = 0;
  }

  /**
   * A private clone like constructor
   * 
   * @param seq the original WikiCharSequence which hold the special char to token mapping
   * @param string the new string content of the WikiCharSequence
   */
  private WikiCharSequence(WikiCharSequence seq, String string) {
    this.content = seq.content;
    this.chars = new StringBuffer(string);
    this.characterTokenMap = seq.characterTokenMap;
    this.filter = seq.filter;
    this.subSequenceEnd = string.length();
    this.subSequenceStart = 0;
  }

  // Only used for sub sequences construction
  // Subsequences share the full sequence data but change their offset relative to subsequence
  // bounds
  private WikiCharSequence(WikiCharSequence superSeq, int subSequenceStart, int subSequenceEnd) {
    this.content = superSeq.content;
    this.chars = superSeq.chars;
    this.characterTokenMap = superSeq.characterTokenMap;
    this.filter = superSeq.filter;
    this.subSequenceStart = subSequenceStart;
    this.subSequenceEnd = subSequenceEnd;
  }



  private int allocateCharacterFor(Token tok) {
    // Assign a new char to the token
    int ch;
    if (tok instanceof Template) {
      ch = firstAvailableTemplateChar++;
      if (ch > TEMPLATES_RANGE.getEnd()) {
        throw new RuntimeException("Too many templates in current WikiText");
      }
    } else if (tok instanceof ExternalLink) {
      ch = firstAvailableExternalLinkChar++;
      if (ch > EXTERNAL_LINKS_RANGE.getEnd()) {
        throw new RuntimeException("Too many external links in current WikiText");
      }
    } else if (tok instanceof InternalLink) {
      ch = firstAvailableInternalLinkChar++;
      if (ch > INTERNAL_LINKS_RANGE.getEnd()) {
        throw new RuntimeException("Too many internal links in current WikiText");
      }
    } else if (tok instanceof Heading) {
      ch = firstAvailableHeaderChar++;
      if (ch > HEADERS_RANGE.getEnd()) {
        throw new RuntimeException("Too many headings in current WikiText");
      }
    } else if (tok instanceof IndentedItem) {
      ch = firstAvailableListChar++;
      if (ch > LISTS_RANGE.getEnd()) {
        throw new RuntimeException("Too many indented items in current WikiText");
      }
    } else {// TODO: consider all possible tokens
      throw new RuntimeException("Cannot allocate atomization char for unsupported token type.");
    }
    characterTokenMap.put(ch, tok);
    return ch;
  }

  private void fillChars() {
    fillChars(content);
  }

  private void fillChars(WikiContent content) {
    fillChars(content.tokens());
  }

  private void fillChars(List<Token> tokens) {
    for (Token token : tokens) {
      fillChars(token);
    }
  }

  private void fillChars(String s) {
    chars.append(s);
    currentOffset += s.length();
  }

  private void fillChars(Token token) {
    Action a = filter.apply(token);

    if (a instanceof OpenContentClose) {
      Function<Token, List<Token>> contentSelector = ((Content) a).getter;

      int o = allocateCharacterFor(token);
      fillChars("〔" + new String(Character.toChars(o))); // LEFT TORTOISE SHELL BRACKET (\u3014)
      fillChars(contentSelector.apply(token));
      fillChars(new String(Character.toChars(o)) + "〕"); // RIGHT TORTOISE SHELL BRACKET (\u3015)

    } else if (a instanceof Content) {
      Function<Token, List<Token>> contentSelector = ((Content) a).getter;
      fillChars(contentSelector.apply(token));
    } else if (a instanceof KeepAsis) {
      chars.append(token.getFullContent(), token.offset.start, token.offset.end);
      currentOffset += (token.offset.end - token.offset.start);
    } else if (a instanceof Atomize) {
      int o = allocateCharacterFor(token);
      fillChars(new String(Character.toChars(o)));
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
    if (0 > index) {
      throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", index));
    }
    int realIndex = index + subSequenceStart;
    if (realIndex >= subSequenceEnd) {
      throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", index));
    }
    if (subSequenceEnd > 0 && realIndex >= subSequenceEnd) {
      throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", index));
    }
    return chars.charAt(realIndex);
  }

  /**
   * Returns the character (Unicode code point) at the specified
   * index. The index refers to {@code char} values
   * (Unicode code units) and ranges from {@code 0} to
   * {@link #length()}{@code  - 1}.
   *
   * <p> If the {@code char} value specified at the given index
   * is in the high-surrogate range, the following index is less
   * than the length of this {@code WikiCharSequence}, and the
   * {@code char} value at the following index is in the
   * low-surrogate range, then the supplementary code point
   * corresponding to this surrogate pair is returned. Otherwise,
   * the {@code char} value at the given index is returned.
   *
   * @param      index the index to the {@code char} values
   * @return     the code point value of the character at the
   *             {@code index}
   * @exception  IndexOutOfBoundsException  if the {@code index}
   *             argument is negative or not less than the length of this
   *             string.
   */
  public int codePointAt(int index) {
    return chars.codePointAt(index);
  }

  @Override
  public String toString() {
    return (subSequenceEnd == -1) ? chars.substring(subSequenceStart)
        : chars.substring(subSequenceStart, subSequenceEnd);
  }

  @Override
  public CharSequence subSequence(int beginIndex, int endIndex) {
    if (beginIndex < 0) {
      throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", beginIndex));
    }
    int subLen = endIndex - beginIndex;
    if (subLen < 0) {
      throw new IndexOutOfBoundsException(
          String.format("Invalid indexes : %d/%d", beginIndex, endIndex));
    }
    int realStart = beginIndex + subSequenceStart;
    int realEnd = endIndex + subSequenceStart;
    if (realStart > subSequenceEnd) {
      throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", beginIndex));
    }
    if (realEnd > subSequenceEnd) {
      throw new IndexOutOfBoundsException(String.format("Index out of bound : %d", endIndex));
    }
    return new WikiCharSequence(this, realStart, realEnd);
  }

  public CharSequence subSequence(int beginIndex) {
    return this.subSequence(beginIndex, this.length());
  }

  public String getSourceContent(CharSequence s) {
    StringBuffer res = new StringBuffer();

    s.codePoints().forEach(c -> {
      if (Character.getType(c) == Character.PRIVATE_USE) {
        res.append(this.getToken(c).toString());
      } else {
        res.append(Character.toChars(c));
      }});
//    for (int i = 0; i < s.length(); i++) {
//      char c = s.charAt(i);
//      if (Character.getType(c) == Character.PRIVATE_USE) {
//        res.append(this.getToken(c).toString());
//      } else {
//        res.append(c);
//      }
//    }
    return res.toString();
  }

  public String getSourceContent() {
    return this.getSourceContent(this);
  }

  public Token getToken(String c) {
    if (c.codePointCount(0, c.length()) != 1) {
      throw new RuntimeException("A token name must be a single (possibly supplementary) character.");
    }
    return this.getToken(c.codePointAt(0));
  }

  public Token getToken(int c) {
    return this.characterTokenMap.get(c);
  }

  ////// UTILITY FUNCTIONS //////////////////

  public WikiCharSequence mutateString(Function<String, String> mutator) {
    String str = mutator.apply(this.toString());
    return new WikiCharSequence(this, str);
  }
}
