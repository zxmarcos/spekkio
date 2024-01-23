package ooo


import chisel3._
import chiseltest._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._


class RAMTests extends FreeSpec with ChiselScalatestTester {
  "Test RAM" in {
    test(new RAM("testdata/test_alu.mem", 1))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.ports(0).write.poke(false.B)
      dut.io.ports(0).dataIn.poke(false.B)
      dut.io.ports(0).addr.poke(0.U)
      dut.clock.step(2)
      for (i <- 0 until 10) {
        dut.io.ports(0).addr.poke(i.U)
        dut.clock.step(2)
        println(dut.io.ports(0).data_out.peek().litValue.toInt.toHexString)
      }

    }
  }
}
