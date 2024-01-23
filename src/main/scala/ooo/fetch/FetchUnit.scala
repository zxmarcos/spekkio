package ooo.fetch

import chisel3._
import chisel3.util.{Queue, Valid}
import ooo.PipelineUnit
import ooo.utils.WishboneIO

class FetchUnit extends PipelineUnit {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val wb = Flipped(new WishboneIO)
    val instr = Output(Valid(UInt(32.W)))
  })

  pipeline.stalled := pipeline.stall || io.wb.stall

  val strobe = RegInit(false.B)
  val addr = RegInit(0.U(32.W))
  val instr = RegInit(0.U(32.W))
  val valid = RegInit(false.B)

  when (!pipeline.stall) {
    strobe := true.B
    addr := io.pc >> 2.U

    printf(cf"IFU PC ${io.pc}\n")
  }.otherwise {
    strobe := false.B
  }

  when(io.wb.ack) {
    instr := io.wb.slave_data
    valid := true.B
  }.otherwise {
    valid := false.B
  }

  io.wb.strobe := strobe
  io.wb.write_enable := false.B
  io.wb.address := addr
  io.wb.master_data := 0.U
  io.wb.cycle := true.B

  io.instr.bits := instr
  io.instr.valid := valid
}
