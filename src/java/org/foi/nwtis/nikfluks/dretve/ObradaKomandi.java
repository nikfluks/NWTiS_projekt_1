package org.foi.nwtis.nikfluks.dretve;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.foi.nwtis.nikfluks.helperi.Korisnik;
import org.foi.nwtis.nikfluks.helperi.KorisnikListaj;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;
import org.foi.nwtis.nikfluks.wsParkiranje.StatusKorisnika;

/**
 *
 * @author Nikola
 */
public class ObradaKomandi extends Thread {

    String urlBaza;
    String korImeBaza;
    String lozinkaBaza;
    String uprProgram;
    Socket socket;
    String komanda;
    String odgovor = "";
    String ime;
    String prezime;
    String korisnickoIme = null;
    String lozinka;
    Matcher m;
    String server;
    String primatelj;
    String posiljatelj;
    String predmet;
    static int brojPoruke = 1;
    String korisnickoImeParkiranje;
    String lozinkaParkiranje;

    public ObradaKomandi(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void interrupt() {
        System.err.println("Obrada komandi se gasi..");
        super.interrupt();
    }

    @Override
    public void run() {
        try {
            long pocetakMillis = System.currentTimeMillis();
            InputStream is = socket.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder komandaBuilder = new StringBuilder();
            int znak = 0;

            while ((znak = in.read()) != -1) {
                komandaBuilder.append((char) znak);
            }
            komanda = komandaBuilder.toString();
            System.out.println("Komanda: " + komanda);

            analizirajKomandu();
            in.close();
            is.close();
            long krajMillis = System.currentTimeMillis();
            long trajanjeMillis = krajMillis - pocetakMillis;
            upisiUDnevnik(trajanjeMillis);
            korisnickoIme = null;
        } catch (Exception ex) {
            System.err.println("Greška kod obrade komandi: " + ex.getLocalizedMessage());
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
            urlBaza = bpk.getServerDatabase() + bpk.getUserDatabaseMySQL();
            korImeBaza = bpk.getUserUsername();
            lozinkaBaza = bpk.getUserPassword();
            server = bpk.getEmailServer_();
            primatelj = bpk.getEmailPrimatelj_();
            posiljatelj = bpk.getEmailPosiljatelj_();
            predmet = bpk.getEmailPredmet_();
            korisnickoImeParkiranje = bpk.getKorisnickoImeParkiranje_();
            lozinkaParkiranje = bpk.getLozinkaParkiranje_();
            uprProgram = bpk.getDriverDatabase();
            Class.forName(uprProgram);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private boolean upisiUDnevnik(long trajanjeMillis) {
        if (korisnickoIme != null) {
            String upit = "INSERT INTO dnevnik "
                    + "(korisnickoIme, sadrzaj, vrsta, url, ipAdresa, trajanjeZahtjeva) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try {
                Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
                PreparedStatement stat = con.prepareStatement(upit);
                stat.setString(1, korisnickoIme);
                stat.setString(2, komanda);
                stat.setString(3, "SOCKET");
                stat.setString(4, "http://localhost:8084/nikfluks_aplikacija_1/");
                stat.setString(5, socket.getInetAddress().getHostAddress());
                stat.setInt(6, (int) trajanjeMillis);
                stat.execute();
                stat.close();
                con.close();
                System.out.println("Upisano u dnevnik!" + "\n");
                return true;
            } catch (SQLException ex) {
                System.err.println("Greska kod upisa u dnevnik! " + ex.getLocalizedMessage());
            }
        }
        return false;
    }

    private void analizirajKomandu() {
        if (provjeriSintaksuZaPosluzitelja()) {
            korisnickoIme = m.group(1);
            lozinka = m.group(2);
            String komandaSplitana[] = komanda.split(";");
            if (komandaSplitana.length == 3 && komandaSplitana[2].contains("DODAJ")) {
                prethodiDodajKorisnika();
            } else {
                prethodiAutenticirajKorisnika(komandaSplitana, true);
            }
            posaljiEmail(komandaSplitana);
        } else if (provjeriSintaksuGrupe()) {
            korisnickoIme = m.group(1);
            lozinka = m.group(2);
            if (ServerSustava.pauziran) {
                odgovor = "ERR 12; Server je pauziran pa su dozvoljene samo poslužiteljske komande!";
            } else {
                String komandaSplitana[] = komanda.split(";");
                prethodiAutenticirajKorisnika(komandaSplitana, false);
            }
        } else {
            odgovor = "ERR; Pogrešna sintaksa!";
        }
        posaljiOdgovor();
    }

    private void posaljiEmail(String[] komandaSplitana) {
        try {
            Properties properties = System.getProperties();
            properties.put("mail.smtp.host", server);
            Session session = Session.getInstance(properties, null);
            MimeMessage message = new MimeMessage(session);
            Address fromAddress = new InternetAddress(posiljatelj);
            message.setFrom(fromAddress);
            Address[] toAddresses = InternetAddress.parse(primatelj);
            message.setRecipients(Message.RecipientType.TO, toAddresses);
            message.setSubject(predmet);
            //message.setText(odrediSadrzajEmaila(komandaSplitana));
            message.setContent(odrediSadrzajEmaila(komandaSplitana), "text/json;charset=utf-8");
            message.saveChanges();
            Transport.send(message);
        } catch (MessagingException ex) {
            System.err.println("Greska kod slanja emaila: " + ex.getLocalizedMessage());
        }
    }

    private String odrediSadrzajEmaila(String[] komandaSplitana) {
        String komandaBezLozinke = "";
        if (komandaSplitana.length == 3) {
            komandaBezLozinke = komandaSplitana[0] + ";" + komandaSplitana[2] + ";";
        } else {
            komandaBezLozinke = komandaSplitana[0] + ";";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
        String vrijeme = sdf.format(new Date());
        String sadrzajPoruke = "{\"id\": " + brojPoruke + ", \"komanda\": \"" + komandaBezLozinke
                + "\", \"vrijeme\": \"" + vrijeme + "\"}";
        brojPoruke++;
        return sadrzajPoruke;
    }

    private void prethodiDodajKorisnika() {
        prezime = m.group(3);
        ime = m.group(4);
        if (dodajKorisnika()) {
            odgovor = "OK 10;";
        } else {
            odgovor = "ERR 10;";
        }
    }

    private void prethodiAutenticirajKorisnika(String[] komandaSplitana, boolean posluzitelj) {
        List<Korisnik> sviKorisnici = dohvatiKorisnike();
        if (sviKorisnici != null) {
            if (autenticirajKorisnika(sviKorisnici)) {
                odgovor = "OK 10;";
                if (komandaSplitana.length == 3) {
                    if (posluzitelj) {
                        obradiOstatakKomandeZaPosluzitelja(komandaSplitana);
                    } else {
                        obradiOstatakKomandeZaGrupe(komandaSplitana);
                    }
                }
            } else {
                odgovor = "ERR 11;";
            }
        } else {
            odgovor = "ERR 11;";
        }
    }

    private void posaljiOdgovor() {
        if (!odgovor.equals("")) {
            try {
                OutputStream os = socket.getOutputStream();
                os.write(odgovor.getBytes(StandardCharsets.UTF_8));
                os.flush();
                socket.shutdownOutput();
                os.close();
            } catch (Exception ex) {
                System.err.println("Greska kod slanja odgovora! " + ex.getLocalizedMessage());
            }
        }
    }

    private boolean provjeriSintaksuZaPosluzitelja() {
        String sintaksa = "^KORISNIK ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\.]+); "
                + "LOZINKA ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\!\\#\\$\\%\\&\\+\\?\\.]+);"
                + "(?: (?:DODAJ ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\.]+) ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\.]+);"
                + "|AZURIRAJ ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\.]+) ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\.]+);"
                + "|PAUZA;|KRENI;|PASIVNO;|AKTIVNO;|STANI;|STANJE;|LISTAJ;))?$";

        Pattern pattern = Pattern.compile(sintaksa);
        m = pattern.matcher(komanda);
        boolean status = m.matches();
        return status;
    }

    private boolean provjeriSintaksuGrupe() {
        String sintaksa = "^KORISNIK ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\.]+); "
                + "LOZINKA ([A-ZŠĐŽĆČa-zšđžćč0-9\\-\\_\\!\\#\\$\\%\\&\\+\\?\\.]+); "
                + "GRUPA (DODAJ;|PREKID;|KRENI;|PAUZA;|STANJE;)$";
        Pattern pattern = Pattern.compile(sintaksa);
        m = pattern.matcher(komanda);
        boolean status = m.matches();
        return status;
    }

    private boolean dodajKorisnika() {
        String upit = "INSERT INTO korisnici "
                + "(korisnickoIme, lozinka, ime, prezime) "
                + "VALUES (?, ?, ?, ?)";

        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            stat.setString(1, korisnickoIme);
            stat.setString(2, lozinka);
            stat.setString(3, ime);
            stat.setString(4, prezime);
            stat.execute();
            stat.close();
            con.close();
            System.out.println("Upisan novi korisnik u bazu!");
            return true;
        } catch (SQLException ex) {
            System.err.println("Greska kod upisa novog korisnika u bazu! " + ex.getLocalizedMessage());
            return false;
        }
    }

    private List<Korisnik> dohvatiKorisnike() {
        String upit = "SELECT * FROM korisnici";
        List<Korisnik> sviKorisnici = new ArrayList<>();
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                sviKorisnici.add(new Korisnik(rs.getInt("id"), rs.getString("korisnickoIme"), rs.getString("lozinka"),
                        rs.getString("ime"), rs.getString("prezime")));
            }
            rs.close();
            stat.close();
            con.close();
            return sviKorisnici;
        } catch (SQLException ex) {
            System.err.println("Greska pri dohvacanju korisnika: " + ex.getLocalizedMessage());
            return null;
        }
    }

