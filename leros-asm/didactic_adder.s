        // Set indirect base address to 0x8000
        loadi   0x00
        loadhi  0x80
        loadh2i 0x00
        store   r1
        ldaddr  r1

wait_change:
        loadi   1           // load ready
        stind   0           // signal ready

wait_loop:
        ldind   0           // Load CCR[0]
        brz     wait_loop   // If 0, loop

        loadi   0
        stind   0           // signal busy

        ldind   1           // Load CCR[1]
        store   r2

        ldind   2           // Load CCR[2]
        add     r2          // r0 = CCR[1] + CCR[2]

        stind   3           // Store result in CCR[3]

        br      wait_change // Loop forever
