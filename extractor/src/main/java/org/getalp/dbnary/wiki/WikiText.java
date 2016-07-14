package org.getalp.dbnary.wiki;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by serasset on 24/01/16.
 */
public class WikiText {
    private String content;
    private int startOffset;
    private int endOffset;
    private WikiContent root;

    /**
     * A segment of text identifies a substring whose first character is at
     * position start and last character is at position end-1;
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
            String c = WikiText.this.content;
            if (-1 == end) {
                if (start + 10 < c.length())
                    return c.substring(start, start + 10) + "...";
                else
                    return c.substring(start, c.length());
            } else {
                return c.substring(start, end);
            }
        }
    }

    public abstract class Token {
        Segment offset;

        public void addToken(Token t) {
        }

        public void setEndOffset(int endOffset) {
            this.offset.setEnd(endOffset);
        }

        public String toString() {
            return (null == offset) ? super.toString() : this.offset.toString();
        }

        public void addFlattenedTokens(Token t) {
            if (t instanceof WikiContent) {
                WikiContent wc = (WikiContent) t;
                for (Token token : wc.tokens) {
                    this.addToken(token);
                }
            } else if (t instanceof Template) {
                Template tmpl = (Template) t;
                this.addFlattenedTokens(tmpl.name);
                if (null != tmpl.args)
                    for (WikiContent arg : tmpl.args) {
                        this.addFlattenedTokens(arg);
                    }
            } else if (t instanceof InternalLink) {
                InternalLink l = (InternalLink) t;
                this.addFlattenedTokens(l.target);
                this.addFlattenedTokens(l.text);
            }
        }
    }

    /**
     * Upper element containing text/links/templates and comments interleaved
     */
    public class WikiContent extends Token {
        ArrayList<Token> tokens = new ArrayList<>();

        public WikiContent(int startOffset) {
            this.offset = new Segment(startOffset);
        }

        @Override
        public void addToken(Token t) {
            tokens.add(t);
        }

        public WikiEventsSequence filteredTokens(WikiEventFilter filter) {
            return new WikiEventsSequence(this, filter);
        }
    }

    public class HTMLComment extends Token {

        public HTMLComment(int startOffset) {
            this.offset = new Segment(startOffset);
        }

    }

    public class Template extends Token {
        protected WikiContent name;
        protected ArrayList<WikiContent> args;
        protected Map<String, String> parsedArgs = null;

        public Template(int startOffset) {
            this.offset = new Segment(startOffset);
            name = new WikiContent(startOffset+2);
        }

        /**
         * sets the end offset to the given position (should point to the first char of the closing "}}")
         * @param position the position of the first character of the enclosing "}}"
         */
        @Override
        public void setEndOffset(int position) {
            if (null == args) {
                this.name.setEndOffset(position);
            } else {
                args.get(args.size()-1).setEndOffset(position);
            }
            super.setEndOffset(position+2);
        }

        public void gotAPipe(int position) {
            if (null == args) {
                this.name.setEndOffset(position);
                args = new ArrayList<>();
                args.add(new WikiContent(position+1));
            } else {
                // got a new parameter separator...
                if (! args.isEmpty()) args.get(args.size()-1).setEndOffset(position);
                args.add(new WikiContent(position+1));
            }
        }

        @Override
        public void addToken(Token t) {
            if (null == args) {
                this.name.addToken(t);
            } else {
                // got a new token inside an arg...
                args.get(args.size()-1).addToken(t);
            }
        }

        public String getName() {
            return name.toString();
        }

        public Map<String, String> getParsedArgs() {
            if (parsedArgs == null) {
                parsedArgs = new HashMap<String, String>();
                if (null != args) {
                    int n = 1; // number for positional args.
                    for (int i = 0; i < args.size(); i++) {
                        String arg = args.get(i).toString();
                        if (null == arg || arg.length() == 0) continue;
                        int eq = arg.indexOf('=');
                        if (eq == -1) {
                            // There is no argument name.
                            parsedArgs.put("" + n, arg);
                            n++;
                        } else {
                            parsedArgs.put(arg.substring(0, eq), arg.substring(eq + 1));
                        }
                    }
                }
            }
            return parsedArgs;
        }

    }

    public abstract class Link extends Token {
        WikiContent target;
        WikiContent text;

        @Override
        public void addToken(WikiText.Token t) {
            if (null == this.text) {
                this.target.addToken(t);
            } else {
                this.text.addToken(t);
            }
        }
    }

    public class InternalLink extends Link {

