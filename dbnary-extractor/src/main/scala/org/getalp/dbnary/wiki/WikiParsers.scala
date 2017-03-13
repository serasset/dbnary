package org.getalp.dbnary.wiki

/**
  * Created by serasset on 27/01/17.
  */

import scala.language.implicitConversions
import scala.util.matching.Regex
import scala.util.matching.Regex.Match
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input._

/** The ''most important'' differences between `RegexParsers` and
  * [[scala.util.parsing.combinator.Parsers]] are:
  *
  *  - `Elem` is defined to be [[scala.Char]]
  *  - There's an implicit conversion from [[java.lang.String]] to `Parser[String]`,
  * so that string literals can be used as parser combinators.
  *  - There's an implicit conversion from [[scala.util.matching.Regex]] to `Parser[String]`,
  * so that regex expressions can be used as parser combinators.
  *  - The parsing methods call the method `skipWhitespace` (defaults to `true`) and, if true,
  * skip any whitespace before each parser is called.
  *  - Protected val `whiteSpace` returns a regex that identifies whitespace.
  *
  * For example, this creates a very simple calculator receiving `String` input:
  *
  * {{{
  *  object Calculator extends RegexParsers {
  *    def number: Parser[Double] = """\d+(\.\d*)?""".r ^^ { _.toDouble }
  *    def factor: Parser[Double] = number | "(" ~> expr <~ ")"
  *    def term  : Parser[Double] = factor ~ rep( "*" ~ factor | "/" ~ factor) ^^ {
  *      case number ~ list => (number /: list) {
  *        case (x, "*" ~ y) => x * y
  *        case (x, "/" ~ y) => x / y
  *      }
  *    }
  *    def expr  : Parser[Double] = term ~ rep("+" ~ log(term)("Plus term") | "-" ~ log(term)("Minus term")) ^^ {
  *      case number ~ list => list.foldLeft(number) { // same as before, using alternate name for /:
  *        case (x, "+" ~ y) => x + y
  *        case (x, "-" ~ y) => x - y
  *      }
  *    }
  *
  *    def apply(input: String): Double = parseAll(expr, input) match {
  *      case Success(result, _) => result
  *      case failure : NoSuccess => scala.sys.error(failure.msg)
  *    }
  *  }
  * }}}
  */
trait WikiParsers extends Parsers {

  type Elem = WikiText#Token

  protected val whiteSpace = """\s+""".r

  def skipWhitespace = whiteSpace.toString.length > 0

  /** Method called to handle whitespace before parsers.
    *
    * It checks `skipWhitespace` and, if true, skips anything
    * matching `whiteSpace` starting from the current offset.
    *
    * @param source The input being parsed.
    * @param offset The offset into `source` from which to match.
    * @return The offset to be used for the next parser.
    */
  protected def handleWhiteSpace(source: CharSequence, offset: Int, length: Int): Int =
    if (skipWhitespace)
      (whiteSpace findPrefixMatchOf (new SubSequence(source, offset, length))) match {
        case Some(matched) => offset + matched.end
        case None => offset
      }
    else
      offset

