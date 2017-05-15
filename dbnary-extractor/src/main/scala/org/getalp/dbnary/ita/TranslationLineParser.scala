package org.getalp.dbnary.ita

import com.hp.hpl.jena.rdf.model.Resource
import grizzled.slf4j.Logger
import org.getalp.dbnary.wiki.{WikiCharSequence, WikiPattern, WikiText}
import org.getalp.dbnary.{AbstractGlossFilter, IWiktionaryDataHandler, StructuredGloss}

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator._

class TranslationLineParser extends RegexParsers {

  /*
  Redefine the handling of patterns so that they return the match instead of the matched string
   */
  def matching(r: Regex): Parser[Match] = (in: Input) => {
    val source = in.source
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    (r findPrefixMatchOf (source.subSequence(start, source.length))) match {
      case Some(matched) =>
        Success(matched,
          in.drop(start + matched.end - offset))
      case None =>
        val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
        Failure("string matching regex `" + r + "' expected but " + found + " found", in.drop(start - offset))
    }
  }

  private val logger = Logger(classOf[TranslationLineParser])
  private var currentEntry: String = null
  private var source: WikiCharSequence = null

  val LinkRegex = """\[\[(?:([^:\|\]]*)|([^\]\|]*:[^\]\|]*))(?:\|([^\]]*))?\]\]""".r
  val Link2 = """\[\[([^]]*)\]\]""".r
  val TranslationTemplate = """\{\{[tnп](?:\|([^\}\|]+)(?:\|([^\}\|]+)(?:\|([^\}]+))?)?)?\}\}""".r
  val TemplateWithoutArgs = """\{\{([^\}|]+)\}\}""".r
  val Template = WikiPattern.toStandardPattern("""\p{Template}""").r
  val Link = WikiPattern.toStandardPattern("""\p{InternalLink}""").r
  val SenseNumber = """\[\s*(\d+)\s*\]""".r

  def languageName: Parser[Language] = """[ _\p{L}]+""".r ^^ {
    case ln => Language(ln, null)
  }

