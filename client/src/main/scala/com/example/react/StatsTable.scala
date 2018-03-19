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

/**
 * Shows a table with sort buttons has the headers of the columns.
 *
 * @author werewolf
 */
object StatsTable {
  import StatsTableInternal._

  class Sorter[T]( implicit val ordering: Ordering[T] ) {

    def compare( cols: List[Column[Any]], row1: Row, row2: Row, index: Int ) = {
      ordering.compare( row1(index).asInstanceOf[T], row2(index).asInstanceOf[T])
    }

  }

  /**
   * @param columns the columns in order that should be used to sort the rows.
   * Each column has two values that is specified in a Tuple2.  The first value is
   * the ID of the column, and the second is an override Ordering object.  If no override is specified, then the ordering
   * defined in that column is used.
   */
  class MultiColumnSorter[T](
      columns: (String, Option[Ordering[_]])*
    )(
      implicit
      ordering: Ordering[T]
  ) extends Sorter[T] {

    override
    def compare( cols: List[Column[Any]], row1: Row, row2: Row, index: Int ) = {
      val cs = columns.toList.asInstanceOf[List[(String,Option[Ordering[Any]])]]

      @tailrec
      def comp( orderlist: List[(String,Option[Ordering[Any]])] ): Int = {
        if (orderlist.isEmpty) 0
        else {
          val cur = orderlist.head
          val id = cur._1
          cols.find( c => c.id == id ) match {
            case Some(nextcol) =>
              val i = cols.indexWhere( c => c.id == id )
              val ordering = cur._2.getOrElse( nextcol.sorter.ordering )
              val eq = ordering.compare( row1(i), row2(i) )
              if (eq == 0) comp(orderlist.tail)
              else eq
            case None =>
              comp(orderlist.tail)
          }
        }
      }

      val rc = comp(cs)
//      log.fine(s"MultiColumnSort rc=${rc},\n  row1=${row1},\n  row2=${row2}")
      rc
    }

  }

  class MultiColumnSort[T]( cols: String* )( implicit ordering: Ordering[T] ) extends MultiColumnSorter( cols.map( i => (i,None) ): _* )(ordering)

  object Sorter {
    implicit object StringSorter extends Sorter[String]
    implicit object IntSorter extends Sorter[Int]
    implicit object DoubleSorter extends Sorter[Double]

  }

  /**
   * @param name the column header
   * @param formatter a function to get the data value as a string
   * @param ordering Ordering object to allow sorting the column
   */
  class Column[T](
      val id: String,
      val name: String,
      val formatter: T=>String,
      val initialSortOnSelectAscending: Boolean = false,
      val useAdditionalDataWhenSorting: Boolean = false
    )(
      implicit
      val sorter: Sorter[T]
    )

  type Row = List[Any]

  case class Props(
      columns: List[Column[Any]],
      rows: List[ Row ],
      initialSort: Option[String] = None,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      additionalRows: Option[()=>List[Row]] = None,
      totalRows: Option[()=>List[Row]] = None
    )

  /**
   * Shows a table with sort buttons has the headers of the columns.
   *
   * If a header and/or footer is defined, then it MUST have the the
   * same number of elements (th, td) as there are columns in each tr element.
   *
   * @param columns the column headers in the stats table.
   * @param rows the rows in the stats table.  The data type of rows[i] must match the type in column[i].
   * all rows must be the same length os the columns parameter.
   * @param initialSort the index of the columns that should be used as the initial sort
   * @param header One or more tr elements that will be inserted at the top of the thead element.
   * @param footer One or more tr elements that will be inserted into a tfoot element.
   */
  def apply(
      columns: List[Column[Any]],
      rows: List[ Row ],
      initialSort: Option[String] = None,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      additionalRows: Option[()=>List[Row]] = None,
      totalRows: Option[()=>List[Row]] = None
    ) = component( Props(columns,rows,initialSort,header,footer,additionalRows,totalRows))

}

object StatsTableInternal {
  import StatsTable._

  val log = Logger("bridge.StatsTable")

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause State to leak.
   *
   */
  case class State( currentSort: Option[String], ascending: Boolean = false ) {

    /**
     * If a new column is selected, then
     */
    def setOrToggleSort( col: Column[_] ) = {
      val ( newcs, newas ) =
        if (col.initialSortOnSelectAscending) {
          currentSort match {
            case Some(b) if b==col.id =>
              if (ascending) (Some(col.id),false)
              else (None, true)
            case _ =>
              (Some(col.id),true)
          }
        } else {
          currentSort match {
            case Some(b) if b==col.id =>
              if (ascending) (None, true)
              else (Some(col.id),true)
            case _ =>
              (Some(col.id),false)
          }
        }

      copy( currentSort = newcs, ascending=newas )
    }

    def isCurrentSort( i: Int ) = currentSort.map( b => b==i ).getOrElse(false)
  }

