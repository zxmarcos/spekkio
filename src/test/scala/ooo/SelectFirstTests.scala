package ooo
import chisel3._
import chisel3.util.{OHToUInt, log2Ceil}
import chiseltest.{ChiselScalatestTester, WriteVcdAnnotation}
import ooo.utils.{SelectFirst, SelectFirstWrapper}
import org.scalatest.FreeSpec
import chiseltest._



class SelectFirstTests extends FreeSpec with ChiselScalatestTester {
  "Test SelectFirst" in {
    test(new SelectFirstWrapper(5,2))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.io.in.poke(0.U)
        dut.io.output(0).expect(0.U)
        dut.io.output(1).expect(0.U)
        dut.clock.step(1)
        dut.io.in.poke("b10000".U)
        dut.io.output(0).expect(4.U)
        dut.io.output(1).expect(0.U)
        dut.clock.step(1)
        dut.io.in.poke("b10100".U)
        dut.io.output(0).expect(2.U)
        dut.io.output(1).expect(4.U)

      }
  }
}
