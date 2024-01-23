package ooo.decode


import chisel3._
import chisel3.util.{Cat, DeqIO, Fill, MuxCase, MuxLookup, Valid}
import ooo.{CoreParams, InstructionDecoded, PipelineUnit}
import ooo.execute.ALUOps
import ooo.utils.RISCVDasm

class DecodeUnit(implicit p: CoreParams) extends PipelineUnit {
  val io = IO(new Bundle {
    val instr = Input(Valid(UInt(32.W)))
    val decoded = Output(Valid(new InstructionDecoded))
  })

  val instr = io.instr.bits

  val rd = instr(11, 7)
  val funct3 = instr(14, 12)
  val funct7 = instr(31, 25)
  val opcode = instr(6, 0)

  val rs1 = instr(19, 15)
  val rs2 = instr(24, 20)

  // Immediate values
  val imm_u_type = Cat(instr(31, 12), 0.U(12.W))
  val imm_i_type = Cat(Fill(20, instr(31)), instr(31, 20))
  val imm_b_type = Cat(Fill(19, instr(31)), instr(31), instr(7), instr(30, 25), instr(11, 8), 0.U(1.W))
  val imm_j_type = Cat(Fill(11, instr(31)), instr(31), instr(19, 12), instr(20), instr(30, 21), 0.U(1.W))
  val imm_s_type = Cat(Fill(20, instr(31)), instr(31, 25), instr(11, 7))


  val is_i_type = opcode === "b0010011".U
  val is_r_type = opcode === "b0110011".U
  val is_b_type = opcode === "b1100011".U
  val is_load = opcode === "b0000011".U
  val is_store = opcode === "b0100011".U
  val is_jal = opcode === "b1101111".U
  val is_jalr = opcode === "b1100111".U
  val is_lui = opcode === "b0110111".U
  val is_auipc = opcode === "b0010111".U

  val is_i_r_type = is_i_type | is_r_type
  val altfunct7 = funct7 === "h20".U

  val ir_alu_op = MuxLookup(funct3, ALUOps.A, Seq(
    "b000".U -> Mux((is_i_type), ALUOps.ADD, Mux(altfunct7, ALUOps.SUB, ALUOps.ADD)),
    "b001".U -> ALUOps.SLL,
    "b010".U -> ALUOps.SLT,
    "b011".U -> ALUOps.SLTU,
    "b100".U -> ALUOps.XOR,
    "b101".U -> Mux(altfunct7, ALUOps.SRA, ALUOps.SRL),
    "b110".U -> ALUOps.OR,
    "b111".U -> ALUOps.AND
  ))

  val alu_op = Wire(UInt(4.W))

  // Select ALU operation
  when(is_lui) {
    alu_op := ALUOps.B
  }.elsewhen(is_load) {
    alu_op := ALUOps.ADD
  }.elsewhen(is_i_r_type) {
    alu_op := ir_alu_op
  }.otherwise {
    alu_op := ALUOps.A
  }

  val decoded = Reg(new InstructionDecoded)

  when(pipeline.stall) {
    printf("Decode stalled\n");
  }

  pipeline.stalled := pipeline.stall
  when(!pipeline.stall && io.instr.valid) {

//    printf("IF: ")
//    RISCVDasm(instr)

    decoded.instr := instr

    decoded.rs1 := rs1
    decoded.rs2 := rs2
    decoded.alu_op := alu_op
    decoded.rd := rd
    decoded.is_i_type := is_i_type
    decoded.is_r_type := is_r_type

    decoded.immediate := imm_i_type.asSInt
    decoded.needs_rs2 := is_r_type || is_store || is_b_type

  }.otherwise {
    decoded.rs1 := 0.U
    decoded.rs2 := 0.U
    decoded.alu_op := ALUOps.A
    decoded.rd :=  0.U
    decoded.is_i_type := false.B
    decoded.is_r_type := false.B

    decoded.instr := 0.U

    decoded.immediate :=  0.S
    decoded.needs_rs2 := false.B
  }

  val valid = RegNext(io.instr.valid, !pipeline.stall)
  io.decoded.bits := decoded
  io.decoded.valid := valid
}


