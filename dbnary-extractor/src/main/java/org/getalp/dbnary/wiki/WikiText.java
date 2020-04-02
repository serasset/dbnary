package org.getalp.dbnary.wiki;

import static org.getalp.dbnary.wiki.WikiEventFilter.Action.KEEP;
import static org.getalp.dbnary.wiki.WikiEventFilter.Action.VOID;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private final Matcher protocolsMatcher;
  private final Matcher lexer;

  private Logger log = LoggerFactory.getLogger(WikiText.class);

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

    int start = -1, end = -1;

    public Segment(int start) {
      this.start = start;
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
          return c.substring(start, c.length());
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
    }

    public WikiText getWikiText() {
      return WikiText.this;
    }

    public String toString() {
      return (null == offset) ? super.toString() : this.offset.toString();
    }

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
    public Indentation asIndentation() {
      throw new IllegalStateException("Not an Indentation.");
    }
    public InternalLink asInternalLink() {
      throw new IllegalStateException("Not an InternalLink.");
    }
    public Link asLink() {
      throw new IllegalStateException("Not an Link.");
    }
    public ListItem asListItem() {
      throw new IllegalStateException("Not an ListItem.");
    }
    public NumberedListItem asNumberedListItem() {
      throw new IllegalStateException("Not an NumberedListItem.");
    }
    public Template asTemplate() {
      throw new IllegalStateException("Not an Template.");
    }
    public Text asText() {
      throw new IllegalStateException("Not an Text.");
    }
    public WikiContent asWikiContent() {
      throw new IllegalStateException("Not an WikiContent.");
    }
    public WikiSection asWikiSection() {
      throw new IllegalStateException("Not an WikiSection.");
    }
  }

  /**
   * Upper element containing text/links/templates and comments interleaved
   */
  public final class WikiContent extends Token {

    private ArrayList<Token> tokens = new ArrayList<>();

    private WikiContent(int startOffset) {
      this.offset = new Segment(startOffset);
    }

    @Override
    protected void addToken(Token t) {
      tokens.add(t);
    }

    /**
     * returns an List of wikiTokens ignoring Text tokens that may be intertwined.
     *
     * @return a List of wikiTokens (any wiki token but texts
     */
    public ArrayList<Token> wikiTokens() {
      return tokens;
    }

    /**
     * returns an List of wikiTokens including Text tokens that may be intertwined.
     *
     * @return a list of tokens (either text or wikiTokens)
     */
    public ArrayList<Token> tokens() {
      return tokens(this.offset.start);
    }

    protected ArrayList<Token> tokens(int start) {
      int size = tokens.size();
      ArrayList<Token> toks = new ArrayList<>(size * 2);
      int tindex = start;
      for (Token token : tokens) {
        if (token.offset.start > tindex) {
          toks.add(new Text(tindex, token.offset.start));
        }
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
      filter.allowHeading();
      return filteredTokens(filter);
    }

    public WikiEventsSequence headers(int level) {
      return headers().and(tok -> (((Heading) tok).getLevel() == level) ? KEEP : VOID);
      // return filteredTokens(tok -> (tok instanceof Heading && ((Heading) tok).getLevel() ==
      // level));
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

    public String getText() {
      return sourceContent.substring(this.offset.start, this.offset.end);
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
      throw new RuntimeException("Cannot add flatened tokens to HTML Comment");
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

    public String getName() {
      return name.toString();
    }

    public Map<String, String> getParsedArgs() {
      Map<String, WikiContent> args = getArgs();
      Map<String, String> argsAsString = new HashMap<>();
      for (Map.Entry<String, WikiContent> e : args.entrySet()) {
        argsAsString.put(e.getKey(), e.getValue().toString());
      }
      return argsAsString;
    }

    /**
     * return the argName/argValue Map. argNae being a String and argValue a WikiContent. When
     * iterated, the map will provide values or entries in insertion order, hence iterating over the
     * map will give args in the order they were defined.
     *
     * @return the argName/argVal map
     */
    public Map<String, WikiContent> getArgs() {
      if (parsedArgs == null) {
        parsedArgs = new LinkedHashMap<String, WikiContent>();
        if (null != args) {
          int n = 1; // number for positional args.
          for (int i = 0; i < args.size(); i++) {
            WikiContent arg = args.get(i);
            if (null == arg) {
              continue;
            }
            int spos = arg.offset.start;
            int epos = arg.offset.end;
            if (arg.tokens.size() > 0) {
              epos = arg.tokens.get(0).offset.start;
            }
            int p = spos;
            while (p < epos && sourceContent.charAt(p) != '=') {
              p++;
            }
            if (p == epos) {
              // There is no argument name.
              parsedArgs.put("" + n, arg);
              n++;
            } else {
              arg.offset.start = p + 1; // start after the = sign
              String key = sourceContent.substring(spos, p).trim();
              if (parsedArgs.containsKey(key)) {
                log.debug("Duplicate arg name | {} | in [ {} ] entry : {}", key, this.name,
                    pagename);
                // Keep the first version of the arg
              } else {
                parsedArgs.put(key, arg);
              }
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

    private boolean isCorrectExternalLink() {
      protocolsMatcher.region(this.target.offset.start, this.target.offset.end);
      return protocolsMatcher.lookingAt();
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

    private Heading(int position, int level) {
      this.level = level;
      this.offset = new Segment(position);
      this.content = new WikiContent(position + level);
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

    /**
     * sets the end offset to the given position (should point to the first char of the closing "]")
     *
     * @param position the position of the first character of the enclosing "]"
     */
    @Override
    protected void setEndOffset(int position) {
      super.setEndOffset(position);
      this.content.setEndOffset(position);
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
      this.content = new WikiContent(null == heading ? getWikiText().getStartOffset() : heading.offset.end);
      this.offset = new Segment(null == heading ? getWikiText().getStartOffset() : heading.offset.start);
    }

    public WikiText.Heading getHeading() {
      return heading;
    }

    public WikiText.WikiContent getContent() {
      return content;
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
      return "WikiSection{" +
          "offset=" + offset +
          ", heading=" + heading +
          ", content=" + content +
          '}';
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

  /** A wiki content structured as a document, i.e. with a tree structure
   * representing sections and subsections.
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
      while (! stack.isEmpty()) {
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
    protocolsMatcher = protocolPattern.matcher(sourceContent);
    lexer = lexerPattern.matcher(sourceContent);

  }

  private static StringBuffer protocols = new StringBuffer();
  private static final Pattern protocolPattern;

  static {
    protocols.append("^(?:").append("bitcoin:").append("|").append("ftp://").append("|")
        .append("ftps://").append("|").append("geo:").append("|").append("git://").append("|")
        .append("gopher://").append("|").append("http://").append("|").append("https://")
        .append("|").append("irc://").append("|").append("ircs://").append("|").append("magnet:")
        .append("|").append("mailto:").append("|").append("mms://").append("|").append("news:")
        .append("|").append("nntp://").append("|").append("redis://").append("|").append("sftp://")
        .append("|").append("sip:").append("|").append("sips:").append("|").append("sms:")
        .append("|").append("ssh://").append("|").append("svn://").append("|").append("tel:")
        .append("|").append("telnet://").append("|").append("urn:").append("|")
        .append("worldwind://").append("|").append("xmpp:").append("|").append("//").append(")");

    protocolPattern = Pattern.compile(protocols.toString());
  }

  private static StringBuffer lexerCode = new StringBuffer();
  private static final Pattern lexerPattern;

  static {
    lexerCode.append("(?<").append("OT").append(">").append("\\{\\{")
        .append(")|")
        .append("(?<").append("OIL").append(">").append("\\[\\[")
        .append(")|")
        .append("(?<").append("OEL").append(">").append("\\[")
        .append(")|")
        .append("(?<").append("CT").append(">").append("\\}\\}")
        .append(")|")
        .append("(?<").append("CIL").append(">").append("\\]\\]")
        .append(")|")
        .append("(?<").append("CEL").append(">").append("\\]")
        .append(")|")
        .append("(?<").append("OXC").append(">").append("<!--")
        .append(")|")
        .append("(?<").append("CH").append(">").append("\\={2,6}$")
        .append(")|")
        .append("(?<").append("OH").append(">").append("={2,6}")
        .append(")|")
        .append("(?<").append("OLIST").append(">").append("\\*+")
        .append(")|")
        .append("(?<").append("OINDENT").append(">").append(":+")
        .append(")|")
        .append("(?<").append("ONUMLIST").append(">").append("\\#+")
        .append(")|")
        .append("(?<").append("PIPE").append(">").append("\\|")
        .append(")|")
        .append("(?<").append("SPACE").append(">").append(" ")
        .append(")|")
        .append("(?<").append("NL").append(">").append("\r?\n|\r")
        .append(")|")
        .append("(?<").append("CHAR").append(">")
        .append(".").append(")");

    lexerPattern = Pattern.compile(lexerCode.toString(), Pattern.MULTILINE | Pattern.DOTALL);
  }

  private WikiContent parse() {
    int pos = this.startOffset;
    int end = this.endOffset;
    boolean atLineBeginning = true;
    boolean newlineFlag = false;

    Stack<Token> stack = new Stack<>();
    stack.push(new WikiContent(pos));

    while (pos < end) {
      lexer.region(pos, end);
      if (lexer.lookingAt()) {
        String g;
        if (null != (g = lexer.group("OT"))) {
          // Template Start
          stack.push(new Template(pos));
          pos += g.length();
        } else if (null != (g = lexer.group("OIL"))) {
          // InternalLink start
          stack.push(new InternalLink(pos));
          pos += g.length();
        } else if (null != (g = lexer.group("OEL"))) {
          // External Link
          stack.push(new ExternalLink(pos));
          pos += g.length();
        } else if (null != (g = lexer.group("CT"))) {
          // Template End
          // If there is a list item nested in Template, close it before ending the template
          int height = findHighestClosableTemplate(stack);
          if (height != -1) {
            for (int i = stack.size() - 1; i > height; i--) {
              if (stack.peek() instanceof Link) {
                Link t = (Link) stack.pop();
                stack.peek().addFlattenedTokens(t);
              } else if (stack.peek() instanceof ListItem) {
                ListItem li = (ListItem) stack.pop();
                li.setEndOffset(pos);
                stack.peek().addToken(li);
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
        } else if (null != (g = lexer.group("CIL")) || null != (g = lexer.group("CEL"))) {
          // InternalLink end
          if (stack.peek() instanceof InternalLink && null != lexer.group("CIL")) {
            InternalLink t = (InternalLink) stack.pop();
            t.setLinkEnd(pos);
            pos += 2;
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
          } else if (stack.peek() instanceof ExternalLink) {
            ExternalLink t = (ExternalLink) stack.pop();
            // check if the link is indeed an external link
            t.setEndOffset(pos);
            // if external link is not an URL, consider it as a text
            if (t.isCorrectExternalLink()) {
              stack.peek().addToken(t);
            } else {
              stack.peek().addFlattenedTokens(t);
            }
            pos += 1;
          } else {
            // consider the closing element as a simple text
            pos += g.length();
          }
        } else if (null != (g = lexer.group("OXC"))) {
          // HTML comment start, just pass through text ignoring everything
          HTMLComment t = new HTMLComment(pos);
          pos = pos + 4;
          while (pos != end && (null == peekString(pos, "-->"))) {
            pos++;
          }
          if (pos != end) {
            pos = pos + 3;
          }
          t.setEndOffset(pos + 1);
          stack.peek().addToken(t);
        } else if (null != (g = lexer.group("CH"))) {
          int level = g.length();
          if (stack.peek() instanceof Heading) {
            Heading h = (Heading) stack.pop();
            int ldiff = h.level - level;
            if (ldiff > 0) {
              h.level = level;
              h.content.offset.start = h.content.offset.start - ldiff;
            }
            h.setEndOffset(pos + level);
            stack.peek().addToken(h);
          }
          pos += level;
        } else if (null != (g = lexer.group("OH")) && atLineBeginning) {
          int level = g.length();
          stack.push(new Heading(pos, level));
          pos += level;
        } else if (null != (g = lexer.group("OLIST")) && atLineBeginning) {
          stack.push(new ListItem(pos, g));
          pos += g.length();
        } else if (null != (g = lexer.group("OINDENT")) && atLineBeginning) {
          stack.push(new Indentation(pos, g));
          pos += g.length();
        } else if (null != (g = lexer.group("ONUMLIST")) && atLineBeginning) {
          stack.push(new NumberedListItem(pos, g));
          pos += g.length();
        } else if (null != (g = lexer.group("PIPE"))) {
          // if in Template or InternalLink, it's a special char
          Token t = stack.peek();
          if (t instanceof Template) {
            Template template = (Template) t;
            template.gotAPipe(pos);
          } else if (t instanceof InternalLink) {
            InternalLink template = (InternalLink) t;
            template.gotAPipe(pos);
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
          // TODO Handle nowiki tags
        } else if (null != (g = lexer.group("NL"))) {
          // First close and void any token that cannot contain a newline
          while (true) {
            Token t = stack.peek();
            if (t instanceof Heading || t instanceof Link) {
              stack.pop();
              stack.peek().addFlattenedTokens(t);
              continue;
            } else {
              break;
            }
          }
          // if in ListItem, it's a closing char
          Token t = stack.peek();
          if (t instanceof IndentedItem) {
            IndentedItem li = (IndentedItem) stack.pop();
            li.setEndOffset(pos);
            stack.peek().addToken(t);
          }
          pos += g.length();
          newlineFlag = true;
          // TODO Handle nowiki tags
        } else {
          // Normal characters, just advance...
          pos++;
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
    Token lastToken = stack.peek();
    if (lastToken instanceof ListItem) {
      ListItem li = (ListItem) stack.pop();
      li.setEndOffset(pos);
      stack.peek().addToken(lastToken);
    }

    while (stack.size() > 1) {
      // error: end of wiki text while elements are being parsed
      // if in ListItem, it's a closing char
      if (lastToken instanceof ListItem) {
        ListItem li = (ListItem) stack.pop();
        li.setEndOffset(pos - 1);
        stack.peek().addToken(lastToken);
      } else {
        // In this case, we assume that unclosed elements are simple textual contents.
        Token t = stack.pop();
        stack.peek().addFlattenedTokens(t);
      }
    }

    Token root = stack.pop();

    ((WikiContent) root).setEndOffset(end);
    return (WikiContent) root;

  }

  /**
   * Return the height of the highest template that may be closed (other tokens may be closed before
   */
  private int findHighestClosableTemplate(Stack<Token> stack) {
    for (int i = stack.size() - 1; i > 0; i--) {
      if (stack.get(i) instanceof ListItem || stack.get(i) instanceof Link) {
        continue;
      } else if (stack.get(i) instanceof Template) {
        return i;
      } else {
        return -1;
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
   * returns all wikiTokens in the wikiText, that is all special media wiki constructs.
   *
   * @return a sequence of Tokens
   */
  public ArrayList<Token> wikiTokens() {
    return content().wikiTokens();
  }

  /**
   * returns all tokens in the wikiText, that is all special media wiki constructs, along with
   * special Text tokens containing textual content.
   *
   * @return a sequence of Tokens (including Text tokens)
   */
  public ArrayList<Token> tokens() {
    return content().tokens();
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
//  document structure as internal classes

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
     *
     * @param content
     * @param level
     * @deprecated
     */
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
    // sequence is of type XXXXXHCCCCCCCCChXXXHCCC where X is to be discarded, H is a header of level
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
      WikiContent sectionContent =
          content.getWikiText().new WikiContent(currentToken.offset.end);
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
