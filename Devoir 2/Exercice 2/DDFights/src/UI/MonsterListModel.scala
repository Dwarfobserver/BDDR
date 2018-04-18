package UI

import common.ActorType
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

class MonsterListModel extends TableModel{


    override def removeTableModelListener(l: TableModelListener): Unit = {

    }

    override def getRowCount: Int = {
        ActorType.values.size
    }

    override def addTableModelListener(l: TableModelListener): Unit = {

    }

    override def getColumnName(columnIndex: Int): String = {
        "MonsteList"
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

    override def setValueAt(aValue: scala.Any, rowIndex: Int, columnIndex: Int): Unit = ???

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false


}
