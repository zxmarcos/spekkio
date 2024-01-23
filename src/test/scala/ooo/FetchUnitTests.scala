package ooo
import chisel3._
import chiseltest._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._
import chisel3.util.Valid
import chiseltest.experimental.expose
import ooo.fetch.FetchUnit
import ooo.utils.WishboneIO

class FetchUnitWBTestsWrapper extends Module {
  val fetch = Module(new FetchUnit)
  val ctrl = Module(new WishboneBlockRAM)
  val ram = Module(new RAM("testdata/test_ifetch.mem", 1))

  ctrl.bram <> ram.io.ports(0)
  fetch.io.wb <> ctrl.wb

  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val instr = Valid(Output(UInt(32.W)))
    val stall = Input(Bool())
    val stalled = Output(Bool())
  })

  fetch.io.pc := io.pc
  fetch.pipeline.stall := io.stall
  io.instr := fetch.io.instr
  io.stalled := fetch.pipeline.stalled

//  expose(fetch.io.wb)
//  expose(fetch.io.stall)
//  expose(fetch.io.pc)
//  expose(fetch.io.instr)
}

class FetchUnitTests extends FreeSpec with ChiselScalatestTester {
  "Test Wishbone FetchUnit" in {
    test(new FetchUnitWBTestsWrapper)
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        dut.io.stall.poke(false.B)
        dut.io.pc.poke(0.U)
        dut.clock.step(1)
        for (i <- 1 until 16) {
          dut.io.pc.poke((i * 4).U)
          dut.clock.step(1)
          while (dut.io.stalled.peekBoolean()) {
            dut.clock.step(1)
          }
        }
        dut.io.stall.poke(true.B)
        dut.clock.step(5)
        dut.io.stall.poke(true.B)
        dut.clock.step(5)
        dut.io.stall.poke(false.B)
        dut.clock.step(5)

      }
  }
}