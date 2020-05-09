package org.getalp.dbnary.ita

import com.typesafe.scalalogging.Logger
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.RDF
import org.getalp.dbnary.wiki.{WikiCharSequence, WikiRegexParsers, WikiText}
import org.getalp.dbnary.{AbstractGlossFilter, IWiktionaryDataHandler}

class TranslationLineParser(page: String) extends WikiRegexParsers {

  private val pagename = page

  private val logger = Logger(classOf[TranslationLineParser])
  private var currentEntry: String = null
  private var source: WikiCharSequence = null

  val SenseNumber = """\[?((?:\s*\d+\s*[.,-]?)+)\]?""".r
  val Italics = """'''([ _\p{L}]+)'''""".r

  def languageName: Parser[Language] =
    """[ _\p{L}]+""".r ^^ {
      case ln => Language(ln, null)
    } | matching(Italics) ^^ {
      case Italics(l) => Language(l, null)
    }

  def languageTemplate: Parser[Language] = template ^^ {
    case lt => Language("", lt.getName)
  }

  def translationAsLink: Parser[String] = internalLink() ^^ {
    // ignore links to external language editions
    case l => {
      val target = l.getTargetText
      if (target.contains(":")) {
        logger.debug("Ignoring translation link " + target + " in " + currentEntry)
        ""
      } else if (target.equals("")) {
        pagename
      } else target
    }
  }

  def links = translationAsLink.+ ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }

  def language: Parser[Language] = ("""\*|:""".r.* ~> (languageName | languageTemplate) <~ ":") | "*".? ~> languageTemplate

  def parens: Parser[String] = """\(.*?\)""".r

  def italics: Parser[String] = """''.*?''""".r

  def simpleString = """[^'\(\{\n\*\#\,][^\s,;\*\#\n]*""".r

  def simpleStrings: Parser[String] = rep1(simpleString) ^^ {
    case list => list filter {
      _.nonEmpty
    } mkString (" ")
  }


  def usageValue: Parser[String] = rep1(italics | parens | """[^\(,;*#\n]+""".r) ^^ {
    case list => source.getSourceContent(list filter {
      _.nonEmpty
    } mkString (" "))
  }

  def linkRegex = """\[\[|\]\]""".r

  def getTranslation(content: WikiText#WikiContent): Translation = {
    Translation("", linkRegex.replaceAllIn(content.toString, ""), null, "")
  }

  def uniqueTranslationValue: Parser[List[Translation]] =
  // Special serbo croatian cases where both transliterations are given as links
    links ~ "/" ~ links ^^ {
      case "" ~ _ ~ _ => List(null)
      case l1 ~ _ ~ l2 => List(Translation("", l1, null, ""), Translation("", l2, null, ""))
    } | links ^^ {
      case "" => List(null)
      case l => List(Translation("", l, null, ""))
    } | template("zh-tradsem") ^^ {
      // {{zh-tradsem|[[英語]]|[[英语]]}} (yīngyǔ);
      // TODO: Should I create 2 translations (one for traditional chinese, one for simplified ?)
      // TODO: Or else ?
      case tmpl => List(getTranslation(tmpl.getArgs.get("1")), getTranslation(tmpl.getArgs.get("2")))
    } | simpleStrings ^^ {
      case s =>
        logger.debug("Ignoring translation value " + source.getSourceContent(s) + " in " + currentEntry)
        List(null)
    } | "" ^^ {
      case s => List(null)
    }


  def cleanTranslationValue: Parser[List[Translation]] =
  // Special serbo croatian cases where both transliterations are given as links
    uniqueTranslationValue ~ opt(usageValue) ^^ {
      case lt ~ None => lt
      case lt ~ Some(u) => lt map {
        case null => null
        case t: Translation => t.addUsage(u)
      }
    }

  def cleanTranslationValues: Parser[List[Translation]] = cleanTranslationValue ~ rep( """,|;""".r ~> cleanTranslationValue) ^^ {
    case null ~ null => null
    case null ~ moreTrans => moreTrans.flatten filter {
      _ != null
    }
    case trans ~ null => trans filter {
      _ != null
    }
    case trans ~ moreTrans => {
      (trans ::: moreTrans.flatten) filter {
        _ != null
      }
    }
  }

  var localGloss: String = null;
  val garbagePrefix = """:*(?:\))?(?:\s*(?:[tT]o|[Aa]|≈)\s+)?""".r

  def hint: Parser[String] = matching(SenseNumber) ^^ {
    case SenseNumber(d) => {
      TranslationLineParser.this.localGloss = d
      TranslationLineParser.this.localGloss
    }
  } | template("Term") ^^ {
    case tmpl => {
      TranslationLineParser.this.localGloss = tmpl.getArgs.get("1").toString
      TranslationLineParser.this.localGloss
    }
  } | parens ^^ {
    case ptext => {
      TranslationLineParser.this.localGloss = ptext
      TranslationLineParser.this.localGloss
    }
  }

  def hintAndTranslation: Parser[List[Translation]] = (hint.? <~ garbagePrefix) ~ cleanTranslationValue ^^ {
    case _ ~ lt => lt map {
      case null => null
      case t: Translation => t.gloss = localGloss; t
    }
  }

  def hintedTranslations: Parser[List[Translation]] = repsep(hintAndTranslation, """,|;""".r) ^^ {
    case rl => rl.flatten filter {
      _ != null
    }
  }

  def translations: Parser[List[Translation]] = hintedTranslations // keep garbage ? | simpleTranslations | garbageTranslations

  def translationLine: Parser[List[Translation]] = language ~ translations ^^ {
    case Language(name, null) ~ list => {
      // TODO: handle plain text language names
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

  def extractTranslationLine(input: WikiCharSequence, gloss: Resource, delegate: IWiktionaryDataHandler, filter: AbstractGlossFilter): Unit = {
    parseTranslationLine(input, delegate.currentLexEntry()).foreach {
      case Translation(lg, wr, null, use) => delegate.registerTranslation(lg, gloss, use, wr)
      case Translation(lg, wr, localGloss, use) => {
        val g2 = createGloss(gloss, localGloss, delegate, filter)
        delegate.registerTranslation(lg, g2, use, wr)
      }
    }
  }

  def getGlossString(g: Resource): String = {
    g match {
      case null => ""
      case gloss =>
        gloss.getProperty(RDF.value) match {
          case null => ""
          case p => p.getString
        }
    }
  }

  def createGloss(globalGloss: Resource, localGloss: String, delegate: IWiktionaryDataHandler, filter: AbstractGlossFilter): Resource =
    delegate.createGlossResource(filter.extractGlossStructure(getGlossString(globalGloss) + "|" + localGloss))

}

// case class Translations(trans: List[Translation])

case class Translation(var language: String, writtenRep: String, var gloss: String, var usage: String) {
  def addUsage(u: String): Translation = {
    this.usage = this.usage + u
    this
  }
}


case class Language(name: String, code: String)

