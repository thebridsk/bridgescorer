package com.github.thebridsk.bridge.clientcommon.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import scala.math.Ordering
import scala.annotation.tailrec
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles
import com.github.thebridsk.bridge.clientcommon.pages.BaseStyles.baseStyles
import com.github.thebridsk.bridge.data.util.Strings
import com.github.thebridsk.utilities.logging.Logger
import scala.math.Ordering.Double.TotalOrdering

/**
  * Shows a table with sort buttons has the headers of the columns.
  *
  * @author werewolf
  */
object Table {
  import TableInternal._

  class Sorter[T](implicit val ordering: Ordering[T]) {

    def compare(
        cols: List[ColumnBase],
        row1: Row,
        row2: Row,
        index: Int
    ): Int = {
      ordering.compare(row1(index).asInstanceOf[T], row2(index).asInstanceOf[T])
    }

  }

  /**
    * @param columns the columns in order that should be used to sort the rows.
    * Each column has three values that is specified in a Tuple3.
    * The first value is the ID of the column,
    * and the second is an override Ordering object.
    * The third is a boolean that indicates whether the sort should be reversed.
    * If no override is specified, then the ordering
    * defined in that column is used.
    * @param ordering the sorting used when this column is used MultiColumnSort
    */
  class MultiColumnSort[T](
      columns: (String, Option[Ordering[_]], Boolean)*
  )(implicit
      ordering: Ordering[T]
  ) extends Sorter[T] {

    override def compare(
        cols: List[ColumnBase],
        row1: Row,
        row2: Row,
        index: Int
    ): Int = {
      val cs = columns.toList
        .asInstanceOf[List[(String, Option[Ordering[Any]], Boolean)]]

      @tailrec
      def comp(
          orderlist: List[(String, Option[Ordering[Any]], Boolean)]
      ): Int = {
        if (orderlist.isEmpty) 0
        else {
          val cur = orderlist.head
          val id = cur._1
          cols.find(c =>
            if (c.isInstanceOf[SortableColumn[_]])
              c.asInstanceOf[SortableColumn[_]].id == id
            else false
          ) match {
            case Some(nextcol: SortableColumn[_]) =>
              val i = cols.indexWhere(c =>
                if (c.isInstanceOf[SortableColumn[_]])
                  c.asInstanceOf[SortableColumn[_]].id == id
                else false
              )
              val order = cur._2.getOrElse(
                nextcol.asInstanceOf[SortableColumn[Any]].sorter.ordering
              )
              val eq = order.compare(row1(i), row2(i))
              if (eq == 0) comp(orderlist.tail)
              else if (cur._3) -eq
              else eq
            case _ =>
              comp(orderlist.tail)
          }
        }
      }

      val rc = comp(cs)
//      log.fine(s"MultiColumnSort rc=${rc},\n  row1=${row1},\n  row2=${row2}")
      rc
    }

  }

  object MultiColumnSort {

    def apply[T](cols: (String, Option[Ordering[_]], Boolean)*)(implicit
        ordering: Ordering[T]
    ) = new MultiColumnSort(cols: _*)(ordering)
    def create2[T](cols: (String, Boolean)*)(implicit ordering: Ordering[T]) =
      new MultiColumnSort(cols.map(c => (c._1, None, c._2)): _*)(ordering)
    def create[T](cols: String*)(implicit ordering: Ordering[T]) =
      new MultiColumnSort(cols.map(c => (c, None, false)): _*)(ordering)

  }

  object Sorter {
    implicit object StringSorter extends Sorter[String]
    implicit object IntSorter extends Sorter[Int]
    implicit object DoubleSorter extends Sorter[Double]

  }

  /**
    * defines a column.  The name field will be used as the text for the last thead row.
    */
  trait ColumnBase {
    val name: TagMod
    val hidden: Boolean

    def getData(value: Any): TagMod
  }

  /**
    * A column definitions.
    *
    * The values in [[Row]]s must be of type TagMod
    *
    * @param name the column header
    * @param hidden true if the column is not displayed
    */
  class Column(
      val name: TagMod,
      val hidden: Boolean = false
  ) extends ColumnBase {

    def getData(value: Any): TagMod = value.asInstanceOf[TagMod]

  }

