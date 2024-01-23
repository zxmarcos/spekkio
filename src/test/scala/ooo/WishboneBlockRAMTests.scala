package ooo

import chisel3._
import chiseltest._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.experimental.expose
import ooo.utils.WishboneIO

class WishboneBlockRAMWrapper extends Module {
  val ctrl = Module(new WishboneBlockRAM)
  val ram = Module(new RAM("testdata/test_ifetch.mem", 1))

  val io = IO(new WishboneIO)
  io <> ctrl.wb
  ctrl.bram <> ram.io.ports(0)
}

class WishboneBlockRAMTests extends FreeSpec with ChiselScalatestTester {
  "Test Wishbone BlockRAM" in {
    test(new WishboneBlockRAMWrapper)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.step(1)

        def readAddress(addr: UInt, pipelined: Boolean = false) = {
          dut.io.address.poke(addr)
          dut.io.cycle.poke(true.B)
          dut.io.strobe.poke(true.B)
          dut.io.write_enable.poke(false.B)

          dut.clock.step(1)
          dut.io.strobe.poke(false.B)

          if (!pipelined) {
            while (!dut.io.ack.peek().litToBoolean)
              dut.clock.step(1)
            dut.io.cycle.poke(false.B)
            dut.io.strobe.poke(false.B)
          }
        }

        def readTest(pipelined: Boolean): Unit = {
          for (i <- 1 until 5) {
            readAddress(i.U, pipelined)
            if (!pipelined)
              dut.clock.step(1)
          }
          if (pipelined) {
            dut.io.cycle.poke(false.B)
            dut.io.strobe.poke(false.B)
          }

          dut.clock.step(3)
        }

        readTest(false)
        readTest(true)
      }
  }
}