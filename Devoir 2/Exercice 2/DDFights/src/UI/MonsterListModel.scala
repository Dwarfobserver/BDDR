package UI

import common.ActorType
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

import scala.collection.mutable.ListBuffer

class MonsterListModel extends TableModel{

    private var _listeners: ListBuffer[TableModelListener] = ListBuffer()

    override def removeTableModelListener(l: TableModelListener): Unit = {
        _listeners -= l
    }

    override def getRowCount: Int = {
        ActorType.values.size
    }

    override def addTableModelListener(l: TableModelListener): Unit = {
        _listeners += l
    }

    override def getColumnName(columnIndex: Int): String = {
        "Monste List"
    }

    override def getColumnClass(columnIndex: Int): Class[_] = {
        ActorType.getClass
    }

    override def getColumnCount: Int = {
        1
    }

    override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
        ActorType.values.toList(rowIndex)
    }

    override def setValueAt(aValue: scala.Any, rowIndex: Int, columnIndex: Int): Unit = {}

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false


}
