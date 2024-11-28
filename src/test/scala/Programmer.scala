package leros

import chisel3._
import chisel3.util._
import java.nio.file._
import leros.shared.Constants._
import leros.uart._

/**
 * Sends content of a file over uart
 * frequency: clock frequency in hz
 * baudrate: baudrate in baud/s
 * txFile: path to file to be sent
 */
class Programmer(frequency: Int, baudRate: Int, txFile : String) extends Module {
  val io = IO(new Bundle {
    val txd = Output(UInt(1.W))
  })

  val tx = Module(new BufferedTx(frequency, baudRate))

  io.txd := tx.io.txd

  val txDataFile = Files.readAllBytes(Paths.get(txFile))
  val txData = VecInit(txDataFile.map(_.S(8.W)))
  val len = txData.length
  val cntReg = RegInit(0.U(log2Ceil(len).W))

  val instr = txData(cntReg).asUInt ## txData(cntReg-1.U).asUInt
  val enable = RegInit(true.B)
  enable := cntReg =/= len.U

  tx.io.channel.bits := txData(cntReg).asUInt
  tx.io.channel.valid := cntReg =/= len.U || enable

  when(tx.io.channel.ready && cntReg =/= len.U) {        
    cntReg := cntReg + 1.U
  }
}