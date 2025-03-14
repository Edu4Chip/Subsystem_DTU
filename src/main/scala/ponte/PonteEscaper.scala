package ponte

import chisel3._
import chisel3.util._

import leros.uart.UartIO

class PonteEscaper extends Module {

  val io = IO(new Bundle {
    val in = Flipped(new UartIO)
    val valid = Output(Bool())
    val startRead = Output(Bool())
    val startWrite = Output(Bool())
    val data = Output(UInt(8.W))
    val stall = Input(Bool())
  })

  val escaped = RegInit(1.B)
  val isEscape = io.in.bits === Ponte.ESC.U

  when(isEscape && io.in.valid) {
    escaped := 1.B
  }.elsewhen(escaped && io.in.valid) {
    escaped := 0.B
  }

  io.valid := Mux(isEscape, 0.B, io.in.valid)
  io.startRead := io.in.bits === Ponte.START_RD.U
  io.startWrite := io.in.bits === Ponte.START_WR.U
  io.data := io.in.bits ^ Mux(escaped, Ponte.ESC_MASK.U, 0.U)

  io.in.ready := !io.stall

}
