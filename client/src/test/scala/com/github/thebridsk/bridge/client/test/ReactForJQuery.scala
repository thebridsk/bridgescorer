package com.github.thebridsk.bridge.client.test

import org.scalajs.dom.raw.Element
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Text
import com.github.thebridsk.bridge.client.test.utils.JQuery
import japgolly.scalajs.react.test.ReactTestUtils
import org.scalajs.dom.raw.DocumentType
import org.scalajs.dom.raw.ProcessingInstruction
import org.scalajs.dom.raw.CDATASection
import org.scalajs.dom.raw.Comment
import org.scalajs.dom.raw.HTMLButtonElement
import org.scalajs.dom.raw.HTMLElement
import japgolly.scalajs.react.test.Simulation
import org.scalajs.dom.raw.HTMLInputElement
import scala.scalajs.js
import japgolly.scalajs.react.ReactDOM
import scala.scalajs.runtime.UndefinedBehaviorError
import japgolly.scalajs.react.component.Scala.MountedRoot

import scala.language.higherKinds
import japgolly.scalajs.react.test.SimEvent
import com.github.thebridsk.utilities.logging.Logger
import com.github.thebridsk.bridge.client.test.utils.JQuery

object ReactForJQuery {
  val log = Logger("bridge.ReactForJQuery")
}

/**
 * @author werewolf
 */
class ReactForJQuery( val component: Element ) {
  import ReactForJQuery._

  private var jqueryComponentInternal: JQuery = null

  def jqueryComponent = {
//    if (jqueryComponentInternal == null)
    val e = component
      jqueryComponentInternal = JQuery(e)
    jqueryComponentInternal
  }

  def jquery( selector: String ) = {
    jqueryComponent.find(selector)
  }

  private val ELEMENT_NODE = Node.ELEMENT_NODE   // 1
  private val ATTRIBUTE_NODE = Node.ATTRIBUTE_NODE   // 2
  private val TEXT_NODE = Node.TEXT_NODE   // 3
  private val CDATA_SECTION_NODE = Node.CDATA_SECTION_NODE   // 4
  private val ENTITY_REFERENCE_NODE = Node.ENTITY_REFERENCE_NODE   // 5
  private val ENTITY_NODE = Node.ENTITY_NODE   // 6
  private val PROCESSING_INSTRUCTION_NODE = Node.PROCESSING_INSTRUCTION_NODE   // 7
  private val COMMENT_NODE = Node.COMMENT_NODE   // 8
  private val DOCUMENT_NODE = Node.DOCUMENT_NODE   // 9
  private val DOCUMENT_TYPE_NODE = Node.DOCUMENT_TYPE_NODE   // 10
  private val DOCUMENT_FRAGMENT_NODE = Node.DOCUMENT_FRAGMENT_NODE   // 11
  private val NOTATION_NODE = Node.NOTATION_NODE   // 12

  private def getText( element: Node ) = {
    val x = element.valueOf()
    val xt = x.asInstanceOf[Text]
    xt
  }

  def show( element: Node = component, indent: String = ""): Unit = {
    val s = indent+"-------Start-----------" +
            showToString(element, "\n"+indent) +
            "\n" + indent+"------End---------"
    log.info(s)
  }

  def showToString( element: Node = component, indent: String = ""): String = {

    val nodetype = element.nodeType

    nodetype match {
      case `ELEMENT_NODE` =>
        showWithChildren(element, indent)
      case `ATTRIBUTE_NODE` =>
        ""
//        showWithChildren(element, indent)
      case `ENTITY_REFERENCE_NODE` =>
        showWithChildren(element, indent)
      case `ENTITY_NODE` =>
        showWithChildren(element, indent)
      case `DOCUMENT_NODE` =>
        showWithChildren(element, indent)
      case `DOCUMENT_FRAGMENT_NODE` =>
        showWithChildren(element, indent)

      case `DOCUMENT_TYPE_NODE` =>
        val doctype = element.asInstanceOf[DocumentType]
        indent+"<#doctype name="+doctype.name+" />"
      case `PROCESSING_INSTRUCTION_NODE` =>
        val pi = element.asInstanceOf[ProcessingInstruction]
        indent+"<#pi target="+pi.target+">"+pi.data+"</#pi>"
      case `TEXT_NODE` =>
//        val text=element.asInstanceOf[Text]
//        val t = if (js.isUndefined(text.wholeText)) "" else text.wholeText
        indent+element.nodeValue
      case `CDATA_SECTION_NODE` =>
        val cdata = element.asInstanceOf[CDATASection]
        indent+"<#cdata>"+cdata.data+"</#cdata>"
      case `COMMENT_NODE` =>
//        val comment = element.asInstanceOf[Comment]
//        val c = try {
//          comment.data
//        } catch {
//          case e: UndefinedBehaviorError => "<undefined text in comment>"
//        }
        val c = element.nodeValue
        indent+"<!-- "+c+" -->"
      case `NOTATION_NODE` =>
        indent+"<#notation name="+element.nodeName+" />"
    }
  }