  val StatsTableHeader = ScalaComponent.builder[(Props,State,Backend)]("StatsTableHeader")
                          .render_P { args =>
                            val (props,state,backend) = args
                            <.thead(
                              props.header.whenDefined,
                              <.tr(
                                props.columns.zipWithIndex.map { entry =>
                                  val (c,i) = entry
                                  val selected = Some(c.id)==state.currentSort
                                  <.td(
                                    AppButton(
                                        c.id,
                                        c.name,
                                        if (selected) {
                                          TagMod(
                                            " ",
                                            <.span(
                                              if (state.ascending) {
                                                Strings.upArrowHead
                                              } else {
                                                Strings.downArrowHead
                                              }
                                            )
                                          )
                                        } else {
                                          EmptyVdom
                                        },
                                        BaseStyles.highlight( selected = selected ),
                                        ^.onClick --> backend.setOrToggleSort(c)
                                    )
                                  )
                                }.toTagMod
                              )
                            )
                          }.build

  val StatsTableRow = ScalaComponent.builder[(Props,State,Backend, Row, TagMod)]("StatsTableRow")
                        .render_P { args =>
                          val (props,state,backend,row, attrs) = args
                          <.tr(
                            row.zip( props.columns ).map { entry =>
                              val (r, col) = entry
                              <.td(
                                col.formatter(r)
                              )
                            }.toTagMod,
                            attrs,
                          )
                        }.build

  class SortOrder( col: Int, cols: List[Column[Any]] )( implicit sorter: Sorter[_]) extends Ordering[Row] {
    def compare( l: Row, r: Row ) = sorter.compare(cols, l, r, col)
  }

  class ReverseSortOrder( col: Int, cols: List[Column[Any]] )( implicit sorter: Sorter[_]) extends Ordering[Row] {
    def compare( l: Row, r: Row ) = - sorter.compare(cols, l, r, col)
  }

  /**
   * Internal state for rendering the component.
   *
   * I'd like this class to be private, but the instantiation of component
   * will cause Backend to leak.
   *
   */
  class Backend(scope: BackendScope[Props, State]) {

    def setOrToggleSort(col: Column[_] ) = scope.modState( s => s.setOrToggleSort(col) )

    def render( props: Props, state: State ) = {
      log.fine( s"StatsTable columns=${props.columns}" )
      val rows = state.currentSort.map { sortid =>
                   props.columns.indexWhere(c => c.id == sortid) match {
                     case sort if sort < 0 =>
                       props.rows
                     case sort =>
                       val sortColumn = props.columns(sort)
                       val sorter = sortColumn.sorter
                       implicit val ordering = if (state.ascending) new SortOrder(sort, props.columns)(sorter) else new ReverseSortOrder(sort, props.columns)(sorter)
                       val data = if (sortColumn.useAdditionalDataWhenSorting) {
                         props.rows ::: props.additionalRows.map( f => f() ).getOrElse(List())
                       } else {
                         props.rows
                       }
                       data.sorted
                   }
                 }.getOrElse( props.rows )
      <.table(
        baseStyles.tableStats,
        StatsTableHeader((props,state,this)),
        <.tbody(
          rows.zipWithIndex.map { entry =>
            val (row,i) = entry
            val x = StatsTableRow.withKey(i)((props,state,this,row,TagMod()))
            x
          }.toTagMod,
          props.totalRows.map { f =>
            val tpds = f()
            tpds.zipWithIndex.map { entry =>
              val (row,i) = entry
              val attr = if (i==0) baseStyles.tableStatsFirstTotalRow else TagMod()
              val x = StatsTableRow.withKey(s"T$i")((props,state,this,row,attr))
              x
            }.toTagMod
          }.whenDefined,
          props.footer.map { tm =>
            <.tfoot( tm )
          }.whenDefined
        )
      )
    }
  }

  val component = ScalaComponent.builder[Props]("StatsTable")
                            .initialStateFromProps { props => State(props.initialSort) }
                            .backend(new Backend(_))
                            .renderBackend
                            .build
}
