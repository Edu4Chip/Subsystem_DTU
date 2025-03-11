package mem

import chisel3._
import chisel3.util.log2Ceil

import misc.Helper.WordToByte
import misc.Helper.BytesToWord

object RegMemory {
  def create(words: Int): AbstractMemory = {
    val m = Module(new RegMemory(words))
    m.io.wordAddr := DontCare
    m.io.write := 0.B
    m.io.wrData := DontCare
    m.io.mask := DontCare
    m
  }
}
class RegMemory(words: Int) extends Module with AbstractMemory {
  val addrWidth = log2Ceil(words)

  val io = IO(new Bundle {
    val wordAddr = Input(UInt(addrWidth.W))
    val write = Input(Bool())
    val wrData = Input(UInt(32.W))
    val rdData = Output(UInt(32.W))
    val mask = Input(UInt(4.W))
  })

  val mem = RegInit(VecInit.fill(words, 4)(0.U(8.W)))

  val addrReg = RegNext(io.wordAddr)
  io.rdData := mem(addrReg).toWord

  when(io.write) {
    val data = io.wrData.toBytes(4)
    val mask = io.mask.asBools
    (mem(io.wordAddr), data, mask).zipped.foreach { (m, d, msk) =>
      when(msk) {
        m := d
      }
    }
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
