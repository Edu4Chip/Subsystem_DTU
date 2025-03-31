package ponte

import chisel3._
import chisel3.util._

import apb.ApbTargetPort
import leros.uart.UartIO

import misc.FormalHelper._

/** The `PonteDecoder` implements the decoding of the Ponte protocol. It
  * receives bytes through a handshake interface, uses the `PonteEscaper` to
  * unescape the bytes and decodes the protocol in a state machine. The decoded
  * transactions are the executed on the APB bus.
  */
class PonteDecoder extends Module {

  val io = IO(new Bundle {
    val in = Flipped(new UartIO)
    val out = new UartIO
    val apb = Flipped(new ApbTargetPort(16, 32))
  })

  properties {
    io.apb.masterPortProperties("PonteDecoder")
  }

  val dec = Module(new PonteEscaper)
  dec.io.in <> io.in

  object State extends ChiselEnum {
    val Start, ReadLen, Address, Data, Setup, Wait, Resp, UpdateReadLen = Value
  }

  val stateReg = RegInit(State.Start)
  val cntReg = RegInit(0.U(2.W))
  val addrReg = RegInit(0.U(16.W))
  val isWriteReg = RegInit(0.B)
  val readLenReg = RegInit(0.U(8.W))
  val dataReg = RegInit(0.U(32.W))

  io.apb.psel := 0.B
  io.apb.penable := 0.B
  io.apb.pwrite := isWriteReg
  io.apb.paddr := addrReg
  io.apb.pwdata := dataReg
  io.apb.pstrb := 0xf.U

  io.out.valid := 0.B
  io.out.bits := dataReg(7, 0)

  dec.io.stall := 0.B

  switch(stateReg) {
    is(State.Start) { // wait for new transactions
      cntReg := 1.U
      isWriteReg := dec.io.startWrite

      when(dec.io.valid) {
        when(dec.io.startRead) { // start read transaction
          stateReg := State.ReadLen
        }.elsewhen(dec.io.startWrite) {
          stateReg := State.Address // start write transaction
        }
      }
    }
    is(State.ReadLen) { // read the length of a read transaction
      cntReg := 1.U
      readLenReg := dec.io.data
      when(dec.io.valid) {
        stateReg := State.Address
      }
    }
    is(State.Address) { // read the 2-byte address
      when(dec.io.valid) {
        cntReg := cntReg - 1.U
        addrReg := Cat(dec.io.data, addrReg(15, 8))
        when(cntReg === 0.U) {
          stateReg := Mux(isWriteReg, State.Data, State.Setup)
          cntReg := 3.U
        }
      }
    }
    is(State.Data) { // read 4 bytes of write data
      when(dec.io.valid) {
        cntReg := cntReg - 1.U
        dataReg := Cat(dec.io.data, dataReg(31, 8))
        when(cntReg === 0.U) {
          stateReg := State.Setup
        }
      }
    }
    is(State.Setup) { // setup phase on the APB bus
      dec.io.stall := 1.B
      io.apb.psel := 1.B
      stateReg := State.Wait
    }
    is(State.Wait) { // access phase on the APB bus
      dec.io.stall := 1.B
      io.apb.psel := 1.B
      io.apb.penable := 1.B
      cntReg := 3.U
      when(io.apb.pready) {
        dataReg := io.apb.prdata
        addrReg := addrReg + 4.U
        stateReg := Mux(isWriteReg, State.Data, State.Resp)
      }
    }
    is(State.Resp) { // send 4 bytes of read data back
      dec.io.stall := 1.B
      io.out.valid := 1.B
      when(io.out.ready) {
        dataReg := dataReg(31, 8)
        cntReg := cntReg - 1.U
        when(cntReg === 0.U) {
          stateReg := State.UpdateReadLen
        }
      }
    }
    is(State.UpdateReadLen) { // decrement read counter
      dec.io.stall := 1.B
      readLenReg := readLenReg - 1.U
      stateReg := Mux(readLenReg === 0.U, State.Start, State.Setup)
    }

  }

  // when the FSM is not performing a APB transactions
  // a new Ponte transaction may be started, aborting 
  // the current one
  when(dec.io.valid && !dec.io.stall) {
    when(dec.io.startRead) {
      isWriteReg := 0.B
      stateReg := State.ReadLen
    }.elsewhen(dec.io.startWrite) {
      isWriteReg := 1.B
      stateReg := State.Address
      cntReg := 1.U
    }
  }

}
