package ooo.utils

import chisel3._
import chisel3.util.{UIntToOH, Valid, log2Ceil}

class BusyTableCheck(regs: Int) extends Module {
  val io = IO(new Bundle {
    val busy_table = Input(UInt(regs.W))
    val index = Input(UInt(log2Ceil(regs).W))
    val busy = Output(Bool())
  })

  io.busy := (io.busy_table & UIntToOH(io.index, regs)).orR
}

class BusyTable(regs: Int, setPorts: Int = 1, clearPorts: Int = 1) extends Module {
  val addrSize = log2Ceil(regs)

  val io = IO(new Bundle {
    val bits = Output(UInt(regs.W))
    val set = Input(Vec(setPorts, Valid(UInt(addrSize.W))))
    val clear = Input(Vec(clearPorts, Valid(UInt(addrSize.W))))
  })

  val data = RegInit(UInt(regs.W), 0.U)
  val set_data = Seq.tabulate(setPorts) { i => Mux(io.set(i).valid, UIntToOH(io.set(i).bits, regs), 0.U(regs.W))
  }.reduce(_ | _)
  val clr_data = Seq.tabulate(clearPorts) { i => (~Mux(io.clear(i).valid, UIntToOH(io.clear(i).bits, regs), 0.U(regs.W))).asUInt
  }.reduce(_ & _)

  io.bits := data
  data := (data | set_data) & clr_data
}

//class BusyTable(regs: Int) extends Module {
//  val addrSize = log2Ceil(regs)
//
//  val io = IO(new Bundle {
//    val bits = Output(UInt(regs.W))
//    val set = Input(Valid(UInt(addrSize.W)))
//    val clear = Input(Valid(UInt(addrSize.W)))
//  })
//
//  val data = RegInit(UInt(regs.W), 0.U)
//  val set_data = UIntToOH(io.set.bits, regs)
//  val clr_data = UIntToOH(io.clear.bits, regs)
//  val next_set = Mux(io.set.valid, set_data, 0.U(regs.W))
//  val next_clr = (~Mux(io.clear.valid, clr_data, 0.U(regs.W))).asUInt
//
//  io.bits := data
//  data := (data | next_set) & next_clr
//}