  def languageTemplate: Parser[Language] = Template ^^ {
    case lc => Language("", source.getToken(lc).asInstanceOf[WikiText#Template].getName)
  }

  def link: Parser[String] = Link ^^ {
    // ignore links to external language editions
    case l => {
      val target = source.getToken(l).asInstanceOf[WikiText#InternalLink].getTargetText
      if (target.contains(":")) {
        logger.debug("Ignoring link " + target + " in " + currentEntry )
        ""
      } else target

    }
  }

//  def translationTemplate: Parser[Translation] = matching(TranslationTemplate) ^^ {
//    case TranslationTemplate(lg, wf, null) => Translation(lg, wf, "")
//    case TranslationTemplate(lg, anchor, wf) => Translation(lg, wf, "")
//  }

  def links = link.+ ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }

  def language: Parser[Language] = "*" ~> (languageName | languageTemplate) <~ ":"

  def parens: Parser[String] = """\(.*?\)""".r

  def italics: Parser[String] = """''.*?''""".r

  def simpleString = """[^'\(\{\n\*\#\,][^\s,;\*\#\n]*""".r

  def simpleStrings: Parser[String] = rep(simpleString) ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }


  def usageValue: Parser[String] = rep1(italics | parens | """[^\(,;*#\n]+""".r) ^^ {
    case list => source.getSourceContent(list filter {
      _.nonEmpty
    } mkString (""))
  }

  def cleanTranslationValue: Parser[List[Translation]] =
  // Special serbo croatian cases where both transliterations are given as links
    links ~ "/" ~ links ~ opt(usageValue) ^^{
      case "" ~ _ ~ _ ~ _ => List(null)
      case l1 ~ _ ~ l2 ~ Some(u) => List(Translation("", l1, null, u), Translation("", l2, null, u))
      case l1 ~ _ ~ l2 ~ None => List(Translation("", l1, null, ""), Translation("", l2, null, ""))
    } | links ~ opt(usageValue) ^^ {
    case "" ~ _ => List(null)
    case l ~ Some(u) => List(Translation("", l, null, u))
    case l ~ None => List(Translation("", l, null, ""))
  }
  // before simple strings
  //  | translationTemplate ~ opt(usageValue) ^^ {
  // case t ~ None => List(t)
  // case t ~ Some(s) => {
  //  t.usage = s
  //  List(t)
  //}
//}

  def translationValue: Parser[List[Translation]] =
    cleanTranslationValue |
    simpleStrings ~ opt(usageValue) ^^ {
    case s ~ u =>
      logger.trace("Ignoring translation value: " + s + "/" + u + " | line : " + source.getSourceContent +  "in " + currentEntry)
      List(null)
  }

 def cleanTranslationValues: Parser[List[Translation]] = cleanTranslationValue ~ rep( """,|;""".r ~> cleanTranslationValue) ^^ {
    case null ~ null => null
    case null ~ moreTrans => moreTrans.flatten filter { _ != null }
    case trans ~ null => trans filter { _ != null }
    case trans ~ moreTrans => {
      (trans ::: moreTrans.flatten) filter { _ != null }
    }
 }

  def simpleTranslations: Parser[List[Translation]] = translationValue ~ rep( """,|;""".r ~> translationValue) ^^ {
    case trans ~ moreTrans => {
      (trans ::: moreTrans.flatten) filter { _ != null }
    }
  }
    /**
    * Fallback parser for translations values. Allows ill-formed translations to be suppressed.
    *
    * @return
    */
  def garbageTranslations: Parser[List[Translation]] =
    """[^\#\*]*""".r ^^ {
      case "" => Nil // Simply ignore empty values.
      case s => {
        logger.debug("Parse error in language translation for \"" + currentEntry + "\": " + s)
        Nil
      }
    }

  def senseNumber: Parser[String] = matching(SenseNumber) ^^{
    case SenseNumber(d) => d
  }

  def glossAndTranslations: Parser[List[Translation]] = senseNumber ~ cleanTranslationValues ^^ {
    case n ~ l => l.map(t => {t.gloss = n; t})
  }

  def numberedTranslations: Parser[List[Translation]] = glossAndTranslations ~ rep(""",|;""".r ~> glossAndTranslations) ^^{
    case nl ~ rl => (nl ::: rl.flatten) filter {_ != null}
  }

  def translations: Parser[List[Translation]] = numberedTranslations | simpleTranslations | garbageTranslations

  def translationLine: Parser[List[Translation]] = language ~ translations ^^ {
    case Language(name, null) ~ list => {
      logger.debug("Unhandled language name in \"" + currentEntry + "\": " + name)
      Nil
    }
    case Language(_, code) ~ list =>
      list.map(t => {
        t.language = code
        t
      })
  }


  def parseTranslationLine(input: WikiCharSequence, entry: String): List[Translation] = {
    currentEntry = entry
    source = input
    parseAll(translationLine, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => {
        logger.debug("Parse error in translation for \"" + entry + "\": " + failure.msg)
        Nil
      }
    }
  }

  def extractTranslationLine(input: WikiCharSequence, gloss: String, delegate: IWiktionaryDataHandler, filter: AbstractGlossFilter): Unit =
  {
    val g = createGloss(gloss, delegate, filter)
    parseTranslationLine(input, delegate.currentLexEntry()).foreach {
      case Translation(lg, wr, null, use) => delegate.registerTranslation(lg, g, use, wr)
      case Translation(lg, wr, num, use) => {
        val g2 = delegate.createGlossResource(new StructuredGloss(num, gloss))
        delegate.registerTranslation(lg, g2, use, wr)
      }
    }
  }

  def createGloss(g: String, delegate: IWiktionaryDataHandler, filter: AbstractGlossFilter): Resource =
    delegate.createGlossResource(filter.extractGlossStructure(g))

}

// case class Translations(trans: List[Translation])

case class Translation(var language: String, writtenRep: String, var gloss: String, var usage: String)

case class Language(name: String, code: String)

/*
* английски:
# [[city]],[[town]]
# [[hail]]
* арабски: [[]]
* арменски: [[]]
* африкаанс: [[]]
* белоруски: [[]]
* гръцки: [[πόλη]]
* датски: [[]]
* есперанто:
# [[urbo]]
# [[]]
* естонски: [[]]
* иврит: [[]]
* индонезийски: [[]]
* ирландски: [[]]
* исландски: [[]]
* испански: [[ciudad]]
* италиански: [[città]]
* китайски: [[]]
* корейски: [[]]
* латвийски: [[]]
* латински: [[]]
* литовски: [[]]
 */