    private List<KorisnikListaj> dohvatiKorisnikeListaj() {
        String upit = "SELECT id, korisnickoIme, prezime, ime FROM korisnici";
        List<KorisnikListaj> sviKorisnici = new ArrayList<>();
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                sviKorisnici.add(new KorisnikListaj(rs.getInt("id"), rs.getString("korisnickoIme"), rs.getString("prezime"),
                        rs.getString("ime")));
            }
            rs.close();
            stat.close();
            con.close();
            return sviKorisnici;
        } catch (SQLException ex) {
            System.err.println("Greska pri dohvacanju korisnika: " + ex.getLocalizedMessage());
            return null;
        }
    }

    private boolean autenticirajKorisnika(List<Korisnik> sviKorisnici) {
        for (Korisnik k : sviKorisnici) {
            if (k.getKorisnickoIme().equals(korisnickoIme)) {
                if (k.getLozinka().equals(lozinka)) {
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized void obradiOstatakKomandeZaPosluzitelja(String[] komandaSplitana) {
        String kom = komandaSplitana[2].trim();
        if (kom.equals("PAUZA")) {
            pauziraj();
        } else if (kom.equals("KRENI")) {
            pokreni();
        } else if (kom.equals("PASIVNO")) {
            pasiviraj();
        } else if (kom.equals("AKTIVNO")) {
            aktiviraj();
        } else if (kom.equals("STANI")) {
            zaustavi();
        } else if (kom.equals("STANJE")) {
            vratiStanje();
        } else if (kom.equals("LISTAJ")) {
            listaj();
        } else if (kom.contains("AZURIRAJ")) {
            prethodiAzuirajKorisnika();
        }
    }

    private void prethodiAzuirajKorisnika() {
        prezime = m.group(5);
        ime = m.group(6);
        if (azurirajKorisnika()) {
            odgovor = "OK 10;";
        } else {
            odgovor = "ERR 10;";
        }
    }

    private boolean azurirajKorisnika() {
        String upit = "UPDATE korisnici SET ime = '" + ime + "', prezime = '" + prezime
                + "' WHERE korisnickoIme = '" + korisnickoIme + "'";
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            stat.execute();
            stat.close();
            con.close();
            System.out.println("Ažuriran korisnik u bazi!");
            return true;
        } catch (SQLException ex) {
            System.err.println("Greska kod upisa ažuriranja korisnika u bazi! " + ex.getLocalizedMessage());
            return false;
        }
    }

    private synchronized void pauziraj() {
        if (ServerSustava.pauziran) {
            odgovor = "ERR 12;";
        } else {
            ServerSustava.pauziran = true;
            odgovor = "OK 10;";
        }
    }

    private synchronized void pokreni() {
        if (!ServerSustava.pauziran) {
            odgovor = "ERR 13;";
        } else {
            ServerSustava.pauziran = false;
            odgovor = "OK 10;";
        }
    }

    private synchronized void pasiviraj() {
        if (PreuzimanjeMeteoroloskihPodataka.aktivan) {
            PreuzimanjeMeteoroloskihPodataka.aktivan = false;
            odgovor = "OK 10;";
        } else {
            odgovor = "ERR 14;";
        }
    }

    private synchronized void aktiviraj() {
        if (!PreuzimanjeMeteoroloskihPodataka.aktivan) {
            PreuzimanjeMeteoroloskihPodataka.aktivan = true;
            odgovor = "OK 10;";
        } else {
            odgovor = "ERR 15;";
        }
    }

    private synchronized void zaustavi() {
        if (ServerSustava.radi && PreuzimanjeMeteoroloskihPodataka.radi) {
            ServerSustava.radi = false;
            PreuzimanjeMeteoroloskihPodataka.radi = false;
            odgovor = "OK 10;";
        } else {
            odgovor = "ERR 16;";
        }
    }

    private synchronized void vratiStanje() {
        if (!ServerSustava.pauziran && PreuzimanjeMeteoroloskihPodataka.aktivan) {
            odgovor = "OK 11;";
        } else if (!ServerSustava.pauziran && !PreuzimanjeMeteoroloskihPodataka.aktivan) {
            odgovor = "OK 12;";
        } else if (ServerSustava.pauziran && PreuzimanjeMeteoroloskihPodataka.aktivan) {
            odgovor = "OK 13;";
        } else if (ServerSustava.pauziran && !PreuzimanjeMeteoroloskihPodataka.aktivan) {
            odgovor = "OK 14;";
        }
    }

    private synchronized void listaj() {
        List<KorisnikListaj> sviKorisniciListaj = dohvatiKorisnikeListaj();
        if (sviKorisniciListaj != null && sviKorisniciListaj.size() > 0) {
            String json = new Gson().toJson(sviKorisniciListaj);
            odgovor = "OK 10; " + json;
        } else {
            odgovor = "ERR 17;";
        }
    }

    private void obradiOstatakKomandeZaGrupe(String[] komandaSplitana) {
        String kom = komandaSplitana[2].trim();
        if (kom.equals("GRUPA DODAJ")) {
            registrirajGrupu();
        } else if (kom.equals("GRUPA PREKID")) {
            deregistrirajGrupu();
        } else if (kom.equals("GRUPA KRENI")) {
            aktivirajGrupu();
        } else if (kom.equals("GRUPA PAUZA")) {
            blokirajGrupu();
        } else if (kom.equals("GRUPA STANJE")) {
            dohvatiStatusGrupe();
        }
    }

    private void registrirajGrupu() {
        StatusKorisnika status = dohvatiStatusGrupe();
        switch (status) {
            case AKTIVAN:
                odgovor = "ERR 20;";
                break;
            case REGISTRIRAN:
                odgovor = "ERR 20;";
                break;
            case DEREGISTRIRAN:
                boolean registrirana = registrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (registrirana) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 20;";
                }
                break;
            case BLOKIRAN:
                odgovor = "ERR 20;";
                break;
            case NEPOSTOJI:
                boolean registrirana2 = registrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (registrirana2) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 20;";
                }
                break;
            case PASIVAN:
                boolean registrirana3 = registrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (registrirana3) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 20;";
                }
                break;
            case NEAKTIVAN:
                boolean registrirana4 = registrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (registrirana4) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 20;";
                }
                break;
        }
    }

    private void deregistrirajGrupu() {
        StatusKorisnika status = dohvatiStatusGrupe();
        switch (status) {
            case AKTIVAN:
                boolean deregistrirana3 = deregistrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (deregistrirana3) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 21;";
                }
                break;
            case REGISTRIRAN:
                boolean deregistrirana2 = deregistrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (deregistrirana2) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 21;";
                }
                break;
            case DEREGISTRIRAN:
                odgovor = "ERR 21;";
                break;
            case BLOKIRAN:
                boolean deregistrirana = deregistrirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (deregistrirana) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 21;";
                }
                break;
            case NEPOSTOJI:
                odgovor = "ERR 21;";
                break;
            case PASIVAN:
                odgovor = "ERR 21;";
                break;
            case NEAKTIVAN:
                odgovor = "ERR 21;";
                break;
        }
    }

    private void aktivirajGrupu() {
        StatusKorisnika status = dohvatiStatusGrupe();
        switch (status) {
            case AKTIVAN:
                odgovor = "ERR 22;";
                break;
            case REGISTRIRAN:
                boolean aktivirana2 = aktivirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (aktivirana2) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 22;";
                }
                break;
            case DEREGISTRIRAN:
                odgovor = "ERR 22;";
                break;
            case BLOKIRAN:
                boolean aktivirana = aktivirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (aktivirana) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 22;";
                }
                break;
            case NEPOSTOJI:
                odgovor = "ERR 21;";
                break;
            case PASIVAN:
                odgovor = "ERR 22;";
                break;
            case NEAKTIVAN:
                odgovor = "ERR 22;";
                break;
        }

    }

    private void blokirajGrupu() {
        StatusKorisnika status = dohvatiStatusGrupe();
        switch (status) {
            case AKTIVAN:
                boolean blokirana = blokirajGrupu(korisnickoImeParkiranje, lozinkaParkiranje);
                if (blokirana) {
                    odgovor = "OK 20;";
                } else {
                    odgovor = "ERR 23;";
                }
                break;
            case REGISTRIRAN:
                odgovor = "ERR 23;";
                break;
            case DEREGISTRIRAN:
                odgovor = "ERR 23;";
                break;
            case BLOKIRAN:
                odgovor = "ERR 23;";
                break;
            case NEPOSTOJI:
                odgovor = "ERR 21;";
                break;
            case PASIVAN:
                odgovor = "ERR 23;";
                break;
            case NEAKTIVAN:
                odgovor = "ERR 23;";
                break;
        }
    }

    private StatusKorisnika dohvatiStatusGrupe() {
        StatusKorisnika status = dajStatusGrupe(korisnickoImeParkiranje, lozinkaParkiranje);
        switch (status) {
            case AKTIVAN:
                odgovor = "OK 21;";//
                break;
            case REGISTRIRAN:
                odgovor = "OK 23;";
                break;
            case DEREGISTRIRAN:
                odgovor = "ERR 22;";
                break;
            case BLOKIRAN:
                odgovor = "OK 22;";//
                break;
            case NEPOSTOJI:
                odgovor = "ERR 21;";//
                break;
            case PASIVAN:
                odgovor = "ERR 23;";
                break;
            case NEAKTIVAN:
                odgovor = "ERR 24;";
                break;
        }
        return status;
    }

    private static Boolean registrirajGrupu(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.registrirajGrupu(korisnickoIme, korisnickaLozinka);
    }

    private static Boolean deregistrirajGrupu(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.deregistrirajGrupu(korisnickoIme, korisnickaLozinka);
    }

    private static Boolean aktivirajGrupu(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.aktivirajGrupu(korisnickoIme, korisnickaLozinka);
    }

    private static Boolean blokirajGrupu(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.blokirajGrupu(korisnickoIme, korisnickaLozinka);
    }

    private static StatusKorisnika dajStatusGrupe(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.dajStatusGrupe(korisnickoIme, korisnickaLozinka);
    }

}
