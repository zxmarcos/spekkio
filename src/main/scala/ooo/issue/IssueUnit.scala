package ooo.issue

import chisel3._
import chisel3.util.Valid
import ooo.utils.{MultiportMemoryWritePort, SequenceCircularBuffer}
import ooo.{CoreParams, InstructionDispatched, InstructionIssued, PipelineUnit}


class IssueIO(implicit p: CoreParams) extends Bundle {
  val instruction = Input(Valid(new InstructionIssued))
  val selected = Output(Valid(UInt(p.issueSize.W)))

  val dispatched = Output(Valid(new InstructionDispatched))

  val commit_write_port = Input(new MultiportMemoryWritePort)
}


class IssueUnit(implicit p: CoreParams) extends PipelineUnit {
  val io = IO(new IssueIO)
  val free_list = Module(new SequenceCircularBuffer(p.issueEntries, p.issueSize))

  // We stall when there is no free slot
  pipeline.stalled := !free_list.io.initalized || pipeline.stall || free_list.io.is_empty

  when(pipeline.stall) {
    printf("Issue stalled\n");
  }


  // Our reservation stations
  val rsv_stations = Module(new IssueQueue)

  rsv_stations.io.wakeup(0).reg.bits := io.commit_write_port.addr
  rsv_stations.io.wakeup(0).reg.valid := io.commit_write_port.enable


  // Allocate next free slot for instruction
  when(!pipeline.stalled && io.instruction.valid) {
    rsv_stations.io.allocate.valid := true.B
    // Get free slot from our list
    rsv_stations.io.allocate.bits.index := free_list.io.deq.deq()
    // ...
    rsv_stations.io.allocate.bits.rd := io.instruction.bits.rd
    rsv_stations.io.allocate.bits.rs1 := io.instruction.bits.rs1
    rsv_stations.io.allocate.bits.rs2 := io.instruction.bits.rs2
    rsv_stations.io.allocate.bits.rs1_valid := io.instruction.bits.rs1_valid
    // Send always valid for r2 when not needed...
    rsv_stations.io.allocate.bits.rs2_valid := Mux(io.instruction.bits.needs_rs2, io.instruction.bits.rs2_valid, true.B)
    rsv_stations.io.allocate.bits.immediate := io.instruction.bits.immediate
    rsv_stations.io.allocate.bits.alu_op := io.instruction.bits.alu_op
    rsv_stations.io.allocate.bits.needs_rs2 := io.instruction.bits.needs_rs2

    rsv_stations.io.allocate.bits.instr := io.instruction.bits.instr
  }.otherwise {
    free_list.io.deq.nodeq()
    rsv_stations.io.allocate.bits.index := 0.U
    rsv_stations.io.allocate.valid := false.B
    rsv_stations.io.allocate.bits.rd := 0.U
    rsv_stations.io.allocate.bits.rs1 := 0.U
    rsv_stations.io.allocate.bits.rs2 := 0.U
    rsv_stations.io.allocate.bits.rs1_valid := false.B
    rsv_stations.io.allocate.bits.rs2_valid := false.B
    rsv_stations.io.allocate.bits.needs_rs2 := false.B
    rsv_stations.io.allocate.bits.alu_op := false.B
    rsv_stations.io.allocate.bits.immediate := 0.S
    rsv_stations.io.allocate.bits.instr := 0.U
  }


  free_list.io.enq.bits := 0.U
  free_list.io.enq.valid := false.B

  val dispatch = Module(new Scheduler)
  dispatch.io.ready_list <> rsv_stations.io.ready_list

  // If there is a work to dispatch and pipeline is not stalled or stalled by no slots left in free_list
  when(!dispatch.io.empty && (!pipeline.stalled || free_list.io.is_empty)) {
    io.selected.valid := true.B
    io.selected.bits := dispatch.io.selected_idx
    val slot = rsv_stations.io.slots(dispatch.io.selected_idx)

    // Free selected slot.
    free_list.io.enq.enq(dispatch.io.selected_idx)
    rsv_stations.io.free.bits := dispatch.io.selected_idx
    rsv_stations.io.free.valid := true.B

    io.dispatched.valid := true.B
    io.dispatched.bits.rd := slot.rd
    io.dispatched.bits.rs1 := slot.rs1
    io.dispatched.bits.rs2 := slot.rs2
    io.dispatched.bits.alu_op := slot.alu_op
    io.dispatched.bits.needs_rs2 := slot.needs_rs2
    io.dispatched.bits.immediate := slot.immediate
    io.dispatched.bits.instr := slot.instr
  }.otherwise {
    free_list.io.enq.noenq()
    io.selected.valid := false.B
    io.selected.bits := DontCare
    io.dispatched.bits := DontCare
    io.dispatched.valid := false.B
    rsv_stations.io.free.valid := false.B
    rsv_stations.io.free.bits := 0.U
  }

}
