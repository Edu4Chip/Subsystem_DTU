package mem


import chisel3._
import chisel3.util.log2Ceil

object ChipFoundrySram extends MemoryFactory {
  def create(words: Int): AbstractMemory = {
    val m = Module(new ChipFoundrySram(words))
    m.io.wordAddr := DontCare
    m.io.write := DontCare
    m.io.wrData := DontCare
    m.io.strb := DontCare
    m.io.valid := 0.B
    m
  }
}

class ChipFoundrySram(words: Int) extends Module with AbstractMemory {

  val io = IO(new Bundle {
    val valid = Input(Bool())
    val wordAddr = Input(UInt(log2Ceil(words).W))
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val write = Input(Bool())
    val strb = Input(UInt(4.W))
  })

  val mem = Module(words match {
    case 256 => new ChipFoundrySramBlackbox()
    case _ => throw new Exception(s"Unsupported memory size $words for ChipFoundrySram")
  })

  mem.io.wb_clk_i := clock
  mem.io.wb_rst_i := reset.asBool
  mem.io.wbs_stb_i := io.valid
  mem.io.wbs_cyc_i := io.valid
  mem.io.wbs_we_i := io.write
  mem.io.wbs_sel_i := io.strb
  mem.io.wbs_dat_i := io.wrData
  mem.io.wbs_adr_i := io.wordAddr
  io.rdData := mem.io.wbs_dat_o
  // mem.io.wbs_ack_o is ignored

  def read(wordAddr: UInt): UInt = {
    io.wordAddr := wordAddr
    io.write := 0.B
    io.valid := 1.B
    io.rdData
  }

  def write(wordAddr: UInt, data: UInt, strb: UInt): Unit = {
    io.wordAddr := wordAddr
    io.wrData := data
    io.strb := strb
    io.write := 1.B
    io.valid := 1.B
  }

}


class ChipFoundrySramBlackbox extends BlackBox {

  override val desiredName = "CF_SRAM_1024x32_wb_wrapper"

  val io = IO(new Bundle {
    val wb_clk_i = Input(Clock())
    val wb_rst_i = Input(Bool())
    val wbs_stb_i = Input(Bool())
    val wbs_cyc_i = Input(Bool())
    val wbs_we_i = Input(Bool())
    val wbs_sel_i = Input(UInt(4.W))
    val wbs_dat_i = Input(UInt(32.W))
    val wbs_adr_i = Input(UInt(32.W))
    val wbs_ack_o = Output(Bool())
    val wbs_dat_o = Output(UInt(32.W))
  })
}
