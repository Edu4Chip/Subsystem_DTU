package ponte

import chisel3._
import chisel3.util._

import leros.uart.{Rx, Tx}
import io.UartPins
import apb.ApbTargetPort

object Ponte {
  val START_WR = 0xaa
  val START_RD = 0xab
  val ESC = 0x5a
  val ESC_MASK = 0x20
}

class Ponte(frequency: Int, baudRate: Int) extends Module {

  val io = IO(new Bundle {
    val uart = new UartPins
    val apb = Flipped(new ApbTargetPort(16, 32))
  })

  val uartRx = Module(new Rx(frequency, baudRate))
  val uartTx = Module(new Tx(frequency, baudRate))

  io.uart.tx := uartTx.io.txd
  uartRx.io.rxd := io.uart.rx

  val ponteDecoder = Module(new PonteDecoder)
  ponteDecoder.io.in <> uartRx.io.channel
  ponteDecoder.io.apb <> io.apb
  ponteDecoder.io.out <> uartTx.io.channel

}
