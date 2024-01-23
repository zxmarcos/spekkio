package ooo.utils

import chisel3._
import chisel3.util.{Counter, Decoupled, EnqIO, isPow2, log2Ceil}

class CircularBufferIO(bitWidth: Int) extends Bundle {

  val enq = Flipped(Decoupled(UInt(bitWidth.W)))
  val deq = Decoupled(UInt(bitWidth.W))
  val is_full = Output(Bool())
  val is_empty = Output(Bool())

  def sequenceFill(start: Int, end: Int): Bool = {
    val counter = Counter(Math.abs(end - start))
    enq.enq(counter.value + start.U)
    when(enq.fire) {
      when(counter.inc()) {
        true.B
      }
    }
    false.B
  }
}

class CircularBuffer(entries: Int, bitWidth: Int) extends Module {
  require(entries > 1)
  require(isPow2(entries))
  val addr_size = log2Ceil(entries)

  val io = IO(new CircularBufferIO(bitWidth))

//  val mem = RegInit(VecInit.tabulate(entries) { _.U(addr_size.W) })
  val mem = Mem(entries, UInt(bitWidth.W))

  // Enqueue/Deque pointers
  val enq_ptr = RegInit(0.U(addr_size.W))
  val deq_ptr = RegInit(0.U(addr_size.W))

  val maybe_full = RegInit(false.B)

  val enq_deq_ptr_match = (enq_ptr === deq_ptr)
  val is_empty = enq_deq_ptr_match && !maybe_full
  val is_full = enq_deq_ptr_match && maybe_full
  val enq_next_ptr = (enq_ptr +% 1.U)
  val deq_next_ptr = (deq_ptr +% 1.U)

  val is_last_enq = enq_next_ptr === deq_ptr

  io.is_full := is_full
  io.is_empty := is_empty

  io.deq.valid := !is_empty
  io.deq.bits := mem(deq_ptr)
  when(io.deq.fire) {
    deq_ptr := deq_next_ptr
    when(deq_ptr =/= enq_ptr) {
      maybe_full := false.B
    }
  }

  // When fifo is ready to enqueue and valid bit is asserted
  io.enq.ready := !is_full
  when(io.enq.fire) {
    mem(enq_ptr) := io.enq.bits
    enq_ptr := enq_next_ptr
    // detect maybe_full
    when(is_last_enq) {
      maybe_full := true.B
    }
  }

}

class SequenceCircularBufferIO(bitWidth: Int) extends CircularBufferIO(bitWidth) {
  val initalized = Output(Bool())
}

class SequenceCircularBuffer(entries: Int, bitWidth: Int) extends Module {
  val io = IO(new SequenceCircularBufferIO(bitWidth))
  val buffer = Module(new CircularBuffer(entries, bitWidth))
  val is_initialized = RegInit(false.B)

  io.deq <> buffer.io.deq
  io.is_full <> buffer.io.is_full
  io.is_empty <> buffer.io.is_empty


  val counter = Counter(entries)

  when(!is_initialized) {
    // We do not accept deque in this state
    buffer.io.deq.ready := false.B
    io.enq.ready := false.B

    buffer.io.enq.enq(counter.value)
    when(buffer.io.enq.fire) {
      when(counter.inc()) {
        is_initialized := true.B
      }
    }
  }.otherwise {
    buffer.io.enq <> io.enq
    buffer.io.deq.ready := io.deq.ready
  }

  io.initalized := is_initialized
}
