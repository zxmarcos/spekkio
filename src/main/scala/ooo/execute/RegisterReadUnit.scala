package ooo.execute

import ooo.{CoreParams, InstructionDispatched, InstructionReady, PipelineUnit}
import chisel3._
import chisel3.util.Valid
import ooo.utils.MultiportMemoryReadPort

class RegisterReadUnit(implicit p: CoreParams) extends PipelineUnit {

  val io = IO(new Bundle {
    val instr = Input(Valid(new InstructionDispatched))
    val ready = Output(Valid(new InstructionReady))

    val prf_read_port = Flipped(Vec(2, new MultiportMemoryReadPort(p.prfMemoryConfig)))
  })

  pipeline.stalled := false.B

  val ready = Reg(Valid(new InstructionReady))

  io.prf_read_port(0).read(io.instr.bits.rs1)
  io.prf_read_port(1).read(io.instr.bits.rs2)

  when(io.instr.valid) {
    ready.bits.rs1 := io.prf_read_port(0).data
    ready.bits.rs2 := io.prf_read_port(1).data
    ready.bits.alu_op := io.instr.bits.alu_op
    ready.bits.needs_rs2 := io.instr.bits.needs_rs2
    ready.bits.immediate := io.instr.bits.immediate
    ready.bits.rd := io.instr.bits.rd
    ready.bits.instr := io.instr.bits.instr
    ready.valid := true.B
  }.otherwise {
    ready.bits.rs1 := 0.U
    ready.bits.rs2 := 0.U
    ready.bits.alu_op := ALUOps.A
    ready.bits.needs_rs2 := false.B
    ready.bits.immediate := 0.S
    ready.bits.rd := 0.U
    ready.bits.instr := 0.U
    ready.valid := false.B
  }

  io.ready := ready
}
