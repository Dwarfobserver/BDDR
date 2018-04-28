package UI

import common.ActorType
import javax.swing.table.AbstractTableModel

class MonsterListModel extends AbstractTableModel{

    override def getRowCount: Int = {
        ActorType.values.size
    }

    override def getColumnName(columnIndex: Int): String = {
        "Monster List"
    }

    override def getColumnCount: Int = {
        1
    }

    override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
        ActorType.values.toList(rowIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false


}
