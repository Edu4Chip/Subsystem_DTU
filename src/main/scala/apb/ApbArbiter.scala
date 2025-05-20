package apb

import chisel3._
import chisel3.util._
import misc.FormalHelper.formalProperties

object ApbArbiter {
  def apply(masterLeft: ApbPort, masterRight: ApbPort): ApbPort = {
    val addrWidth = math.max(masterLeft.addrWidth, masterRight.addrWidth)
    val dataWidth = math.max(masterLeft.dataWidth, masterRight.dataWidth)
    val arb = Module(new ApbArbiter(addrWidth, dataWidth))
    arb.io.merged.addChild = (child: ApbTarget) => {
      masterLeft.addChild(child)
      masterRight.addChild(child)
    }
    arb.io.masters(0) <> masterLeft
    arb.io.masters(1) <> masterRight
    arb.io.merged
  }
  def apply(masters: Seq[ApbPort]): ApbPort = {
    VecInit(masters).reduceTree(ApbArbiter(_, _))
  }
}

class ApbArbiter(addrWidth: Int, dataWidth: Int) extends Module {
  val io = IO(new Bundle {
    val masters = Vec(2, ApbPort.targetPort(addrWidth, dataWidth))
    val merged = ApbPort.masterPort(addrWidth, dataWidth)
  })

  formalProperties {
    io.merged.masterPortProperties("ApbArbiter.merged")
    io.masters.foreach(_.targetPortProperties("ApbArbiter.masters"))
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
