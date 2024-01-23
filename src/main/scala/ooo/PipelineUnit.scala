package ooo

import chisel3._
class PipelineUnit extends Module {
  val pipeline = IO(new Bundle {
    val stalled = Output(Bool())
    val stall = Input(Bool())
  })
  // Not stalled by default...
  pipeline.stalled := false.B
}