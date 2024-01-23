package ooo.utils


import chisel3._
import chisel3.util.{Cat, Fill, is, switch}

object RISCVDasm {
  def apply(instr: UInt): Unit = {
    val rd = instr(11, 7)
    val funct3 = instr(14, 12)
    val funct7 = instr(31, 25)
    val opcode = instr(6, 0)

    val rs1 = instr(19, 15)
    val rs2 = instr(24, 20)

    // Immediate values
    val imm_u_type = Cat(instr(31, 12), 0.U(12.W)).asSInt
    val imm_i_type = Cat(Fill(20, instr(31)), instr(31, 20)).asSInt
    val imm_b_type = Cat(Fill(19, instr(31)), instr(31), instr(7), instr(30, 25), instr(11, 8), 0.U(1.W)).asSInt
    val imm_j_type = Cat(Fill(11, instr(31)), instr(31), instr(19, 12), instr(20), instr(30, 21), 0.U(1.W)).asSInt
    val imm_s_type = Cat(Fill(20, instr(31)), instr(31, 25), instr(11, 7)).asSInt

    when(opcode === "b0110111".U) {
      printf(cf"lui x$rd%d,$imm_u_type\n")
    }.elsewhen(opcode === "b0010111".U) {
      printf(cf"auipc x$rd%d,$imm_u_type\n")
    }.elsewhen(opcode === "b0010011".U) {
      when(rd === rs1 && rd === 0.U) {
        printf("nop\n")
      }.otherwise{
        switch(funct3) {
          is("b000".U) {
            printf(cf"addi x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
          is("b001".U) {
            printf(cf"slli x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
          is("b010".U) {
            printf(cf"slti x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
          is("b011".U) {
            printf(cf"sltui x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
          is("b100".U) {
            printf(cf"xori x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
          is("b101".U) {
            when(funct7 === "h20".U) {
              printf(cf"sra x$rd%d, x$rs1%d, $imm_i_type%d\n")
            }.otherwise {
              printf(cf"srl x$rd%d, x$rs1%d, $imm_i_type%d\n")
            }
          }
          is("b110".U) {
            printf(cf"ori x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
          is("b111".U) {
            printf(cf"andi x$rd%d, x$rs1%d, $imm_i_type%d\n")
          }
        }
      }
    }.elsewhen(opcode === "b0110011".U) {
      when(rd === 0.U) {
        printf("nop\n")
      }.otherwise {

        switch(funct3) {

          is("b000".U) {

            when(funct7 === "h20".U) {
              printf(cf"sub x$rd%d, x$rs1%d, x$rs2%d\n")
            }.otherwise {
              printf(cf"add x$rd%d, x$rs1%d, x$rs2%d\n")
            }

          }
          is("b001".U) {
            printf(cf"sll x$rd%d, x$rs1%d, x$rs2%d\n")
          }
          is("b010".U) {
            printf(cf"slt x$rd%d, x$rs1%d, x$rs2%d\n")
          }
          is("b011".U) {
            printf(cf"sltu x$rd%d, x$rs1%d, x$rs2%d\n")
          }
          is("b100".U) {
            printf(cf"xor x$rd%d, x$rs1%d, x$rs2%d\n")
          }
          is("b101".U) {

            when(funct7 === "h20".U) {
              printf(cf"sra x$rd%d, x$rs1%d, x$rs2%d\n")
            }.otherwise {
              printf(cf"srl x$rd%d, x$rs1%d, x$rs2%d\n")
            }


          }
          is("b110".U) {
            printf(cf"or x$rd%d, x$rs1%d, x$rs2%d\n")
          }
          is("b111".U) {
            printf(cf"and x$rd%d, x$rs1%d, x$rs2%d\n")
          }
        }
      }
    }.otherwise {
      printf("???\n")
    }
  }
}
