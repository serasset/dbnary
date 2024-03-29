package org.getalp.dbnary.languages.bul

import com.typesafe.scalalogging.Logger
import org.apache.jena.rdf.model.Resource
import org.getalp.dbnary.api.IWiktionaryDataHandler
import org.getalp.dbnary.languages.AbstractGlossFilter

import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator._

class TranslationsParser extends RegexParsers with PackratParsers {

  /*
  Redefine the handling of patterns so that they return the match instead of the matched string
   */
  def matching(r: Regex): Parser[Match] = (in: Input) => {
    val source = in.source
    val offset = in.offset
    val start = handleWhiteSpace(source, offset)
    r findPrefixMatchOf source.subSequence(start, source.length) match {
      case Some(matched) =>
        Success(matched,
          in.drop(start + matched.end - offset))
      case None =>
        val found = if (start == source.length()) "end of source" else "`" + source.charAt(start) + "'"
        Failure("string matching regex `" + r + "' expected but " + found + " found", in.drop(start - offset))
    }
  }

  private val logger = Logger(classOf[TranslationsParser])
  private var currentEntry: String = _
  val Link: Regex = """\[\[(?:([^:\|\]]*)|([^\]\|]*:[^\]\|]*))(?:\|([^\]]*))?\]\]""".r
  val Link2: Regex = """\[\[([^]]*)\]\]""".r
  val TranslationTemplate: Regex = """\{\{[tnп](?:\|([^\}\|]+)(?:\|([^\}\|]+)(?:\|([^\}]+))?)?)?\}\}""".r
  val TemplateWithoutArgs: Regex = """\{\{([^\}|]+)\}\}""".r

  def languageName: Regex = """[ _\p{L}]+""".r

  def languageTemplate: Parser[String] = matching(TemplateWithoutArgs) ^^ {
    case TemplateWithoutArgs(lc) => lc
  }

  def link: Parser[String] = matching(Link) ^^ {
    case Link(null, interwikilink, _) => ""
    case Link(target, null, null) => target.trim()
    case Link(target, null, value) => value.trim()
    case m => {
      logger.debug("Unhandled link structure in \"" + currentEntry + "\": " + m)
      ""
    }
  }

  def translationTemplate: Parser[Translation] = matching(TranslationTemplate) ^^ {
    case TranslationTemplate(lg, wf, null) => Translation(lg, wf, "", "")
    case TranslationTemplate(lg, anchor, wf) => Translation(lg, wf, "", "")
  }

  def links: Parser[String] = link.+ ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }

  def language: Parser[Language] = "*" ~> (languageName | languageTemplate) <~ ":" ^^ { case ln =>
    Language(ln, BulgarianLangtoCode.threeLettersCode(ln))
  }

  def parens: Parser[String] = """\(.*?\)""".r

  def italics: Parser[String] = """''.*?''""".r

  def simpleString = """[^'\(\{\n\*\#\,][^\s,;\*\#\n]*""".r

  def simpleStrings: Parser[String] = rep(simpleString) ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }


  def usageValue: Parser[String] = rep1(italics | parens | """[^\(,;*#\n]+""".r) ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString ("")
  }

  lazy val simpleTranslationValue: PackratParser[Translation] = links ~ opt(usageValue) ^^ {
    case "" ~ u => null
    case l ~ Some(u) => Translation("", l, u, "")
    case l ~ None => Translation("", l, "", "")
  } | translationTemplate ~ opt(usageValue) ^^ {
    case t ~ None => t
    case t ~ Some(s) => {
      t.usage = s
      t
    }
  } | simpleStrings ~ opt(usageValue) ^^ {
    case "" ~ u => null
    case l ~ Some(u) => Translation("", l, u, "")
    case l ~ None => Translation("", l, "", "")
  }

  /*
  * испански: [[ciudad]]
   */
  lazy val simpleTranslationValues: PackratParser[List[Translation]] = simpleTranslationValue ~ rep( """,|;""".r ~> simpleTranslationValue) ^^ {
    case trans ~ moreTrans => {
      trans :: moreTrans filter {
        _ != null
      }
    }
  }

  /*
  * английски:
  # [[city]],[[town]]
  # [[hail]]
   */
  lazy val numberedTranslationValues: PackratParser[List[Translation]] = ("""#|\*:""".r ~> simpleTranslationValues).+ ^^ {
    // TODO: keep sense number in gloss part...
    case listoflist =>
      var res: List[Translation] = Nil
      listoflist.view.zipWithIndex foreach { case (translist, index) =>
        res = translist.map({
          case trans =>
            trans.gloss = (index + 1) + "|" + trans.gloss
            trans
        }) ::: res
      }
      res
  }

  /**
    * Fallback parser for translations values. Allows ill-formed translations to be suppressed.
    *
    * @return
    */
  def garbageTranslationValues: Parser[List[Translation]] =
    """[^\#\*]*""".r ^^ {
      case "" => Nil // Simply ignore empty values.
      case s => {
        logger.debug("Parse error in language translation for \"" + currentEntry + "\": " + s)
        Nil
      }
    }

  lazy val translationValues: PackratParser[List[Translation]] = numberedTranslationValues | simpleTranslationValues | garbageTranslationValues

  def translationsForALanguage: Parser[List[Translation]] = language ~ translationValues ^^ {
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

  def gloss = """[^\*]*""".r

  def translations: Parser[List[Translation]] = gloss ~ translationsForALanguage.* ^^ {
    case null ~ list => list.flatten
    case "" ~ list => list.flatten
    case gloss ~ list => {
      list.flatten.map(t => {
        t.gloss = t.gloss + "|" + gloss
        t
      })
    }
  }

  def parseTranslations(input: String, entry: String): List[Translation] = {
    currentEntry = entry
    parseAll(translations, input) match {
      case Success(result, _) => result
      case failure: NoSuccess => {
        logger.debug("Parse error in translation for \"" + entry + "\": " + failure.msg)
        Nil
      }
    }
  }

  val TBracket = """\[([^\]]*)\]""".r

  def extractTranslations(input: String, delegate: IWiktionaryDataHandler): Unit =
    parseTranslations(input, delegate.currentPagename()).foreach {
      t => {
        t.writtenRep match {
          case TBracket(v) => delegate.registerTranslation(t.language, createGloss(t.gloss, delegate), t.usage, v)
          case _ => delegate.registerTranslation(t.language, createGloss(t.gloss, delegate), t.usage, t.writtenRep)
        }
      }
    }

  def createGloss(g: String, delegate: IWiktionaryDataHandler): Resource =
    delegate.createGlossResource(g)

}

// case class Translations(trans: List[Translation])

case class Translation(var language: String, writtenRep: String, var usage: String, var gloss: String)

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