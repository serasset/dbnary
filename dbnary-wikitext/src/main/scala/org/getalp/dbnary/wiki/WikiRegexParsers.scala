package org.getalp.dbnary.wiki

import java.util
import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator.RegexParsers

/**
  * Created by serasset on 22/05/17.
  */
trait WikiRegexParsers extends RegexParsers {


  override protected val whiteSpace: Regex = WikiPattern.toStandardPattern("""\p{White_Space}+""").r

  /** A parser that matches a template with given name */
  def template(templateName: String): Parser[WikiText#Template] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    if (start < source.length()) {
      val c = source.codePointAt(start)
      if (WikiCharSequence.TEMPLATES_RANGE.contains(c)) {
        val tmpl = source.getToken(c).asInstanceOf[WikiText#Template]
        if (tmpl.getName == templateName)
          Success(tmpl, in.drop(start - offset + Character.charCount(c)))
        else
          Failure("Template with name `" + templateName + "' expected but Template named" + tmpl.getName + " found", in.drop(start - offset))
      }
      else
        Failure("Template with name `" + templateName + "' expected but " + source.getSourceContent(strFromCodePoint(c)) + " found", in.drop(start - offset))
    }
    else
      Failure("Template with name `" + templateName + "' expected but end of source found", in.drop(start - offset))
  }

  /** A parser that matches any template */
  def template(): Parser[WikiText#Template] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    if (start < source.length()) {
      val c = source.codePointAt(start)
      if (WikiCharSequence.TEMPLATES_RANGE.contains(c))
        Success(source.getToken(c).asInstanceOf[WikiText#Template], in.drop(start - offset + Character.charCount(c)))
      else
        Failure("Template expected but " + source.getSourceContent(strFromCodePoint(c)) + " found", in.drop(start - offset))
    }
    else
      Failure("Template expected but end of source found", in.drop(start - offset))
  }

  /** A parser that matches a character satisfied given predicate */
  def wikiChar(charName: String, matches: Int => Boolean): Parser[WikiText#Token] = (in: Input) => {
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

  /*--------- Simple matchers on token types ------------*/
  /** A parser that matches any link */
  def link(): Parser[WikiText#Link] =
    wikiChar(
      "Link",
      c => WikiCharSequence.EXTERNAL_LINKS_RANGE.contains(c) || WikiCharSequence.INTERNAL_LINKS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#Link])

  /** A parser that matches any internal link */
  def internalLink(): Parser[WikiText#InternalLink] =
    wikiChar(
      "Internal link",
      c => WikiCharSequence.INTERNAL_LINKS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#InternalLink])

  /** A parser that matches any external link */
  def externalLink(): Parser[WikiText#ExternalLink] =
    wikiChar(
      "External link",
      c => WikiCharSequence.EXTERNAL_LINKS_RANGE.contains(c)
    ) ^^ (l => l.asInstanceOf[WikiText#ExternalLink])

  /*
  Redefine the handling of patterns so that they return the match instead of the matched string
 */
  def matching(r: Regex): Parser[Match] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    (r findPrefixMatchOf (source.subSequence(start, source.length))) match {
      case Some(matched) =>
        Success(matched,
          in.drop(start + matched.end - offset))
      case None =>
        val found = if (start == source.length()) "end of source" else "`" + strFromCodePoint(source.codePointAt(start)) + "'"
        Failure("String matching regex `" + r + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
    }
  }

  def strFromCodePoint(c: Int): String =  new String(Character.toChars(c))

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

  /** A parser that matches a regex and return the matched WikiCharSequence (should be implicit ?)*/
  def gwikiCharSequenceMatching(r: Regex): Parser[WikiCharSequence]
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
