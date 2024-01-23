package ooo.issue

import chisel3._
import chisel3.util.{PriorityEncoder, PriorityEncoderOH}
import ooo.{CoreParams}

class Scheduler(implicit p: CoreParams) extends Module {

  val io = IO(new Bundle {
    val ready_list = Input(Vec(p.issueEntries, Bool()))
    val empty = Output(Bool())
    val selected_idx = Output(UInt(p.issueSize.W))
  })

  io.empty := !(io.ready_list.asUInt).orR
  // Select first valid index...
  val selected_idx = PriorityEncoder(io.ready_list)

  io.selected_idx := selected_idx

}
