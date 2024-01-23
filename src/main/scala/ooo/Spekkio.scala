package ooo

import chisel3._
import ooo.commit.{CommitUnit, ReorderBuffer}
import ooo.decode.DecodeUnit
import ooo.execute.{ALUUnit, RegisterReadUnit}
import ooo.fetch.FetchUnit
import ooo.issue.IssueUnit
import ooo.rename.RenameUnit
import ooo.utils.{BusyTable, MPRAM, MultiportMemoryDBGRAM, MultiportMemoryLVT, MultiportMemoryREGRAM}

class Spekkio(params: CoreParams) extends Module {
  val io = IO(new Bundle {
    val valid = Output(Bool())
    val result = Output(UInt(32.W))
  });

  implicit val p = params

  val prf_busy_table = Module(new BusyTable(p.prfEntries))
  val rat: MPRAM = if (p.simulation) Module(new MultiportMemoryDBGRAM(p.ratMemoryConfig)) else Module(new MultiportMemoryLVT(p.ratMemoryConfig))
//  val rrf: MPRAM = if (p.simulation) Module(new MultiportMemoryDBGRAM(p.rrfMemoryConfig)) else Module(new MultiportMemoryLVT(p.rrfMemoryConfig))
  val ram = Module(new RAM("testdata/test_alu.mem", 1))
  val prf = if (p.simulation) Module(new MultiportMemoryDBGRAM(p.prfMemoryConfig)) else Module(new MultiportMemoryLVT(p.prfMemoryConfig))
  val bram_memctrl = Module(new WishboneBlockRAM(true))
  val pc = Module(new ProgramCounter)
  val fetch = Module(new FetchUnit)
  val decode = Module(new DecodeUnit)
  val rename = Module(new RenameUnit)
  val issue = Module(new IssueUnit)
  val regread = Module(new RegisterReadUnit)
  val alu = Module(new ALUUnit)
  val commit = Module(new CommitUnit)
//  val rob = Module(new ReorderBuffer)


  // Connect fetch unit to wishbone bram controller
  bram_memctrl.bram <> ram.io.ports(0)
  fetch.io.wb <> bram_memctrl.wb

  // Front-End
  pc.pipeline.stall := fetch.pipeline.stalled
  fetch.pipeline.stall := decode.pipeline.stalled
  // Stall decode stage when there is no free slot in reorder buffer.
  decode.pipeline.stall := rename.pipeline.stalled
  rename.pipeline.stall := issue.pipeline.stalled

  // Back-end
  issue.pipeline.stall := false.B
  regread.pipeline.stall := false.B
  alu.pipeline.stall := false.B
  commit.pipeline.stall := false.B

  fetch.io.pc := pc.io.value

  decode.io.instr <> fetch.io.instr

  // Rename...
  rename.io.instr <> decode.io.decoded
  rename.io.rat_read(0) <> rat.io.read(0)
  rename.io.rat_read(1) <> rat.io.read(1)
  rename.io.rat_write <> rat.io.write

  // Rename / Busy
  rename.io.prf_busy_table := prf_busy_table.io.bits
  rename.io.prf_busy_table_set <> prf_busy_table.io.set(0)

  // Issue
  issue.io.instruction <> rename.io.issued

  // Execute
  regread.io.instr <> issue.io.dispatched
  regread.io.prf_read_port(0) <> prf.io.read(0)
  regread.io.prf_read_port(1) <> prf.io.read(1)

  alu.io.instruction <> regread.io.ready

  // Commit
  commit.io.result <> alu.io.result
  commit.io.prf_write_port <> prf.io.write(0)

  issue.io.commit_write_port <> commit.io.prf_write_port

  // Busy write
  prf_busy_table.io.clear(0).valid := commit.io.prf_write_port.enable
  prf_busy_table.io.clear(0).bits := commit.io.prf_write_port.addr

  io.result := commit.io.prf_write_port.data
  io.valid := commit.io.prf_write_port.enable
}
