package ooo.utils

import chisel3._
import chisel3.util.{OHToUInt, PriorityEncoderOH, log2Ceil}

// Cascade n-PriorityEncoderOH
object SelectFirst {
  def apply(reg: UInt, n: Int): Vec[UInt] = {
    val select = Wire(Vec(n, UInt(reg.getWidth.W)))
    var mask = reg
    for (i <- 0 until n) {
      select(i) := PriorityEncoderOH(reg & mask)
      mask = mask & (~select(i)).asUInt
    }
    select
  }
}

class SelectFirstWrapper(inWidth: Int, n: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(inWidth.W))
    val output = Output(Vec(n, UInt(log2Ceil(inWidth).W)))
  })

  val selects = SelectFirst(io.in, n)
  for (i <- 0 until n) {
    io.output(i) := OHToUInt(selects(i))
  }
}
