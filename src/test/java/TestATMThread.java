import csci2020u.lab10.server.ATMServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class TestATMThread {
    public static final int SERVER_PORT = 16789;
    private static final Thread thread = new Thread(ATMServer::new);
    private static PrintWriter networkOut;
    private static BufferedReader networkIn;

    private static String readLineTimeout() throws ExecutionException, InterruptedException, TimeoutException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(networkIn::readLine);

        try {
            return future.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @BeforeAll
    public static void setUp() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        thread.start();
        Thread.sleep(3000);

        Socket socket = new Socket("localhost", SERVER_PORT);
        networkOut = new PrintWriter(socket.getOutputStream(), true);
        networkIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        readLineTimeout();
        readLineTimeout();

        networkOut.println("UID admin");
        readLineTimeout();
        networkOut.println("PWD admin");
        readLineTimeout();
    }

    @Test
    void testProcessWITH_BreakCommand() throws InterruptedException {

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        System.setOut(new PrintStream(outContent));
        networkOut.println("WITH BREAK");
        Thread.sleep(1000);
        System.setOut(originalOut);

        String normalizedOutput = outContent.toString().replaceAll("\r\n", "\n");
        // Assert the output
        assertEquals("Client #1: Caught command WITH from the user\n" +
                "Client #1: Allowing authorized commands for user: admin\n" +
                "Client #1: WITH Nothing further from the user.\n" , normalizedOutput, "Incorrect log when BREAK");
    }

    @Test
    void testProcessWITH_InvalidNumber() throws ExecutionException, InterruptedException, TimeoutException {
        networkOut.println("WITH invalid");
        String message = readLineTimeout();

        assertEquals(message, "400 Bad request", "Incorrect response when argument is not a valid number.");
    }

    @Test
    void testProcessWITH_NoBalance() {
        Assertions.assertTimeout(Duration.ofSeconds(10), () -> {

            networkOut.println("NEW test test");
            readLineTimeout();
            networkOut.println("UID test");
            readLineTimeout();
            networkOut.println("PWD test");
            readLineTimeout();
            networkOut.println("WITH 50");
            String message = readLineTimeout();

            assertEquals(message, "400 Bad request", "Incorrect response when argument is more than the balance.");
        });
    }

    @Test
    void testProcessWITH_SufficientBalance() {
        Assertions.assertTimeout(Duration.ofSeconds(10), () -> {
            networkOut.println("UID admin");
            readLineTimeout();
            networkOut.println("PWD admin");
            readLineTimeout();
            networkOut.println("WITH 50");
            String message = readLineTimeout();

            assertEquals(message, "202 9950", "Incorrect response when argument withdraws balance.");

            networkOut.println("WITH");
            message = readLineTimeout();
            assertEquals("100 9950", message, "Incorrect response when argument is null.");
        });
    }
}
