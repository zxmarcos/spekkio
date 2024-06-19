package ooo


import chisel3._
import chiseltest._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chiseltest.experimental.expose
import ooo.utils.{ICache, ICacheIO, WishboneIO}

class ICacheWrapper extends Module {
  val ctrl = Module(new WishboneBlockRAM(true))
  val cache = Module(new ICache)
  val ram = Module(new RAM("testdata/test_icache_2wide.mem", 1))

  val io = IO(new ICacheIO)
  cache.io <> io

  ctrl.bram <> ram.io.ports(0)
  cache.io.wb <> ctrl.wb

}

class ICacheTests extends FreeSpec with ChiselScalatestTester {
  "Test ICache read" in {
    test(new ICacheWrapper)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.step(1)

        def readAddress(addr: UInt) = {
          dut.io.addr.poke(addr)
          dut.io.reqvalid.poke(true.B)
          while (!dut.io.valid.peek().litToBoolean) {
            dut.clock.step(1)
            dut.io.reqvalid.poke(false.B)
          }
        }
        readAddress(0.U)
        dut.clock.step()
        readAddress(1.U)
        dut.clock.step()
        readAddress(2.U)
        dut.clock.step()
        readAddress(3.U)
        dut.clock.step()
        readAddress(0.U)
        dut.clock.step()
        readAddress(1.U)
        dut.clock.step()
        readAddress(2.U)
        dut.clock.step()
        readAddress(3.U)
        dut.clock.step()
        readAddress(1.U)
        dut.clock.step()
        readAddress(2.U)
        dut.clock.step()
        readAddress(1.U)
        dut.clock.step()
        readAddress(2.U)
        dut.clock.step()
      }
  }
}