package ooo

import chisel3._
import chiseltest._
import ooo.utils.{BusyTable}
import org.scalatest.FreeSpec


class BusyTableTests extends FreeSpec with ChiselScalatestTester {
  "Test BusyTable" in {
    test(new BusyTable(8))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.clear(0).valid.poke(false.B)
        dut.io.clear(0).bits.poke(0.U)
        dut.io.set(0).valid.poke(false.B)
        dut.io.set(0).bits.poke(0.U)
        dut.clock.step()

        for (i <- 0 until 8) {
          dut.io.set(0).valid.poke(true.B)
          dut.io.set(0).bits.poke(i.U)
          dut.clock.step()
        }

        dut.io.set(0).valid.poke(false.B)
        dut.io.set(0).bits.poke(0.U)

        for (i <- 0 until 8) {
          dut.io.clear(0).valid.poke(true.B)
          dut.io.clear(0).bits.poke(i.U)
          dut.clock.step()
        }

      }
  }
}