  protected def handleWhitespace(in: Reader[WikiText#Token]): Reader[WikiText#Token] =
    if (skipWhitespace) {
      in.first match {
        case t: WikiText#Text =>
          val sc = in.source
          val offset = in.offset
          val start = handleWhiteSpace(sc, offset, t.offset.end - offset)
          if (start > offset)
            in.drop(start - offset)
          else
            in
        case _ => in
      }
    } else {
      in
    }


  /** A parser that matches a literal string */
  implicit def literal(s: String): Parser[WikiText#Token] = new Parser[WikiText#Token] {
    def apply(input: Input) = {
      val in: Input = handleWhitespace(input)
      val offset = in.offset // character offset

      in.first match {
        case t: WikiText#Text =>
          val sc = in.source
          var i = 0
          var j = offset
          while (i < s.length && j < t.offset.end && s.charAt(i) == sc.charAt(j)) {
            i += 1
            j += 1
          }
          if (i == s.length)
            Success(t.subText(offset, j), in.drop(j - offset))
          else {
            val found = if (offset == t.offset.end) "end of source" else "`" + sc.charAt(offset) + "'"
            Failure("`" + s + "' expected but " + found + " found", in)
          }
        case _ => Failure("No textual content at this offet", in)
      }
    }
  }

  /** A parser that matches a regex string */
  implicit def regex(r: Regex): Parser[WikiText#Token] = new Parser[WikiText#Token] {
    def apply(input: Input) = {
      val in: Input = handleWhitespace(input)
      val offset = in.offset
      val source = in.source

      in.first match {
        case t: WikiText#Text =>

          r findPrefixMatchOf new SubSequence(source, offset, t.offset.end - offset) match {
            case Some(matched) =>
              Success(t.subText(offset, offset + matched.end),
                in.drop(matched.end))
            case None =>
              val found = if (offset == t.offset.end) "end of source" else "`" + source.charAt(offset) + "'"
              Failure("string matching regex `" + r + "' expected but " + found + " found", in)
          }
        case _ => Failure("No textual content at this offset", in)
      }
    }
  }


  /** A parser that matches a regex string and returns the match instead of the matched string */
  def matching(r: Regex): Parser[Match] = (input: Input) => {
    val in: Input = handleWhitespace(input)
    val offset = in.offset
    val source = in.source
    in.first match {
      case t: WikiText#Text =>
        (r findPrefixMatchOf new SubSequence(source, offset, t.offset.end - offset)) match {
          case Some(matched) =>
            Success(matched,
              in.drop(matched.end))
          case None =>
            val found = if (offset == source.length()) "end of source" else "`" + source.charAt(offset) + "'"
            Failure("string matching regex `" + r + "' expected but " + found + " found", in)
        }
      case _ => Failure("No textual content at this offset", in)
    }
  }


  /** A parser matching a template whose name matches regular expresse `r` */
  def Template(re: Regex) : Parser[WikiText#Template] = new Parser[WikiText#Template] {
    def apply(input: Input) = {
      val in: Input = handleWhitespace(input)
      val offset = in.offset
      val sc = in.source

      in.first match {
        case t: WikiText#Template =>
          t.getName match {
            case re(_*) =>
              Success(t, in.drop(t.offset.end - offset))
            case _ =>
              val found = "`{{" + t.getName + "|...}}'"
              Failure("Template matching regex `" + re + "' expected but " + found + " found", in)
          }
        case _ => Failure("No template content at this offset", in)
      }
    }
  }

  /** A parser matching any template */
  def Template : Parser[WikiText#Template] = Template(_ => true)
//    new Parser[WikiText#Template] {
//    def apply(input: Input) = {
//      val in: Input = handleWhitespace(input)
//      val offset = in.offset
//      val sc = in.source
//
//      in.first match {
//        case t: WikiText#Template =>
//           Success(t, in.drop(t.offset.end - offset))
//        case _ => Failure("No template content at this offset", in)
//      }
//    }
//  }

  /** A parser matching templates satisfying function `f` */
  def Template(f: WikiText#Template => Boolean) : Parser[WikiText#Template] = (input: Input) => {
    val in: Input = handleWhitespace(input)
    val offset = in.offset
    val sc = in.source

    in.first match {
      case t: WikiText#Template =>
        if (f(t))
          Success(t, in.drop(t.offset.end - offset))
        else {
          val found = t.toString
          Failure("Template satisfying function `" + f.toString() + "' expected but " + found + " found", in)
        }
      case _ => Failure("No template content at this offset", in)
    }
  }


  /** A parser matching template with name `name` */
  def Template(name: String) : Parser[WikiText#Template] = new Parser[WikiText#Template] {
    def apply(input: Input) = {
      val in: Input = handleWhitespace(input)
      val offset = in.offset
      val sc = in.source
      in.first match {
        case t: WikiText#Template if t.getName == name =>
          Success(t, in.drop(t.offset.end - offset))
        case t: WikiText#Template =>
          Failure("Expected Template `" + name + "' but found template `" + t.getName + "'", in)
        case _ => Failure("No template content at this offset", in)
      }
    }
  }

  def Link : Parser[WikiText#Link] = InternalLink ^^{
    l => l.asInstanceOf[WikiText#Link]
  } | ExternalLink ^^{
    l => l.asInstanceOf[WikiText#Link]
  }

  /** A parser matching an internal link  */
  def InternalLink : Parser[WikiText#InternalLink] = (input: Input) => {
    val in: Input = handleWhitespace(input)
    val offset = in.offset
    val sc = in.source
    in.first match {
      case t: WikiText#InternalLink =>
        Success(t, in.drop(t.offset.end - offset))
      case null => Failure("Expected Internal Link but found end of input", in)
      case t => Failure("Expected Internal Link but found `" + t.toString + "'", in)
    }
  }

  /** A parser matching an internal link  */
  def ExternalLink : Parser[WikiText#ExternalLink] = (input: Input) => {
    val in: Input = handleWhitespace(input)
    val offset = in.offset
    val sc = in.source
    in.first match {
      case t: WikiText#ExternalLink =>
        Success(t, in.drop(t.offset.end - offset))
      case t => Failure("Expected External Link but found `" + t.toString + "'", in)
    }
  }

  def consumeUntil(in: Reader[WikiText#Token], r: Regex): Reader[WikiText#Token] = {
    val source = in.source
    val offset = in.offset
    in.first match {
      case t: WikiText#Text =>
        if (t.offset.end == t.offset.start && t.offset.start == in.asInstanceOf[WikiReader].endOffset)
          in
        else
          r findFirstMatchIn new SubSequence(source, offset, t.offset.end - t.offset.start) match {
            case Some(m) => in.drop(m.start - t.offset.start)
            case None => consumeUntil(in.drop(t.offset.end  - t.offset.start), r)
          }
      case t: WikiText#Token => consumeUntil(in.drop(t.offset.end - t.offset.start), r)
    }
  }

  /** A parser matching everything (including empty match) before the first occurence of r and returning the source String */
  def AnythingBefore(r: Regex): Parser[String] = (input: Input) => {
    val offset = input.offset
    val sc = input.source
    val remaining = consumeUntil(input, r)
    Success(sc.subSequence(offset, remaining.offset).toString, remaining)
  }

  /** A parser matching everything (but an empty String) before the first occurence of r and returning the source String */
  def SomethingBefore(r: Regex): Parser[String] = new Parser[String] {
    def apply(input: Input) =  {
    val in = handleWhitespace(input)
    val offset = in.offset
    val sc = in.source
    val remaining = consumeUntil(in, r)
    if (remaining.offset == offset)
      Failure("Expecting a non empty string before" + r + " at " + in.first, in)
    else
      Success(sc.subSequence(offset, remaining.offset).toString, remaining)
  }
  }


  /** A parser that only succeeds when input is exhausted (after whitespace handling) */
  def EoI : Parser[Boolean] = (input: Input) => {
    val in: Input = handleWhitespace(input)
    if (in.atEnd)
      Success(true, in)
    else
      Failure("End of Input expected but " + in.first + " found", in)
  }

  /** `positioned` decorates a parser's result with the start position of the input it consumed.
    * If whitespace is being skipped, then it is skipped before the start position is recorded.
    *
    * @param p a `Parser` whose result conforms to `Positional`.
    * @return A parser that has the same behaviour as `p`, but which marks its result with the
    *         start position of the input it consumed after whitespace has been skipped, if it
    *         didn't already have a position.
    */
    override def positioned[T <: Positional](p: => Parser[T]): Parser[T] = {
      val pp = super.positioned(p)
      new Parser[T] {
        def apply(input: Input) = {
          val in: Input = handleWhitespace(input)
          pp(in)
        }
      }
    }

  // NOTE: A Parser uses an Input which is a Reader of T.
  // Reader of T define source as a CharSequence.
  // TODO: check if this may be refined or if another field may be used (do not use source).

  /**
    * A parser generator delimiting whole phrases (i.e. programs).
    *
    * `phrase(p)` succeeds if `p` succeeds and no input is left over after `p`.
    *
    * @param p the parser that must consume all input for the resulting parser
    *          to succeed.
    * @return a parser that has the same result as `p`, but that only succeeds
    *         if `p` consumed all the input.
    */
  override def phrase[T](p: Parser[T]): Parser[T] =
    super.phrase(p <~ EoI)

  /** Parse some prefix of reader `in` with parser `p`. */
  def parse[T](p: Parser[T], in: Reader[WikiText#Token]): ParseResult[T] =
    p(in)

  /** Parse some prefix of character sequence `in` with parser `p`. */
  def parse[T](p: Parser[T], in: java.lang.CharSequence): ParseResult[T] =
    p(WikiReader(in))

  /** Parse some prefix of a WikiContent `wc` with parser `p`. */
  def parse[T](p: Parser[T], in: WikiText#WikiContent): ParseResult[T] =
    p(WikiReader(in))

  /** Parse all of reader `in` with parser `p`. */
  def parseAll[T](p: Parser[T], in: Reader[WikiText#Token]): ParseResult[T] =
    parse(phrase(p), in)

  /** Parse all of character sequence `in` with parser `p`. */
  def parseAll[T](p: Parser[T], in: java.lang.CharSequence): ParseResult[T] =
    parse(phrase(p), in)

  /** Parse all of WikiContent `wc` with parser `p`. */
  def parseAll[T](p: Parser[T], in: WikiText#WikiContent): ParseResult[T] =
    parse(phrase(p), in)
}

