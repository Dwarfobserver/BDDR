package UI

import engine.{Actor, ActorModel}
import javax.swing.table.AbstractTableModel

class ActionListModel() extends AbstractTableModel{

    private var _actor: Actor = _

    override def getColumnCount: Int = 1

    override def getColumnName(columnIndex: Int): String = "Action List"

    override def getRowCount: Int = {
        if(_actor != null) ActorModel.from(_actor.t).actions.size
        else 0
    }

    override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
        ActorModel.from(_actor.t).actions.toVector(rowIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

    def setActor(actor: Actor): Unit ={
        _actor = actor
        fireTableStructureChanged()
    }

}
