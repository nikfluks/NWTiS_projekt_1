package org.foi.nwtis.nikfluks.dretve;

import java.net.ServerSocket;
import java.net.Socket;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;

/**
 *
 * @author Nikola
 */
public class ServerSustava extends Thread {

    public static boolean radi = true;
    public static boolean pauziran = false;
    int port;
    Socket socket;
    ServerSocket serverSocket;
    ObradaKomandi obradaKomandi;

    public ServerSustava() {
    }

    @Override
    public void interrupt() {
        try {
            System.out.println("Server se gasi...");
            radi = false;
            /*if (obradaKomandi != null) {
                obradaKomandi.interrupt();
            }*/
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
            super.interrupt();
        } catch (Exception ex) {
            System.err.println("Greska kod gasenja servera: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (radi) {
                socket = serverSocket.accept();
                obradaKomandi = new ObradaKomandi(socket);
                obradaKomandi.start();
            }
            interrupt();
        } catch (Exception ex) {
            System.err.println("Greška kod obrade servera sustava: " + ex.getLocalizedMessage());
        }
    }

    @Override
    public synchronized void start() {
        if (dohvatiPodatkeIzKonfiguracije()) {
            super.start();
        } else {
            System.err.println("Greska kod dohvacanja podataka iz konfiguracije! Dretva nije startana!");
        }
    }

    private boolean dohvatiPodatkeIzKonfiguracije() {
        try {
            BP_Konfiguracija bpk = (BP_Konfiguracija) SlusacAplikacije.getServletContext().getAttribute("BP_Konfig");
            port = Integer.parseInt(bpk.getPortServera_());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
