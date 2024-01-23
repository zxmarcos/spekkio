package ooo

import chisel3._

class MicroOp(implicit p: CoreParams) extends Bundle {
  val instr = UInt(32.W)
  val rs1 = UInt(p.prfSize.W)
  val rs2 = UInt(p.prfSize.W)
  val rd = UInt(p.prfSize.W)
  val alu_op = UInt(4.W)
  val immediate = SInt(32.W)
  val needs_rs2 = Bool()
  val func_unit = UInt(2.W)
  val rob_idx = UInt(p.robSize.W)
}

class BaseInstruction extends Bundle {
  val instr = UInt(32.W)
}

class InstructionDecoded(implicit p: CoreParams) extends BaseInstruction {
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)
  val rd = UInt(5.W)
  val alu_op = UInt(4.W)
  val immediate = SInt(32.W)
  val is_i_type = Bool()
  val is_r_type = Bool()
  val needs_rs2 = Bool()
  val rob_idx = UInt(p.robSize.W)
}

class InstructionRenamed(implicit p: CoreParams) extends BaseInstruction {
  val rs1 = UInt(p.prfSize.W)
  val rs2 = UInt(p.prfSize.W)
  val rd = UInt(p.prfSize.W)
  val alu_op = UInt(4.W)
  val immediate = SInt(32.W)

  val is_i_type = Bool()
  val is_r_type = Bool()

  val needs_rs2 = Bool()
}

class InstructionIssued(implicit p: CoreParams) extends InstructionRenamed {
  val rs1_valid = Bool()
  val rs2_valid = Bool()
}

class InstructionDispatched(implicit  p: CoreParams) extends BaseInstruction {
  val rs1 = UInt(p.prfSize.W)
  val rs2 = UInt(p.prfSize.W)
  val rd = UInt(p.prfSize.W)
  val alu_op = UInt(4.W)
  val immediate = SInt(32.W)
  val needs_rs2 = Bool()
}

class InstructionReady(implicit  p: CoreParams) extends BaseInstruction {
  val rs1 = UInt(32.W)
  val rs2 = UInt(32.W)
  val rd = UInt(p.prfSize.W)
  val alu_op = UInt(4.W)
  val immediate = SInt(32.W)
  val needs_rs2 = Bool()
}

class InstructionResult(implicit  p: CoreParams) extends BaseInstruction {
  val rd = UInt(p.prfSize.W)
  val value = UInt(32.W)
}