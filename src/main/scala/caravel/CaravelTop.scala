package caravel

import mem.MemoryFactory
import dtu.DtuSubsystemConfig

import chisel3._
import wishbone.WishboneMux

object CaravelTop extends App {

  circt.stage.ChiselStage.emitSystemVerilogFile((new CaravelTop(115200)).printMemoryMap(), Array("-td", "generated"), Array("--lowering-options=disallowLocalVariables,disallowPackedArrays"))
  
}

class CaravelTop(baud: Int) extends Module with CaravelUserProject {
  override def desiredName: String = "CaravelTop"

  val io = IO(new CaravelUserProjectIO(DtuSubsystemConfig.default.copy(gpioPins = 24, apbAddrWidth = 32)))

  // Caravel user project logic goes here

  val conf = DtuSubsystemConfig.default.copy(
    gpioPins = 8,
    frequency = 10_000_000,
    lerosBaudRate = baud,
    ponteBaudRate = baud,
    instructionMemorySize = 1 << 10,
    dataMemorySize = 1 << 10,
    romProgramPath = "leros-asm/selftest.s",
  )

  val lerosCfram = MemoryFactory.using(mem.ChiselSyncMemory) {
    Module(new LerosCaravel(conf, "ChipFoundrySram"))
  }
  lerosCfram.io.gpio.in := io.gpio.in

  val lerosSky130 = MemoryFactory.using(mem.ChiselSyncMemory) {
    Module(new LerosCaravel(conf, "ChipFoundrySram"))
  }
  lerosSky130.io.gpio.in := io.gpio.in


  val lerosDffram = MemoryFactory.using(mem.ChiselSyncMemory) {
    Module(new LerosCaravel(conf, "ChipFoundrySram"))
  }
  lerosDffram.io.gpio.in := io.gpio.in

  WishboneMux(io.wb)(
    lerosCfram.io.wb -> 0x00000000,
    lerosSky130.io.wb -> 0x00001000,
    lerosDffram.io.wb -> 0x00002000
  )

  io.gpio.out := 0.U
  io.gpio.oe := 0.U
  io.la.out := 0.U
  io.user_irq := 0.U
}