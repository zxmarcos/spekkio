package ooo

import chisel3._
import chisel3.Wire
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.experimental.FlatIO

class RegFileWritePort() extends Bundle {
  val enable = Input(Bool())
  val addr = Input(UInt(5.W))
  val data = Input(UInt(32.W))
}

class RegFileReadPort() extends Bundle {
  val addr = Input(UInt(5.W))
  val data = Output(UInt(32.W))
}

class RegisterFile1w2r() extends Module {
  val io = IO(new Bundle {
    val read = Vec(2, new RegFileReadPort())
    val write = new RegFileWritePort()
  })

  val reg = Mem(32, UInt(32.W))

  when(io.write.enable) {
    reg.write(io.write.addr, io.write.data)
  }

  io.read(0).data := Mux(io.read(0).addr === 0.U, 0.U, reg(io.read(0).addr))
  io.read(1).data := Mux(io.read(1).addr === 0.U, 0.U, reg(io.read(1).addr))
}

class RegisterFileBank1wNr(readPorts: Int) extends Module {
  val io = IO(new Bundle {
    val read = Vec(readPorts, new RegFileReadPort())
    val write = new RegFileWritePort()
  })

  for (i <- 0 until readPorts / 2) {
    val rf = Module(new RegisterFile1w2r())
    rf.io.write <> io.write
    rf.io.read(0) <> io.read(i * 2 + 0)
    rf.io.read(1) <> io.read(i * 2 + 1)
  }
}

class RegisterFileBankMwNr(readPorts: Int, writePorts: Int) extends Module {
  val io = IO(new Bundle {
    val read = Vec(readPorts, new RegFileReadPort)
    val write = Vec(writePorts, new RegFileWritePort)
  })

  val banks = for (wr <- 0 until writePorts) yield {
    val writeBank = Module(new RegisterFileBank1wNr(readPorts))
    val readBanks = Wire(Vec(readPorts, UInt(32.W)))

    // 1 write port for all read ports in this bank
    writeBank.io.write <> io.write(wr) // Wire read addresses...
    writeBank.io.read <> io.read
    for (rd <- 0 until readPorts) {
      readBanks(rd) := writeBank.io.read(rd).data
    }

    readBanks
  }

  val LVT = Mem(32, UInt(log2Ceil(writePorts).W))
  for (i <- 0 until writePorts) {
    when(io.write(i).enable) {
      LVT.write(io.write(i).addr, i.asUInt)
    }
  }

  for (rd <- 0 until readPorts) {
    // Current "live value index" in LVT table for read address
    val lvtWriteBank = LVT.read(io.read(rd).addr)

    val lookupLvt = banks.zipWithIndex.map { case (bank, index) => (lvtWriteBank === index.U) -> bank(rd) }
    io.read(rd).data := MuxCase(0.U, lookupLvt)
  }
}

