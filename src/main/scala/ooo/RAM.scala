package ooo

import chisel3._
import chisel3.util.experimental.{loadMemoryFromFileInline, loadMemoryFromFile}

class RAMMemoryPort extends Bundle {
  val addr = Input(UInt(16.W))
  val data_out = Output(UInt(32.W))
  val write = Input(Bool())
  val dataIn = Input(UInt(32.W))
}

class RAM(memoryFile: String = "", ramPorts: Int = 2) extends Module {
  val io = IO(new Bundle {
    val ports = Vec(ramPorts, new RAMMemoryPort)
  })
  val mem = SyncReadMem(4096, UInt(32.W))
  if (memoryFile.trim().nonEmpty) {
    loadMemoryFromFileInline(mem, memoryFile)
    //loadMemoryFromFile(mem, memoryFile)
  }

  val out = Reg(Vec(ramPorts, UInt(32.W)))

  for (i <- 0 until ramPorts) {
    when (io.ports(i).write) {
      mem.write(io.ports(i).addr, io.ports(i).dataIn)
    }
    out(i) := mem.read(io.ports(i).addr)
    io.ports(i).data_out := out(i)
  }
}
