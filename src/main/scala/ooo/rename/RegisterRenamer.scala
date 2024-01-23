package ooo.rename

import chisel3._
import chisel3.util.{Counter, Decoupled, Valid, log2Ceil}
import ooo.utils.CircularBuffer

class RegisterRenamer(entries: Int = 16) extends Module {
  val addr_size = log2Ceil(entries*2)
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(UInt(addr_size.W)))
    val res = Valid(UInt(addr_size.W))
    val empty = Output(Bool())
    val full = Output(Bool())
  })

  val free_list = Module(new CircularBuffer(entries, addr_size))

  // Fill register free list with all registers...
  val reg_counter = Counter(entries)
  val is_initialized = RegInit(false.B)

  // Default state...
  free_list.io.deq.ready := false.B
  io.res.bits := DontCare
  io.res.valid := false.B

  io.empty := free_list.io.is_empty
  io.full := free_list.io.is_full

  when(!is_initialized) {
    // We start from 16 -> length(PRF)
    free_list.io.enq.enq(reg_counter.value + 16.U)

    // Check if we can enqueue data to buffer
    when(free_list.io.enq.fire) {
      when(reg_counter.inc()) {
        is_initialized := true.B
      }
    }
  }.otherwise {
    free_list.io.enq.noenq()
  }

  when(io.req.fire) {
    io.res.bits := free_list.io.deq.deq()
  }


  when (is_initialized) {
    // Deque is only valid when fully initialized
    io.res.valid := free_list.io.deq.valid
    // Can only request a rename operation when fully initialized
    io.req.ready := true.B
  }.otherwise {
    io.req.ready := false.B
  }

}
