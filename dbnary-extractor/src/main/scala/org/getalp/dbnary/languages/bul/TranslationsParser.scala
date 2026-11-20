package org.getalp.dbnary.languages.bul

import com.typesafe.scalalogging.Logger
import org.apache.jena.rdf.model.Resource
import org.getalp.dbnary.api.IWiktionaryDataHandler

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
  val TranslationTemplate: Regex = """\{\{[tnп](?:\|([^\}\|]+)(?:\|([^\}\|]*)(?:\|([^\}]+))?)?)?\}\}""".r
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

  // A translation template may contain several comma separated forms: {{п|en|plague, pestilence}}
  def translationTemplate: Parser[List[Translation]] = matching(TranslationTemplate) ^^ {
    case TranslationTemplate(lg, wf, null) => templateForms(lg, wf)
    case TranslationTemplate(lg, anchor, wf) => templateForms(lg, wf)
  }

  private def templateForms(lg: String, wf: String): List[Translation] =
    Option(wf).toList.flatMap(_.split("[,;]")).map(_.trim).filter(_.nonEmpty).map(Translation(lg, _, "", ""))

  def links: Parser[String] = link.+ ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }

  def language: Parser[Language] = "*" ~> (languageName | languageTemplate) <~ ":" ^^ { case ln =>
    Language(ln, BulgarianLangtoCode.threeLettersCode(ln))
  }

  // parentheses that do not contain a translation (those are handled by parenAlternative)
  def parens: Parser[String] = """\((?:(?!\{\{)[^)])*\)""".r

  def italics: Parser[String] = """''.*?''""".r

  // stops before a template, so that a glued article is kept as prefix: das{{п|de|Ammoniak}}
  def simpleString = """[^'\(\{\n\*\#\,][^\s,;\*\#\n\{]*""".r

  def simpleStrings: Parser[String] = rep(simpleString) ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }


  // A translation template, possibly introduced by / or или (used as separators)
  private val NextTranslation = """\{\{[tnп]\||/\s*\{\{[tnп]\||или\s*\{\{[tnп]\|"""

  // free text usage stops before the next translation, even when no separator is given
  def usageValue: Parser[String] = rep1(italics | parens | ("""(?:(?!""" + NextTranslation + """)[^\(,;*#\n])+""").r) ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString ("")
  }

  /*
  * английски: {{п|en|authorization}} (алтерн. {{п|en|authorisation}})  ==> authorisation, with usage алтерн.
   */
  def parenAlternative: Parser[List[Translation]] = "(" ~> simpleStrings ~ translationTemplate ~ """[^)]*""".r <~ ")" ^^ {
    case note ~ trans ~ rest =>
      val usage = List(note, rest.trim).filter(_.nonEmpty).mkString("|")
      trans.foreach(_.usage = usage)
      trans
  }

  def usagePrefix: Parser[String] = parens <~ opt("""[-–]""".r) ^^ { p => p.substring(1, p.length - 1).trim }

  /*
  * немски: die {{п|de|Flosse}}  ==> Flosse, with usage prefix=die
  * английски: (под черта) - {{п|en|foot-note}}  ==> foot-note, with usage под черта
   */
  lazy val simpleTranslationValue: PackratParser[List[Translation]] = links ~ opt(usageValue) ^^ {
    case "" ~ u => Nil
    case l ~ Some(u) => List(Translation("", l, u, ""))
    case l ~ None => List(Translation("", l, "", ""))
  } | opt(usagePrefix) ~ simpleStrings ~ translationTemplate ~ opt(usageValue) ~ rep(parenAlternative) ^^ {
    case before ~ prefix ~ trans ~ after ~ alternatives =>
      val usage = (before.toList ++ Option(prefix).filter(_.nonEmpty).map("prefix=" + _) ++ after.map(_.trim))
        .filter(_.nonEmpty).mkString("|")
      trans.foreach(_.usage = usage)
      trans ::: alternatives.flatten
  } | simpleStrings ~ opt(usageValue) ^^ {
    case "" ~ u => Nil
    case l ~ Some(u) => List(Translation("", l, u, ""))
    case l ~ None => List(Translation("", l, "", ""))
  }

  // fails instead of succeeding without consuming input (avoids looping on the empty separator)
  private def progressing[T](p: Parser[T]): Parser[T] = Parser { in =>
    p(in) match {
      case Success(_, rest) if rest.offset == in.offset => Failure("no progress", in)
      case r => r
    }
  }

  /*
  * испански: [[ciudad]]
  * испански: {{п|es|Estútgart}} или {{п|es|Estucardia}}
  * китайски: {{п|zh|洋蔥}} / {{п|zh|洋葱}}
  * испански: {{п|es|lleno}} {{п|es|relleno}}   (no separator before a translation template)
   */
  def separator: Parser[String] = ("""[,;、]|/(?=\s*\{\{[tnп]\|)|или(?=\s*\{\{[tnп]\|)|(?=\{\{[tnп]\|)""").r

  lazy val simpleTranslationValues: PackratParser[List[Translation]] = simpleTranslationValue ~ rep(separator ~> progressing(simpleTranslationValue)) ^^ {
    case trans ~ moreTrans => trans ::: moreTrans.flatten
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

  // Unparsable end of line, ignored so that the remaining languages are still extracted
  def lineRest: Parser[String] = """[^\*\n][^\n]*""".r ^^ { r =>
    logger.debug("Ignored end of translation line in \"" + currentEntry + "\": " + r)
    r
  }

  def translationsForALanguage: Parser[List[Translation]] = language ~ translationValues <~ opt(lineRest) ^^ {
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

  // e.g. * ''добавете превод на друг език тук'' : ignored instead of failing the whole section
  def unparsableLine: Parser[List[Translation]] = """\*[^\n]*""".r ^^ { l =>
    logger.debug("Ignored translation line in \"" + currentEntry + "\": " + l)
    Nil
  }

  def translations: Parser[List[Translation]] = gloss ~ (translationsForALanguage | unparsableLine).* ^^ {
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