package io

import chisel3._
import chisel3.util._
import leros._
import leros.uart._
import config._

class Peripherals() extends Module {
  
  val io = IO(new Bundle {
    val regPort = Flipped(new SingleRegPort(IO_REG_MAP.COUNT, IO_REG_MAP.WIDTH))

    val pmodGpio = new PmodGpioPort()
    val uartTx = Output(UInt(1.W))
    val uartRx = Input(UInt(1.W))
  })

  val uartRx = Module(new UARTRx(GLOBAL.CLOCK_FREQ, LEROS_CONFIG.UART_BAUDRATE))
  val uartTx = Module(new BufferedTx(GLOBAL.CLOCK_FREQ, LEROS_CONFIG.UART_BAUDRATE))

  io.uartTx := uartTx.io.txd
  uartRx.io.rxd := io.uartRx

  val registerMap = RegInit(VecInit(Seq.fill(IO_REG_MAP.COUNT)(0.U(8.W))))

  registerMap(IO_REG_MAP.UART_RX_DATA.U) := uartRx.io.out.bits
  registerMap(IO_REG_MAP.UART_RX_VALID) := uartRx.io.out.valid
  uartRx.io.out.ready := registerMap(IO_REG_MAP.UART_RX_READY)

  uartTx.io.channel.bits := registerMap(IO_REG_MAP.UART_TX_DATA.U)
  uartTx.io.channel.valid := registerMap(IO_REG_MAP.UART_TX_VALID)
  registerMap(IO_REG_MAP.UART_TX_READY) := uartTx.io.channel.ready

  io.pmodGpio.oe := registerMap(IO_REG_MAP.PMOD_OE)
  io.pmodGpio.gpo := registerMap(IO_REG_MAP.PMOD_GPO)
  registerMap(IO_REG_MAP.PMOD_GPI) := io.pmodGpio.gpi


  io.regPort.rdData := registerMap(io.regPort.index)

  when(io.regPort.wr) {
    registerMap(io.regPort.index) := io.regPort.wrData    
  }
}
