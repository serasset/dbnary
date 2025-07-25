package org.getalp.dbnary.languages.common

case class Derivation(target: String, var note: String) {
  def addNote(u: String): Derivation = {
    this.note match {
      case null => this.note = u
      case _ => this.note = this.note + "|" + u
    }
    this
  }
}
