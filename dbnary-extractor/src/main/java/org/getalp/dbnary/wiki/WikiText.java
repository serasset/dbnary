package org.getalp.dbnary.wiki;

import static org.getalp.dbnary.wiki.WikiEventFilter.Action.KEEP;
import static org.getalp.dbnary.wiki.WikiEventFilter.Action.VOID;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by serasset on 24/01/16.
 */
public class WikiText {

  private final String pagename;
  public final String sourceContent;
  private final int startOffset;
  private final int endOffset;
  private WikiContent root;
  private final Matcher protocolsOrTemplateMatcher;
  private final Matcher strictProtocolsMatcher;
  private final Matcher invalidCharsMatcher;
  private final Matcher lexer;

  private final Logger log = LoggerFactory.getLogger(WikiText.class);

  public int getStartOffset() {
    return startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  /**
   * A segment of text identifies a substring whose first character is at position start and last
   * character is at position end-1;
   */
  public class Segment {

    int start, end;

    public Segment(int start) {
      this(start, -1);
    }

    public Segment(int start, int end) {
      this.start = start;
      this.end = end;
    }

    public void setEnd(int end) {
      this.end = end;
    }

    public String toString() {
      String c = WikiText.this.sourceContent;
      if (-1 == end) {
        if (start + 10 < c.length()) {
          return c.substring(start, start + 10) + "...";
        } else {
          return c.substring(start);
        }
      } else {
        return c.substring(start, end);
      }
    }
  }

  public abstract class Token implements Visitable {

    Segment offset;

    protected abstract void addToken(Token t);

    protected void setEndOffset(int endOffset) {
      this.offset.setEnd(endOffset);
    }

    protected void addFlattenedTokens(Token t) {
      // TODO: the flattened token may contain text that is of importance to this. Override this
      // method to handle such cases. The token that will receive elements of the flatten one do
      if (t instanceof WikiContent) {
        WikiContent wc = (WikiContent) t;
        for (Token token : wc.tokens) {
          this.addToken(token);
        }
      } else if (t instanceof Template) {
        Template tmpl = (Template) t;
        this.addFlattenedTokens(tmpl.name);
        if (null != tmpl.args) {
          for (WikiContent arg : tmpl.args) {
            this.addFlattenedTokens(arg);
          }
        }
      } else if (t instanceof InternalLink) {
        InternalLink l = (InternalLink) t;
        this.addFlattenedTokens(l.target);
        this.addFlattenedTokens(l.text);
      } else if (t instanceof ExternalLink) {
        ExternalLink l = (ExternalLink) t;
        this.addFlattenedTokens(l.target);
        this.addFlattenedTokens(l.text);
      }
      // know what are the elements to consider
    }

    public WikiText getWikiText() {
      return WikiText.this;
    }

    public String toString() {
      return (null == offset) ? super.toString() : this.offset.toString();
    }

    /**
     * returns the content as a String. All tokens will be rendered as their source text, without
     * any html comments that were present in the wiki source. NOTE: the toString() method will
     * return the wikiSource WITH comments.
     *
     * @return a list of tokens (either text or wikiTokens)
     */
    public String getText() {
      StringBuilder r = new StringBuilder(
          (-1 == this.offset.end ? sourceContent.length() : this.offset.end) - this.offset.start);
      this.fillText(r);
      return r.toString();
    }

    protected abstract void fillText(StringBuilder s);

    /**
     * returns the full source content that support the wikiText as its specified offsets.
     *
     * @return the MediaWiki source String
     */
    public String getFullContent() {
      return sourceContent;
    }

    // Simplifying typeCasting in Streams
    public ExternalLink asExternalLink() {
      throw new IllegalStateException("Not an ExternalLink.");
    }

    public Heading asHeading() {
      throw new IllegalStateException("Not an Heading.");
    }

    public HTMLComment asHTMLComment() {
      throw new IllegalStateException("Not an HTMLComment.");
    }

    public InternalLink asInternalLink() {
      throw new IllegalStateException("Not an InternalLink.");
    }

    public Link asLink() {
      throw new IllegalStateException("Not a Link.");
    }

    public IndentedItem asIndentedItem() {
      throw new IllegalStateException("Not an IndentedItem.");
    }

    public Indentation asIndentation() {
      throw new IllegalStateException("Not an Indentation.");
    }

    public Item asItem() {
      throw new IllegalStateException("Not an Item.");
    }

    public ListItem asListItem() {
      throw new IllegalStateException("Not a ListItem.");
    }

    public NumberedListItem asNumberedListItem() {
      throw new IllegalStateException("Not a NumberedListItem.");
    }

    public Template asTemplate() {
      throw new IllegalStateException("Not a Template.");
    }

    public Text asText() {
      throw new IllegalStateException("Not a Text.");
    }

    public WikiContent asWikiContent() {
      throw new IllegalStateException("Not a WikiContent.");
    }

    public WikiSection asWikiSection() {
      throw new IllegalStateException("Not a WikiSection.");
    }
  }

  /**
   * Upper element containing text/links/templates and comments interleaved
   */
  public final class WikiContent extends Token {

    private ArrayList<Token> tokens = new ArrayList<>();
    private List<Token> wikiTokensNoComments = null;
    private ArrayList<Token> tokensAndText = null;

    private WikiContent(int startOffset) {
      this.offset = new Segment(startOffset);
    }

    @Override
    protected void addToken(Token t) {
      tokens.add(t);
    }

    /**
     * returns all wikiTokens in the wikiText, that is all special media wiki constructs, excluding
     * html comments.
     *
     * @return a sequence of Tokens
     */
    public List<Token> wikiTokens() {
      if (wikiTokensNoComments == null) {
        wikiTokensNoComments =
            tokens.stream().filter(t -> !(t instanceof HTMLComment)).collect(Collectors.toList());
      }
      return wikiTokensNoComments;
    }

    /**
     * returns all wikiTokens in the wikiText, that is all special media wiki constructs, including
     * html comments.
     *
     * @return a sequence of Tokens
     */
    public List<Token> wikiTokensWithHtmlComments() {
      return tokens;
    }

    /**
     * returns an List of wikiTokens including Text tokens that may be intertwined. HTML comments
     * are ignored and 2 successive Texts may be found if a comment was present in the wiki source.
     *
     * @return a list of tokens (either text or wikiTokens)
     */
    public List<Token> tokens() {
      return tokens(this.offset.start, false);
    }

    /**
     * returns an List of wikiTokens including Text tokens that may be intertwined. HTML comments
     * are included in the list.
     *
     * @return a list of tokens (either text or wikiTokens)
     */
    public List<Token> tokensWithHtmlComments() {
      return tokens(this.offset.start, true);
    }

    @Override
    public void fillText(StringBuilder r) {
      for (Token t : this.tokens()) {
        t.fillText(r);
      }
    }

    protected List<Token> tokens(int start, boolean withComments) {
      int size = tokens.size();
      ArrayList<Token> toks = new ArrayList<>(size * 2);
      int tindex = start;
      for (Token token : tokens) {
        if (token.offset.start > tindex) {
          toks.add(new Text(tindex, token.offset.start));
        }
        if (withComments || !(token instanceof HTMLComment))
          toks.add(token);
        tindex = token.offset.end;
      }
      if (tindex < this.offset.end) {
        toks.add(new Text(tindex, this.offset.end));
      }
      return toks;
    }

    public WikiEventsSequence filteredTokens(WikiEventFilter filter) {
      return new WikiEventsSequence(this, filter);
    }

    public WikiSectionsSequence sections(int level) {
      return new WikiSectionsSequence(this, level);
    }

    public Text endOfContent() {
      return new Text(this.offset.end, this.offset.end);
    }

    // frequent simple access to the wiki text
    public WikiEventsSequence links() {
      ClassBasedFilter filter = new ClassBasedFilter();
      filter.allowLink();
      return filteredTokens(filter);
    }

    public WikiEventsSequence templatesOnUpperLevel() {
      ClassBasedFilter filter = new ClassBasedFilter();
      filter.allowTemplates();
      return filteredTokens(filter);
    }

    // When parsing throw templates, some of them may be found in headers, lists, etc.
    public WikiEventsSequence templates() {
      ClassBasedFilter filter = new ClassBasedFilter();
      filter.allowTemplates().enterAll();
      return filteredTokens(filter);
    }

    public WikiEventsSequence headers() {
      ClassBasedFilter filter = new ClassBasedFilter();
      // By default, enter sections so that we get headers even on a WikiDocument
      filter.allowHeading().enterSections();
      return filteredTokens(filter);
    }

    public WikiEventsSequence sections() {
      ClassBasedFilter filter = new ClassBasedFilter();
      filter.allowSections();
      return filteredTokens(filter);
    }

    public WikiEventsSequence headers(int level) {
      return headers().and(tok -> (((Heading) tok).getLevel() == level) ? KEEP : VOID);
    }

    public WikiEventsSequence headersMatching(Pattern pattern) {
      return headers()
          .and(tok -> ((pattern.matcher(((Heading) tok).getContent().toString()).matches())) ? KEEP
              : VOID);
    }

    public WikiEventsSequence headersMatching(int level, Pattern pattern) {
      return headers(level)
          .and(tok -> ((pattern.matcher(((Heading) tok).getContent().toString()).matches())) ? KEEP
              : VOID);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public WikiContent asWikiContent() {
      return this;
    }
  }

  public final class Text extends Token {

    private Text(int startOffset, int endOffset) {
      this.offset = new Segment(startOffset, endOffset);
    }

    @Override
    protected void addToken(Token t) {
      throw new RuntimeException("Cannot add tokens to Text");
    }

    @Override
    protected void setEndOffset(int endOffset) {
      throw new RuntimeException("Cannot modify Text token");
    }

    @Override
    protected void addFlattenedTokens(Token t) {
      throw new RuntimeException("Cannot add flatened tokens to Text");
    }

    @Override
    public void fillText(StringBuilder s) {
      s.append(sourceContent, this.offset.start, this.offset.end);
    }

    public Text subText(int startOffset, int endOffset) {
      return new Text(startOffset, endOffset);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Text asText() {
      return this;
    }
  }

  public final class HTMLComment extends Token {

    public HTMLComment(int startOffset) {
      this.offset = new Segment(startOffset);
    }

    @Override
    protected void addToken(Token t) {
      throw new UnsupportedOperationException("Cannot add token to HTML Comment");
    }

    @Override
    protected void addFlattenedTokens(Token t) {
      throw new RuntimeException("Cannot add flattened tokens to HTML Comment");
    }

    @Override
    public void fillText(StringBuilder s) {
      ;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public HTMLComment asHTMLComment() {
      return this;
    }
  }

  public final class Template extends Token {

    private WikiContent name;
    private ArrayList<WikiContent> args;
    private Map<String, WikiContent> parsedArgs = null;

    private Template(int startOffset) {
      this.offset = new Segment(startOffset);
      name = new WikiContent(startOffset + 2);
    }

    /**
     * sets the end offset to the given position (should point to the first char of the closing
     * "}}")
     *
     * @param position the position of the first character of the enclosing "}}"
     */
    @Override
    protected void setEndOffset(int position) {
      if (null == args) {
        this.name.setEndOffset(position);
      } else {
        args.get(args.size() - 1).setEndOffset(position);
      }
      super.setEndOffset(position + 2);
    }

    private void gotAPipe(int position) {
      if (null == args) {
        this.name.setEndOffset(position);
        args = new ArrayList<>();
      } else {
        // got a new parameter separator...
        if (!args.isEmpty()) {
          args.get(args.size() - 1).setEndOffset(position);
        }
      }
      args.add(new WikiContent(position + 1));
    }

    @Override
    protected void addToken(Token t) {
      if (null == args) {
        this.name.addToken(t);
      } else {
        // got a new token inside an arg...
        args.get(args.size() - 1).addToken(t);
      }
    }

    @Override
    public void fillText(StringBuilder r) {
      r.append("{{");
      r.append(this.name.getText());
      if (null != this.args) {
        this.args.stream().map(WikiContent::getText).forEach(s -> {
          r.append('|');
          r.append(s);
        });
      }
      r.append("}}");
    }

    public String getName() {
      return name.getText();
    }

    /**
     * return the argName/argValue Map. argName being a String and argValue a String.
     *
     * When iterated, the map will provide values or entries in insertion order, hence iterating
     * over the map will give args in the order they were defined.
     *
     * @return the argName/argVal map
     */

    public Map<String, String> getParsedArgs() {
      Map<String, WikiContent> args = getArgs();
      Map<String, String> argsAsString = new LinkedHashMap<>();
      for (Map.Entry<String, WikiContent> e : args.entrySet()) {
        argsAsString.put(e.getKey(), e.getValue().getText());
      }
      return argsAsString;
    }

    public String getParsedArg(String key) {
      return this.getParsedArgs().get(key);
    }


    /**
     * return the argName/argValue Map. argName being a String and argValue a WikiContent. When
     * iterated, the map will provide values or entries in insertion order, hence iterating over the
     * map will give args in the order they were defined.
     *
     * @return the argName/argVal map
     */
    public Map<String, WikiContent> getArgs() {
      if (parsedArgs == null) {
        parsedArgs = new LinkedHashMap<>();
        if (null != args) {
          int n = 1; // number for positional args.
          for (int i = 0; i < args.size(); i++) {
            WikiContent arg = args.get(i);
            if (null == arg) {
              continue;
            }
            // find the arg title in the leading text tokens (ignoring HTML comments)
            List<Token> argTokens = arg.tokensWithHtmlComments();
            StringBuilder key = new StringBuilder();
            WikiContent value = null;
            int j, l = argTokens.size();
            int equalSignPosition = -1;
            for (j = 0; j < l; j++) {
              Token t = argTokens.get(j);
              if (t instanceof Text) {
                int spos = t.offset.start;
                int epos = t.offset.end;
                int p = spos;
                while (p < epos && sourceContent.charAt(p) != '=') {
                  p++;
                }
                key.append(sourceContent, spos, p);
                if (p < epos) {
                  // we found an equal sign
                  equalSignPosition = p;
                  value = new WikiContent(p + 1);
                  value.setEndOffset(arg.offset.end);
                  break;
                }
              } else if (!(t instanceof HTMLComment)) {
                // We found a token that cannot belong to the arg name, there is no argname
                break;
              }
            }
            if (value != null) {
              String argname = key.toString().trim();
              if (parsedArgs.containsKey(argname)) {
                log.debug("Duplicate arg name | {} | in [ {} ] entry : {}", argname, this.name,
                    pagename);
                // Keep the first version of the arg
              } else {
                for (; j < l; j++) {
                  Token t = argTokens.get(j);
                  if (!(t instanceof Text)) {
                    value.addToken(t);
                  }
                }
                parsedArgs.put(argname, value);
              }
            } else {
              // There is no argument name.
              parsedArgs.put("" + n, arg);
              n++;
            }
          }
        }
      }
      return parsedArgs;
    }

    public WikiContent getContent() {
      WikiContent res = new WikiContent(name.offset.start);
      int end = name.offset.end;
      res.addFlattenedTokens(name);
      for (WikiText.WikiContent arg : this.args) {
        res.addFlattenedTokens(arg);
        end = arg.offset.end;
      }
      res.setEndOffset(end);
      return res;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Template asTemplate() {
      return this;
    }
  }

  public abstract class Link extends Token {

    protected WikiContent target;
    protected WikiContent text;

    @Override
    protected void addToken(WikiText.Token t) {
      if (null == this.text) {
        this.target.addToken(t);
      } else {
        this.text.addToken(t);
      }
    }

    // TODO: handle links with anchors
    public String getFullTargetText() {
      return target.toString();
    }

    public WikiContent getTarget() {
      return target;
    }

    public boolean hasAnchor() {
      return target.toString().contains("#");
    }

    public String getAnchorText() {
      String t = target.toString();
      int p = t.indexOf('#');
      return (p == -1) ? null : t.substring(p + 1);
    }

    public String getTargetText() {
      String t = target.toString();
      int p = t.indexOf('#');
      return (p == -1) ? t : t.substring(0, p);
    }

    public String getLinkText() {
      if (null == this.text) {
        return target.toString();
      } else {
        return text.toString();
      }
    }

    public WikiContent getLink() {
      return (null == this.text) ? target : text;
    }

    @Override
    public Link asLink() {
      return this;
    }
  }

  public final class InternalLink extends Link {

    protected WikiContent suffix = null;

    private InternalLink(int startOffset) {
      this.offset = new Segment(startOffset);
      this.target = new WikiContent(startOffset + 2);
    }

    protected void gotAPipe(int position) {
      if (null == this.text) {
        this.target.setEndOffset(position);
        this.text = new WikiContent(position + 1);
      }
    }

    protected void setSuffix(WikiContent suffix) {
      this.suffix = suffix;
    }

    @Override
    public String getLinkText() {
      return super.getLinkText() + ((null == suffix) ? "" : suffix);
    }

    public WikiContent getSuffix() {
      return this.suffix;
    }

    @Override
    public void fillText(StringBuilder r) {
      r.append("[[");
      r.append(this.target.getText());
      if (null != this.text) {
        r.append("|");
        r.append(this.text.getText());
      }
      r.append("]]");
      if (null != suffix)
        r.append(this.suffix.getText());
    }

    /**
     * sets the end offset of the link to the given position (should point to the first char of the
     * closing "]]")
     *
     * @param position the position of the first character of the enclosing "]]"
     */
    private void setLinkEnd(int position) {
      if (null == this.text) {
        this.target.setEndOffset(position);
      } else {
        this.text.setEndOffset(position);
      }
    }

    /**
     * Wikitext parser considers an internal link as invalid if it contains curly brace Here, we
     * authorize templates but no "free" curly braces or other token.
     *
     * @return true iff the this is a VALID internal link
     */
    public boolean isValidInternalLink() {
      // Only allow Templates and texts without curly braces in the target
      // Force sequencial filtering as the Matcher is a singleton
      boolean targetIsValid = target.tokens().stream().sequential()
          .filter(t -> !(t instanceof Template)).allMatch(t -> t instanceof Text
              && !invalidCharsMatcher.region(t.offset.start, t.offset.end).find());
      return targetIsValid;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public InternalLink asInternalLink() {
      return this;
    }
  }

  public final class ExternalLink extends Link {

    private ExternalLink(int startOffset) {
      this.offset = new Segment(startOffset);
      this.target = new WikiContent(startOffset + 1);
    }

    @Override
    public void fillText(StringBuilder r) {
      r.append("[");
      r.append(this.target.getText());
      if (null != this.text) {
        r.append(" ");
        r.append(this.text.getText());
      }
      r.append("]");
    }

    /**
     * sets the end offset to the given position (should point to the first char of the closing "]")
     *
     * @param position the position of the first character of the enclosing "]"
     */
    @Override
    protected void setEndOffset(int position) {
      super.setEndOffset(position + 1);
      if (null == this.text) {
        this.target.setEndOffset(position);
      } else {
        this.text.setEndOffset(position);
      }
    }

    private void gotASpace(int position) {
      if (null == this.text) {
        this.target.setEndOffset(position);
        this.text = new WikiContent(position + 1);
      }
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public ExternalLink asExternalLink() {
      return this;
    }
  }

  public final class Heading extends Token {

    private int level;
    private WikiContent content;
    private WikiSection section = null;

    private Heading(int position, int level) {
      this.level = level;
      this.offset = new Segment(position);
      this.content = new WikiContent(position + level);
    }

    @Override
    public void fillText(StringBuilder r) {
      r.append(StringUtils.repeat("=", this.level));
      r.append(this.content.getText());
      r.append(StringUtils.repeat("=", this.level));
    }

    /**
     * sets the end offset to the given position (should point just after the last char of the
     * closing "===...")
     *
     * @param position the position after the last character of the enclosing "===..."
     */
    @Override
    protected void setEndOffset(int position) {
      super.setEndOffset(position);
      this.content.setEndOffset(position - level);
    }

    @Override
    protected void addToken(Token t) {
      this.content.addToken(t);
    }

    public WikiContent getContent() {
      return content;
    }

    public int getLevel() {
      return level;
    }

    public WikiSection getSection() {
      if (null == section) {
        getWikiText().asStructuredDocument();
      }
      return section;
    }

    public void setSection(WikiSection section) {
      this.section = section;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Heading asHeading() {
      return this;
    }
  }

  public abstract class IndentedItem extends Token {

    protected WikiContent content;
    protected String listPrefix;
    // int level;

    protected IndentedItem(int position, String listPrefix) {
      super();
      this.listPrefix = listPrefix;
      this.offset = new Segment(position);
      this.content = new WikiContent(position + listPrefix.length());
    }

    @Override
    protected void setEndOffset(int position) {
      super.setEndOffset(position);
      this.content.setEndOffset(position);
    }

    @Override
    public void fillText(StringBuilder r) {
      r.append(this.listPrefix);
      r.append(this.content.getText());
    }

    @Override
    protected void addToken(Token t) {
      this.content.addToken(t);
    }

    public WikiContent getContent() {
      return this.content;
    }

    public int getLevel() {
      return this.listPrefix.length();
    }

    public String getListPrefix() {
      return listPrefix;
    }

    public IndentedItem asIndentedItem() {
      return this;
    }
  }

  public final class ListItem extends IndentedItem {

    public ListItem(int position, String listPrefix) {
      super(position, listPrefix);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public ListItem asListItem() {
      return this;
    }
  }

  public final class NumberedListItem extends IndentedItem {

    private NumberedListItem(int position, String listPrefix) {
      super(position, listPrefix);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public NumberedListItem asNumberedListItem() {
      return this;
    }
  }


  public final class Indentation extends IndentedItem {

    private Indentation(int position, String listPrefix) {
      super(position, listPrefix);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Indentation asIndentation() {
      return this;
    }
  }

  public final class Item extends IndentedItem {

    private Item(int position, String listPrefix) {
      super(position, listPrefix);
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public Item asItem() {
      return this;
    }
  }
  ////////////////////////////////////////////////////////////////////////
  /// View wiki text as a document structured in sections and subsections
  ////////////////////////////////////////////////////////////////////////

  public final class WikiSection extends Token {

    private WikiText.Heading heading;
    private WikiText.WikiContent content;

    private WikiSection(WikiText.Heading heading, WikiText.WikiContent content) {
      this.heading = heading;
      this.content = content;
    }

    private WikiSection(WikiText.Heading heading) {
      this.heading = heading;
      this.content =
          new WikiContent(null == heading ? getWikiText().getStartOffset() : heading.offset.end);
      this.offset =
          new Segment(null == heading ? getWikiText().getStartOffset() : heading.offset.start);
    }

    public WikiText.Heading getHeading() {
      return heading;
    }

    public WikiText.WikiContent getContent() {
      return content;
    }

    @Override
    public void fillText(StringBuilder r) {
      r.append(this.heading.getText());
      r.append(this.content.getText());
    }

    @Override
    protected void addToken(Token t) {
      this.content.addToken(t);
    }

    @Override
    protected void setEndOffset(int position) {
      super.setEndOffset(position);
      this.content.setEndOffset(position);
    }

    public int getLevel() {
      return null == heading ? 0 : heading.getLevel();
    }

    @Override
    public String toString() {
      return "WikiSection{" + "offset=" + offset + ", heading=" + heading + ", content=" + content
          + '}';
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }

    @Override
    public WikiSection asWikiSection() {
      return this;
    }
  }

  /**
   * A wiki content structured as a document, i.e. with a tree structure representing sections and
   * subsections.
   */
  public final class WikiDocument implements Visitable {

    private Deque<WikiSection> stack = new ArrayDeque<>();
    private WikiContent content;

    private WikiDocument() {
      buildDoc();
    }

    private void registerToken(Token h) {
      if (h instanceof Heading) {
        while (stack.peek().getLevel() >= h.asHeading().getLevel()) {
          WikiSection top = stack.pop();
          top.setEndOffset(h.offset.start);
        }
        WikiSection ws = new WikiSection(h.asHeading());
        h.asHeading().setSection(ws);
        stack.peek().addToken(ws);
        stack.push(ws);
      } else {
        stack.peek().addToken(h);
      }
    }

    private void buildDoc() {
      WikiText.this.content();
      stack.push(new WikiSection(null));
      WikiText.this.wikiTokens().stream().forEach(this::registerToken);
      WikiSection top = null;
      while (!stack.isEmpty()) {
        top = stack.pop();
        top.setEndOffset(WikiText.this.getEndOffset());
      }
      content = top.getContent();
    }


    public WikiContent getContent() {
      return content;
    }

    @Override
    public void accept(Visitor visitor) {
      visitor.visit(this);
    }
  }


  private static String invalidCharsInLink = "[\\{\\}]";
  private static Pattern invalidCharsPattern = Pattern.compile(invalidCharsInLink);

  ////////////////////////////////////////////////////////////////////////
  /// WikiText external methods
  ////////////////////////////////////////////////////////////////////////

  public WikiText(String sourceContent) {
    this(null, sourceContent);
  }

  public WikiText(String pagename, String sourceContent) {
    this(pagename, sourceContent, 0, sourceContent.length());
  }

  public WikiText(String pagename, String sourceContent, int startOffset, int endOffset) {
    this.pagename = pagename;
    if ((startOffset < 0) || (startOffset > sourceContent.length())) {
      throw new IndexOutOfBoundsException("startOffset");
    }
    if ((endOffset < 0) || (endOffset > sourceContent.length())) {
      throw new IndexOutOfBoundsException("endOffset");
    }
    if (startOffset > endOffset) {
      throw new IndexOutOfBoundsException("start > end");
    }
    this.sourceContent = sourceContent;
    this.startOffset = startOffset;
    this.endOffset = endOffset;
    protocolsOrTemplateMatcher = protocolOrTemplatePattern.matcher(sourceContent);
    strictProtocolsMatcher = strictProtocolPattern.matcher(sourceContent);
    invalidCharsMatcher = invalidCharsPattern.matcher(sourceContent);
    lexer = lexerPattern.matcher(sourceContent);

  }

  private static StringBuilder protocols = new StringBuilder();
  private static final Pattern protocolOrTemplatePattern;
  private static final Pattern strictProtocolPattern;

  static {
    protocols.append("^(?:").append("bitcoin:").append("|");
    protocols.append("ftp://").append("|");
    protocols.append("ftps://").append("|");
    protocols.append("geo:").append("|");
    protocols.append("git://").append("|");
    protocols.append("gopher://").append("|");
    protocols.append("http://").append("|");
    protocols.append("https://").append("|");
    protocols.append("irc://").append("|");
    protocols.append("ircs://").append("|");
    protocols.append("magnet:").append("|");
    protocols.append("mailto:").append("|");
    protocols.append("mms://").append("|");
    protocols.append("news:").append("|");
    protocols.append("nntp://").append("|");
    protocols.append("redis://").append("|");
    protocols.append("sftp://").append("|");
    protocols.append("sip:").append("|");
    protocols.append("sips:").append("|");
    protocols.append("sms:").append("|");
    protocols.append("ssh://").append("|");
    protocols.append("svn://").append("|");
    protocols.append("tel:").append("|");
    protocols.append("telnet://").append("|");
    protocols.append("urn:").append("|");
    protocols.append("worldwind://").append("|");
    protocols.append("xmpp:").append("|");
    protocols.append("//").append(")");

    strictProtocolPattern = Pattern.compile(protocols.toString());
    // We assume that a template call will generate a valid protocol
    protocols.insert(protocols.length() - 1, "|\\{\\{");
    protocolOrTemplatePattern = Pattern.compile(protocols.toString());
  }

  private static StringBuffer lexerCode = new StringBuffer();
  private static final Pattern lexerPattern;

  // TODO: Fichier, File, Image sont des préfixes de liens qui autorisent visiblement les retours à
  // la ligne…
  // TODO: Lorsqu'un [ ouvre un lien externe dans un template, les arguments du template qui suivent
  // ne seront pas analysés correctement
  // DONE: déterminer si un lien interne peut contenir des retours à la ligne (dans la partie titre
  // ou cible) --> oui, donc on les accepte.
  static {
    lexerCode.append("(?<OT>").append("\\{\\{").append(")|");
    lexerCode.append("(?<OIL>").append("\\[\\[").append(")|");
    lexerCode.append("(?<OEL>").append("\\[").append(")|");
    lexerCode.append("(?<CT>").append("\\}\\}").append(")|");
    lexerCode.append("(?<CIEL>").append("\\]\\]\\]").append(")|");
    lexerCode.append("(?<CIL>").append("\\]\\]").append(")|");
    lexerCode.append("(?<CEL>").append("\\]").append(")|");
    lexerCode.append("(?<OXC>").append("<!--").append(")|");
    lexerCode.append("(?<CH>").append("\\={2,6})(?:\\s|<!--.*?-->)*$").append("|");
    lexerCode.append("(?<OH>").append("={2,6}").append(")|");
    lexerCode.append("(?<OLIST>").append("\\*+").append(")|");
    lexerCode.append("(?<OINDENT>").append(":+").append(")|");
    lexerCode.append("(?<ONUMLIST>").append("\\#+").append(")|");
    lexerCode.append("(?<OITEM>").append(";").append(")|");
    lexerCode.append("(?<PIPE>").append("\\|").append(")|");
    lexerCode.append("(?<SPACE>").append(" ").append(")|");
    lexerCode.append("(?<NL>").append("\r?\n|\r").append(")|");
    lexerCode.append("(?<CHAR>").append(".").append(")");

    lexerPattern = Pattern.compile(lexerCode.toString(), Pattern.MULTILINE | Pattern.DOTALL);
  }

  private WikiContent parse() {
    int pos = this.startOffset;
    int end = this.endOffset;
    boolean atLineBeginning = true;
    boolean newlineFlag = false;
    int lineno = 1;

    Stack<Token> stack = new Stack<>();
    stack.push(new WikiContent(pos));

    // TODO Handle nowiki tags
    // TODO: lexical analysis check if we could use matcher.find (and not consider normal chars)
    while (pos < end) {
      lexer.region(pos, end);
      if (lexer.lookingAt()) {
        String g;
        if (null != (g = lexer.group("CHAR"))) {
          // Normal characters, just advance...
          pos++;
        } else if (null != (g = lexer.group("OT"))) {
          // Template Start
          stack.push(new Template(pos));
          pos += g.length();
        } else if (null != (g = lexer.group("OIL"))) {
          // InternalLink start
          // Warn: When the internal link contains a valid url, it is considered as an
          // external link preceded by a textual square bracket
          strictProtocolsMatcher.region(lexer.end(), end);
          if (strictProtocolsMatcher.lookingAt()) {
            stack.push(new ExternalLink(pos + 1));
          } else {
            stack.push(new InternalLink(pos));
          }
          pos += g.length();
        } else if (null != (g = lexer.group("OEL"))) {
          // External links cannot be nested, The second external link can not be valid if the first
          // is not.
          if (findHighestClosableExternalLink(stack) == -1) {
            // External Link is starting only if we peek a valid protocol after the bracket
            protocolsOrTemplateMatcher.region(lexer.end(), end);
            if (protocolsOrTemplateMatcher.lookingAt()) {
              stack.push(new ExternalLink(pos));
            }
          }
          pos += g.length();
        } else if (null != (g = lexer.group("CT"))) {
          // Template End
          // If there is a list item nested in Template, close it before ending the template
          int height = findHighestClosableTemplate(stack);
          if (height != -1) {
            int top = stack.size();
            for (int i = stack.size() - 1; i > height; i--) {
              if (stack.peek() instanceof Link) {
                Link t = (Link) stack.pop();
                log.trace("UNCLOSED LINK | {}  IN TEMPLATE {} | '{}' [{}]", t,
                    stack.get(height).asTemplate().getName(), this.pagename, lineno);
                // Can the link contain a pipe that is to be used by template ?
                stack.peek().addFlattenedTokens(t);
              } else if (stack.peek() instanceof IndentedItem) {
                closeIndentedItem(pos, stack);
              }
            }
            assert stack.peek() instanceof Template;
            Template t = (Template) stack.pop();
            t.setEndOffset(pos);
            stack.peek().addToken(t);
          } else {
            // consider the closing element as a simple text
          }
          pos += g.length();
        } else if (null != (g = lexer.group("CIEL"))) {
          // Internal AND External Link end
          int height;
          if ((height = findHighestClosableLink(stack)) != -1) {
            // Flatten pending token on top of links
            flattenStackToHeight(stack, height, lineno);
            assert stack.peek() instanceof Link;
            // Valid closing of internal link
            if (stack.peek() instanceof InternalLink) {
              pos = closeInternalLink(pos, end, stack, lineno);
            } else if (stack.peek() instanceof ExternalLink) {
              pos = closeExternalLink(pos, stack);
            } else {
              // Just a text
              // consider the closing element as a simple text
              pos += g.length();
            }
          } else {
            // Just a text
            // consider the closing element as a simple text
            pos += g.length();
          }
        } else if (null != (g = lexer.group("CIL"))) {
          // InternalLink end
          int height;
          if ((height = findHighestClosableInternalLink(stack)) != -1) {
            // Flatten pending token on top of links
            flattenStackToHeight(stack, height, lineno);
            assert stack.peek() instanceof InternalLink;
            // Valid closing of internal link
            pos = closeInternalLink(pos, end, stack, lineno);
          } else if ((height = findHighestClosableExternalLink(stack)) != -1) {
            // Flatten pending token on top of the external link
            flattenStackToHeight(stack, height, lineno);
            assert stack.peek() instanceof ExternalLink;
            // Valid closing of internal link
            pos = closeExternalLink(pos, stack);
          } else {
            // Just a text
            // consider the closing element as a simple text
            pos += g.length();
          }
        } else if (null != (g = lexer.group("CEL"))) {
          int height;
          if ((height = findHighestClosableExternalLink(stack)) != -1) {
            // Flatten pending token on top of the external link
            flattenStackToHeight(stack, height, lineno);
            assert stack.peek() instanceof ExternalLink;
            // Valid closing of internal link
            pos = closeExternalLink(pos, stack);
          } else {
            pos += g.length();
          }
        } else if (null != (g = lexer.group("OXC"))) {
          // HTML comment start, just pass through text ignoring everything
          HTMLComment t = new HTMLComment(pos);
          pos = pos + 4;
          while (pos != end && (null == peekString(pos, "-->"))) {
            char cc = sourceContent.charAt(pos);
            if (cc == '\n') {
              lineno++;
            } else if (cc == '\r') {
              if (pos + 1 < end && sourceContent.charAt(pos + 1) == '\n') {
                pos++;
              }
              lineno++;
            }
            pos++;
          }
          if (pos != end) {
            pos = pos + 3;
          }
          t.setEndOffset(pos);
          stack.peek().addToken(t);
        } else if (null != (g = lexer.group("CH"))) {
          if (stackWillCloseHeading(stack)) {
            int level = g.length();
            while (stack.peek() instanceof Link) {
              invalidateHypothesis(stack.pop(), stack);
            }
            if (stack.peek() instanceof Heading) {
              Heading h = stack.peek().asHeading();
              if (level != h.level) {
                log.trace("HEADING LEVELS MISMATCH | {} | '{}' [{}]", h, this.pagename, lineno);
              }
              closeHeading(pos, stack, level);
            } else {
              log.error("UNEXPECTED TOKEN IN HEADING\t{}\t '{}' [{}]\n", stack.peek(),
                  this.pagename, lineno);
              assert false;
            }
          }
          pos += lexer.group().length(); // The CH capture extra spaces that should be ignored.
        } else if (null != (g = lexer.group("OH"))) {
          int level = g.length();
          if (atLineBeginning) {
            stack.push(new Heading(pos, level));
          }
          pos += level;
        } else if (null != (g = lexer.group("OLIST"))) {
          if (atLineBeginning) {
            stack.push(new ListItem(pos, g));
          }
          pos += g.length();
        } else if (null != (g = lexer.group("OINDENT"))) {
          if (atLineBeginning) {
            stack.push(new Indentation(pos, g));
          }
          pos += g.length();
        } else if (null != (g = lexer.group("ONUMLIST"))) {
          if (atLineBeginning) {
            stack.push(new NumberedListItem(pos, g));
          }
          pos += g.length();
        } else if (null != (g = lexer.group("OITEM"))) {
          if (atLineBeginning) {
            stack.push(new Item(pos, g));
          }
          pos += g.length();
        } else if (null != (g = lexer.group("PIPE"))) {
          // if in Template or InternalLink, it's a special char
          Token t = stack.peek();
          if (t instanceof Template) {
            t.asTemplate().gotAPipe(pos);
          } else if (t instanceof InternalLink) {
            t.asInternalLink().gotAPipe(pos);
          } else {
            ; // It is a normal character
          }
          pos += g.length();
        } else if (null != (g = lexer.group("SPACE"))) {
          // if in ExternalLink, it's a special char
          Token t = stack.peek();
          if (t instanceof ExternalLink) {
            ExternalLink template = (ExternalLink) t;
            template.gotASpace(pos);
          }
          pos += g.length();
        } else if (null != (g = lexer.group("NL"))) {
          // First close and void any token that cannot contain a newline
          // TODO: Close lower priority elements (e.g. Links when contained in IndentedItem)
          boolean closeInternalLinks = stackWillCloseIndentedItem(stack);
          while (true) {
            Token t = stack.peek();
            if (t instanceof Heading) {
              log.trace("UNCLOSED HEADING | {} | '{}' [{}]", t, this.pagename, lineno);
              invalidateHypothesis(stack.pop(), stack);
              continue;
            } else if (t instanceof ExternalLink) {
              log.trace("UNCLOSED LINK | {} | '{}' [{}]", t, this.pagename, lineno);
              invalidateHypothesis(stack.pop(), stack);
              continue;
            } else if (closeInternalLinks && t instanceof Link) {
              log.trace("UNCLOSED LINK | {} | '{}' [{}]", t, this.pagename, lineno);
              invalidateHypothesis(stack.pop(), stack);
              continue;
            } else {
              break;
            }
          }
          // if in IndentedItem, it's a closing char
          Token t = stack.peek();
          if (t instanceof IndentedItem) {
            closeIndentedItem(pos, stack);
          }

          pos += g.length();
          newlineFlag = true;
          lineno++;
        } else {
          // Fallback that should never happen if case analysis is correct...
          assert false;
          break;
        }
      } else {
        assert false; // Avoid infinite loop
        break;
      }

      atLineBeginning = newlineFlag;
      newlineFlag = false;
    }

    // the end of text is considered as a new line... Handle it.
    // if in ListItem, it's a closing char
    while (stack.size() > 1) {
      Token t = stack.peek();
      if (t instanceof Heading) {
        log.trace("UNCLOSED HEADING | {} | '{}' [{}]", t, this.pagename, lineno);
        invalidateHypothesis(stack.pop(), stack);
        // stack.peek().addFlattenedTokens(t);
      } else if (t instanceof Link) {
        log.trace("UNCLOSED LINK | {} | '{}' [{}]", t, this.pagename, lineno);
        invalidateHypothesis(stack.pop(), stack);
        // stack.peek().addFlattenedTokens(t);
      } else if (t instanceof Template) {
        log.trace("UNCLOSED TEMPLATE | {} | '{}' [{}]", t, this.pagename, lineno);
        invalidateHypothesis(stack.pop(), stack);
        // stack.peek().addFlattenedTokens(t);
      } else if (t instanceof IndentedItem) {
        closeIndentedItem(pos, stack);
      }
    }

    Token parsedRoot = stack.pop();

    ((WikiContent) parsedRoot).setEndOffset(end);
    return (WikiContent) parsedRoot;
  }

  /**
   * check if the stack contains an indented item that will be closed by the end of line token For
   * this to be true, there must be an indented item in the stack containing only pending tokens
   * less priority (i.e. pending Links, but not pending templates)
   * 
   * @param stack the stack to be inspected
   * @return true iff the current token will close the top most indented item
   */
  private boolean stackWillCloseIndentedItem(Stack<Token> stack) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      if (stack.get(i) instanceof Link)
        continue;
      if (stack.get(i) instanceof Heading)
        continue;
      if (stack.get(i) instanceof IndentedItem)
        return true;
      return false;
    }
    return false;
  }

  /**
   * check if the stack contains a heading that will be closed by the end of heading token For this
   * to be true, there must be a heading in the stack containing only pending tokens with less
   * priority (i.e. pending Links, but not pending templates)
   * 
   * @param stack the stack to be inspected
   * @return true iff the current token will close the top most heading
   */
  private boolean stackWillCloseHeading(Stack<Token> stack) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      if (stack.get(i) instanceof Link)
        continue;
      if (stack.get(i) instanceof Heading)
        return true;
      return false;
    }
    return false;
  }

  private static final Pattern newlinePattern = Pattern.compile("\r?\n|\r");
  private static final Pattern closingHeadingPattern =
      Pattern.compile("\\={2,6}(?:\\s|<!--.*?-->)*$");
  private Matcher closeHeadingMatcher = closingHeadingPattern.matcher("");
  private Matcher newlineMatcher = newlinePattern.matcher("");

  /**
   * Invalidate the hypothesis in the stack The Hypothesis is a Token that has been created and for
   * which we determine that it is broken. To avoid backtracking as much as possible, we use the
   * elements that were already parsed in this hypothesis. These elements should now be associated
   * with the hypothesis at the top of the stack. As the top of the stack may find a closing element
   * in the invalidated hypothesis, it may itself be completed (or invalidated) and popped from the
   * stack.
   *
   * @param hypothesis the token that is invalidated
   * @param stack the stack on which this token's inner elements should be added.
   */
  private void invalidateHypothesis(Token hypothesis, Stack<Token> stack) {
    // Create a WikiContent to hold all inner tokens
    WikiContent tokens = new WikiContent(hypothesis.offset.start);
    tokens.addFlattenedTokens(hypothesis);
    invalidateHypothesis(tokens, stack);
  }

  private void invalidateHypothesis(WikiContent content, Stack<Token> stack) {
    if (stack.peek() instanceof IndentedItem) {
      List<Token> innerTokens = content.tokens();
      int i;
      for (i = 0; i < innerTokens.size(); i++) {
        Token inner = innerTokens.get(i);
        if (inner instanceof Text) {
          newlineMatcher.reset(inner.asText().getText());
          if (newlineMatcher.find()) {
            // The newline closes the IndentedItem.
            closeIndentedItem(newlineMatcher.start() + inner.offset.start, stack);
            WikiContent remainingContent =
                new WikiContent(newlineMatcher.end() + inner.offset.start);
            fillContentWithTokensFrom(innerTokens, i, remainingContent);
            invalidateHypothesis(remainingContent, stack);
            break;
          }
        } else {
          stack.peek().addToken(inner);
        }
      }
    } else if (stack.peek() instanceof Heading) {
      List<Token> innerTokens = content.tokens();
      int i;
      for (i = 0; i < innerTokens.size(); i++) {
        Token inner = innerTokens.get(i);
        if (inner instanceof Text) {
          closeHeadingMatcher.reset(inner.asText().getText());
          if (closeHeadingMatcher.find()) {
            // The newline closes the IndentedItem.
            closeHeading(closeHeadingMatcher.start() + inner.offset.start, stack,
                closeHeadingMatcher.group(0).length());
            WikiContent remainingContent =
                new WikiContent(closeHeadingMatcher.end() + inner.offset.start);
            fillContentWithTokensFrom(innerTokens, i, remainingContent);
            invalidateHypothesis(remainingContent, stack);
            break;
          } else {
            stack.peek().addToken(inner);
          }
        }
      }
    } else {
      for (Token inner : content.wikiTokens()) {
        stack.peek().addToken(inner);
      }
    }
  }

  private void fillContentWithTokensFrom(List<Token> innerTokens, int i,
      WikiContent remainingContent) {
    Token inner;
    for (i++; i < innerTokens.size(); i++) {
      inner = innerTokens.get(i);
      if (!(inner instanceof Text))
        remainingContent.addToken(inner);
    }
  }

  private void closeHeading(int pos, Stack<Token> stack, int level) {
    Heading h = stack.pop().asHeading();
    int ldiff = h.level - level;
    if (ldiff > 0) {
      h.level = level;
      h.content.offset.start = h.content.offset.start - ldiff;
    }
    h.setEndOffset(pos + level);
    stack.peek().addToken(h);
  }

  private int closeExternalLink(int pos, Stack<Token> stack) {
    ExternalLink t = (ExternalLink) stack.pop();
    t.setEndOffset(pos);
    stack.peek().addToken(t);
    pos += 1;
    return pos;
  }

  private void flattenStackToHeight(Stack<Token> stack, int height, int lineno) {
    for (int i = stack.size() - 1; i > height; i--) {
      Token t = stack.pop();
      log.trace("UNCLOSED TOKEN | {}  IN LINK {} | '{}' [{}]", t, stack.get(height), this.pagename,
          lineno);
      stack.peek().addFlattenedTokens(t);
    }
  }

  private int closeInternalLink(int pos, int end, Stack<Token> stack, int lineno) {
    InternalLink t = (InternalLink) stack.pop();
    t.setLinkEnd(pos);
    pos += 2;
    if (t.isValidInternalLink()) {
      int linkEnd = pos;
      while (pos != end && Character.isLetter(sourceContent.charAt(pos))) {
        pos++;
      }
      if (pos != linkEnd) {
        WikiContent suffixContent = new WikiContent(linkEnd);
        suffixContent.setEndOffset(pos);
        t.setSuffix(suffixContent);
      }
      t.setEndOffset(pos);
      stack.peek().addToken(t);
    } else {
      // The link is invalid, consider it as a text
      log.trace("INVALID INTERNAL LINK | {} | '{}' [{}]", t, this.pagename, lineno);
      stack.peek().addFlattenedTokens(t);
    }
    return pos;
  }

  private void closeIndentedItem(int pos, Stack<Token> stack) {
    IndentedItem li = (IndentedItem) stack.pop();
    li.setEndOffset(pos);
    stack.peek().addToken(li);
  }

  /**
   * Return the height of the highest template that may be closed (other tokens may be closed before
   */
  private int findHighestClosableTemplate(Stack<Token> stack) {
    for (int i = stack.size() - 1; i > 0; i--) {
      if (stack.get(i) instanceof IndentedItem || stack.get(i) instanceof Link) {
        continue;
      } else if (stack.get(i) instanceof Template) {
        return i;
      } else {
        return -1;
      }
    }
    return -1;
  }

  /**
   * Return the height of the highest template that may be closed (other tokens may be closed before
   */
  private int findHighestClosableInternalLink(Stack<Token> stack) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      if (stack.get(i) instanceof InternalLink) {
        return i;
      }
    }
    return -1;
  }

  private int findHighestClosableExternalLink(Stack<Token> stack) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      if (stack.get(i) instanceof Template) {
        return -1; // A template has always priority over a link
      }
      if (stack.get(i) instanceof ExternalLink) {
        return i;
      }
    }
    return -1;
  }

  private int findHighestClosableLink(Stack<Token> stack) {
    for (int i = stack.size() - 1; i >= 0; i--) {
      if (stack.get(i) instanceof Link) {
        return i;
      }
    }
    return -1;
  }

  private String peekString(int pos, String s) {
    int i = 0;
    int pi;
    int slength = s.length();
    int wtlength = sourceContent.length();
    while (i != slength && (pi = pos + i) != wtlength && s.charAt(i) == sourceContent.charAt(pi)) {
      i++;
    }
    if (i == slength) {
      return s;
    } else {
      return null;
    }
  }

  public WikiContent content() {
    if (null == root) {
      root = parse();
    }
    return root;
  }

  /**
   * returns all wikiTokens in the wikiText, that is all special media wiki constructs, excluding
   * html comments.
   *
   * @return a sequence of Tokens
   */
  public List<Token> wikiTokens() {
    return content().wikiTokens();
  }

  /**
   * returns all wikiTokens in the wikiText, that is all special media wiki constructs, including
   * html comments.
   *
   * @return a sequence of Tokens
   */
  public List<Token> wikiTokensWithHtmlComments() {
    return content().wikiTokensWithHtmlComments();
  }

  /**
   * returns all tokens in the wikiText, that is all special media wiki constructs, along with
   * special Text tokens containing textual content.
   *
   * @return a sequence of Tokens (including Text tokens)
   */
  public List<Token> tokens() {
    return content().tokens();
  }

  /**
   * returns all tokens in the wikiText, that is all special media wiki constructs, along with
   * special Text tokens containing textual content. The List will include Html Comments
   *
   * @return a sequence of Tokens (including Text tokens)
   */
  public List<Token> tokensWithHtmlComments() {
    return content().tokensWithHtmlComments();
  }

  public Text endOfContent() {
    return content().endOfContent();
  }

  public WikiEventsSequence filteredTokens(WikiEventFilter filter) {
    return content().filteredTokens(filter);
  }

  // frequent simple access to the wiki text
  public WikiEventsSequence links() {
    return content().links();
  }

  public WikiEventsSequence templatesOnUpperLevel() {
    return content().templatesOnUpperLevel();
  }

  public WikiEventsSequence templates() {
    return content().templates();
  }

  public WikiEventsSequence headers() {
    return content().headers();
  }

  public WikiEventsSequence headers(int level) {
    return content().headers(level);
  }

  public WikiEventsSequence headersMatching(Pattern pattern) {
    return content().headersMatching(pattern);
  }

  public WikiEventsSequence headersMatching(int level, Pattern pattern) {
    return content().headersMatching(level, pattern);
  }

  public WikiSectionsSequence sections(int level) {
    return content().sections(level);
  }

  private WikiDocument doc = null;

  public WikiDocument asStructuredDocument() {
    if (null == doc) {
      doc = new WikiDocument();
    }
    return doc;
  }

  // TODO: make all creation/parsing time methods private and add wiki section iterator and
  // document structure as internal classes

  private String wikiTextString = null;

  @Override
  public String toString() {
    if (null == wikiTextString) {
      wikiTextString = sourceContent.substring(startOffset, endOffset);
    }
    return wikiTextString;
  }

  public static class LevelBasedWikiSectionsIterator implements Iterator<WikiSection> {

    int level;
    WikiContent content;
    Iterator<Token> baseIterator;
    Token currentToken = null;

    /**
     * @param content the content from which we get the sections
     * @param level the expected level
     * @deprecated
     */
    @Deprecated
    public LevelBasedWikiSectionsIterator(WikiContent content, int level) {
      this.content = content;
      this.level = level;
      this.baseIterator = content.tokens().iterator();
      init();
      advanceToNextHeading();
    }


    // My model 1 primitives...
    public void init() {
      if (baseIterator.hasNext()) {
        currentToken = baseIterator.next();
      }
    }

    public boolean eof() {
      return currentToken == null;
    }

    public void advance() {
      if (!baseIterator.hasNext()) {
        currentToken = null;
      } else {
        currentToken = baseIterator.next();
      }
    }

    // Collect next element and prepare for the following
    // sequence is of type XXXXXHCCCCCCCCChXXXHCCC where X is to be discarded, H is a header of
    // level
    // n and h is a higher level header
    // init state is ^currentToken
    // final state is ^currentToken
    private void advanceToNextHeading() {
      while (!eof() && !isOpeningHeading(currentToken)) {
        advance();
      }
      // either at eof or on an opening heading
    }

    public boolean isOpeningHeading(Token tok) {
      return tok instanceof Heading && ((Heading) tok).getLevel() == level;
    }

    public boolean isClosingHeading(Token tok) {
      return tok instanceof Heading && ((Heading) tok).getLevel() <= level;
    }

    @Override
    public boolean hasNext() {
      return !eof();
    }

    @Override
    public WikiSection next() {
      if (eof()) {
        throw new NoSuchElementException("No remaining section in this wikitext.");
      }
      Heading sectionHeading = (Heading) currentToken;
      WikiContent sectionContent = content.getWikiText().new WikiContent(currentToken.offset.end);
      advance();
      while (!eof() && !isClosingHeading(currentToken)) {
        sectionContent.addToken(currentToken);
        advance();
      }
      if (eof()) {
        sectionContent.setEndOffset(content.getWikiText().getEndOffset());
      } else {
        sectionContent.setEndOffset(currentToken.offset.start);
      }
      advanceToNextHeading();
      return content.getWikiText().new WikiSection(sectionHeading, sectionContent);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
