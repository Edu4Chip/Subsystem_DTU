package apb

import chisel3._
import chisel3.util._
import misc.FormalHelper.properties

class ApbArbiter(addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val masters = Vec(2, new ApbTargetPort(addrWidth, dataWidth))
    val merged = Flipped(new ApbTargetPort(addrWidth, dataWidth))
  })

  properties {
    io.merged.masterPortProperties()
    io.masters.foreach(_.targetPortProperties())
  }

  object State extends ChiselEnum {
    val Idle, SetupLeft, ServeLeft, SetupRight, ServeRight = Value
  }

  val stateReg = RegInit(State.Idle)

  io.masters.foreach(_ <> io.merged)
  io.masters.foreach(_.pready := 0.B)
  io.masters.foreach(_.pslverr := 0.B)
  io.merged.psel := 0.B
  io.merged.penable := 0.B


  switch(stateReg) {
    is(State.Idle) {
      when(io.masters(0).psel) {
        stateReg := State.SetupLeft
      }.elsewhen(io.masters(1).psel) {
        stateReg := State.SetupRight
      }
    }
    is(State.SetupLeft) {
      io.merged <> io.masters(0)
      io.merged.penable := 0.B
      stateReg := State.ServeLeft
    }
    is(State.ServeLeft) {
      io.merged <> io.masters(0)
      when(io.merged.pready) {
        stateReg := State.Idle
      }
    }
    is(State.SetupRight) {
      io.merged <> io.masters(1)
      io.merged.penable := 0.B
      stateReg := State.ServeRight
    }
    is(State.ServeRight) {
      io.merged <> io.masters(1)
      when(io.merged.pready) {
        stateReg := State.Idle
      }
    }
  }
}
