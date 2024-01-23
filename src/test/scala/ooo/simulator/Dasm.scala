package ooo.simulator

object Dasm {
  final val LuiOpcode = Integer.parseInt("0110111", 2)
  final val AuipcOpcode = Integer.parseInt("0010111", 2)
  final val JalOpcode = Integer.parseInt("1101111", 2)
  final val JarlOpcode = Integer.parseInt("1100111", 2)

  final val ITypeOpcode = Integer.parseInt("0010011", 2)
  final val RTypeOpcode = Integer.parseInt("0110011", 2)
  final val BTypeOpcode = Integer.parseInt("1100011", 2)

  final val LoadOpcode = Integer.parseInt("0000011", 2)
  final val StoreOpcode = Integer.parseInt("0100011", 2)


  def apply(instr: Int): String = {
    val opcode = instr & 0x1F
    val rd = (instr >> 7) & 0x1F
    val funct3 = (instr >> 12) & 7
    val rs1 = (instr >> 15) & 0x1F
    val rs2 = (instr >> 20) & 0x1F
    val funct7 = (instr >> 25) & 0x7F
    val iImm = (instr >> 20) | (if (instr < 0) -1 << 20 else 0)

    val dasm = opcode match {
      case AuipcOpcode => "auipc";
      case LuiOpcode => "lui";
      case JalOpcode => "jal";
      case JarlOpcode => "jarl";
      case RTypeOpcode => {
        val oper = funct3 match {
          case 0x0 => if (funct7 == 0x20) "sub" else "add";
          case 0x1 => "sll";
          case 0x2 => "slt";
          case 0x3 => "sltu";
          case 0x4 => "xor";
          case 0x5 => if (funct7 == 0x20) "sra" else "srl";
          case 0x6 => "or";
          case 0x7 => "and";
          case _ => "rt ???"
        }
        s"$oper x$rd, $rs1, $rs2"
      };
      case ITypeOpcode => {
        val oper = funct3 match {
          case 0x0 => if (funct7 == 0x20) "subi" else "addi";
          case 0x1 => "slli";
          case 0x2 => "slti";
          case 0x3 => "sltui";
          case 0x4 => "xori";
          case 0x5 => if (funct7 == 0x20) "srai" else "srli";
          case 0x6 => "ori";
          case 0x7 => "andi";
          case _ => "it ???"
        }
        s"$oper x$rd, x$rs1, $iImm"
      };
      case BTypeOpcode => {
        val oper = funct3 match {
          case 0x0 => "beq";
          case 0x1 => "bne";
          case 0x2 => "blt";
          case 0x3 => "bge";
          case 0x4 => "bltu";
          case 0x5 => "bgeu";
          case _ => "bt ???"
        }
        oper
      }
      case _ => "---"
    }

    dasm
  }
}
