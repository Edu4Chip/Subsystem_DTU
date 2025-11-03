package dtu.peripherals

import chisel3._
import chisel3.util._

import leros.DataMemIO

import leros.uart.{BufferedTx, UARTRx}

/** Memory mapped UART peripheral attached to a data memory interface.
  *
  * Address 0: Reading returns the status of the UART peripheral. Bit 0
  * indicates whether new data is available. Bit 1 indicates whether the UART
  * peripheral is ready to accept new data. Writing to this address pops the receive
  * buffer.
  *
  * Address 1: Writing enqueues the lower byte into the transmit buffer. Reading
  * returns data from the receive buffer.
  *
  * @param frequency
  *   the frequency of the system clock
  * @param baud
  *   the baud rate of the UART
  */
class Uart(frequency: Int, baud: Int) extends Module {

  val uartPins = IO(new io.UartPins)
  val dmemPort = IO(new DataMemIO(1))

  val tx = Module(new BufferedTx(frequency, baud))
  val rx = Module(new UARTRx(frequency, baud))

  tx.io.channel.valid := 0.B
  tx.io.channel.bits := dmemPort.wrData
  uartPins.tx := tx.io.txd

  rx.io.out.ready := 0.B
  rx.io.rxd := uartPins.rx

  when(RegNext(dmemPort.rdAddr === 0.U)) {
    dmemPort.rdData := tx.io.channel.ready ## rx.io.out.valid
  } otherwise {
    dmemPort.rdData := rx.io.out.bits
  }

  when(dmemPort.wr) {
    switch(dmemPort.wrAddr) {
      is(0.U) { // write to status register pops the receive buffer
        rx.io.out.ready := 1.B
      }
      is(1.U) {
        tx.io.channel.valid := 1.B
      }
    }
  }

}