  private def showWithChildren( element: Node, indent: String = ""): String = {
    var r = indent+"<"+element.localName
    val attrs = element.attributes
    val alen = attrs.length
    (0 until alen).foreach { i =>
      val child = attrs.item(i)
      val name = child.name
      val value = child.value
      r = r + " "+name+"=\""+value+"\""
    }

    val children = element.childNodes
    val len = children.length

    if (len == 0) {
      r = r + " />"
    } else {
      r = r + ">"
      (0 until len).foreach {i =>
        val child = children.item(i)
        r = r + showToString(child,indent+"  ")
      }
      r = r + indent+"</"+element.localName+">"
    }
    r
  }

  def findTagById( tag: String, id: String ) = {
    val r = jquery( tag+"#"+id )
    assert( r.length == 1, "Did not find only one element named "+tag+" with id "+id)
    r(0)
  }

  def isElementDisabled( e: Element ) = {
    e.hasAttribute("disabled") && e.getAttribute("disabled") == "true"
  }

  def isDisabledTagById( tag: String, id: String ) = {
    val c = findTagById(tag, id)
    isElementDisabled(c)
  }

  def clickTagById( tag: String, id: String ) = {
    val c = findTagById(tag, id)
    assertFalse(isElementDisabled(c), "Tag "+tag+" with id "+id+" is disabled")
    Simulation.click run c
  }

  def setInputFieldById( id: String, value: String ) = {
    val c = findTagById("input", id)
    val i = c.asInstanceOf[HTMLInputElement]
    assertFalse(isElementDisabled(c), "Tag input with id "+id+" is disabled")
    i.value = value
    SimEvent.Change(value) simulate c
  }

  def findTagByName( tag: String, name: String ) = {
    val r = jquery( tag+"[name="+name+"]" )
    assert( r.length == 1, "Did not find only one element named "+tag+" with name "+name)
    r(0)
  }

  def clickTagByName( tag: String, name: String ) = {
    val c = findTagByName(tag, name)
    assertFalse(isElementDisabled(c), "Tag "+tag+" with name "+name+" is disabled")
    Simulation.click run c
  }

  def setInputFieldByName( name: String, value: String ) = {
    val c = findTagByName("input", name)
    val n = c
    val i = n.asInstanceOf[HTMLInputElement]
    assertFalse(isElementDisabled(c), "Tag input with name "+name+" is disabled")
    i.value = value
    SimEvent.Change(value) simulate c
  }

  def isComponent(id: String)(c: Element) = {
      c.hasOwnProperty("id") && c.id == id
  }

  def isComponentName(name: String)(c: Element) = {
      c.hasAttribute("name") && c.getAttribute("name") == name
  }

  def isComponentTag(name: String)(c: Element) = {
      c.nodeName == name
  }

  def findAllById( id: String ) = {
//    ReactTestUtils.findAllInRenderedTree(component, isComponent(id) _)
    val r = jquery( "#"+id )

    var z = new Array[HTMLElement](r.length)
    (0 to z.length).foreach( i => {
      z(i) = r(i).asInstanceOf[HTMLElement]
    })
    z
  }

  def findAllByName( name: String ) = {
    show(component, "looking for "+name)
//    ReactTestUtils.findAllInRenderedTree(component, isComponentName(name) _)
    val r = jquery( "*[name="+name+"]" )

    var z = new Array[HTMLElement](r.length)
    (0 to z.length).foreach( i => {
      z(i) = r(i).asInstanceOf[HTMLElement]
    })
    z
  }

  def findAllByTag( name: String ) = {
    show( component, "looking for "+name)
//    ReactTestUtils.findAllInRenderedTree(component, isComponentTag(name) _)
    val r = jquery( name )

    var z = new Array[HTMLElement](r.length)
    (0 to z.length).foreach( i => {
      z(i) = r(i).asInstanceOf[HTMLElement]
    })
    z
  }

