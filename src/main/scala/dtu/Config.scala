package dtu

object GLOBAL {
  val CLOCK_FREQ = 100000000

}

object IO_REG_MAP {
  import GLOBAL._

  val COUNT = 9

  val WIDTH = 8

  val UART_RX_DATA = 0
  val UART_RX_VALID = 1
  val UART_RX_READY = 2

  val UART_TX_DATA = 3
  val UART_TX_VALID = 4
  val UART_TX_READY = 5

  val PMOD_OE = 6
  val PMOD_GPO = 7
  val PMOD_GPI = 8
}

object LEROS_CONFIG {
  val DMEM_ADDR_WIDTH = 16
  val IMEM_ADDR_WIDTH = 16

  val INSTR_WIDTH = 16

  val UART_BAUDRATE = 115200

  val ACCU_SIZE = 32
}

object LEROS_ADDR_SPACE {
  val DMEM_START  = 0x0000
  val DMEM_END    = 0x03BF

  // IO is now mapped to 0x0f00, but wrAddr counts in 32-bit words
  val IO_START    = 0x03C0 
  val IO_END      = 0x04BF

  // Read only from Leros
  val READ_CCR_START = 0x04C0
  val READ_CCR_END   = 0x04CF

  // Write only from Leros
  val WRITE_CCR_START = 0x04D0
  val WRITE_CCR_END   = 0x04DF
}

object APB_CONFIG {
  val N_READ_REGS = 4
  val N_WRITE_REGS = 4
  val REGS_WIDTH = 8

  val BASE_ADDR  = 0x01052000
  val ADDR_RANGE = 0x00001000

  val ADDR_WIDTH = 32
  val DATA_WIDTH = 32
}

object APB_ADDR_SPACE {
  import APB_CONFIG._

  val IMEM_START = 0x000
  val IMEM_END = 0xFF8

  val READ_CCR_START = 0xFF8
  val READ_CCR_END = (READ_CCR_START + N_READ_REGS) - 1

  val WRITE_CCR_START = READ_CCR_END + 1
  val WRITE_CCR_END = (WRITE_CCR_START + N_WRITE_REGS) - 1

  val RESET_REG = 0xFFC
}