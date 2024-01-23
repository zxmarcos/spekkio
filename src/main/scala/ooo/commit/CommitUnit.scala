package ooo.commit

import ooo.{CoreParams, InstructionResult, PipelineUnit}
import chisel3._
import chisel3.util.Valid
import ooo.utils.MultiportMemoryWritePort

class CommitUnit(implicit p: CoreParams) extends PipelineUnit {
  val io = IO(new Bundle {
    val result = Input(Valid(new InstructionResult))
    val prf_write_port = Flipped(new MultiportMemoryWritePort())
  })

  when(io.result.valid) {
    io.prf_write_port.write(io.result.bits.rd, io.result.bits.value)
  }.otherwise {
    io.prf_write_port.addr := 0.U
    io.prf_write_port.data := 0.U
    io.prf_write_port.enable := false.B
  }
}
