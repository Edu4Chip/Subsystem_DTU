package mem

import chisel3._
import chisel3.util._
import chisel3.BlackBox
import os.copy.over

object Sky130Sram extends MemoryFactory {

  def create(words: Int): AbstractMemory = {
    val m = Module(new Sky130Sram(words))
    m.io.wordAddr := DontCare
    m.io.write := 0.B
    m.io.wrData := DontCare
    m.io.mask := DontCare
    m
  }

}

class Sky130Sram(words: Int) extends Module with AbstractMemory {

  override def desiredName: String = s"Sky130Sram$words"

  val addrWidth = log2Ceil(words)

  val io = IO(new Bundle {
    val wordAddr = Input(UInt(addrWidth.W))
    val write = Input(Bool())
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val mask = Input(UInt(4.W))
  })

  val mem: Sky130SramBlackBox = Module(words match {
    case 256 => new Sky130Sram32x256
    case _ =>
      throw new Exception(s"Unsupported memory size $words for Sky130Sram")
  })

  mem.io.clk0 := clock
  mem.io.csb0 := 0.B
  mem.io.web0 := !io.write
  mem.io.wmask0 := io.mask
  mem.io.addr0 := io.wordAddr
  mem.io.din0 := io.wrData
  io.rdData := mem.io.dout0

  mem.io.clk1 := 0.B.asClock
  mem.io.csb1 := 1.B
  mem.io.addr1 := 0.U

  def read(wordAddr: UInt): UInt = {
    io.wordAddr := wordAddr
    io.rdData
  }

  def write(wordAddr: UInt, data: UInt, strb: UInt): Unit = {
    io.wordAddr := wordAddr
    io.wrData := data
    io.mask := strb
    io.write := 1.B
  }

}

class Sky130SramPort(addrWidth: Int) extends Bundle {
  val clk0 = Input(Clock())
  val csb0 = Input(Bool())
  val web0 = Input(Bool())
  val wmask0 = Input(UInt(4.W))
  val addr0 = Input(UInt(addrWidth.W))
  val din0 = Input(UInt(32.W))
  val dout0 = Output(UInt(32.W))

  val clk1 = Input(Clock())
  val csb1 = Input(Bool())
  val addr1 = Input(UInt(addrWidth.W))
  val dout1 = Output(UInt(32.W))
}

abstract class Sky130SramBlackBox(name: String, addrWidth: Int)
    extends BlackBox(Map("VERBOSE" -> 0)) {
  val io: Sky130SramPort = IO(new Sky130SramPort(addrWidth))
  override val desiredName: String = name
}

class Sky130Sram32x256
    extends Sky130SramBlackBox(
      "sky130_sram_1kbyte_1rw1r_32x256_8",
      8
    )
