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
    IndentPrinter printer = new IndentPrinter();
    printer.println("Document :");
    printer.incrementIndent();
    printContentForrest(printer, doc.getContent());
    printer.decrementIndent();
    printer.flush();
  }

  public static void printContentForrest(WikiContent content) {
    printContentForrest(new PrintWriter(System.out), content);
  }

  public static void printContentForrest(PrintWriter stream, WikiContent content) {
    IndentPrinter printer = new IndentPrinter(stream);
    content.tokens().stream().forEach(t -> printToken(printer, t));
    printer.flush();
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
      this(new PrintWriter(System.out), "  ");
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
