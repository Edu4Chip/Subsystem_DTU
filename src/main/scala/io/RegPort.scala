package io

import chisel3._
import chisel3.util._

class DualRegPort(nReadRegs:Int, nWriteRegs:Int, dataWidth:Int) extends Bundle() {
    val wrIndex = Output(UInt((log2Ceil(nWriteRegs)).W))
    val wrData = Output(UInt(dataWidth.W))
    val wr = Output(Bool()) 

    val rdIndex = Output(UInt((log2Ceil(nReadRegs)).W))
    val rdData = Input(UInt(dataWidth.W))
}

class SingleRegPort(nRegs:Int, dataWidth:Int) extends Bundle {
    val index = Output(UInt((log2Ceil(nRegs)).W))
    val wrData = Output(UInt(dataWidth.W))
    val wr = Output(Bool())
    val rdData = Input(UInt(dataWidth.W))
}