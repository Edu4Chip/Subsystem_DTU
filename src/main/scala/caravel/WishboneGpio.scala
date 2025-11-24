package caravel

import io.GpioPins
import chisel3._
import chisel3.util._
import misc.FormalHelper.formalProperties

class WishboneGpio(n: Int) extends Module {

  val io = IO(new Bundle {
    val wb = wishbone.WishbonePort.targetPort(2)
    val gpio = new GpioPins(n)
  })

  formalProperties {
    io.wb.targetPortProperties("WishboneGpio.wb")
  }

  val accessReg = RegInit(0.B)

  when(accessReg) {
    accessReg := 0.B
  }.elsewhen(io.wb.cyc) {
    accessReg := 1.B
  }

  val outputEnables = RegInit(0.U(n.W))
  val outputs = RegInit(0.U(n.W))
  val inputs = io.gpio.in

  io.gpio.out := outputs
  io.gpio.oe := outputEnables

  io.wb.ack := accessReg
  io.wb.dat_o := MuxLookup(RegNext(io.wb.adr), outputEnables)(
    Seq(
      0.U -> outputEnables,
      1.U -> outputs,
      2.U -> inputs
    )
  )

  when(accessReg && io.wb.we) {
    switch(io.wb.adr) {
      is(0.U) {
        outputEnables := io.wb.dat_i
      }
      is(1.U) {
        outputs := io.wb.dat_i
      }
    }
  }

}