        public InternalLink(int startOffset) {
            this.offset = new Segment(startOffset);
            this.target = new WikiContent(startOffset+2);
        }

        /**
         * sets the end offset to the given position (should point to the first char of the closing "]]")
         * @param position the position of the first character of the enclosing "]]"
         */
        @Override
        public void setEndOffset(int position) {
            super.setEndOffset(position+2);
            if (null == this.text) {
                this.target.setEndOffset(position);
            } else {
                this.text.setEndOffset(position);
            }
        }

        public void gotAPipe(int position) {
            if (null == this.text) {
                this.target.setEndOffset(position);
                this.text = new WikiContent(position + 1);
            }
        }

        public String getTarget() {
            return target.toString();
        }

        public String getLinkText() {
            if (null == this.text)
                return target.toString();
            else
                return text.toString();
        }

    }

    public class ExternalLink extends Link {

        public ExternalLink(int startOffset) {
            this.offset = new Segment(startOffset);
            this.target = new WikiContent(startOffset+1);
        }

        /**
         * sets the end offset to the given position (should point to the first char of the closing "]")
         * @param position the position of the first character of the enclosing "]"
         */
        @Override
        public void setEndOffset(int position) {
            super.setEndOffset(position+1);
            if (null == this.text) {
                this.target.setEndOffset(position);
            } else {
                this.text.setEndOffset(position);
            }
        }

        public void gotASpace(int position) {
            if (null == this.text) {
                this.target.setEndOffset(position);
                this.text = new WikiContent(position + 1);
            }
        }

    }

    public class Heading extends Token {
        int level;
        WikiContent text;

        public Heading(int position, int level) {
            this.level = level;
            this.offset = new Segment(position);
            this.text = new WikiContent(position + level);
        }

        /**
         * sets the end offset to the given position (should point to the first char of the closing "]")
         * @param position the position of the first character of the enclosing "]"
         */
        @Override
        public void setEndOffset(int position) {
            super.setEndOffset(position+1);
            this.text.setEndOffset(position);
        }

        @Override
        public void addToken(Token t) {
            this.text.addToken(t);
        }

    }

    public class ListItem extends Token {
        int level;
        WikiContent content;

        public ListItem(int position, int level) {
            this.level = level;
            this.offset = new Segment(position);
            this.content = new WikiContent(position + level);
        }

        /**
         * sets the end offset to the given position (should point to the first char of the closing "]")
         * @param position the position of the first character of the enclosing "]"
         */
        @Override
        public void setEndOffset(int position) {
            super.setEndOffset(position+1);
            this.content.setEndOffset(position);
        }

        @Override
        public void addToken(Token t) {
            this.content.addToken(t);
        }

        public WikiContent getContent() {
            return this.content;
        }

        public int getLevel() {
            return this.level;
        }

    }

    public WikiText(String content) {
        this(content, 0, content.length());
    }

