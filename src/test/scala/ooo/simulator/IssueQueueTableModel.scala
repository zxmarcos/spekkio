package ooo.simulator


import chiseltest.simulator.SimulatorContext

import javax.swing.event.{TableModelEvent, TableModelListener}
import javax.swing.table.DefaultTableModel
import scala.collection.mutable.ArrayBuffer

object IntToBoolean {
  def apply(data: Int): Boolean = {
    if (data > 0)
      return true
    false
  }
}

case class IssueEntryModel(i: Int) {
  val valid = s"iq_${i}_valid"
  val rs1 = s"iq_${i}_rs1"
  val rs2 = s"iq_${i}_rs2"
  val rs1_valid = s"iq_${i}_rs1_valid"
  val rs2_valid = s"iq_${i}_rs2_valid"
  val needs_rs2 = s"iq_${i}_needs_rs2"
  val imm = s"iq_${i}_imm"
  val rd = s"iq_${i}_rd"
  val alu_op = s"iq_${i}_alu_op"
  val ready = s"iq_${i}_ready"
}


class IssueQueueTableModel(dut: SimulatorContext, nrEntries: Int) extends DefaultTableModel {

  lazy val entries = ArrayBuffer[IssueEntryModel]()

  for (i <- 0 until nrEntries) {
    entries.addOne(IssueEntryModel(i))
  }


  override def getRowCount: Int = {
    entries.length
  }

  override def getColumnCount: Int = 10

  override def getColumnName(columnIndex: Int): String = {

    columnIndex match {
      case (0) => "v"
      case (1) => "rd"
      case (2) => "rs1"
      case (3) => "rs1v"
      case (4) => "rs2"
      case (5) => "rs2v"
      case (6) => "rs2n"
      case (7) => "imm"
      case (8) => "alu"
      case (9) => "ready"
      case _ => ""
    }
  }

  override def getColumnClass(columnIndex: Int): Class[_] = {
    columnIndex match {
      case (0) => classOf[Boolean]
      case (1) => classOf[Int]
      case (2) => classOf[Int]
      case (3) => classOf[Boolean]
      case (4) => classOf[Int]
      case (5) => classOf[Boolean]
      case (6) => classOf[Boolean]
      case (7) => classOf[Int]
      case (8) => classOf[Int]
      case (9) => classOf[Boolean]
      case _ => classOf[String]
    }
  }

  override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false

  override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
    if (rowIndex < entries.length && columnIndex < getColumnCount) {
      val r = entries(rowIndex)
      return columnIndex match {
        case (0) => IntToBoolean(dut.peek(r.valid).toInt).asInstanceOf[AnyRef]
        case (1) => dut.peek(r.rd)
        case (2) => dut.peek(r.rs1)
        case (3) => IntToBoolean(dut.peek(r.rs1_valid).toInt).asInstanceOf[AnyRef]
        case (4) => dut.peek(r.rs2)
        case (5) => IntToBoolean(dut.peek(r.rs2_valid).toInt).asInstanceOf[AnyRef]
        case (6) => IntToBoolean(dut.peek(r.needs_rs2).toInt).asInstanceOf[AnyRef]
        case (7) => dut.peek(r.imm)
        case (8) => dut.peek(r.alu_op)
        case (9) => IntToBoolean(dut.peek(r.ready).toInt).asInstanceOf[AnyRef]
        case _ => None
      }
    }
    None
  }

  override def setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int): Unit = {}

  override def addTableModelListener(l: TableModelListener): Unit = {}

  override def removeTableModelListener(l: TableModelListener): Unit = {}
}

