package ooo

import chisel3._

class ProgramCounter extends PipelineUnit {
  val io = IO(new Bundle {
    val value = Output(UInt(32.W))
  })

  val pc = RegInit(UInt(32.W), 0.U)

  when(!pipeline.stall) {
    pc := pc +% 4.U
  }
  io.value := pc
}
