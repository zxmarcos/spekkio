package ooo.simulator

import chisel3.PrintableHelper
import chiseltest.simulator.SimulatorContext

import javax.swing.event.{TableModelEvent, TableModelListener}
import javax.swing.table.DefaultTableModel
import scala.collection.mutable.ArrayBuffer

case class RegisterSignal(addr: Int, name: String, signal: String, busySignal: String="")


class RegisterTableModel(dut: SimulatorContext, hasValid: Boolean = false) extends DefaultTableModel {
  lazy val registers = ArrayBuffer[RegisterSignal]()

  def append(signal: RegisterSignal): Unit = {
    registers.addOne(signal)
    fireTableChanged(new TableModelEvent(this))
  }

  override def getRowCount: Int = {
    registers.length
  }

  override def getColumnCount: Int = if (hasValid)  3 else 2

  override def getColumnName(columnIndex: Int): String = {
    columnIndex match {
      case(0) => "Nome"
      case(1) => "Valor"
      case(2) => "ST"
      case _ => ""
    }
  }

  override def getColumnClass(columnIndex: Int): Class[_] = {
    columnIndex match {
      case (0) => classOf[String]
      case (1) => classOf[Int]
      case (2) => classOf[Boolean]
      case _ => classOf[String]
    }
  }

  override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

  override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
    if (rowIndex < registers.length && columnIndex < 3) {
      val r = registers(rowIndex)
      return columnIndex match {
        case (0) => r.name
        case (1) => dut.peek(r.signal)
        case (2) => IntToBoolean(dut.peek(r.busySignal).toInt).asInstanceOf[AnyRef]
        case _ => None
      }
    }
    None
  }

  override def setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int): Unit = {}

  override def addTableModelListener(l: TableModelListener): Unit = {}

  override def removeTableModelListener(l: TableModelListener): Unit = {}
}
