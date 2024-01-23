package ooo

import chisel3._
import chiseltest._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.experimental.expose
import ooo.issue.IssueUnit
import ooo.CoreParams
import ooo.utils.SequenceCircularBuffer

class IssueUnitTests extends FreeSpec with ChiselScalatestTester {
  "Test IssueUnit" in {
    test(new IssueUnit()(CoreParams()))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.clock.step(60)
      }
  }
}