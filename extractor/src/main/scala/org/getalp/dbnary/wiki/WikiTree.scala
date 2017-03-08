package org.getalp.dbnary.wiki

import scala.collection.GenTraversableOnce
import scala.collection.JavaConversions._


/**
  * Created by serasset on 11/01/17.
  */

  sealed abstract class WikiEvent

  case class Template(name: String, args: Map[String, List[WikiEvent]]) extends WikiEvent
  case class HTMLComment() extends WikiEvent
  case class ExternalLink(target: List[WikiEvent], text: List[WikiEvent]) extends WikiEvent
  case class InternalLink(target: List[WikiEvent], text: List[WikiEvent], suffix: String) extends WikiEvent
  case class Heading(level: Int, events: List[WikiEvent]) extends WikiEvent
  case class ListItem(level: Int, events: List[WikiEvent]) extends WikiEvent
  case class Indentation(level: Int, events: List[WikiEvent]) extends WikiEvent
  case class Text(text: String) extends WikiEvent

  object Link {
   // def apply(left : String, right : String) = new Mul(left, right)
    def unapply(m : WikiEvent) = m match {
     case x : ExternalLink => Some(x.target, x.text)
     case x : InternalLink => Some(x.target, x.text)
     case _ => None
    }
  }

  object WikiContent {

    def apply(t: WikiText#Token): WikiEvent = t match {
      case t: WikiText#HTMLComment => apply(t.asInstanceOf[WikiText#HTMLComment])
      case t: WikiText#ExternalLink => apply(t.asInstanceOf[WikiText#ExternalLink])
      case t: WikiText#InternalLink => apply(t.asInstanceOf[WikiText#InternalLink])
      case t: WikiText#Heading => apply(t.asInstanceOf[WikiText#Heading])
      case t: WikiText#Template => apply(t.asInstanceOf[WikiText#Template])
      // keep indentation before ListItem as it is defined as a subtype in WikiText
      case t: WikiText#Indentation => apply(t.asInstanceOf[WikiText#Indentation])
      case t: WikiText#ListItem => apply(t.asInstanceOf[WikiText#ListItem])
      case t: WikiText#Text => apply(t.asInstanceOf[WikiText#Text])
      case _ => throw new Exception
    }

    def apply(c: WikiText#WikiContent): List[WikiEvent] = c.tokens().iterator() map (t => this(t)) toList

    def apply(t: WikiText#HTMLComment): WikiEvent = HTMLComment()

    def apply(t: WikiText#ExternalLink): WikiEvent =
      ExternalLink(this(t.getTarget), this(t.getLink))

    def apply(t: WikiText#InternalLink): WikiEvent =
      InternalLink(this(t.getTarget), this(t.getLink), t.getSuffix)

    def apply(t: WikiText#Heading): WikiEvent =
      Heading(t.getLevel, this(t.content))

    def apply(c: WikiText#Template): WikiEvent =
      Template(c.name.toString,
        c.getArgs.toMap map {
          case (k, v) => (k, this(v))
        })

    def apply(l: WikiText#ListItem) : WikiEvent =
      ListItem(l.getLevel, this(l.getContent))

    def apply(l: WikiText#Indentation) : WikiEvent =
      Indentation(l.getLevel, this(l.getContent))

    def apply(t: WikiText#Text) : WikiEvent =
      Text(t.getText)


    def apply(content: String) : GenTraversableOnce[WikiEvent] = apply(content, 0, content.length)

    def apply(content: String, start: Int, end: Int) : GenTraversableOnce[WikiEvent] = apply(null, content, start, end)

    def apply(pagename: String, content: String, start: Int, end: Int) : GenTraversableOnce[WikiEvent] =
        apply(new WikiText(pagename, content, start, end))

    def apply(wikiText: WikiText) : GenTraversableOnce[WikiEvent] = {
      this(wikiText.content())
    }

  }