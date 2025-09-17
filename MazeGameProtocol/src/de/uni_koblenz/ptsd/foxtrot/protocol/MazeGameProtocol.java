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

public class MazeGameProtocol implements AutoCloseable {
    private final CommandHandler handler;
    private final MessageParser parser = new MessageParser();

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private Thread readerThread;
    private volatile boolean running = true;

    public MazeGameProtocol(CommandHandler handler) {
        this.handler = handler;
    }

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
                    } else {
                        if (line.equals("RDY.")) {
                            Platform.runLater(() -> GameStatusModel.getInstance().setReady(true));
                        } else {
                            Command command = this.parser.parse(line);
                            if (command != null) {
                                this.handler.enqueueCommand(command);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection closed: " + e.getMessage());
            }
        });
        this.readerThread.start();
    }

    public void sendHello(String nickname) throws IOException {
        this.send("HELO;" + nickname);
    }

    public void sendMazeQuery() throws IOException {
        this.send("MAZ?");
    }

    public void sendStep() throws IOException {
        this.send("STEP");
    }

    public void sendTurn(char direction) throws IOException {
        if (direction != 'l' && direction != 'r') {
            throw new IllegalArgumentException();
        }
        this.send("TURN;" + direction);
    }

    public void sendBye() throws IOException {
        this.send("BYE!");
    }

    public void send(String message) throws IOException {
        this.out.write(message);
        this.out.write("\r\n");
        this.out.flush();
    }

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