  object Column {
    def apply(
        name: TagMod,
        hidden: Boolean = false
    ) = new Column(name, hidden)
  }

  /**
    * A sortable column definitions.
    *
    * The values in [[Row]]s must be of type T
    *
    * @param T the type of the data in this column.
    *
    * @param id the id of the sorting button in the table header
    * @param name the column header
    * @param formatter a function to get the data value as a TagMod
    * @param initialSortOnSelectAscending the sort order when first selecting the column
    * @param useAdditionalDataWhenSorting if true additional rows are also displayed
    * @param hidden true if the column is not displayed
    * @param sorter the sorter to sort rows based on this column.
    */
  class SortableColumn[T](
      val id: String,
      val name: TagMod,
      val formatter: T => TagMod,
      val initialSortOnSelectAscending: Boolean = false,
      val useAdditionalDataWhenSorting: Boolean = false,
      val hidden: Boolean = false
  )(implicit
      val sorter: Sorter[T]
  ) extends ColumnBase {

    override def getData(value: Any): TagMod = {
      val t = value.asInstanceOf[T]
      formatter(t)
    }
  }

  type Row = List[Any]

  case class Props(
      columns: List[ColumnBase],
      rows: List[Row],
      initialSort: Option[String] = None,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      additionalRows: Option[() => List[Row]] = None,
      totalRows: Option[List[Row]] = None,
      caption: Option[TagMod] = None
  ) {

    def shownColumns: List[ColumnBase] = columns.filter(c => !c.hidden)
  }

  /**
    * Shows a table with sort buttons has the headers of the columns.
    *
    * If a header and/or footer is defined, then it MUST have the the
    * same number of elements (th, td) as there are columns in each tr element.
    *
    * @param columns the column headers in the stats table, this becomes the last tr in the thead section.
    * @param rows the rows in the stats table.  The data type of rows[i] must match the type in column[i].
    * all rows must be the same length os the columns parameter.
    * @param initialSort the index of the columns that should be used as the initial sort
    * @param header One or more tr elements that will be inserted at the top of the thead element.
    * @param footer One or more tr elements that will be inserted into a tfoot element.
    * @param additionalRows additional rows to add when certain columns are selected for sorting
    * @param totalRows rows added at the bottom of the table, not affected with sorting
    * @param caption the caption for the table
    */
  def apply(
      columns: List[ColumnBase],
      rows: List[Row],
      initialSort: Option[String] = None,
      header: Option[TagMod] = None,
      footer: Option[TagMod] = None,
      additionalRows: Option[() => List[Row]] = None,
      totalRows: Option[List[Row]] = None,
      caption: Option[TagMod] = None
  ) =
    component(
      Props(
        columns,
        rows,
        initialSort,
        header,
        footer,
        additionalRows,
        totalRows,
        caption
      )
    ) // scalafix:ok ExplicitResultTypes; ReactComponent

}

object TableInternal {
  import Table._
  import Utils._

  val log: Logger = Logger("bridge.Table")

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause State to leak.
    */
  case class State(currentSort: Option[String], ascending: Boolean = false) {

    /**
      * If a new column is selected, then the sorting is set and ascending is set to the value in the column object.
      * If the same column is selected, then the ascending is toggled or sorting is turned off.
      */
    def setOrToggleSort(col: SortableColumn[_]): State = {
      val (newcs, newas) =
        if (col.initialSortOnSelectAscending) {
          currentSort match {
            case Some(b) if b == col.id =>
              if (ascending) (Some(col.id), false)
              else (None, true)
            case _ =>
              (Some(col.id), true)
          }
        } else {
          currentSort match {
            case Some(b) if b == col.id =>
              if (ascending) (None, true)
              else (Some(col.id), true)
            case _ =>
              (Some(col.id), false)
          }
        }

      copy(currentSort = newcs, ascending = newas)
    }

    def isCurrentSort(i: Int): Boolean =
      currentSort.map(b => b == i).getOrElse(false)
  }

