import chisel3._
import chiseltest._


object TestingHelper {


    implicit class ClockExtension(c: Clock) {
        def stepUntil(cond: => Boolean, maxCycles: Int = 1000): Unit = {
            var cycles = 0
            while (!cond && cycles < maxCycles) {
                c.step()
                cycles += 1
            }
            assert(cycles < maxCycles, "Timeout")
        }
    }

}
