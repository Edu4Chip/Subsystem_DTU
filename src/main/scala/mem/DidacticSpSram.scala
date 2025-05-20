package mem

import chisel3._
import chisel3.util._

object DidacticSpSram {
  def create(words: Int): AbstractMemory = {
    val m = Module(new DidacticSpSram(words))
    m.io.wordAddr := DontCare
    m.io.write := DontCare
    m.io.req := 0.B
    m.io.wrData := DontCare
    m.io.mask := DontCare
    m
  }
}

class DidacticSpSram(words: Int) extends Module with AbstractMemory {
  val addrWidth = log2Ceil(words)

  val io = IO(new Bundle {
    val req = Input(Bool())
    val wordAddr = Input(UInt(addrWidth.W))
    val write = Input(Bool())
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val mask = Input(UInt(4.W))
  })

  val mem: DidacticSpSramBlackBox = Module(
    new DidacticSpSramBlackBox(words, 32)
  )

  mem.io.clk_i := clock
  mem.io.rst_ni := !reset.asBool

  mem.io.req_i := io.req
  mem.io.we_i := io.write
  mem.io.addr_i := io.wordAddr
  mem.io.wdata_i := io.wrData
  mem.io.be_i := io.mask
  mem.io.wuser_i := false.B

  io.rdData := mem.io.rdata_o

  def read(wordAddr: UInt): UInt = {
    io.req := true.B
    io.write := false.B
    io.wordAddr := wordAddr
    io.rdData
  }

  def write(wordAddr: UInt, data: UInt, strb: UInt): Unit = {
    io.req := true.B
    io.wordAddr := wordAddr
    io.wrData := data
    io.mask := strb
    io.write := true.B
  }
}

class DidacticSpSramBlackBox(words: Int, dataWidth: Int)
    extends BlackBox(
      Map(
        "INIT_FILE" -> "",
        "DATA_WIDTH" -> s"$dataWidth",
        "NUM_WORDS" -> s"$words"
      )
    )
    with HasBlackBoxPath {

  override val desiredName: String = "sp_sram"

  val io = IO(new Bundle {
    val clk_i = Input(Clock())
    val rst_ni = Input(Bool())

    val req_i = Input(Bool())
    val we_i = Input(Bool())
    val addr_i = Input(UInt(log2Ceil(words).W))
    val wdata_i = Input(UInt(dataWidth.W))
    val be_i = Input(UInt(((dataWidth + 7) / 8).W))
    val rdata_o = Output(UInt(dataWidth.W))

    val ruser_o = Output(Bool())
    val wuser_i = Input(Bool())
  })

}
