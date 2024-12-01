package io

import chisel3._
import chisel3.util._

class ApbLoader(apbAddrWidth:Int, apbDataWidth:Int, apbBaseAddr:Int, lerosCtrlRegAddr:Int, lerosMemAddrWidth:Int, lerosMemDataWidth:Int) extends Module {
    val io = IO(new Bundle{
        val apb = new ApbTargetPort(apbAddrWidth, apbDataWidth)
        
        val req = Output(Bool())
        val wrAddr = Output(UInt(lerosMemAddrWidth.W))
        val wrData = Output(UInt(lerosMemDataWidth.W))
        val wr = Output(Bool())
        val wrMask = Output(UInt((lerosMemDataWidth/8).W))

        val lerosReset = Output(Bool())
    })

    val idle :: write :: read :: Nil = Enum(3)
    val state = RegInit(idle)

    val lerosResetReg = RegInit(false.B)

    io.apb.pready := false.B
    io.apb.prdata := 0.U
    io.apb.pslverr := false.B

    io.req := false.B
    io.wrAddr := 0.U
    io.wrData := 0.U
    io.wrMask := 0.U
    io.wr := false.B    

    io.lerosReset := lerosResetReg

    switch(state) {
        is(idle) {
            io.apb.pready := false.B
            io.apb.prdata := 0.U
            io.apb.pslverr := false.B

            io.req := false.B
            io.wrAddr := 0.U
            io.wrData := 0.U
            io.wrMask := 0.U
            io.wr := false.B    

            when(io.apb.psel && io.apb.pwrite) {
                state := write
            }
            . elsewhen(io.apb.psel && !io.apb.pwrite) {
                state := read
            }
        }
        is(write) {
            when(io.apb.paddr === lerosCtrlRegAddr.U) {
                lerosResetReg := Mux(io.apb.pwdata === 0.U, false.B, true.B)
            }
            .otherwise {
                io.req := true.B
                io.wrAddr := io.apb.paddr - apbBaseAddr.U
                io.wrData := io.apb.pwdata
                io.wrMask := "b11".U
                io.wr := true.B    
            }
            
            io.apb.pslverr := (apbBaseAddr.U > io.apb.paddr)
            io.apb.pready := true.B

            when(~io.apb.psel) {
                state := idle
            }            
        }
        // Read not supported therefore raise error
        is(read) {
            io.apb.prdata := 0.U
            
            io.req := false.B
            io.wrAddr := 0.U
            io.wrData := 0.U
            io.wrMask := 0.U
            io.wr := false.B  

            io.apb.pready := true.B
            io.apb.pslverr := true.B

            when(~io.apb.psel) {
                state := idle        
            }            
        }
    }

}
