        // Set indirect base address to 0x8000
        loadi   0x00
        loadhi  0x80
        store   r1
        ldaddr  r1

wait_change:
        ldind   0           // Load CCR[0]
        store   r2          // Store previous value

wait_loop:
        ldind   0           // Load CCR[0] again
        sub     r2          // Compare with previous
        brz     wait_loop   // If same, loop

        load    r2
        andi    1           // Check for rising edge: new == 1 && old == 0
        brz     wait_change // If not 1, go back to wait for next change

        ldind   1           // Load CCR[1]
        store   r2

        ldind   2           // Load CCR[2]
        add     r2          // r0 = CCR[1] + CCR[2]

        stind   3           // Store result in CCR[3]

        br      wait_change // Loop forever
