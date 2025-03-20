package ponte

import chisel3._
import chisel3.util._

import apb.ApbTargetPort
import leros.uart.UartIO

import misc.FormalHelper._

class PonteDecoder extends Module {

  val io = IO(new Bundle {
    val in = Flipped(new UartIO)
    val out = new UartIO
    val apb = Flipped(new ApbTargetPort(16, 32))
  })

  properties {
    io.apb.masterPortProperties()
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
    is(State.Start) {
      cntReg := 1.U
      isWriteReg := dec.io.startWrite

      when(dec.io.valid) {
        when(dec.io.startRead) {
          stateReg := State.ReadLen
        }.elsewhen(dec.io.startWrite) {
          stateReg := State.Address
        }
      }
    }
    is(State.ReadLen) {
      cntReg := 1.U
      readLenReg := dec.io.data
      when(dec.io.valid) {
        stateReg := State.Address
      }
    }
    is(State.Address) {
      when(dec.io.valid) {
        cntReg := cntReg - 1.U
        addrReg := Cat(dec.io.data, addrReg(15, 8))
        when(cntReg === 0.U) {
          stateReg := Mux(isWriteReg, State.Data, State.Setup)
          cntReg := 3.U
        }
      }
    }
    is(State.Data) {
      when(dec.io.valid) {
        cntReg := cntReg - 1.U
        dataReg := Cat(dec.io.data, dataReg(31, 8))
        when(cntReg === 0.U) {
          stateReg := State.Setup
        }
      }
    }
    is(State.Setup) {
      dec.io.stall := 1.B
      io.apb.psel := 1.B
      stateReg := State.Wait
    }
    is(State.Wait) {
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
    is(State.Resp) {
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
    is(State.UpdateReadLen) {
      dec.io.stall := 1.B
      readLenReg := readLenReg - 1.U
      stateReg := Mux(readLenReg === 0.U, State.Start, State.Setup)
    }

  }

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
