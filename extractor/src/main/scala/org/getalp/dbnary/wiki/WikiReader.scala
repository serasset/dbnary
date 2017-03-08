package org.getalp.dbnary.wiki

import java.util

import scala.util.parsing.input.{Position, Reader}

/**
  * Created by serasset on 01/02/17.
  */

object WikiReader {
  // final val EofTok: WikiText#Text = new WikiText#Text(-1, -1)

  /** Create a `WikiReader` from a `WikiText`.
    *
    * @param in the `WikiText` that provides the underlying
    *           stream of wiki tokens for this Reader.
    */
  def apply(in: WikiText): WikiReader = {
    new WikiReader(in.tokens(), in.getStartOffset, in.sourceContent, in.getEndOffset, in.endOfContent())
  }

  /** Create a `WikiReader` from a `WikiContent`.
    *
    * @param in the `WikiContent` that provides the underlying
    *           stream of wiki tokens for this Reader.
    */
  def apply(in: WikiText#WikiContent): WikiReader = {
    new WikiReader(in.tokens(), in.offset.start, in.getFullContent, in.offset.end, in.endOfContent())
  }

  def apply(in: CharSequence): WikiReader = {
    apply(new WikiText(in.toString))
  }
}

/**
  *
  * @param tokens an arrayList of WikiTokens
  * @param tindex the index of the wiki token in source that encompasses offset
  * @param charOffset the character offset in the underlying character sequence (the original media wiki source string)
  */
class WikiReader private (tokens: util.ArrayList[_ <: WikiText#Token], tindex: Int, charOffset: Int, txt: CharSequence, end: Int, eof: WikiText#Text)  extends Reader[WikiText#Token] {

  /**
    * Public constructor for a new WikiReader, taking an arrayList of tokens and its startOffset
    */
  def this(tokens: util.ArrayList[_ <: WikiText#Token], charOffset: Int, txt: CharSequence, end: Int, eof: WikiText#Text) =
    this(tokens, 0, charOffset, txt, end, eof)

  def endOffset = this.end

  def src: util.ArrayList[_ <: WikiText#Token] = this.tokens

  override def source = txt

  override def offset: Int =
    this.charOffset

  /** Returns the first element of the reader, or EofCh if reader is at its end.
    * */
  override def first =
    if (tindex < tokens.size()) {
      val tok = tokens.get(tindex)
      if (tok.isInstanceOf[WikiText#Text] && offset > tok.offset.start)
        tok.asInstanceOf[WikiText#Text].subText(offset, tok.offset.end)
      else
        tok
    } else eof

  override def rest: Reader[WikiText#Token] =
    new WikiReader(tokens, tindex + 1, if (tindex < tokens.size()) tokens.get(tindex).offset.end else end, txt, end, eof)

  override def pos: Position = new Position {
    def line = 0

    def column = charOffset + 1

    def lineContents = tokens.get(tindex).toString
  }

  override def drop(n: Int): WikiReader = {
    var ti = tindex
    val ni = charOffset + n
    while (ti < tokens.size() && tokens.get(ti).offset.end <= ni) {
      ti += 1
    }
    new WikiReader(tokens, ti, ni, txt, end, eof)
  }

  override def atEnd: Boolean =
    charOffset == end

  override def toString: String = "[" + charOffset + "," + end + "] " + txt.subSequence(charOffset, end)

}
