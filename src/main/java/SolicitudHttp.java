import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    public SolicitudHttp(Socket socket) throws Exception {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            procesarSolicitud();
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }

    public void procesarSolicitud() throws Exception {
        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream()); // Usado para strings y bytes

        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));

        String lineaDeLaSolicitudHttp = in.readLine();

        // Extraer información importante de la solicitud.
        StringTokenizer partesSolicitud = new StringTokenizer(lineaDeLaSolicitudHttp);
        String metodo = partesSolicitud.nextToken();
        String archivo = partesSolicitud.nextToken();
        
        System.out.println("Solicitud: " + lineaDeLaSolicitudHttp);
        System.out.println("Metodo: " + metodo);
        System.out.println("Archivo: " + archivo);

        // Abre el archivo solicitado.
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("." + archivo);
        File file = new File("src/main/resources" + archivo);
        
        if (archivo.endsWith("/")) {
            archivo = "/emptyRequest.html";
            inputStream = ClassLoader.getSystemResourceAsStream("./emptyRequest.html");
            file = new File("src/main/resources/emptyRequest.html");
        }
        int filesize = (int) file.length();
        
        String lineaDeEstado = null;
        String lineaDeEncabezado = null;

        // Enviar el archivo solicitado.
        if (inputStream != null) {
            lineaDeEstado = "HTTP/1.1 200 OK";
            lineaDeEncabezado = contentType(archivo) + CRLF
                              + "Content-Length: " + filesize + CRLF;
            
            enviarString(lineaDeEstado, out);
            enviarString(lineaDeEncabezado, out);
            enviarString(CRLF, out);

            enviarBytes(inputStream, out);
            inputStream.close();

        } else {
            archivo = "/404.html";
            inputStream = ClassLoader.getSystemResourceAsStream("./404.html");
            
            lineaDeEstado = "HTTP/1.1 404 Not Found";
            int errorFileSize = inputStream.available();
            lineaDeEncabezado = contentType(archivo) + CRLF
                              + "Content-Length: " + errorFileSize + CRLF;
            
            enviarString(lineaDeEstado, out);
            enviarString(lineaDeEncabezado, out);
            enviarString(CRLF, out);

            enviarBytes(inputStream, out);
            inputStream.close();
        }

        // Cierra los streams y el socket.
        out.flush();
        out.close();
        in.close();
        socket.close();

    }

    private static void enviarString(String line, OutputStream os) throws Exception {
        os.write(line.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviarBytes(InputStream fis, OutputStream os) throws Exception {
        // Construye un buffer de 1KB para guardar los bytes cuando van hacia el socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia el archivo solicitado hacia el output stream del socket.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String nombreArchivo) {
        if(nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
                return "text/html";
        }
        if(nombreArchivo.endsWith(".jpg") || nombreArchivo.endsWith(".jpeg")) {
                return "image/jpeg";
        }
        if(nombreArchivo.endsWith(".gif")) {
                return "image/gif";
        }
        return "application/octet-stream";
}

}
