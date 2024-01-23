package ooo

import chisel3._
import ooo.utils.WishboneIO

class DelayN(n: Int) extends Module {
  val in = IO(Input(Bool()))
  val out = IO(Output(Bool()))
  out := (0 until n).foldLeft(in) {
    case (last, x) => RegNext(last)
  }
}

class WishboneBlockRAM(pipeline: Boolean = true) extends Module {
  val wb = IO(new WishboneIO)
  val bram = IO(Flipped(new RAMMemoryPort))

  val address = RegInit(0.U(32.W))
  val write_data = RegInit(0.U(32.W))
  val write_enable = RegInit(false.B)

  val pipe = Module(new DelayN(3))
  val busy = RegInit(false.B)

  pipe.in := false.B

  write_enable := false.B

  when(wb.strobe && (if (pipeline) true.B else !busy)) {
    when(wb.write_enable) {
      write_data := wb.master_data
      write_enable := true.B
    }
    address := wb.address
    busy := true.B
    pipe.in := true.B
  }.elsewhen(busy && pipe.out) {
    busy := false.B
  }

  bram.addr := address
  bram.dataIn := write_data
  bram.write := write_enable

  wb.stall := (if (pipeline) false.B else busy)
  wb.slave_data := bram.data_out
  wb.ack := pipe.out
}
