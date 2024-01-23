package ooo.utils.experimental

import chisel3._
import chisel3.util.Counter
import ooo.{CoreParams, Spekkio}
import ooo.issue.IssueSlot
import ooo.utils.SelectFirst


object Experiments extends App {


  val numbers = List(2, 4, 6, 2)
  val results = numbers.scanLeft(1)(_ + _)
  println(results)
}


class FuncionalUnitFilter(fuCode: Int)(implicit p: CoreParams) extends Module {
  val io = IO(new Bundle {
    val slots = Input(Vec(p.issueEntries, new IssueSlot()))
    val fu_slots = Output(Vec(p.issueEntries, Bool()))
  })

  // Compare using OneHot encoding
  val fu_slots = io.slots.map((_.alu_op === fuCode.U)).zipWithIndex
  val mask = Wire(Vec(p.issueEntries, Bool()))
  for ((v, i) <- fu_slots) {
    mask(i) := v
  }
  io.fu_slots := mask
}

class FuncionalUnitSelect(n:Int)(implicit p: CoreParams) extends Module {
  val io = IO(new Bundle {
    val slots = Input(Vec(p.issueEntries, new IssueSlot()))
    val ready_list = Input(Vec(p.issueEntries, Bool()))
    val selected_slots = Output(Vec(n, UInt(p.issueEntries.W)))
  })

  val filter = Module(new FuncionalUnitFilter(2))
  val fu_slots = Wire(Vec(p.issueEntries, Bool()))
  filter.io.slots <> io.slots
  fu_slots := filter.io.fu_slots

  val valid_mask = (fu_slots.asUInt & io.ready_list.asUInt)
  io.selected_slots := SelectFirst(valid_mask, n)
}

class XRAT()(implicit p: CoreParams) extends Module {
  val io = IO(new Bundle {
    val check = Input(Bool())
    val waddr = Input(UInt(p.prfSize.W))
    val wdata = Input(UInt(p.prfSize.W))
    val wen = Input(Bool())

    val restore = Input(Bool())
    val restoreidx = Input(UInt(3.W))

    val rd1addr = Input(UInt(p.ratSize.W))
    val rd2addr = Input(UInt(p.ratSize.W))
    val rd1data = Output(UInt(p.prfSize.W))
    val rd2data = Output(UInt(p.prfSize.W))
  })

  val r = Reg(Vec(p.ratEntries, UInt(p.prfSize.W)))

  when(io.wen) {
    r(io.waddr) := io.wdata
  }

  val activeCheckpoint = Counter(4)
  val currentCheckpoint = Reg(UInt(2.W))
  val checkPoint = Reg(Vec(4, Vec(p.ratEntries, UInt(p.prfSize.W))))
  when(io.check) {
    checkPoint(activeCheckpoint.value) := r
    currentCheckpoint := activeCheckpoint.value
    activeCheckpoint.inc()
  }

  when(io.restore) {
    r := checkPoint(io.restoreidx)
  }

  io.rd1data := r(io.rd1addr)
  io.rd2data := r(io.rd2addr)
}

object Main extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new XRAT()(CoreParams()), Array("--target-dir", "output/", "--emission-options", "disableMemRandomization,disableRegisterRandomization"))
}