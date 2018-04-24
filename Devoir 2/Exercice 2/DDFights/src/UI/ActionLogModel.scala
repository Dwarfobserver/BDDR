package UI

import engine.Actor
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

import scala.collection.mutable.ListBuffer

class ActionLogModel() extends TableModel{

    private var _actor: Actor = _
    private var _listeners: ListBuffer[TableModelListener] = ListBuffer()

    override def removeTableModelListener(l: TableModelListener): Unit = _listeners -= l

    override def getColumnClass(columnIndex: Int): Class[_] = "".getClass

    override def getColumnCount: Int = 1

    override def getColumnName(columnIndex: Int): String = "Action Log"

    override def getRowCount: Int = 0

    override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = ""

    override def addTableModelListener(l: TableModelListener): Unit = _listeners += l

    override def setValueAt(aValue: scala.Any, rowIndex: Int, columnIndex: Int): Unit = {}

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

    def setActor(actor: Actor): Unit ={
        _actor = actor
    }

}
