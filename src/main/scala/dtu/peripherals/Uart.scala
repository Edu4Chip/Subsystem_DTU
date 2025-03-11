package dtu.peripherals

import chisel3._
import chisel3.util._

import leros.DataMemIO

import leros.uart.{BufferedTx, UARTRx}

class UartPins extends Bundle {
  val tx = Output(Bool())
  val rx = Input(Bool())
}

class Uart(bufferSize: Int, frequency: Int, baud: Int) extends Module {

  val uartPins = IO(new UartPins)

  val dmemPort = IO(new DataMemIO(1))

  val tx = Module(new BufferedTx(frequency, baud))
  val rx = Module(new UARTRx(frequency, baud))

  tx.io.channel.valid := 0.B
  tx.io.channel.bits := dmemPort.wrData
  uartPins.tx := tx.io.txd

  rx.io.out.ready := 0.B
  rx.io.rxd := uartPins.rx

  when(dmemPort.rdAddr === 0.U) {
    dmemPort.rdData := tx.io.channel.ready ## rx.io.out.valid
  } otherwise {
    dmemPort.rdData := rx.io.out.bits
  }

  when(dmemPort.wr) {
    switch(dmemPort.wrAddr) {
      is(0.U) {
        // nothing to do
      }
      is(1.U) {
        tx.io.channel.valid := 1.B
      }
    }
  }

}
