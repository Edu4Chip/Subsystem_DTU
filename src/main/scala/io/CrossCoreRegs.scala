package io

import chisel3._
import chisel3.util._
import leros._
import config._



class CrossCoreRegs() extends Module {
  
  val io = IO(new Bundle {
    val lerosCCR = Flipped(new DualRegPort(APB_CONFIG.N_READ_REGS, APB_CONFIG.N_WRITE_REGS, APB_CONFIG.REGS_WIDTH))
    val apbCCR =  Flipped(new DualRegPort(APB_CONFIG.N_READ_REGS, APB_CONFIG.N_WRITE_REGS, APB_CONFIG.REGS_WIDTH))
  })

  // Read only from APB bus perspective
  // Write only from Leros perspective
  val apbReadRegs = RegInit(VecInit(Seq.fill(APB_CONFIG.N_READ_REGS)(0.U(8.W))))

  // Write only registers from APB bus perspective
  // Read only registers from Leros perspective
  val apbWriteRegs = RegInit(VecInit(Seq.fill(APB_CONFIG.N_WRITE_REGS)(0.U(8.W))))

  when(io.apbCCR.wr) {
    apbWriteRegs(io.apbCCR.wrIndex) := io.apbCCR.wrData
  }
  io.apbCCR.rdData := apbReadRegs(io.apbCCR.rdIndex)

  when(io.lerosCCR.wr) {
    apbReadRegs(io.lerosCCR.wrIndex) := io.lerosCCR.wrData
  }
  io.lerosCCR.rdData := apbWriteRegs(io.lerosCCR.rdIndex)
}
