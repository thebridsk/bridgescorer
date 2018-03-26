package com.example.react

import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.Element
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import scala.math.Ordering
import scala.annotation.tailrec
import com.example.pages.BaseStyles
import com.example.pages.BaseStyles.baseStyles
import com.example.data.util.Strings
import utils.logging.Logger
import org.scalajs.dom.ext.Color

/**
 * Shows a table with sort buttons has the headers of the columns.
 *
 * @author werewolf
 */
object PieChartTable {
  import PieChartTableInternal._

  /**
   * @param name the column header
   * @param formatter a function to get the data value as a string
   * @param ordering Ordering object to allow sorting the column
   */
  case class Column(
    name: TagMod,
  )

  case class Data[+TColor](
    size: Int,
    color: List[TColor],
    value: List[Double],
    title: Option[String] = None
  )

  case class Row[+TColor](
      name: String,
      data: List[List[Data[TColor]]]
  )

  case class Props private[PieChartTable](
      firstColumn: Column,
      columns: List[Column],
      rows: List[ Row[Any] ],
      colorMap: Any => Color,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      totalRows: Option[()=>List[Row[Any]]] = None,
      caption: Option[TagMod] = None
    ) {

  }

  /**
   * Shows a table with sort buttons has the headers of the columns.
   *
   * If a header and/or footer is defined, then it MUST have the the
   * same number of elements (th, td) as there are columns in each tr element.
   *
   * @param firstColumn the header for the first column
   * @param columns the column headers in the stats table.
   * @param rows the rows in the table.  The size the data field of each row must equal the size of columns.
   * @param colorMap function to determine color of pie slice
   * @param header One or more tr elements that will be inserted at the top of the thead element.
   * @param footer One or more tr elements that will be inserted into a tfoot element.
   * @param additionalRows additional rows to add when certain columns are selected for sorting
   * @param totalRows rows added at the bottom of the table, not affected with sorting
   * @param caption the caption for the table
   * @param showZero true, show a size of zero with a black square
   */
  def apply[TColor](
      firstColumn: Column,
      columns: List[Column],
      rows: List[ Row[TColor] ],
      colorMap: TColor => Color,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      totalRows: Option[()=>List[Row[TColor]]] = None,
      caption: Option[TagMod] = None
    ) = component( Props(firstColumn, columns,rows,colorMap.asInstanceOf[Any=>Color],header,footer,totalRows,caption))

  /**
   * Shows a table with sort buttons has the headers of the columns.
   *
   * If a header and/or footer is defined, then it MUST have the the
   * same number of elements (th, td) as there are columns in each tr element.
   *
   * @param firstColumn the header for the first column
   * @param firstColumnValues values for each row for the the first column
   * @param columns the column headers in the stats table.
   * @param rows the rows in the stats table.  The size of rows must equal the size of firstColumnValues.  The size of each row must equal the size of columns.
   * all rows must be the same length os the columns parameter.
   * @param colorMap function to determine color of pie slice
   * @param header One or more tr elements that will be inserted at the top of the thead element.
   * @param footer One or more tr elements that will be inserted into a tfoot element.
   * @param additionalRows additional rows to add when certain columns are selected for sorting
   * @param totalRows rows added at the bottom of the table, not affected with sorting
   * @param caption the caption for the table
   * @param showZero true, show a size of zero with a black square
   */
  def props[TColor](
      firstColumn: Column,
      columns: List[Column],
      rows: List[ Row[TColor] ],
      colorMap: TColor => Color,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      totalRows: Option[()=>List[Row[TColor]]] = None,
      caption: Option[TagMod] = None
    ) = Props(firstColumn, columns,rows,colorMap.asInstanceOf[Any=>Color],header,footer,totalRows,caption)

}

object PieChartTableInternal {
  import PieChartTable._
  import Utils._

  val log = Logger("bridge.PieChartTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( )

  val PieChartTableHeader = ScalaComponent.builder[(Props,State,Backend)]("PieChartTableHeader")
                          .render_P { args =>
                            val (props,state,backend) = args
                            <.thead(
                              props.header.whenDefined,
                              <.tr(
                                <.td(
                                  props.firstColumn.name
                                ),
                                props.columns.map { c =>
                                  <.td(
                                    c.name
                                  )
                                }.toTagMod
                              )
                            )
                          }.build

  val titleAttr    = VdomAttr("data-title")

  val PieChartTableRow = ScalaComponent.builder[(Props,State,Backend, Row[Any])]("PieChartTableRow")
                        .render_P { args =>
                          val (props,state,backend,row) = args
                          <.tr(
                            <.td(
                              row.name
                            ),
                            row.data.zip( props.columns ).map { entry =>
                              val (cell, col) = entry
                              <.td(
                                <.div(
                                  cell.map { r =>
                                    <.div(
                                      r.title.whenDefined { title =>
                                        TagMod(
                                          titleAttr:=title,
                                          baseStyles.hover
                                        )
                                      },
                                      PieChartOrSquareForZero(
                                        size = r.size,
                                        Color.Black,
                                        slices = r.value,
                                        colors = r.color.map( v => props.colorMap(v) ),
                                        chartTitle = None,
                                      )
                                    )
                                  }.toTagMod
                                )
                              )
                            }.toTagMod
                          )
                        }.build

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def render( props: Props, state: State ) = {
      log.fine( s"PieChartTable columns=${props.columns}" )
      <.table(
        baseStyles.tablePieChart,
        props.caption.whenDefined { c => <.caption(c) },
        PieChartTableHeader((props,state,this)),
        (props.footer.isDefined||props.totalRows.isDefined) ?= <.tfoot(
          props.totalRows.map { f =>
            val tpds = f()
            tpds.zipWithIndex.map { entry =>
              val (row,i) = entry
              val x = PieChartTableRow.withKey(s"T$i")((props,state,this,row))
              x
            }.toTagMod
          }.whenDefined,
          props.footer.whenDefined
        ),
        <.tbody(
          props.rows.zipWithIndex.map { entry =>
            val (row,i) = entry
            val x = PieChartTableRow.withKey(i)((props,state,this,row))
            x
          }.toTagMod
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("PieChartTable")
                            .initialStateFromProps { props => State() }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}

