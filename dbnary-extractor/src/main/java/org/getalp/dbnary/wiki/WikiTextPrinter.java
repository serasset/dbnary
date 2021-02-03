package org.getalp.dbnary.wiki;

import java.io.PrintStream;
import java.io.PrintWriter;
import org.apache.commons.text.WordUtils;
import org.getalp.dbnary.wiki.WikiText.Token;
import org.getalp.dbnary.wiki.WikiText.WikiContent;
import org.getalp.dbnary.wiki.WikiText.WikiDocument;
import org.getalp.dbnary.wiki.WikiText.WikiSection;

public class WikiTextPrinter {

  public static void printDocumentTree(WikiDocument doc) {
    printDocumentTree(System.out, doc);
  }

  public static void printDocumentTree(PrintStream out, WikiDocument doc) {
    IndentPrinter printer = new IndentPrinter(out);
    printer.println("Document :");
    printer.incrementIndent();
    printContentForrest(printer, doc.getContent());
    printer.decrementIndent();
    printer.flush();
  }

  public static void printTextTree(WikiText text) {
    printTextTree(System.out, text);
  }

  public static void printTextTree(PrintStream out, WikiText text) {
    IndentPrinter printer = new IndentPrinter(out);
    printer.println("[Content :");
    printer.incrementIndent();
    printContentForrest(printer, text.content());
    printer.decrementIndent();
    printer.println("]");
    printer.flush();
  }

  public static void printContentForrest(WikiContent content) {
    printContentForrest(new PrintWriter(System.out), content);
  }

  public static void printContentForrest(PrintWriter stream, WikiContent content) {
    printContentForrest(new IndentPrinter(stream), content);
  }

  private static void printContentForrest(IndentPrinter printer, WikiContent content) {
    content.tokens().stream().forEach(t -> printToken(printer, t));
  }

  public static void printToken(IndentPrinter printer, Token token) {
    if (token instanceof WikiSection) {
      printSectionTree(printer, token.asWikiSection());
    } else {
      printer.printIndent();
      printer.print("+ ");
      printer.print(token.getClass().getCanonicalName());
      printer.print(" : ");
      printer.println(WordUtils.abbreviate(token.toString().trim(), 10, 20, "..."));
    }
  }

  public static void printSectionTree(IndentPrinter printer, WikiSection section) {
    printer.printIndent();
    printer.print("+ Section Heading [");
    printer.print(String.valueOf(section.getHeading().getLevel()));
    printer.print("] :");
    printer
        .println(WordUtils.abbreviate(section.getHeading().getContent().toString(), 10, 20, "..."));
    printer.incrementIndent();
    printContentForrest(printer, section.getContent());
    printer.decrementIndent();
  }

  public static class IndentPrinter {

    private int indentLevel;
    private String indent;
    private PrintWriter out;

    public IndentPrinter() {
      this(System.out);
    }

    public IndentPrinter(PrintStream out) {
      this(new PrintWriter(out), "  ");
    }

    public IndentPrinter(PrintWriter out) {
      this(out, "  ");
    }

    public IndentPrinter(PrintWriter out, String indent) {
      this.out = out;
      this.indent = indent;
    }

    public void println(Object value) {
      out.print(value.toString());
      out.println();
    }

    public void println(String text) {
      out.print(text);
      out.println();
    }

    public void print(String text) {
      out.print(text);
    }

    public void printIndent() {
      for (int i = 0; i < indentLevel; i++) {
        out.print(indent);
      }
    }

    public void println() {
      out.println();
    }

    public void incrementIndent() {
      ++indentLevel;
    }

    public void decrementIndent() {
      --indentLevel;
    }

    public int getIndentLevel() {
      return indentLevel;
    }

    public void setIndentLevel(int indentLevel) {
      this.indentLevel = indentLevel;
    }

    public void flush() {
      out.flush();
    }
  }
}
