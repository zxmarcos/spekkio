package ooo.utils
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Cat, Counter, UIntToOH, is, switch}
import ooo.utils.ICacheState.{FILL, HIT_AFTER_FILL, IDLE, LINE_WRITE}

// 2 instrs per cycle.
class DirectCache32x96b extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val line = Output(UInt(96.W))
    val hit = Output(Bool())
    val we = Input(Bool())
    val wdata = Input(UInt(96.W))
  })


  // 32 entries - 16 bytes
  // T = Tag
  // B = Block
  // P = Block Set
  // I = Index
  // 31                                     0
  //  TTTT_TTTT TTTT_TTTT TTTT_TTTT BBBB_BIII

  val tags = Mem(32, UInt(21.W))
  val cache = Mem(32, UInt(96.W))
  val valid = Mem(32, Bool())

  val addr_blk = io.addr(7,3)
  val addr_tag = io.addr(31,8)

  val tag_match = tags(addr_blk) === addr_tag

  io.hit := valid(addr_blk) && tag_match
  io.line := cache(addr_blk)

  when(io.we) {
    cache(addr_blk) := io.wdata
    valid(addr_blk) := true.B
    tags(addr_blk) := addr_tag
  }
}

object ICacheState extends ChiselEnum {
  val IDLE, FILL, LINE_WRITE, HIT_AFTER_FILL = Value
}


class ICacheIO extends Bundle {
  val reqvalid = Input(Bool())
  val addr = Input(UInt(30.W))
  val instr = Output(Vec(2, UInt(32.W)))
  val stalled = Output(Bool())
  val valid = Output(Bool())
  val wb = Flipped(new WishboneIO())
}

// Tiny cache - 2 instr per cycle
class ICache extends Module {
  val io = IO(new ICacheIO)

  val sets = Seq.fill(2)(Module(new DirectCache32x96b()))
  val has_hit = sets(0).io.hit | sets(1).io.hit
  val line = Mux(sets(0).io.hit, sets(0).io.line, sets(1).io.line)

  val state = RegInit(IDLE)
  val wb_addr = RegInit(0.U(28.W))
  val wb_strobe = RegInit(false.B)
  val wb_cycle = RegInit(false.B)
  val word_read_counter = Counter(3)
  val word_request_counter = Counter(4)
  val line_write = RegInit(false.B)
  val query_addr = RegInit(0.U(28.W))

  // Default values...
  io.valid := false.B
  io.wb.cycle := wb_cycle
  io.wb.strobe := wb_strobe
  io.wb.address := wb_addr
  io.wb.write_enable := false.B
  io.wb.master_data := DontCare

  // Stall whenever state is not idle
  io.stalled := state =/= IDLE;

  val line_buffer = Reg(UInt(96.W))
  val query_addr_wire = Mux(state === IDLE, io.addr ## 0.U(2.W), query_addr ## 0.U(2.W))

  for (i <- 0 until 2) {
    sets(i).io.addr := query_addr_wire
    sets(i).io.wdata := line_buffer
    sets(i).io.we := line_write
  }

  switch(state) {
    is(IDLE) {
      when(io.reqvalid) {
        when(has_hit) {
          io.valid := true.B
        }.otherwise {
          state := FILL
          io.valid := false.B
          query_addr := io.addr

          // start bus transaction
          wb_addr := io.addr
          wb_cycle := true.B
          wb_strobe := true.B
          word_read_counter.reset()
          word_request_counter.reset()
        }
      }
    }
    is(FILL) {
      when(word_request_counter.value === 2.U && !io.wb.stall) {
        wb_strobe := false.B
      }.otherwise {
        // Increment request counter
        when(!io.wb.stall) {
          word_request_counter.inc()
          wb_addr := wb_addr + 1.U
        }
        // Wait until memory is not busy...
      }

      when(io.wb.ack) {
        // valid data, shift line buffer.
        line_buffer := io.wb.slave_data ## line_buffer(95, 32)
        when(word_read_counter.inc()) {
          state := LINE_WRITE
          line_write := true.B
          // end bus transaction.
          wb_cycle := false.B
          wb_strobe := false.B
          word_request_counter.reset()
        }
      }
    }
    is(LINE_WRITE) {
      line_write := false.B
      state := HIT_AFTER_FILL
    }
    is(HIT_AFTER_FILL) {
      state := IDLE
      io.valid := true.B
    }
  }

  // Align instructions...
  val is_second_instr = query_addr_wire(2)
  io.instr(0) := Mux(is_second_instr, line(63,32), line(31, 0))
  io.instr(1) := Mux(is_second_instr, line(95,64), line(63,32))
}

object Main extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(
    new ICache(),
    Array("--target-dir", "output/",
      "--emission-options", "disableMemRandomization,disableRegisterRandomization"))
}

