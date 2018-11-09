/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foi.nwtis.nikfluks.helperi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Nikola
 */
public class TestKomandi {

    StringBuilder odgovor;

    public static void main(String[] args) {
        TestKomandi tk = new TestKomandi();
        tk.preuzmiKontrolu();
    }


    public void preuzmiKontrolu() {
        String komandaZaSlanje = "KORISNIK nikfluks3; LOZINKA 123; KRENI;";
        System.out.println("komandaZaSlanje: " + komandaZaSlanje);

        try {
            Socket socket = new Socket("localhost", 15323);

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            os.write(komandaZaSlanje.getBytes());
            os.flush();
            socket.shutdownOutput();

            odgovor = new StringBuilder();
            int znak;
            //ako nije UTF-8 hrvatske znakove ne interpretira dobro kad dodu preko soketa
            BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.displayName()));
            while ((znak = in.read()) != -1) {
                odgovor.append((char) znak);
            }
            System.out.println("Odgovor: " + odgovor);

            in.close();
        } catch (IOException ex) {
            System.err.println("Greska kod TestKomandi: " + ex.getLocalizedMessage());
        }
    }

}
