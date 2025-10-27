package mem


import chisel3._
import chisel3.util.log2Ceil

object ChipFoundrySram extends MemoryFactory {
  def create(words: Int): AbstractMemory = {
    val m = Module(new ChipFoundrySram(words))
    m.io.wordAddr := DontCare
    m.io.wrData := DontCare
    m.io.strb := DontCare
    m.io.we := 0.B
    m
  }
}

class ChipFoundrySram(words: Int) extends Module with AbstractMemory {

  val io = IO(new Bundle {
    val wordAddr = Input(UInt(log2Ceil(words).W))
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val we = Input(Bool())
    val strb = Input(UInt(4.W))
  })

  val mem = Module(words match {
    case 256 => new ChipFoundrySramBlackbox()
    case _ => throw new Exception(s"Unsupported memory size $words for ChipFoundrySram")
  })

  mem.io.clk_i := clock
  mem.io.rst_i := reset.asBool
  mem.io.addr_i := io.wordAddr
  mem.io.we_i := io.we
  mem.io.sel_i := io.strb
  mem.io.wr_data_i := io.wrData
  io.rdData := mem.io.rd_data_o

  def read(wordAddr: UInt): UInt = {
    io.wordAddr := wordAddr
    io.rdData
  }

  def write(wordAddr: UInt, data: UInt, strb: UInt): Unit = {
    io.wordAddr := wordAddr
    io.wrData := data
    io.strb := strb
    io.we := 1.B
  }

}


class ChipFoundrySramBlackbox extends BlackBox {

  override val desiredName = "CF_SRAM_1024x32_wrapper"

  val io = IO(new Bundle {
    val clk_i = Input(Clock())
    val rst_i = Input(Bool())
    val addr_i = Input(UInt(8.W))
    val we_i = Input(Bool())
    val sel_i = Input(UInt(4.W))
    val wr_data_i = Input(UInt(32.W))
    val rd_data_o = Output(UInt(32.W))
  })
}
