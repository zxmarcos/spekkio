package ooo.utils

import chisel3._
import chisel3.util.{Decoupled, Valid}

// From slave perspective
class WishboneIO extends Bundle {
  val cycle = Input(Bool())
  val strobe = Input(Bool())
  val write_enable = Input(Bool())
  val address = Input(UInt(32.W))
  val master_data = Input(UInt(32.W))
  val slave_data = Output(UInt(32.W))
  val stall = Output(Bool())
  val ack = Output(Bool())

  def fire: Bool = cycle && strobe
}
