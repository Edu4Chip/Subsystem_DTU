package mem

import chisel3._
import chisel3.util.log2Ceil
import chisel3.util.HasBlackBoxPath
import os.copy.over


object DffRam extends MemoryFactory {
  def create(words: Int): AbstractMemory = {
    val m = Module(new DffRam(words))
    m.io.wordAddr := DontCare
    m.io.write := 0.U
    m.io.wrData := DontCare
    m.io.en := 0.B
    m
  }
}


class DffRam(words: Int) extends Module with AbstractMemory {
  val addrWidth = log2Ceil(words)

  val io = IO(new Bundle {
    val wordAddr = Input(UInt(addrWidth.W))
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val write = Input(UInt(4.W))
    val en = Input(Bool())
  })

  val mem: DffRamBlackbox = Module(words match {
    case 32 => new DffRamBlackbox(32)
    case 128 => new DffRamBlackbox(128)
    case 256 => new DffRamBlackbox(256)
    case _ => throw new Exception(s"Unsupported memory size $words for DffRam")
  })

  mem.io.CLK := clock
  mem.io.EN0 := io.en
  mem.io.A0 := io.wordAddr
  mem.io.Di0 := io.wrData
  mem.io.WE0 := io.write
  io.rdData := mem.io.Do0

  def read(wordAddr: UInt): UInt = {
    io.en := 1.B
    io.wordAddr := wordAddr
    io.rdData
  }

  def write(wordAddr: UInt, data: UInt, strb: UInt): Unit = {
    io.wordAddr := wordAddr
    io.wrData := data
    io.write := strb
    io.en := 1.B
  }

}

class DffRamBlackbox(words: Int) extends BlackBox {
  val addrWidth = log2Ceil(words)
  override def desiredName: String = s"RAM${words}"

  val io = IO(new Bundle {
    val CLK = Input(Clock())
    val EN0 = Input(Bool())
    val A0 = Input(UInt(addrWidth.W))
    val Di0 = Input(UInt(32.W))
    val Do0 = Output(UInt(32.W))
    val WE0 = Input(UInt(4.W))
  })
}