package org.getalp.dbnary.languages.eng

import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.StringUtils
import org.getalp.dbnary.wiki.{WikiCharSequence, WikiRegexParsers, WikiText}

import java.util.Map.Entry
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class DerivationsParser(page: String) extends WikiRegexParsers {

  private val pagename = page

  protected val logger: Logger = Logger(classOf[DerivationsParser])
  protected var currentEntry: String = _
  protected var source: WikiCharSequence = _

  protected def derivationsAsTemplate: Parser[List[Derivation]] =
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

  protected def processDerTemplateArgs(k: String, v: WikiText#WikiContent): List[Derivation] = {
    if (StringUtils.isNumeric(k)) {
      val valContent = v
      if (valContent.wikiTokens.isEmpty) { // the value is a string
        List(Derivation(valContent.toString, null))
      }
      else {
        val valueParser = new DerivationsParser(page)
        valueParser.parseDerivationValues(new WikiCharSequence(valContent), currentEntry)
      }
    }
    else {
      logger.debug("Derivation: Unexpected arg in derivation template: {} -> {} || {}", k, v, pagename)
      List()
    }
  }

  protected def derivationValue: Parser[List[Derivation]] = repsep(
    derivationsAsLink | derivationLinkAsTemplate | derivationAsPlainText,
    "or|\\p{Punct}*".r
  ) ^^ (_.flatten)

  protected def derivationAsPlainText: Parser[List[Derivation]] = wikiCharSequenceMatching(".+".r) ^^ (
    s => {
      logger.debug("Derivation Value (Plain Text): {} || {}", s.getSourceContent, pagename)
      List(Derivation(s.getSourceContent, null))
    })

  protected def derivationLinkAsTemplate: Parser[List[Derivation]] = linkTemplate | junkTemplate

  protected def linkTemplate: Parser[List[Derivation]] = template("[lL](ink)?".r) ^^ (
    t => {
      logger.debug("Derivation Value (link Template): {} || {}", t.toString, pagename)
      val args = t.cloneParsedArgs().asScala
      args.remove("1") // removing language declaration
      args.remove("2") // removing target value as it will be taken directly from the template
      if (args.contains("4")) args.addOne("t" -> args("4"))
      args.remove("4")
      List(Derivation(t.getParsedArg("2"), mapAsString(args)))
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

  protected def derivationsAsLink: Parser[List[Derivation]] = internalLink() ^^ (
    l => {
      if (l.getTargetText startsWith "Category") {
        // Ignore trailing Category links
        List.empty
      } else if (l.getTargetText == l.getLinkText)
        List(Derivation(l.getTargetText, null))
      else
        List(Derivation(l.getTargetText, "form=" + l.getLinkText))
    })

  protected def junk: Parser[List[Derivation]] = ".".r ^^ (s => {
    logger.debug("Derivation (Junk): {} || {}", s, pagename)
    List.empty
  })

  protected def derivationSection: Parser[List[Derivation]] =
    rep(derivationsAsTemplate | derivationsAsLink | junk) ^^ (l => l.flatten)


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

  protected def parseDerivationValues(input: WikiCharSequence, entry: String): List[Derivation] = {
    currentEntry = entry
    source = input
    parseAll(derivationValue, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => {
        logger.debug("Could not parse derivation values for \"" + entry + "\": " + failure.msg)
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

