package ooo.commit

import chisel3._
import chisel3.util.{Counter, EnqIO, Valid}
import ooo.utils.MultiportMemoryWritePort
import ooo.{CoreParams, PipelineUnit}

class RobAllocate(implicit p: CoreParams) extends Bundle {
  val rob_idx = UInt(p.robSize.W)
  val prf = UInt(p.prfSize.W)
  val last_prf = UInt(p.prfSize.W)
}

class RobCommit(implicit p: CoreParams) extends Bundle {
  val rob_idx = UInt(p.robSize.W)
}

class RobEntry(implicit p: CoreParams) extends Bundle {
  val rrf = UInt(p.ratSize.W)
  val prf = UInt(p.prfSize.W)
  val last_prf = UInt(p.prfSize.W)
  val ready = Bool()
  val instr = UInt(32.W)
}

class ReorderBuffer(implicit p: CoreParams) extends PipelineUnit {
  val io = IO(new Bundle {
    val is_full = Output(Bool())
    val is_empty = Output(Bool())

    val allocate = EnqIO(new RobAllocate)
    val allocated_rob_idx = Output(UInt(p.robSize.W))
    val commit = Input(Valid(new RobCommit))

    val rrf_write = Flipped(Vec(1, new MultiportMemoryWritePort(p.rrfMemoryConfig)))
  })
  pipeline.stalled := false.B

  val entries = Reg(Vec(p.robEntries, new RobEntry))

  val head_ptr = Counter(p.robEntries)
  val tail_ptr = Counter(p.robEntries)
  val maybe_full = RegInit(false.B)
  val head_tail_ptr_match = (head_ptr.value === tail_ptr.value)
  val is_empty = head_tail_ptr_match && !maybe_full
  val is_full = head_tail_ptr_match && maybe_full

  io.is_full := is_full
  io.is_empty := is_empty

  val oldest = entries(tail_ptr.value)

  // Can commit the oldest instruction!
  io.rrf_write(0).enable := false.B
  io.rrf_write(0).addr := 0.U

  when(io.commit.valid) {
    entries(io.commit.bits.rob_idx).ready := true.B
  }

  when(oldest.ready && !is_empty) {
    tail_ptr.inc()

    // Commit ROB entry to Retirement Register File
    io.rrf_write(0).write(oldest.rrf, oldest.prf)
    // TODO: Push last_prf to PRF Free List

    when(tail_ptr.value =/= head_ptr.value) {
      maybe_full := false.B
    }
  }

  io.allocated_rob_idx := head_ptr.value
  io.allocate.ready := !is_full
  when(io.allocate.fire) {
    entries(head_ptr.value).prf := io.allocate.bits.prf
    entries(head_ptr.value).last_prf := io.allocate.bits.last_prf
    entries(head_ptr.value).ready := false.B

    // detect maybe_full
    when(head_ptr.inc()) {
      maybe_full := true.B
    }
  }


}
