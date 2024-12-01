package leros

import chisel3._
import chisel3.util._

class SramSim(addrWidth: Int, dataWidth: Int) extends Module {
  
  val maskWidth = dataWidth/8

  val io = IO(new Bundle {
    val req = Input(Bool())

    val wrAddr = Input(UInt(addrWidth.W))
    val wrData = Input(UInt(dataWidth.W))
    val wr = Input(Bool())
    val wrMask = Input(UInt((dataWidth/8).W))

    val rdAddr = Input(UInt(addrWidth.W))
    val rdData = Output(UInt(dataWidth.W)) 
  })

  val rdVec = Wire(Vec(maskWidth, UInt(8.W)))
  for (i <- 0 until maskWidth) {
    rdVec(i) := 0.U(8.W)
  }

  val wrVec = Wire(Vec(maskWidth, UInt(8.W)))
  val wrMask = Wire(Vec(maskWidth, Bool()))
  
  for (i <- 0 until maskWidth) {
    wrVec(i) := io.wrData(i*8+7, i*8)
    wrMask(i) := io.wrMask(i)
  }

  val mem = SyncReadMem(math.pow(2, addrWidth).toInt, Vec(maskWidth, UInt(8.W)))

  when(io.req) {
    when(~io.wr) {
        rdVec := mem.read(io.rdAddr)
    }
    . otherwise {
        mem.write(io.wrAddr, wrVec, wrMask)
    }
  }

  io.rdData := Cat(rdVec.reverse)
}
