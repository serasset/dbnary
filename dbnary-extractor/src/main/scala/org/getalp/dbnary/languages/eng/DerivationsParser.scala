package org.getalp.dbnary.languages.eng

import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.StringUtils
import org.getalp.dbnary.wiki.{WikiCharSequence, WikiPattern, WikiRegexParsers, WikiText}

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

class DerivationsParser(page: String) extends WikiRegexParsers {

  private val pagename = page

  protected val logger: Logger = Logger(classOf[DerivationsParser])
  protected var currentEntry: String = _
  protected var source: WikiCharSequence = _

  protected def derivationsAsDerTemplate: Parser[List[Derivation]] = {
    template("""(?:col|der|rel)([12345])?(?:-u)?""".r) ^^ (tmpl => {
      val args = tmpl.cloneArgs.asScala
      // Handle col
      if (args.contains("title"))
        logger.debug("Non empty title in derivation template: {} || {}", args.remove("title"), pagename)
      if (!args.contains("lang")) { // the default language arg is arg 1, unless lang is given
        args.remove("1")
      }
      args.remove("sc")
      args.remove("sort")
      args.remove("collapse")
      //val es: List[Entry[String, WikiText#WikiContent]] = args.entrySet().toList
      args.flatMap(tuple => processDerTemplateArgs(tuple._1, tuple._2)).toList
    })
  }

  protected def processDerTemplateArgs(k: String, v: WikiText#WikiContent): List[Derivation] = {
    if (StringUtils.isNumeric(k)) {
      val valContent = v
      val valueParser = new DerivationsParser(page)
      valueParser.parseDerivationValues(new WikiCharSequence(valContent), currentEntry)
    }
    else {
      logger.debug("Derivation: Unexpected arg in derivation template: {} -> {} || {}", k, v, pagename)
      List()
    }
  }

  protected def derivationNotes: Parser[List[String]] = rep(plainNotes | inlineModifiers)

  protected def plainNotes: Parser[String] =
    "\\([^)]*\\)".r ^^ (s => {
      logger.debug("Derivations: ignoring notes `{}` in {}", s, pagename)
      s
    })

  protected def inlineModifiers: Parser[String] =
    "<[^>]*>".r ^^ (s => {
      logger.debug("Derivations: ignoring notes `{}` in {}", s, pagename)
      s
    })

  protected def derivationValue: Parser[List[Derivation]] = derivationValueSequence | junkValue

  protected def junkValue: Parser[List[Derivation]] = wikiCharSequenceMatching(".*".r) ^^ (s => {
    logger.debug("Ignoring derivation value (Junk Value): `{}` in {}", s.getSourceContent(s), pagename)
    List.empty
  })

  protected def derivationValueSequence: Parser[List[Derivation]] = (
    repsep(
      (derivationsAsLink | derivationLinkAsTemplate | derivationAsPlainText) <~ derivationNotes,
      "or|\\p{Punct}*".r
    ) <~ "\\p{Punct}*".r) ^^ (_.flatten)

  protected def derivationAsPlainText: Parser[List[Derivation]] = wikiCharSequenceMatching(("[^" + WikiPattern.RESERVED + "(<]+").r) ^^ (
    s => {
      logger.debug("Derivation Value (Plain Text): {} || {}", s.getSourceContent, pagename)
      List(Derivation(s.getSourceContent, null))
    })

  protected def derivationLinkAsTemplate: Parser[List[Derivation]] =
    linkTemplate | vernTemplate | wTemplate | junkTemplate

  protected def linkTemplate: Parser[List[Derivation]] = template("[lL](ink)?".r) ^^ (
    t => {
      val args = t.cloneParsedArgs().asScala
      args.remove("1") // removing language declaration
      args.remove("2") // removing target value as it will be taken directly from the template
      if (args.contains("4")) args.addOne("t" -> args("4"))
      args.remove("4")
      List(Derivation(t.getParsedArg("2"), mapAsString(args)))
    })

