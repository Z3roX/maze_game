package de.uni_koblenz.ptsd.foxtrot.protocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import de.uni_koblenz.ptsd.foxtrot.commandhandler.CommandHandler;
import de.uni_koblenz.ptsd.foxtrot.commandhandler.commands.Command;
import de.uni_koblenz.ptsd.foxtrot.gamestatus.model.GameStatusModel;
import javafx.application.Platform;


/**
 * The {@code MazeGameProtocol} class implements the communication
 * between the MazeGame client and the MazeGame server.
 * <p>
 * It manages the socket connection, reads incoming messages in a
 * dedicated thread, parses them into {@link Command} objects, and
 * forwards them to the {@link CommandHandler}. It also provides
 * convenience methods to send protocol-compliant messages to the
 * server.
 * </p>
 * 
 * <p>
 * <b>Note:</b> This JavaDoc was written with the assistance of ChatGPT.
 * </p>
 * 
 */
public class MazeGameProtocol implements AutoCloseable {

    private final CommandHandler handler;
    private final MessageParser parser = new MessageParser();

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread readerThread;
    private volatile boolean running = true;

    /**
     * Creates a new {@code MazeGameProtocol} instance.
     *
     * @param handler the {@link CommandHandler} used to enqueue parsed commands
     */
    public MazeGameProtocol(CommandHandler handler) {
        this.handler = handler;
    }

    /**
     * Establishes a TCP connection to the MazeGame server and starts
     * a background thread to read and process incoming messages.
     *
     * @param host the hostname or IP address of the server
     * @param port the port number of the server
     * @throws IOException if the connection cannot be established
     */
    public void connect(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8));

        this.readerThread = new Thread(() -> {
            String line;
            try {
                while (this.running && (line = this.in.readLine()) != null) {
                    if (line.startsWith("MAZE;")) {
                        String[] header = line.split(";");
                        int width = Integer.parseInt(header[1]);
                        int height = Integer.parseInt(header[2]);
                        String[] rows = new String[height];
                        for (int y = 0; y < height; y++) {
                            rows[y] = this.in.readLine();
                        }
                        Command command = this.parser.parseMaze(width, height, rows);
                        if (command != null) {
                            this.handler.enqueueCommand(command);
                        }
                    } else if (line.equals("RDY.")) {
                        Platform.runLater(() -> GameStatusModel.getInstance().setReady(true));
                    } else {
                        Command command = this.parser.parse(line);
                        if (command != null) {
                            this.handler.enqueueCommand(command);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[MazeGameProtocol] Connection lost: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("[MazeGameProtocol] Unexpected error while reading messages: " + e.getMessage());
                e.printStackTrace();
            }
        }, "MazeGameProtocol Reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    /**
     * Sends a {@code HELO} message to log in with the given nickname.
     *
     * @param nickname the nickname of the player
     * @throws IOException if writing to the socket fails
     */
    public void sendHello(String nickname) throws IOException {
        this.send("HELO;" + nickname);
    }

    /**
     * Sends a {@code MAZ?} message to request the maze data.
     *
     * @throws IOException if writing to the socket fails
     */
    public void sendMazeQuery() throws IOException {
        this.send("MAZ?");
    }

    /**
     * Sends a {@code STEP} message to request moving one step forward.
     *
     * @throws IOException if writing to the socket fails
     */
    public void sendStep() throws IOException {
        this.send("STEP");
    }

    /**
     * Sends a {@code TURN} message to request turning left or right.
     *
     * @param direction 'l' for left or 'r' for right
     * @throws IOException if writing to the socket fails
     * @throws IllegalArgumentException if the direction is invalid
     */
    public void sendTurn(char direction) throws IOException {
        if (direction != 'l' && direction != 'r') {
            throw new IllegalArgumentException("Direction must be 'l' or 'r'");
        }
        this.send("TURN;" + direction);
    }

    /**
     * Sends a {@code BYE!} message to disconnect from the server.
     *
     * @throws IOException if writing to the socket fails
     */
    public void sendBye() throws IOException {
        this.send("BYE!");
    }

    /**
     * Sends a raw message string to the server, appending CRLF.
     *
     * @param message the protocol message to send
     * @throws IOException if writing to the socket fails
     */
    public void send(String message) throws IOException {
        this.out.write(message);
        this.out.write("\r\n");
        this.out.flush();
    }

    /**
     * Closes the protocol by shutting down the reader thread
     * and closing the underlying socket connection.
     *
     * @throws IOException if closing the socket fails
     */
    @Override
    public void close() throws IOException {
        this.running = false;
        if (this.readerThread != null) {
            this.readerThread.interrupt();
        }
        if (this.socket != null) {
            this.socket.close();
        }
    }
}