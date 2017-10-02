package org.getalp.dbnary.wiki

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator.RegexParsers

/**
  * Created by serasset on 22/05/17.
  */
trait WikiRegexParsers extends RegexParsers {


  override protected val whiteSpace = WikiPattern.toStandardPattern("""\p{White_Space}+""").r

  /** A parser that matches a template with given name */
  def template(templateName: String): Parser[WikiText#Template] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    if (start < source.length()) {
      val c = source.charAt(start)
      if (WikiCharSequence.TEMPLATES_RANGE.contains(c)) {
        val tmpl = source.getToken(c).asInstanceOf[WikiText#Template]
        if (tmpl.getName == templateName)
          Success(tmpl, in.drop(start - offset + 1))
        else
          Failure("Template with name `" + templateName + "' expected but Template named" + tmpl.getName + " found", in.drop(start - offset))
      }
      else
        Failure("Template with name `" + templateName + "' expected but " + source.getSourceContent("" + c) + " found", in.drop(start - offset))
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
      val c = source.charAt(start)
      if (WikiCharSequence.TEMPLATES_RANGE.contains(c))
        Success(source.getToken(c).asInstanceOf[WikiText#Template], in.drop(start - offset + 1))
      else
        Failure("Template expected but " + source.getSourceContent("" + c) + " found", in.drop(start - offset))
    }
    else
      Failure("Template expected but end of source found", in.drop(start - offset))
  }

  /** A parser that matches a character satisfied given predicate */
  def wikiChar(charName: String, matches: (Char => Boolean)): Parser[WikiText#Token] = (in: Input) => {
    val source = in.source.asInstanceOf[WikiCharSequence]
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    if (start < source.length()) {
      val c = source.charAt(start)
      if (matches(c))
        Success(source.getToken(c), in.drop(start - offset + 1))
      else
        Failure(charName + " expected but " + source.getSourceContent("" + c) + " found", in.drop(start - offset))
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
    ) ^^ { case l => l.asInstanceOf[WikiText#Link] }

  /** A parser that matches any internal link */
  def internalLink(): Parser[WikiText#InternalLink] =
    wikiChar(
      "Internal link",
      c => WikiCharSequence.INTERNAL_LINKS_RANGE.contains(c)
    ) ^^ { case l => l.asInstanceOf[WikiText#InternalLink] }

  /** A parser that matches any external link */
  def externalLink(): Parser[WikiText#ExternalLink] =
    wikiChar(
      "External link",
      c => WikiCharSequence.EXTERNAL_LINKS_RANGE.contains(c)
    ) ^^ { case l => l.asInstanceOf[WikiText#ExternalLink] }

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
          val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
          Failure("String matching regex `" + r + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
      }
    }

    /** A parser that matches a literal string */
    override implicit def literal(s: String): Parser[String]
    = new Parser[String] {
      def apply(in: Input) = {
        val source = in.source.asInstanceOf[WikiCharSequence]
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
          val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
          Failure("`" + s + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
        }
      }
    }

    /** A parser that matches a regex string */
    override implicit def regex(r: Regex): Parser[String]
    = new Parser[String] {
      def apply(in: Input) = {
        val source = in.source.asInstanceOf[WikiCharSequence]
        val offset = in.offset
        val start = handleWhiteSpace(source, offset)
        r findPrefixMatchOf source.subSequence(start) match {
          case Some(matched) =>
            Success(source.subSequence(start, start + matched.end).toString,
              in.drop(start + matched.end - offset))
          case None =>
            val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
            Failure("string matching regex `" + r + "' expected but " + source.getSourceContent(found) + " found", in.drop(start - offset))
        }
      }
    }


  }
