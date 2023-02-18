package org.getalp.dbnary.languages.eng

import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.StringUtils
import org.getalp.dbnary.wiki.{WikiCharSequence, WikiRegexParsers, WikiText}

import java.util.Map.Entry
import scala.collection.convert.ImplicitConversions.`set asScala`

class DerivationsParser(page: String) extends WikiRegexParsers {

  private val pagename = page

  private val logger = Logger(classOf[DerivationsParser])
  private var currentEntry: String = null
  private var source: WikiCharSequence = null

  def derivationsAsTemplate: Parser[List[Derivation]] =
    template("""(?:col|der|rel)([12345])?(?:-u)?""".r) ^^ (tmpl => {
      val args = tmpl.cloneArgs
      // Handle col
      if (args.containsKey("title"))
        logger.debug("Non empty title in derivation template: {} || {}", args.remove("title"), pagename)
      if (!args.containsKey("lang")) { // the default language arg is arg 1, unless lang is given
        args.remove("1")
      }
      args.remove("sc")
      args.remove("sort")
      args.remove("collapse")
      val es: List[Entry[String, WikiText#WikiContent]] = args.entrySet().toList
      es.flatMap(processDerTemplateArgs)
    })

  def processDerTemplateArgs(e: Entry[String, WikiText#WikiContent]): List[Derivation] = {
    if (StringUtils.isNumeric(e.getKey)) {
      val valContent = e.getValue
      if (valContent.wikiTokens.isEmpty) { // the value is a string
        List(Derivation(valContent.toString, null))
      }
      else {
        val valueParser = new DerivationsParser(page)
        valueParser.parseDerivationValues(new WikiCharSequence(valContent), currentEntry)
      }
    }
    else {
      logger.debug("Derivation: Unexpected arg in derivation template: {} || {}", e, pagename)
      List()
    }
  }

  def derivationValue: Parser[List[Derivation]] = ".*".r ^^ (s => List(Derivation(s, null)))

  def derivationsAsLink: Parser[List[Derivation]] = link() ^^ (_ => List())

  def junk: Parser[List[Derivation]] = ".".r ^^ (_ => List())

  def derivationSection: Parser[List[Derivation]] =
    rep(derivationsAsTemplate | derivationsAsLink | junk) ^^ (l => l.flatten)


  def parseDerivations(input: WikiCharSequence, entry: String): List[Derivation] = {
    currentEntry = entry
    source = input
    parseAll(derivationSection, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => {
        logger.debug("Could not parse derivation section for \"" + entry + "\": " + failure.msg)
        Nil
      }
    }
  }

  def parseDerivationValues(input: WikiCharSequence, entry: String): List[Derivation] = {
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

