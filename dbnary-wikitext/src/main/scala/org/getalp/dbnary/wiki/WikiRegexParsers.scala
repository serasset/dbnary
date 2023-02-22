package org.getalp.dbnary.wiki

import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator.RegexParsers

/**
 * Created by serasset on 22/05/17.
 */
trait WikiRegexParsers extends RegexParsers {


  override protected val whiteSpace: Regex = WikiPattern.toStandardPattern("""\p{White_Space}+""").r

  /** A parser that matches a character satisfied given predicate */
  private def wikiEvent(charName: String, matches: Int => Boolean): Parser[WikiText#Token] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    if (start < source.length()) {
      val c = source.codePointAt(start)
      if (matches(c))
        Success(source.getToken(c), in.drop(start - offset + Character.charCount(c)))
      else
        Failure(charName + " expected but " + source.getSourceContent(strFromCodePoint(c)) + " found", in.drop(start - offset))
    }
    else
      Failure(charName + " expected but end of source found", in.drop(start - offset))
  }

  /** A parser that matches a character satisfied given predicate */
  private def wikiEvent(message: String, charMatches: Int => Boolean, tokenMatches: WikiText#Token => Boolean): Parser[WikiText#Token] =
    (in: Input) => {
      val source = in.source.asInstanceOf[WikiCharSequence]
      val offset = in.offset
      val start = handleWhiteSpace(source, offset)
      if (start < source.length()) {
        val c = source.codePointAt(start)
        if (charMatches(c) && tokenMatches(source.getToken(c))) {
          Success(source.getToken(c), in.drop(start - offset + Character.charCount(c)))
        } else
          Failure(message + " expected but " + source.getSourceContent(strFromCodePoint(c)) + " found", in.drop(start - offset))
      }
      else
        Failure(message + " expected but end of source found", in.drop(start - offset))
    }

  def decorated[T <: WikiText#Token](p: => Parser[T]): Parser[(Int, T)] = Parser { in =>
    p(in) match {
      case Success(t, in1) => {
        val source = in.source.asInstanceOf[WikiCharSequence]
        val start = handleWhiteSpace(source, in.offset)
        Success((source.codePointAt(start), t), in1)
      }
      case ns: NoSuccess => ns
    }
  }

  /**
   * A parser that matches a template whose name matches namePattern
   *
   * @param namePattern the regex pattern that te template should match
   * @return a Parser resulting in a Template
   */
  def template(namePattern: Regex): Parser[WikiText#Template] =
    wikiEvent("Template with name matching `" + namePattern + "'",
      c => WikiCharSequence.TEMPLATES_RANGE.contains(c),
      t => namePattern matches t.asInstanceOf[WikiText#Template].getName)
      .^^(t => t.asInstanceOf[WikiText#Template])

  /** A parser that matches a template whose name is templateName */
  def template(templateName: String): Parser[WikiText#Template] =
    wikiEvent("Template with name `" + templateName + "'",
      c => WikiCharSequence.TEMPLATES_RANGE.contains(c),
      t => templateName == t.asInstanceOf[WikiText#Template].getName)
      .^^(t => t.asInstanceOf[WikiText#Template])

  /*--------- Simple matchers on token types ------------*/

  /** A parser that matches any template */
  def template(): Parser[WikiText#Template] = wikiEvent(
    "Template",
    c => WikiCharSequence.TEMPLATES_RANGE.contains(c)
  ) ^^ (t => t.asInstanceOf[WikiText#Template])

  /** A parser that matches any link */
  def link(): Parser[WikiText#Link] =
    wikiEvent(
      "Link",
      c => WikiCharSequence.EXTERNAL_LINKS_RANGE.contains(c) || WikiCharSequence.INTERNAL_LINKS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#Link])

  /** A parser that matches any internal link */
  def internalLink(): Parser[WikiText#InternalLink] =
    wikiEvent(
      "Internal link",
      c => WikiCharSequence.INTERNAL_LINKS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#InternalLink])

  /** A parser that matches any external link */
  def externalLink(): Parser[WikiText#ExternalLink] =
    wikiEvent(
      "External link",
      c => WikiCharSequence.EXTERNAL_LINKS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#ExternalLink])

  /** A parser that matches any indented item */
  def indentedItem(): Parser[WikiText#IndentedItem] =
    wikiEvent(
      "Indented Item",
      c => WikiCharSequence.LISTS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#IndentedItem])

  /** A parser that matches any list item */
  def listItem(): Parser[WikiText#ListItem] =
    wikiEvent(
      "List Item",
      c => WikiCharSequence.LISTS_RANGE.contains(c),
      t => t.isInstanceOf[WikiText#ListItem]
    ) ^^ (l => l.asInstanceOf[WikiText#ListItem])

  /** A parser that matches any numbered list item */
  def numberedListItem(): Parser[WikiText#NumberedListItem] =
    wikiEvent(
      "External link",
      c => WikiCharSequence.LISTS_RANGE.contains(c),
      t => t.isInstanceOf[WikiText#NumberedListItem]
    ) ^^ (l => l.asInstanceOf[WikiText#NumberedListItem])

  /*
  Redefine the handling of patterns so that they return the match instead of the matched string
 */
  def matching(r: Regex): Parser[Match] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    r findPrefixMatchOf source.subSequence(start, source.length) match {
      case Some(matched) =>
        Success(matched,
          in.drop(start + matched.end - offset))
      case None =>
        val found = if (start == source.length()) "end of source" else "`" + strFromCodePoint(source.codePointAt(start)) + "'"
        Failure("String matching regex `" + r + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
    }
  }

  def strFromCodePoint(c: Int): String = new String(Character.toChars(c))

  def openClose[T](tokenParser: Parser[WikiText#Token], contentParser: Parser[T]): Parser[(WikiText#Token, T)] = Parser { in =>
    var tokenChar = 0
    (openToken ~> decorated(tokenParser)) (in) match {
      case Success((c, token), in1) =>
        tokenChar = c
        (contentParser <~ wikiEvent("Specific token char", _ == tokenChar) ~ closeToken) (in1) match {
          case Success(content, rest) => Success((token, content), rest)
          case ns: NoSuccess => ns
        }
      case ns: NoSuccess => ns
    }
  }

  def openToken: Parser[String] = literal("〔")

  def closeToken: Parser[String] = "〕"

  /** A parser that matches a literal string */
  override implicit def literal(s: String): Parser[String]
  = (in: Input) => {
    val source: WikiCharSequence = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    var i = 0
    var j = start
    while (i < s.length && j < source.length && s.charAt(i) == source.charAt(j)) {
      i += 1
      j += 1
    }
    if (i == s.length)
      Success(source.subSequence(start, j).toString, in.drop(j - offset))
    else {
      val found = if (start == source.length()) "end of source" else "`" + strFromCodePoint(source.codePointAt(start)) + "'"
      Failure("`" + s + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
    }
  }

  /** A parser that matches a regex string */
  override implicit def regex(r: Regex): Parser[String]
  = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start: Int = handleWhiteSpace(source, offset)
    r findPrefixMatchOf source.subSequence(start) match {
      case Some(matched) =>
        Success(source.getSourceContent(source.subSequence(start, start + matched.end).toString),
          in.drop(start + matched.end - offset))
      case None =>
        val found = if (start == source.length()) "end of source" else "`" + strFromCodePoint(source.codePointAt(start)) + "'"
        Failure("string matching regex `" + r + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
    }
  }

  /** A parser that matches a regex and return the matched WikiCharSequence (should be implicit ?) */
  def wikiCharSequenceMatching(r: Regex): Parser[WikiCharSequence]
  = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start: Int = handleWhiteSpace(source, offset)
    r findPrefixMatchOf source.subSequence(start) match {
      case Some(matched) =>
        Success(source.subSequence(start, start + matched.end),
          in.drop(start + matched.end - offset))
      case None =>
        val found = if (start == source.length()) "end of source" else "`" + strFromCodePoint(source.codePointAt(start)) + "'"
        Failure("string matching regex `" + r + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
    }
  }

  /** TODO: A parser that matches a regex and returns a WikiContent */

}
