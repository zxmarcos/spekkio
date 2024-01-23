package ooo.rename

import chisel3._
import chisel3.util.{Counter, Valid}
import ooo.{CoreParams, InstructionDecoded, InstructionIssued, InstructionRenamed, PipelineUnit}
import ooo.utils.{BusyTableCheck, MultiportMemoryReadPort, MultiportMemoryWritePort}


class RenameUnit()(implicit p: CoreParams) extends PipelineUnit {
  val io = IO(new Bundle {
    val instr = Input(Valid(new InstructionDecoded))
    val rat_read = Flipped(Vec(2, new MultiportMemoryReadPort(p.ratMemoryConfig)))
    val rat_write = Flipped(Vec(1, new MultiportMemoryWritePort(p.ratMemoryConfig)))

    val prf_busy_table = Input(UInt(p.prfEntries.W))
    val prf_busy_table_set = Output(Valid(UInt(p.prfEntries.W)))

    val renamed = Output(Valid(new InstructionRenamed))
    val issued = Output(Valid(new InstructionIssued))
  })

  when(pipeline.stall) {
    printf("Rename stalled\n");
  }


  // Stage 1:
  // When not initialized:
  //  - Fill RAT with mappings 1-1 from r0->r15
  // When initialized:
  //  - Allocate RAT entry
  //  - Lookup current mappings for rs1 and rs2
  val reg_counter = Counter(p.ratEntries)
  val is_initialized = RegInit(false.B)


  val renamer = Module(new RegisterRenamer())
  val reg_alloc_is_empty = renamer.io.empty
  val reg_alloc_is_full = renamer.io.full

  // The rename unit is stalled whenever:
  // 1 - Unit is not initialized
  // 2 - Our request port to RegisterRenamer is not ready (busy filling free_list)
  // 3 - There is no register free in free_list
  pipeline.stalled := !renamer.io.req.ready || !is_initialized || reg_alloc_is_empty || pipeline.stall

  when(pipeline.stalled) {
    printf(cf"Stalling Rename ${!renamer.io.req.ready}, ${!is_initialized}, ${reg_alloc_is_empty}, ${pipeline.stall}\n")
  }

  // Request a rename when:
  // 1 - Unit is initialized
  // 2 - Our request por to RegisterRename is ready
  // 3 - Incoming instruction is valid
  renamer.io.req.valid := is_initialized && renamer.io.req.ready && io.instr.valid && !pipeline.stall
  renamer.io.req.bits := DontCare

  val renamed = Reg(Valid(new InstructionRenamed))

  // Register signals...
  when(!pipeline.stalled && io.instr.valid) {
    // Lookup rs1 and rs2 renamed indices
    renamed.bits.rs1 := io.rat_read(0).data
    renamed.bits.rs2 := io.rat_read(1).data
    renamed.bits.rd := renamer.io.res.bits
    renamed.bits.alu_op := io.instr.bits.alu_op
    renamed.bits.immediate := io.instr.bits.immediate
    renamed.bits.is_i_type := io.instr.bits.is_i_type
    renamed.bits.is_r_type := io.instr.bits.is_r_type
    renamed.bits.needs_rs2 := io.instr.bits.needs_rs2
    renamed.bits.instr := io.instr.bits.instr
    renamed.valid := true.B
  }.otherwise {
    renamed.valid := false.B
  }

  // Read RS1 and RS2 from Register Alias Table (RAT)
  io.rat_read(0).addr := 0.U
  io.rat_read(1).addr := 0.U

  when(is_initialized) {
    when(io.instr.valid && !pipeline.stalled) {
      // Write new renamed register to RAT
      io.rat_write(0).write(io.instr.bits.rd, renamer.io.res.bits)
      io.rat_read(0).read(io.instr.bits.rs1)
      io.rat_read(1).read(io.instr.bits.rs2)
    }.otherwise {
      io.rat_write(0).clear()
    }
  }.otherwise {
    // Pre-allocate in RAT normal registers from 0 to 16
    io.rat_write(0).write(reg_counter.value, reg_counter.value)
    when(reg_counter.inc()) {
      is_initialized := true.B
    }
  }

  io.renamed := renamed

  // Stage 2
  val issued = Reg(Valid(new InstructionIssued))
  val rs1_busy = Module(new BusyTableCheck(p.prfEntries))
  val rs2_busy = Module(new BusyTableCheck(p.prfEntries))

  rs1_busy.io.busy_table := io.prf_busy_table
  rs1_busy.io.index := renamed.bits.rs1
  rs2_busy.io.busy_table := io.prf_busy_table
  rs2_busy.io.index := renamed.bits.rs2

  // TODO: Check if stall signal can be used in two stages at same time.
  when(renamed.valid && !pipeline.stall) {
    issued.valid := true.B
    issued.bits.rs1 := renamed.bits.rs1
    issued.bits.rs2 := renamed.bits.rs2
    issued.bits.rd := renamed.bits.rd
    issued.bits.rs1_valid := !rs1_busy.io.busy
    issued.bits.rs2_valid := !rs2_busy.io.busy
    issued.bits.needs_rs2 := renamed.bits.needs_rs2
    issued.bits.immediate := renamed.bits.immediate
    issued.bits.alu_op := renamed.bits.alu_op
    issued.bits.instr := renamed.bits.instr
    // Set renamed RD as busy
    io.prf_busy_table_set.valid := true.B
    io.prf_busy_table_set.bits := renamed.bits.rd
  }.otherwise {
    issued.valid := false.B
    io.prf_busy_table_set.valid := false.B
    io.prf_busy_table_set.bits := DontCare
    issued.bits := DontCare
  }
  io.issued := issued
}
