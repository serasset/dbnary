package org.getalp.dbnary.wiki

/**
  * Created by serasset on 27/01/17.
  */
private[wiki] class SubText(s: Text, start: Int, val length: Int) extends CharSequence {
  def this(s: Text, start: Int) = this(s, start, s.text.length - start)

  def charAt(i: Int) =
    if (i >= 0 && i < length) s.text.charAt(start + i) else throw new IndexOutOfBoundsException(s"index: $i, length: $length")

  def subSequence(_start: Int, _end: Int) = {
    if (_start < 0 || _end < 0 || _end > length || _start > _end)
      throw new IndexOutOfBoundsException(s"start: ${_start}, end: ${_end}, length: $length")

    new SubText(s, start + _start, _end - _start)
  }

  override def toString = s.text.subSequence(start, start + length).toString
}

