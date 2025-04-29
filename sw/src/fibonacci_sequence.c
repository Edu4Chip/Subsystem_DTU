//
// Created by Christine Elisabeth Koppel on 27-04-2025.
//

// This program calculates the (unoptimized) Fibonacci sequence up to a given number n without stdio.h

// Define a constant for the number of Fibonacci numbers to generate
#define MAX_FIB 20

// Function to calculate Fibonacci sequence
void fibonacci(int n, int* result) {
    // Initialize the first two Fibonacci numbers
    result[0] = 0;
    result[1] = 1;

    // Generate the remaining Fibonacci numbers
    for (int i = 2; i < n; i++) {
        result[i] = result[i-1] + result[i-2];
    }
}

// Main function
int main() {
    // Array to store Fibonacci sequence
    int fib_array[MAX_FIB];

    // Calculate Fibonacci sequence
    fibonacci(MAX_FIB, fib_array);

    // Since we're not using stdio.h, we don't print the results
    // In a real application, you might store them or use them somehow

    // Return the last Fibonacci number as the exit code
    return fib_array[MAX_FIB-1];
}