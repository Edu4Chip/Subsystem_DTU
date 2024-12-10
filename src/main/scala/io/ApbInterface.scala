package io

import chisel3._
import chisel3.util._
import config._


class ApbInterface() extends Module {
    val apbAddrWidth = APB_CONFIG.ADDR_WIDTH
    val apbDataWidth = APB_CONFIG.DATA_WIDTH

    val lerosMemAddrWidth = LEROS_CONFIG.IMEM_ADDR_WIDTH
    val lerosMemDataWidth = LEROS_CONFIG.INSTR_WIDTH
    
    val nApbReadRegs = APB_CONFIG.N_READ_REGS
    val nApbWriteRegs = APB_CONFIG.N_WRITE_REGS

    val regsDataWidth = APB_CONFIG.REGS_WIDTH

    val apbBaseAddr = APB_CONFIG.BASE_ADDR

    val io = IO(new Bundle{
      val apb = new ApbTargetPort(apbAddrWidth, apbDataWidth)
      
      val imemReq = Output(Bool())
      val imemWrAddr = Output(UInt(lerosMemAddrWidth.W))
      val imemWrData = Output(UInt(lerosMemDataWidth.W))
      val imemWr = Output(Bool())
      val imemWrMask = Output(UInt((lerosMemDataWidth/8).W))

      val apbCCR = new DualRegPort(nApbReadRegs, nApbWriteRegs, regsDataWidth)

      val lerosReset = Output(Bool())
    })

    val idle :: write :: read :: Nil = Enum(3)
    val stateReg = RegInit(idle)

    val readyReg = WireDefault(false.B)
    val pslverrReg = WireDefault(false.B)
    val prDataReg = WireDefault(0.U(apbDataWidth.W))

    io.apb.pready := readyReg
    io.apb.prdata := prDataReg
    io.apb.pslverr := pslverrReg

    setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
    setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U)

    val lerosResetReg = RegInit(false.B)
    io.lerosReset := lerosResetReg

    val effAddr = io.apb.paddr - apbBaseAddr.U

    switch(stateReg) {
      is(idle) {
        io.apb.pready := false.B
        io.apb.prdata := 0.U

        setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
        setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U)

        when(io.apb.psel && io.apb.pwrite) {
            stateReg := write
        }
        . elsewhen(io.apb.psel && !io.apb.pwrite) {
            stateReg := read
        }
      }
      is(write) {
        when(effAddr >= APB_ADDR_SPACE.IMEM_START.U && effAddr <= APB_ADDR_SPACE.IMEM_END.U) {
          setImemPort(en=true.B, wrData=io.apb.pwdata, wrAddr=effAddr)
          setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U)

          pslverrReg := false.B            
        }
        . elsewhen(effAddr >= APB_ADDR_SPACE.WRITE_CCR_START.U && effAddr <= APB_ADDR_SPACE.WRITE_CCR_END.U) {
          setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
          setRegPort(wr=true.B, wrData=io.apb.pwdata, rdIdx=0.U, wrIdx=effAddr - APB_ADDR_SPACE.WRITE_CCR_START.U)

          pslverrReg := false.B
        }
        . elsewhen(effAddr === APB_ADDR_SPACE.RESET_REG.U) {
          setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
          setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U)
          lerosResetReg := Mux(io.apb.pwdata === 0.U, false.B, true.B)

          pslverrReg := false.B
        }
        . otherwise {
          setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
          setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U)

          pslverrReg := true.B
        }

        readyReg := true.B   

        stateReg := idle
      }

      is(read) {
        when(effAddr >= APB_ADDR_SPACE.READ_CCR_START.U && effAddr <= APB_ADDR_SPACE.READ_CCR_END.U) {
          setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
          setRegPort(wr=false.B, wrData=0.U, rdIdx=effAddr - APB_ADDR_SPACE.READ_CCR_START.U, wrIdx=0.U)
          prDataReg := io.apbCCR.rdData
          pslverrReg := false.B
        }
        . elsewhen(effAddr === APB_ADDR_SPACE.RESET_REG.U) {
          setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
          setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U) 
          prDataReg := lerosResetReg
          pslverrReg := false.B
        }
        . otherwise {
          setImemPort(en=false.B, wrData=0.U, wrAddr=0.U)
          setRegPort(wr=false.B, wrData=0.U, rdIdx=0.U, wrIdx=0.U)
          prDataReg := 0.U
          pslverrReg := true.B
        }

        readyReg := true.B   

        stateReg := idle
      }
      
    }


    def setImemPort(en:Bool, wrData:UInt, wrAddr:UInt) = {
      io.imemReq := en
      io.imemWr := en
      io.imemWrAddr := wrAddr
      io.imemWrData := wrData
      io.imemWrMask := Mux(en, "b11".U, 0.U)
    }

    def setRegPort(wr:Bool, wrData:UInt, rdIdx:UInt, wrIdx:UInt) = {
      io.apbCCR.wr := wr
      io.apbCCR.wrData := wrData
      io.apbCCR.rdIndex := rdIdx
      io.apbCCR.wrIndex := wrIdx
    }

}
