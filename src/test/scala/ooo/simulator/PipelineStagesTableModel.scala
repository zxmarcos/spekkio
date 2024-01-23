package ooo.simulator

import chiseltest.simulator.SimulatorContext

import javax.swing.event.{TableModelEvent, TableModelListener}
import javax.swing.table.DefaultTableModel
import scala.collection.mutable.ArrayBuffer


case class PipelineStageEntry(name: String) {
  val valid = s"${name}_valid"
  val instr = s"${name}_instr"
  val stalled = s"${name}_stalled"
}


class PipelineStagesTableModel(dut: SimulatorContext) extends DefaultTableModel {
  lazy val stages = ArrayBuffer[PipelineStageEntry]()

  stages.addOne(PipelineStageEntry("fetch"))
  stages.addOne(PipelineStageEntry("decode"))
  stages.addOne(PipelineStageEntry("rename1"))
  stages.addOne(PipelineStageEntry("rename2"))
  stages.addOne(PipelineStageEntry("issue"))
  stages.addOne(PipelineStageEntry("regread"))
  stages.addOne(PipelineStageEntry("alu"))
  stages.addOne(PipelineStageEntry("commit"))

  override def getRowCount: Int = {
    stages.length
  }

  override def getColumnCount: Int = 4

  override def getColumnName(columnIndex: Int): String = {
    columnIndex match {
      case 0 => "Nome"
      case 1 => "Instr"
      case 2 => "V"
      case 3 => "Stall"
      case _ => ""
    }
  }

  override def getColumnClass(columnIndex: Int): Class[_] = {
    columnIndex match {
      case 0 => classOf[String]
      case 1 => classOf[String]
      case 2 => classOf[Boolean]
      case 3 => classOf[Boolean]
      case _ => classOf[String]
    }
  }

  override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

  override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
    if (rowIndex < stages.length && columnIndex < 4) {
      val r = stages(rowIndex)
      return columnIndex match {
        case 0 => r.name
        case 1 => Dasm(dut.peek(r.instr).toInt)
        case 2 => IntToBoolean(dut.peek(r.valid).toInt).asInstanceOf[AnyRef]
        case 3 => IntToBoolean(dut.peek(r.stalled).toInt).asInstanceOf[AnyRef]
        case _ => None
      }
    }
    None
  }

  override def setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int): Unit = {}

  override def addTableModelListener(l: TableModelListener): Unit = {}

  override def removeTableModelListener(l: TableModelListener): Unit = {}
}
