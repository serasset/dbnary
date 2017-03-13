package org.getalp.dbnary.wiki

// Extracted from scala source to be visible and usable in Wiki Text Parser

// A shallow wrapper over another CharSequence (usually a String)
//
// See SI-7710: in jdk7u6 String.subSequence stopped sharing the char array of the original
// string and began copying it.
// RegexParsers calls subSequence twice per input character: that's a lot of array copying!
private[wiki] class SubSequence(s: CharSequence, start: Int, val length: Int) extends CharSequence {
  def this(s: CharSequence, start: Int) = this(s, start, s.length - start)

  def charAt(i: Int) =
    if (i >= 0 && i < length) s.charAt(start + i) else throw new IndexOutOfBoundsException(s"index: $i, length: $length")

  def subSequence(_start: Int, _end: Int) = {
    if (_start < 0 || _end < 0 || _end > length || _start > _end)
      throw new IndexOutOfBoundsException(s"start: ${_start}, end: ${_end}, length: $length")

    new SubSequence(s, start + _start, _end - _start)
  }

  override def toString = s.subSequence(start, start + length).toString
}