    public WikiText(String content, int startOffset, int endOffset) {
        if ((startOffset < 0) || (startOffset > content.length()))
            throw new IndexOutOfBoundsException("startOffset");
        if ((endOffset < 0) || (endOffset > content.length()))
            throw new IndexOutOfBoundsException("endOffset");
        if (startOffset > endOffset)
            throw new IndexOutOfBoundsException("start > end");
        this.content = content;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    private WikiContent parse() {
        int pos = this.startOffset;
        int end = this.endOffset;
        Stack<Token> stack = new Stack<>();
        stack.push(new WikiContent(0));
        Matcher m;
        String c;
        while (pos < end) {
            if (null != (c = peekString(pos, "{{"))) {
                // Template Start
                stack.push(new Template(pos));
                pos += c.length();
            } else if (null != (c = peekString(pos, "[["))) {
                // InternalLink start
                stack.push(new InternalLink(pos));
                pos += c.length();
            } else if (null != (c = peekString(pos, "["))) {
                // External Link
                stack.push(new ExternalLink(pos));
                pos += c.length();
            } else if (null != (c = peekString(pos, "}}"))) {
                // Template End
                if (stack.peek() instanceof Template) {
                    Template t = (Template) stack.pop();
                    t.setEndOffset(pos);
                    stack.peek().addToken(t);
                } else  {
                    // consider the closing element as a simple text
                }
                pos += c.length();
            } else if (null != (c = peekString(pos, "]]"))) {
                // InternalLink end
                if (stack.peek() instanceof InternalLink) {
                    InternalLink t = (InternalLink) stack.pop();
                    t.setEndOffset(pos);
                    stack.peek().addToken(t);
                } else {
                    // consider the closing element as a simple text
                }
                pos += c.length();
            } else if (null != (c = peekString(pos, "]"))) {
                // External Link End
                if (stack.peek() instanceof ExternalLink) {
                    ExternalLink t = (ExternalLink) stack.pop();
                    t.setEndOffset(pos);
                    stack.peek().addToken(t);
                } else {
                    // consider the closing element as a simple text
                }
                pos += c.length();
            } else if (null != (c = peekString(pos, "<!--"))) {
                // HTML comment start, just pass through text ignoring everything
                HTMLComment t = new HTMLComment(pos);
                pos = pos + 4;
                while (pos != end && (null == peekString(pos, "-->"))) pos++;
                if (pos != end) pos = pos + 3;
                t.setEndOffset(pos+1);
                stack.peek().addToken(t);
            } else if (null != (m = peekPattern(pos, "^={2,6}"))) {
                //// TODO: ICICICICICICICICICI
                m.group();
                pos += m.group().length();
            } else if (null != (m = peekPattern(pos, "^\\*+"))) {
                int level = m.group().length();
                stack.push(new ListItem(pos, level));
                pos += level;
            } else if (null != (c = peekString(pos, "|"))) {
                // if in Template, it's a special char
                Token t = stack.peek();
                if (t instanceof Template) {
                    Template template = (Template) t;
                    template.gotAPipe(pos);
                } else if (t instanceof InternalLink) {
                    InternalLink template = (InternalLink) t;
                    template.gotAPipe(pos);
                }
                pos += c.length();
            } else if (null != (c = peekString(pos, " "))) {
                // if in ExternalLink, it's a special char
                Token t = stack.peek();
                if (t instanceof ExternalLink) {
                    ExternalLink template = (ExternalLink) t;
                    template.gotASpace(pos);
                }
                pos += c.length();
                //TODO Handle nowiki tags
            } else if (null != (c = peekNewline(pos))) {
                // if in ListItem, it's a closing char
                Token t = stack.peek();
                if (t instanceof ListItem) {
                    ListItem li = (ListItem) stack.pop();
                    li.setEndOffset(pos);
                    stack.peek().addToken(t);
                }
                pos += c.length();
                //TODO Handle nowiki tags
            } else {
                // Normal characters, just advance...
                pos++;
            }
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
            // In this case, we assume that unclosed elements are simple textual contents.
            Token t = stack.pop();
            stack.peek().addFlattenedTokens(t);
        }

        Token root = stack.pop();

        ((WikiContent) root).setEndOffset(end);
        return (WikiContent) root;

    }

    private String peekNewline(int pos) {
        if (pos < content.length() - 1 && content.charAt(pos) == '\r' && content.charAt(pos) == '\n') return "\r\n";
        if (pos < content.length() && content.charAt(pos) == '\n') return "\n";
        if (pos < content.length() && content.charAt(pos) == '\r') return "\r";
        return null;
    }


    public String peekString(int pos, String s) {
        int i = 0; int pi;
        int slength = s.length();
        int wtlength = content.length();
        while (i != slength && (pi = pos + i) != wtlength && s.charAt(i) == content.charAt(pi)) i++;
        if (i == slength)
            return s;
        else
            return null;
    }

    private HashMap<String, Matcher> matcherCache = new HashMap<String, Matcher>();

    private synchronized Matcher matcher(String pat) {
        Matcher m;
        if ((m = matcherCache.get(pat)) == null) {
            m = Pattern.compile(pat).matcher(content);
            matcherCache.put(pat, m);
        }
        return m;
    }

    private Matcher peekPattern(int pos, String pat) {
        Matcher m = matcher(pat);
        m.region(pos, content.length());
        if (m.lookingAt())
            return m;
        else
            return null;
    }

    public WikiContent content() {
        if (null == root) root = parse();
        return root;
    }

    public ArrayList<Token> tokens() {
        return content().tokens;
    }

    public WikiEventsSequence filteredTokens(WikiEventFilter filter) {
        return content().filteredTokens(filter);
    }

    // frequent simple access to the wiki text
    public WikiEventsSequence links() {
        ClassBasedFilter filter = new ClassBasedFilter();
        filter.allowLink();
        return filteredTokens(filter);
    }

    public WikiEventsSequence templates() {
        ClassBasedFilter filter = new ClassBasedFilter();
        filter.allowTemplates();
        return filteredTokens(filter);
    }
}
