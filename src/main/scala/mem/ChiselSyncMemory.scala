package mem

import chisel3._
import chisel3.util.log2Ceil

import misc.Helper.WordToByte
import misc.Helper.BytesToWord

object ChiselSyncMemory extends MemoryFactory {
  def create(words: Int): AbstractMemory = {
    val m = Module(new ChiselSyncMemory(words))
    m.io.wordAddr := DontCare
    m.io.write := 0.B
    m.io.wrData := DontCare
    m.io.mask := DontCare
    m
  }
}
class ChiselSyncMemory(words: Int) extends Module with AbstractMemory {
  val addrWidth = log2Ceil(words)

  val io = IO(new Bundle {
    val wordAddr = Input(UInt(addrWidth.W))
    val write = Input(Bool())
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val mask = Input(UInt(4.W))
  })

  val mem = SyncReadMem(words, Vec(4, UInt(8.W)))

  io.rdData := DontCare

  when(io.write) {
    val data = io.wrData.toBytes(4)
    val mask = io.mask.asBools
    mem.write(io.wordAddr, data, mask)
  }.otherwise {
    io.rdData := mem.read(io.wordAddr).toWord
  }

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
