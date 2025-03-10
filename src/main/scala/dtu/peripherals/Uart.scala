package dtu.peripherals

import chisel3._
import chisel3.util._

import leros.DataMemIO

import leros.uart.UartIO

class UartPins extends Bundle {
  val tx = Output(Bool())
  val rx = Input(Bool())
}

class UartTx(frequency: Int, baudRate: Int) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
    val channel = Flipped(new UartIO())
  })

  val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).asUInt

  val shiftReg = RegInit(0x7ff.U)
  val cntReg = RegInit(0.U(BIT_CNT.getWidth.W))
  val bitsReg = RegInit(0.U(4.W))

  io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
  io.txd := shiftReg(0)

  when(cntReg === 0.U) {

    when(bitsReg =/= 0.U) {
      cntReg := BIT_CNT
      val shift = shiftReg >> 1
      shiftReg := 1.U ## shift(9, 0)
      bitsReg := bitsReg - 1.U
    }.elsewhen(io.channel.valid) {
        cntReg := BIT_CNT
        shiftReg := 3.U(2.W) ## io.channel.bits ## 0.B
        bitsReg := 11.U
    }

  } otherwise {
    cntReg := cntReg - 1.U
  }
}

class Uart(bufferSize: Int, frequency: Int, baud: Int) extends Module {

  val uartPins = IO(new UartPins)

  val dmemPort = IO(new DataMemIO(1))

  val tx = Module(new UartTx(frequency, baud))
  val rx = Module(new leros.uart.Rx(frequency, baud))

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
