package dtu.peripherals

import chisel3._
import chisel3.util._

import leros.DataMemIO

import leros.uart.{Tx, Rx}

class UartPins extends Bundle {
  val tx = Output(Bool())
  val rx = Input(Bool())
}

class Uart(bufferSize: Int, frequency: Int, baud: Int) extends Module {

  val uartPins = IO(new UartPins)

  val dmemPort = IO(new DataMemIO(1))

  val tx = Module(new Tx(frequency, baud))
  val rx = Module(new Rx(frequency, baud))

  val txQueue = Module(new Queue(UInt(8.W), bufferSize))
  val rxQueue = Module(new Queue(UInt(8.W), bufferSize))

  tx.io.channel <> txQueue.io.deq
  txQueue.io.enq.noenq()
  uartPins.tx := tx.io.txd

  rx.io.channel <> rxQueue.io.enq
  rxQueue.io.deq.nodeq()
  rx.io.rxd := uartPins.rx

  when(dmemPort.rdAddr === 0.U) {
    dmemPort.rdData := txQueue.io.enq.ready ## rxQueue.io.deq.valid
  } otherwise {
    dmemPort.rdData := rxQueue.io.deq.deq()
  }

  when(dmemPort.wr) {
    switch(dmemPort.wrAddr) {
      is(0.U) {
        // nothing to do
      }
      is(1.U) {
        txQueue.io.enq.enq(dmemPort.wrData)
      }
    }
  }

}
