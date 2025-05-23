package ponte

import chisel3._
import chisel3.util._

import leros.uart.{Rx, Tx}
import io.UartPins
import apb.ApbPort

object Ponte {
  val START_WR = 0xaa
  val START_RD = 0xab
  val ESC = 0x5a
  val ESC_MASK = 0x20
}

/** `Ponte` is an UART-to-APB bridge that allows us to communicate with the APB
  * network of the DTU Subsystem using a UART interface. The targeted APB bus
  * has 16-bit addresses and 32-bit data words.
  *
  * @param frequency
  *   The frequency of the system clock
  * @param baudRate
  *   The baud rate of the UART interface
  */
class Ponte(frequency: Int, baudRate: Int) extends Module {

  val io = IO(new Bundle {
    val uart = new UartPins
    val apb = ApbPort.masterPort(16, 32)
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
