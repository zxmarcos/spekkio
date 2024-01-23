package ooo.issue

import chisel3._
import chisel3.util.Valid
import ooo.CoreParams

class IssueSlot(implicit p: CoreParams) extends Bundle {
  val instr = UInt(32.W)
  val rs1 = UInt(p.prfSize.W)
  val rs2 = UInt(p.prfSize.W)
  val rs1_valid = Bool()
  val rs2_valid = Bool()
  val rd = UInt(p.prfSize.W)
  val needs_rs2 = Bool()
  val immediate = SInt(32.W)
  val index = UInt(6.W)
  val alu_op = UInt(4.W)
}

class IssueWakeupPort(implicit p: CoreParams) extends Bundle {
  val reg = Valid(UInt(p.prfSize.W))
}

class IssueQueue(implicit p: CoreParams) extends Module {
  val io = IO(new Bundle {
    val allocate = Input(Valid(new IssueSlot))
    val wakeup = Input(Vec(p.issueWakeupPorts, new IssueWakeupPort))
    val slots = Output(Vec(p.issueEntries, new IssueSlot))
    val ready_list = Output(Vec(p.issueEntries, Bool()))

    val free = Input(Valid(UInt(p.issueSize.W)))
    
  })

  val slots = Reg(Vec(p.issueEntries, new IssueSlot))
  val valid_slots = Reg(Vec(p.issueEntries, Bool()))

  io.slots <> slots

  when(io.allocate.valid) {
    slots(io.allocate.bits.index) := io.allocate.bits
    valid_slots(io.allocate.bits.index) := true.B
  }

  when(io.free.valid) {
    valid_slots(io.free.bits) := false.B
  }

  for (i <- 0 until p.issueEntries) {
    for (wakePort <- 0 until p.issueWakeupPorts) {
      when(valid_slots(i)) {
        when(io.wakeup(wakePort).reg.bits === slots(i).rs1 && io.wakeup(wakePort).reg.valid) {
          slots(i).rs1_valid := true.B
        }

        // Compare RS2 only when it is needed
        when(slots(i).needs_rs2) {
          when(io.wakeup(wakePort).reg.bits === slots(i).rs2 && io.wakeup(wakePort).reg.valid) {
            slots(i).rs2_valid := true.B
          }
        }
      }
    }
  }

  io.ready_list := Seq.from(slots).zipWithIndex.map { case(e,idx) => e.rs1_valid && Mux(e.needs_rs2,e.rs2_valid,true.B) && valid_slots(idx) }
}
