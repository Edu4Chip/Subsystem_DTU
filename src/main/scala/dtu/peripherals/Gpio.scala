package dtu.peripherals

import chisel3._
import chisel3.util._

import apb.ApbTargetPort

import leros.DataMemIO

import didactic.PmodGpioPort

class Gpio extends Module {

  val dmemPort = IO(new DataMemIO(2))
  val pmodPort = IO(new PmodGpioPort)

  val oes = RegInit(0.U(4.W))
  val gpos = RegInit(0.U(4.W))
  val gpis = pmodPort.gpi

  dmemPort.rdData := MuxLookup(
    RegNext(dmemPort.rdAddr),
    DontCare,
    Seq(
      0.U -> oes,
      1.U -> gpos,
      2.U -> gpis
    )
  )

  when(dmemPort.wr) {
    switch(dmemPort.wrAddr) {
      is(0.U) {
        oes := dmemPort.wrData
      }
      is(1.U) {
        gpos := dmemPort.wrData
      }
    }
  }

  pmodPort.gpo := gpos
  pmodPort.oe := oes

}
