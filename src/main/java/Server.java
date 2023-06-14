import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;


public class Server implements Runnable{
    private ServerSocket serverSocket;
    private final List<String> validPaths;

    public Server(List<String> validPaths) {
        this.validPaths = validPaths;
    }

    public void connectionHandling(List<String> validPaths) {
        try (
                final Socket socket = serverSocket.accept();
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
        ) {
            final String requestLine = in.readLine();
            final String[] parts = requestLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                return;
            }

            final String path = parts[1];
            if (!validPaths.contains(path)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                return;
            }

            final Path filePath = Path.of(".", "public", path);
            final String mimeType = Files.probeContentType(filePath);

            // special case for classic
            if (path.equals("/classic.html")) {
                final String template = Files.readString(filePath);
                final byte[] content = template.replace(
                        "{time}",
                        LocalDateTime.now().toString()
                ).getBytes();
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(content);
                out.flush();
                return;
            }

            final long length = Files.size(filePath);
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            Files.copy(filePath, out);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        start(9999);
        connectionHandling(validPaths);
    }
}
