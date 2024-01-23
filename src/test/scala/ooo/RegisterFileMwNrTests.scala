package ooo

import chisel3._
import chiseltest._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._

import scala.util.Random


class RegisterFileMwNrTests extends FreeSpec with ChiselScalatestTester {

  "Test RF" in {
    test(new RegisterFileBankMwNr(2,2)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      for (i <- 0 until 2) {
        dut.io.read(i).addr.poke(0.U)
        dut.io.write(i).addr.poke(0.U)
        dut.io.write(i).enable.poke(false.B)
        dut.io.write(i).data.poke(0.U)
      }

      // Test case for register 0
      dut.io.read(0).data.expect(0.U)
      dut.io.read(1).data.expect(0.U)


      for (i <- 1 until 31) {
        var r1v = Random.between(1, 31)
        var r2v = Random.between(1, 31)

        while (r1v == r2v)
          r2v = Random.between(1, 31)

        val r1 = r1v.U
        val r2 = r2v.U

        val r1value = Random.nextInt().abs.U
        val r2value = Random.nextInt().abs.U
        dut.io.write(0).addr.poke(r1)
        dut.io.write(1).addr.poke(r2)
        dut.io.write(0).data.poke(r1value)
        dut.io.write(1).data.poke(r2value)

        dut.io.write(0).enable.poke(true.B)
        dut.io.write(1).enable.poke(true.B)
        println(s"Writing ${r1} <- ${r1value}, ${r2} <- ${r2value}");

        dut.clock.step()

        dut.io.write(0).enable.poke(false.B)
        dut.io.write(1).enable.poke(false.B)

        dut.io.read(0).addr.poke(r1)
        dut.io.read(1).addr.poke(r2)

        println(s"Reading ${r1} <- ${dut.io.read(0).data.peek()}, ${r2} <- ${dut.io.read(1).data.peek()}");

        dut.io.read(0).data.expect(r1value)
        dut.io.read(1).data.expect(r2value)

        dut.clock.step()
        println()
      }
    }
  }

}