  private[react] val TableHeader = ScalaComponent
    .builder[(Props, State, Backend)]("TableHeader")
    .render_P { args =>
      val (props, state, backend) = args
      <.thead(
        props.header.whenDefined,
        <.tr(
          props.shownColumns.map { cc =>
            cc match {
              case c: SortableColumn[_] =>
                val selected = Some(c.id) == state.currentSort
                <.th(
                  AppButton(
                    c.id,
                    TagMod(
                      c.name,
                      selected ?= s""" ${if (state.ascending) {
                        Strings.upArrowHead
                      } else {
                        Strings.downArrowHead
                      }}"""
                    ),
                    BaseStyles.highlight(selected = selected),
                    ^.onClick --> backend.setOrToggleSort(c)
                  )
                )
              case c: ColumnBase =>
                <.th(c.name)
            }
          }.toTagMod
        )
      )
    }
    .build

  private[react] val TableRow = ScalaComponent
    .builder[(Props, State, Backend, Row)]("TableRow")
    .render_P { args =>
      val (props, state, backend, row) = args
      <.tr(
        row
          .zip(props.columns)
          .filter(e => !e._2.hidden)
          .map { entry =>
            val (r, col) = entry
            <.td(col.getData(r))
          }
          .toTagMod
      )
    }
    .build

  class SortOrder(col: Int, cols: List[ColumnBase])(implicit sorter: Sorter[_])
      extends Ordering[Row] {
    def compare(l: Row, r: Row): Int = sorter.compare(cols, l, r, col)
  }

  class ReverseSortOrder(col: Int, cols: List[ColumnBase])(implicit
      sorter: Sorter[_]
  ) extends Ordering[Row] {
    def compare(l: Row, r: Row): Int = sorter.compare(cols, r, l, col)
  }

  /**
    * Internal state for rendering the component.
    *
    * I'd like this class to be private, but the instantiation of component
    * will cause Backend to leak.
    */
  class Backend(scope: BackendScope[Props, State]) {

    def setOrToggleSort(col: SortableColumn[_]): Callback =
      scope.modState(s => s.setOrToggleSort(col))

    def render(props: Props, state: State) = { // scalafix:ok ExplicitResultTypes; React
      log.fine(s"Table columns=${props.columns}")
      val rows = state.currentSort
        .map { sortid =>
          props.columns.indexWhere { c =>
            if (c.isInstanceOf[SortableColumn[_]])
              c.asInstanceOf[SortableColumn[_]].id == sortid
            else false
          } match {
            case sort if sort < 0 =>
              props.rows
            case sort =>
              props.columns(sort) match {
                case sortColumn: SortableColumn[_] =>
                  val sorter = sortColumn.sorter
                  implicit val ordering =
                    if (state.ascending)
                      new SortOrder(sort, props.columns)(sorter)
                    else new ReverseSortOrder(sort, props.columns)(sorter)
                  val data = if (sortColumn.useAdditionalDataWhenSorting) {
                    props.rows ::: props.additionalRows
                      .map(f => f())
                      .getOrElse(List())
                  } else {
                    props.rows
                  }
                  data.sorted
                case _ =>
                  props.rows
              }
          }
        }
        .getOrElse(props.rows)
      <.table(
        baseStyles.tableComponent,
        props.caption.whenDefined { c => <.caption(c) },
        TableHeader((props, state, this)),
        (props.footer.isDefined || props.totalRows.isDefined) ?= <.tfoot(
          props.totalRows.map { tpds =>
            tpds.zipWithIndex.map { entry =>
              val (row, i) = entry
              val x = TableRow.withKey(s"T$i")((props, state, this, row))
              x
            }.toTagMod
          }.whenDefined,
          props.footer.whenDefined
        ),
        <.tbody(
          rows.zipWithIndex.map { entry =>
            val (row, i) = entry
            val x = TableRow.withKey(i)((props, state, this, row))
            x
          }.toTagMod
        )
      )
    }
  }

  private[react] val component = ScalaComponent
    .builder[Props]("Table")
    .initialStateFromProps { props => State(props.initialSort) }
    .backend(new Backend(_))
    .renderBackend
    .build
}
