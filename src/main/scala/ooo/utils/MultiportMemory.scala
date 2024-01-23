package ooo.utils

import chisel3._
import chisel3.util.{MuxCase, log2Ceil}



case class MultiportMemoryConfig(readPorts: Int = 2, writePorts: Int = 1, entries: Int = 32, dataSize: Int = 32, debugPorts: Boolean = false) {
  val addrSize: Int = log2Ceil(entries)
}

class MultiportMemoryWritePort(config: MultiportMemoryConfig = MultiportMemoryConfig()) extends Bundle {
  val enable = Input(Bool())
  val addr = Input(UInt(config.addrSize.W))
  val data = Input(UInt(config.dataSize.W))

  def write(addr: UInt, data: UInt): Unit = {
    this.enable := true.B
    this.addr := addr
    this.data := data
  }

  def clear(): Unit = {
    this.enable := false.B
    this.addr := 0.U
    this.data := 0.U
  }
}

class MultiportMemoryReadPort(config: MultiportMemoryConfig = MultiportMemoryConfig()) extends Bundle {
  val addr = Input(UInt(config.addrSize.W))
  val data = Output(UInt(config.dataSize.W))

  def read(addr: UInt): UInt = {
    this.addr := addr
    this.data
  }
}

abstract class MPRAM(config: MultiportMemoryConfig) extends Module {
  val io = IO(new Bundle {
    val read = Vec(config.readPorts, new MultiportMemoryReadPort(config))
    val write = Vec(config.writePorts, new MultiportMemoryWritePort(config))
  })
}


class MultiportMemory1w2r(config: MultiportMemoryConfig) extends Module {
  val io = IO(new Bundle {
    val read = Vec(2, new MultiportMemoryReadPort(config))
    val write = new MultiportMemoryWritePort(config)
  })

  val reg = Mem(config.entries, UInt(config.dataSize.W))

  when(io.write.enable) {
    reg.write(io.write.addr, io.write.data)
  }

  io.read(0).data := reg(io.read(0).addr)
  io.read(1).data := reg(io.read(1).addr)
}

class MultiportMemoryBank1wNr(config: MultiportMemoryConfig) extends Module {
  val io = IO(new Bundle {
    val read = Vec(config.readPorts, new MultiportMemoryReadPort(config))
    val write = new MultiportMemoryWritePort(config)
  })

  for (i <- 0 until config.readPorts / 2) {
    val rf = Module(new MultiportMemory1w2r(config))
    rf.io.write <> io.write
    rf.io.read(0) <> io.read(i * 2 + 0)
    rf.io.read(1) <> io.read(i * 2 + 1)
  }
}


class MultiportMemoryLVT(config: MultiportMemoryConfig) extends MPRAM(config) {

  val banks = for (wr <- 0 until config.writePorts) yield {
    val writeBank = Module(new MultiportMemoryBank1wNr(config))
    val readBanks = Wire(Vec(config.readPorts, UInt(config.dataSize.W)))

    // 1 write port for all read ports in this bank
    writeBank.io.write <> io.write(wr) // Wire read addresses...
    writeBank.io.read <> io.read
    for (rd <- 0 until config.readPorts) {
      readBanks(rd) := writeBank.io.read(rd).data
    }

    readBanks
  }

  val LVT = Mem(config.entries, UInt(log2Ceil(config.writePorts).W))
  for (i <- 0 until config.writePorts) {
    when(io.write(i).enable) {
      LVT.write(io.write(i).addr, i.asUInt)
    }
  }

  for (rd <- 0 until config.readPorts) {
    // Current "live value index" in LVT table for read address
    val lvtWriteBank = LVT.read(io.read(rd).addr)

    val lookupLvt = banks.zipWithIndex.map { case (bank, index) => (lvtWriteBank === index.U) -> bank(rd) }
    io.read(rd).data := MuxCase(0.U, lookupLvt)
  }

}



class MultiportMemoryREGRAM(config: MultiportMemoryConfig) extends MPRAM(config) {
  // Let synth-tool generate LUTRAM...
  val mem = Mem(config.entries, UInt(log2Ceil(config.dataSize).W))

  for (i <- 0 until config.readPorts) {
    io.read(i).data := mem.read(io.read(i).addr)
  }

  for (i <- 0 until config.writePorts) {
    when(io.write(i).enable) {
      mem(io.write(i).addr) := io.write(i).data
      //mem.write(io.write(i).addr, io.write(i).data)
    }
  }

}

class MultiportMemoryDBGRAM(config: MultiportMemoryConfig) extends MPRAM(config) {
  val dbg_ports = IO(Output(Vec(config.entries, UInt(config.dataSize.W))))
  // Let synth-tool generate LUTRAM...
  val mem = Reg(Vec(config.entries, UInt(config.dataSize.W)))

  val port_wires = Wire(Vec(config.entries, UInt(config.dataSize.W)))
  for (i <- 0 until config.entries) {
    port_wires(i) := mem(i.U)
    dbg_ports(i) := port_wires(i)
  }

  for (i <- 0 until config.readPorts) {
    io.read(i).data := mem(io.read(i).addr)
  }
  for (i <- 0 until config.writePorts) {
    when(io.write(i).enable) {
      mem(io.write(i).addr) := io.write(i).data
      //mem.write(io.write(i).addr, io.write(i).data)
    }
  }
}
