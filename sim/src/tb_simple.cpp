#include <stdlib.h>
#include <iostream>
#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VDtuSubsystem.h"

// Maximum simulation time
#define MAX_SIM_TIME 1000
vluint64_t sim_time = 0;

int main(int argc, char **argv, char **env)
{
    // Initialize Verilator
    Verilated::commandArgs(argc, argv);

    // Create an instance of the DUT
    VDtuSubsystem *dut = new VDtuSubsystem;

    // Enable waveform tracing
    Verilated::traceEverOn(true);
    VerilatedVcdC *tfp = new VerilatedVcdC;
    dut->trace(tfp, 99); // Trace 99 levels of hierarchy
    tfp->open("subsystem_sim_wave.vcd");

    std::cout << "Starting simulation for DtuSubsystem" << std::endl;

    // Initialize inputs
    dut->clock = 0;
    dut->reset = 1; // Active high reset

    // Simulation loop
    while (sim_time < MAX_SIM_TIME)
    {
        // Toggle clock
        dut->clock = !dut->clock;

        // Release reset after a few cycles
        if (sim_time > 10 && dut->reset)
        {
            dut->reset = 0;
        }

        // Evaluate model
        dut->eval();

        // Dump waves
        tfp->dump(sim_time);

        // Increment simulation time
        sim_time++;

        // Add some test patterns here
        if (sim_time == 20)
        {
            std::cout << "Applying test vector at time " << sim_time << std::endl;
            // Add specific inputs to test DTU functionality, for example:
            // dut->io_cmd = 1;  // Example control signal
        }

        // Simple print of outputs for verification
        if (sim_time % 10 == 0 && dut->clock == 1)
        {
            std::cout << "Time: " << sim_time
                      << ", Reset: " << (dut->reset ? "active" : "inactive")
                      << std::endl;
            // Add additional output monitoring, for example:
            // std::cout << "Output value: " << dut->io_result << std::endl;
        }
    }

    // Clean up
    dut->final();
    tfp->close();
    delete tfp;
    delete dut;

    std::cout << "Simulation complete!" << std::endl;
    return EXIT_SUCCESS;
}
