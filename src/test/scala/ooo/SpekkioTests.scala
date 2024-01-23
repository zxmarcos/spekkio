package ooo

import chiseltest._
import chiseltest.experimental.expose
import ooo.utils.{MultiportMemoryDBGRAM, MultiportMemoryREGRAM}
import org.scalatest.FreeSpec

class SpekkioWrapper(p: CoreParams) extends Spekkio(p) {
  if (p.simulation) {
    val pc_value = expose(pc.io.value)
    val fetch_valid = expose(fetch.io.instr.valid)
    val fetch_instr = expose(fetch.io.instr.bits)
    val fetch_stalled = expose(fetch.pipeline.stalled)

    val decode_valid = expose(decode.valid)
    val decode_instr = expose(decode.decoded.instr)
    val decode_stalled = expose(decode.pipeline.stalled)

    val rename1_instr = expose(rename.io.renamed.bits.instr)
    val rename2_instr = expose(rename.io.issued.bits.instr)
    val rename1_valid = expose(rename.io.renamed.valid)
    val rename2_valid = expose(rename.io.issued.valid)
    val rename1_stalled = expose(rename.pipeline.stalled)
    val rename2_stalled = expose(rename.pipeline.stalled)

    val issue_instr = expose(issue.io.dispatched.bits.instr)
    val issue_valid = expose(issue.io.dispatched.valid)
    val issue_stalled = expose(issue.pipeline.stalled)

    val regread_instr = expose(regread.io.instr.bits.instr)
    val regread_valid = expose(regread.io.instr.valid)
    val regread_stalled = expose(regread.pipeline.stalled)

    val alu_instr = expose(alu.io.instruction.bits.instr)
    val alu_valid = expose(alu.io.instruction.valid)
    val alu_stalled = expose(alu.pipeline.stalled)

    val commit_instr = expose(commit.io.result.bits.instr)
    val commit_valid = expose(commit.io.result.valid)
    val commit_stalled = expose(commit.pipeline.stalled)

    for (i <- 0 until p.issueEntries) {
      expose(issue.rsv_stations.slots(i).rs1).suggestName(s"iq_${i}_rs1")
      expose(issue.rsv_stations.slots(i).rs2).suggestName(s"iq_${i}_rs2")
      expose(issue.rsv_stations.slots(i).rs1_valid).suggestName(s"iq_${i}_rs1_valid")
      expose(issue.rsv_stations.slots(i).rs2_valid).suggestName(s"iq_${i}_rs2_valid")
      expose(issue.rsv_stations.slots(i).needs_rs2).suggestName(s"iq_${i}_needs_rs2")
      expose(issue.rsv_stations.slots(i).immediate).suggestName(s"iq_${i}_imm")
      expose(issue.rsv_stations.slots(i).rd).suggestName(s"iq_${i}_rd")
      expose(issue.rsv_stations.slots(i).alu_op).suggestName(s"iq_${i}_alu_op")
      expose(issue.rsv_stations.io.ready_list(i)).suggestName(s"iq_${i}_ready")
      expose(issue.rsv_stations.valid_slots(i)).suggestName(s"iq_${i}_valid")
    }


    for (i <- 0 until p.ratEntries) {
      expose(rat.asInstanceOf[MultiportMemoryDBGRAM].dbg_ports(i)).suggestName(s"rat$i")
    }


    for (i <- 0 until p.prfEntries) {
      expose(prf.asInstanceOf[MultiportMemoryDBGRAM].dbg_ports(i).asSInt).suggestName(s"prf$i")

      expose(prf_busy_table.io.bits(i)).suggestName(s"bprf_$i")
    }
  }
}

class SpekkioTests extends FreeSpec with ChiselScalatestTester {
  "Test Spekkio" in {
    test(new SpekkioWrapper(CoreParams(simulation = false)))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.step(100)
      }
  }
}
