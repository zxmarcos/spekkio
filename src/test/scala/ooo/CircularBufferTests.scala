package ooo

import chisel3._
import chiseltest.{ChiselScalatestTester, WriteVcdAnnotation, decoupledToDriver, testableClock}
import ooo.utils.CircularBuffer
import org.scalatest.FreeSpec

class CircularBufferTests extends FreeSpec with ChiselScalatestTester {

  "Test CircularBuffer" in {
    test(new CircularBuffer(4, 8)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.enq.initSource()
      dut.io.enq.setSourceClock(dut.clock)
      dut.io.deq.initSink()
      dut.io.deq.setSinkClock(dut.clock)


      dut.io.enq.enqueueNow(100.U)
      dut.io.enq.enqueueNow(101.U)
      dut.io.enq.enqueueNow(102.U)
      dut.io.enq.enqueueNow(103.U)
      for (i <- 0 until 5)
        dut.clock.step()

      dut.io.deq.expectDequeueNow(100.U)
      dut.io.deq.expectDequeueNow(101.U)
      dut.io.deq.expectDequeueNow(102.U)
      dut.io.deq.expectDequeueNow(103.U)
      dut.io.deq.expectInvalid()


    }
  }

}
