package ooo
import chisel3._
import chiseltest._
import ooo.rename.RegisterRenamer
import org.scalatest.FreeSpec


class RegisterRenamerTests extends FreeSpec with ChiselScalatestTester {

  "Test Renamer" in {
    test(new RegisterRenamer(16)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      dut.io.req.valid.poke(false.B)
      dut.io.req.bits.poke(15)

      def waitForValid(): Int = {
        var count: Int = 0
        while (dut.io.req.ready.peek().litToBoolean != true) {
          dut.clock.step(1)
          count += 1
        }
        count
      }

      val cycles = waitForValid()
      println(cycles)

//      fork.withRegion(Monitor) {
//
//
//      }.joinAndStep(dut.clock)
      dut.clock.step(1)
      for (i <- 0 until 20) {
        dut.io.req.valid.poke(true.B)
        dut.io.req.bits.poke(i.U)
        println(s"${i} => ${dut.io.res.bits.peek()} : ${dut.io.res.valid.peek()}")
        dut.clock.step(1)
      }

    }
  }

}