  protected def vernTemplate: Parser[List[Derivation]] = template("vern(acular)?".r) ^^ (
    t => {
      val args = t.cloneParsedArgs().asScala
      if (args.contains("2")) args.addOne("plural" -> args("2"))
      args.remove("lang")
      args.remove("novern")
      args.remove("nopedia")
      if (args.contains("pl")) args.addOne("plural" -> (args("1") + args("pl")))
      args.remove("1") // removing target value as it will be taken directly from the template
      List(Derivation(t.getParsedArg("1"), mapAsString(args)))
    })

  protected def wTemplate: Parser[List[Derivation]] = template("[Ww]".r) ^^ (
    t => {
      val args = t.cloneParsedArgs().asScala
      if (!args.contains("1"))
        List.empty
      else {
        var target = args.getOrElse("1", "")
        if (args.contains("2")) {
          args.addOne("wp" -> target)
          target = args("2")
          args.remove("2")
        }
        args.remove("1") // removing target value as it will be taken directly from the template
        List(Derivation(target, mapAsString(args)))
      }
    })

  protected def mapAsString(m: mutable.Map[String, String]): String = {
    if (m.isEmpty)
      null
    else
      m.addString(new mutable.StringBuilder, "|").toString()
  }

  protected def junkTemplate: Parser[List[Derivation]] = template() ^^ (
    s => {
      logger.debug("Derivation Value ignored (Junk Template): {} || {}", s.toString, pagename)
      List.empty
    })

  // TODO: check if links have prefixes (e.g. [[w:XXX]] points to wikipedia)
  protected val ignoredNameSpaces: Regex =
    (for (n <- List("CATEGORY", "IMAGE", "THESAURUS"); s <- List(":" + n + ":", n + ":")) yield s).mkString("|").r
  protected val honoredNameSpaces: Regex = "W:|:W:".r


  protected def derivationsAsLink: Parser[List[Derivation]] = internalLink() ^^ (
    l => {
      val target = l.getTargetText.toUpperCase()

      if ((ignoredNameSpaces findPrefixOf target).isDefined) {
        List.empty
      } else {
        val nameSpace: Option[String] = honoredNameSpaces findPrefixOf target
        val targetText = l.getTargetText.substring(nameSpace.getOrElse("").length)
        if (targetText == l.getLinkText)
          List(Derivation(targetText, null))
        else
          List(Derivation(targetText, "form=" + l.getLinkText))
      }
    })

  protected def derivationAsListItem: Parser[List[Derivation]] =
    openClose(listItem(), derivationValue) ^^ {
      case (_, content) => content
    }

  protected def junk: Parser[List[Derivation]] = knownJunk | unknownJunk

  protected def knownJunk: Parser[List[Derivation]] = "----".r ^^ (s => {
    List.empty
  })

  protected def unknownJunk: Parser[List[Derivation]] = ".".r ^^ (s => {
    logger.debug("Derivation (Junk): {} || {}", s, pagename)
    List.empty
  })

  protected def derivationSection: Parser[List[Derivation]] =
    rep(derivationsAsDerTemplate | derivationsAsLink | derivationAsListItem | junk) ^^ (l => l.flatten)


  protected def parseDerivations(input: WikiCharSequence, entry: String): List[Derivation] = {
    currentEntry = entry
    source = input
    parseAll(derivationSection, input) match {
      case Success(result, _) => result
      case failure: NoSuccess =>
        logger.debug("Could not parse derivation section for \"" + entry + "\": " + failure.msg)
        Nil
    }
  }

  // TODO: handle special formatting
  //  e.g. [[alphanumeric]][[alphanumerical|(-al)]] <small>{{q|by clipping}}</small>
  protected def parseDerivationValues(input: WikiCharSequence, entry: String): List[Derivation] = {
    currentEntry = entry
    source = input
    parseAll(derivationValue, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => {
        logger.debug("Could not parse derivation values {} for \"{}\": {}", source.getSourceContent(source), entry, failure.msg)
        Nil
      }
    }
  }

  def extractDerivations(input: WikiCharSequence, delegate: WiktionaryDataHandler): Unit = {
    parseDerivations(input, delegate.currentPagename()).foreach {
      case Derivation(target, note) => delegate.registerDerivation(target, note)
    }
  }
}

case class Derivation(target: String, var note: String) {
  def addNote(u: String): Derivation = {
    this.note = this.note + u
    this
  }
}


case class Language(name: String, code: String)