  def clickById( id: String ) = {
    val comps = findAllById(id)
    if (comps.length == 0) fail("Did not find element with id "+id+" to click")
    if (comps.length > 1) fail("Find multiple elements with id "+id+" to click")
    val b = comps(0)
    assertFalse(isElementDisabled(b), "Element with id "+id+" is disabled")

    Simulation.click run b
  }

  def assertTrue( bool: Boolean, msg: =>String ): Unit = {
    if (!bool) fail(msg)
  }

  def assertFalse( bool: Boolean, msg: =>String ): Unit = {
    if (bool) fail(msg)
  }

  def fail(msg: String): Nothing =
    throw new AssertionError(msg)

  //
  // scalatest selenium dsl for manipulating page
  //

  trait WebElement {
    def name: String
    def components: Array[HTMLElement]

    def click = {
      val comps = components
      if (comps.length == 0) fail("Did not find element with "+name+" to click")
      else if (comps.length > 1) {
        if (comps.length == 2 && (comps(0) == comps(1) || js.isUndefined(comps(1)))) {
          println("Find 2 identical elements with "+name+" to click")
        } else {
          println("Find multiple elements with "+name+" to click")
          comps.foreach { x => println("  Found "+x.toString()) }
          Console.flush()
          fail("Find multiple elements with "+name+" to click")
        }
      }
      val b = comps(0)
      assertFalse(isElementDisabled(b), "Element with "+name+" is disabled")

      Simulation.click run b
    }

    def findElement = {
      val comps = components
      var i = 0
      if (comps.length == 0) {
        println("findElement not found: Did not find element with "+name)
        show( component,"findElement not found: ")
        fail("Did not find element with "+name)
      }
      if (comps.length > 1) {
        if (comps.length == 2 && comps(0) == comps(1)) {
          println("Find 2 identical elements with "+name)
        } else if (comps.length == 2) {
          if (comps(0) /* .getDOMNode() */ .isInstanceOf[HTMLElement]) i=0
          else if (comps(1) /* .getDOMNode() */.isInstanceOf[HTMLElement]) i=1
          else {
            println("Find multiple elements with "+name+" no HTMLElement: "+comps(0) +" " +comps(1))
            fail("Find multiple elements with "+name+" no HTMLElement")
          }
        } else {
          println("Find multiple elements with "+name)
          comps.foreach { x => println("  Found "+x.toString()) }
          Console.flush()
          fail("Find multiple elements with "+name)
        }
      }
      val c = comps(i)
//      val n = ReactDOM.findDOMNode(c)
      Some(c) //(n.asInstanceOf[HTMLElement])
    }

    def findField = findElement.get.asInstanceOf[HTMLInputElement]

    def getField = {
      findField.value
    }

    def setField( v: String ) = {
      val f = findField
      f.value = v
      val dis = isElementDisabled(f)
      assertTrue(!dis, "Input with "+name+" is disabled")
      SimEvent.Change(v) simulate f
    }

    def getText = findField.textContent
  }

  def id( elementId: String ): WebElement = new WebElement {
    def name = "Id "+elementId
    def components = findAllById(elementId)
  }

  def name( elementName: String ): WebElement = new WebElement {
    def name = "Name "+elementName
    def components = findAllByName(elementName)
  }

  def tag( elementName: String ): WebElement = new WebElement {
    def name = "Name "+elementName
    def components = findAllByTag(elementName)
  }

  object click {
    def on( element: WebElement ) = {
      element.click
    }
  }

  class TextField( element: WebElement ) {
    def value: String = element.getField

    def value_=( v: String ): Unit = element.setField(v)
  }

  def textField( element: WebElement) = new TextField(element)

  def textField( elementName: String ) = new TextField( name(elementName))

  class MyElement( e: HTMLElement ) {
    def text = e.textContent

    def isEnabled = !isElementDisabled(e)
  }

  def find( query: String ): Option[MyElement] = {
    id(query).findElement match {
      case Some(e) => Some(new MyElement(e))
      case _ =>
        name(query).findElement match {
          case Some(e) => Some(new MyElement(e))
          case _ => None
        }
    }
  }

  def find( element: WebElement ): Option[MyElement] = {
    element.findElement match {
      case Some(e) => Some(new MyElement(e))
      case _ => None
    }
  }

  import scala.language.implicitConversions
  /**
   * Simple class to allow
   * <code><pre>
   * xx mustBe yy
   * </pre></code>
   *
   */
  class MustMatcher[A]( left: A ) {
    def mustBe(right: A) = if (left != right) fail("Left "+left+" does not equal right "+right)
  }
  implicit def toMustMatchers[A](left: A) = new MustMatcher(left)

}
