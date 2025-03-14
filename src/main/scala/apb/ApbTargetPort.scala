package apb

import chisel3._
import chisel3.util._

class ApbTargetPort(
    val addrWidth: Int,
    val dataWidth: Int
) extends Bundle {
  val paddr = Input(UInt(addrWidth.W))
  val psel = Input(Bool())
  val penable = Input(Bool())
  val pwrite = Input(Bool())
  val pstrb = Input(UInt((dataWidth / 8).W))
  val pwdata = Input(UInt(dataWidth.W))
  val pready = Output(Bool())
  val prdata = Output(UInt(dataWidth.W))
  val pslverr = Output(Bool())
}
