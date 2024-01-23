package ooo.execute
import chisel3._
import chisel3.util.{MuxLookup, Valid}
import ooo.{CoreParams, InstructionDispatched, InstructionReady, InstructionResult, PipelineUnit}

object ALUOps {
  val ADD  = 0.U(4.W)
  val SLL  = 1.U(4.W)
  val SLT  = 2.U(4.W)
  val SLTU = 3.U(4.W)
  val XOR  = 4.U(4.W)
  val SRL  = 5.U(4.W)
  val OR   = 6.U(4.W)
  val AND  = 7.U(4.W)
  val SUB  = 8.U(4.W)
  val SRA  = 9.U(4.W)
  val A    = 10.U(4.W)
  val B    = 11.U(4.W)
  val EQ   = 12.U(4.W)
}

class ALU extends Module {
  val io = IO(new Bundle {
    val a      = Input(UInt(32.W))
    val b      = Input(UInt(32.W))
    val op     = Input(UInt(4.W))
    val result = Output(UInt(32.W))
  })

  val shamt = io.b(4,0).asUInt

  io.result := MuxLookup(io.op, ALUOps.A, Seq(
    ALUOps.ADD  -> (io.a + io.b),
    ALUOps.SLL  -> (io.a << shamt),
    ALUOps.SLT  -> (io.a.asSInt < io.b.asSInt).asUInt,
    ALUOps.SLTU -> (io.a < io.b).asUInt,
    ALUOps.XOR  -> (io.a ^ io.b),
    ALUOps.SRL  -> (io.a >> shamt),
    ALUOps.OR   -> (io.a | io.b),
    ALUOps.AND  -> (io.a & io.b),
    ALUOps.SUB  -> (io.a - io.b),
    ALUOps.SRA  -> (io.a.asSInt >> shamt).asUInt,
    ALUOps.A    -> (io.a),
    ALUOps.B    -> (io.b),
    ALUOps.EQ   -> (io.a === io.b)
  ))
}

class ALUUnit(implicit p: CoreParams) extends PipelineUnit {

  val io = IO(new Bundle {
    val instruction = Input(Valid(new InstructionReady))
    val result = Output(Valid(new InstructionResult))
  })

  val alu = Module(new ALU)
  // Always ready...
  pipeline.stalled := false.B

  val result = Reg(Valid(new InstructionResult))
  alu.io.a := io.instruction.bits.rs1
  alu.io.b := Mux(io.instruction.bits.needs_rs2, io.instruction.bits.rs2, io.instruction.bits.immediate.asUInt)
  alu.io.op := io.instruction.bits.alu_op

  when(io.instruction.valid) {
    result.valid := true.B
    result.bits.rd := io.instruction.bits.rd
    result.bits.value := alu.io.result
    result.bits.instr := io.instruction.bits.instr
  }.otherwise {
    result.valid := false.B
    result.bits.rd := 0.U
    result.bits.value := 0.U
    result.bits.instr := 0.U
  }

  io.result := result
}
