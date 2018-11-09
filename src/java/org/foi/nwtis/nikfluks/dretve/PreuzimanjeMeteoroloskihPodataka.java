package org.foi.nwtis.nikfluks.dretve;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.foi.nwtis.nikfluks.helperi.Lokacija;
import org.foi.nwtis.nikfluks.helperi.MeteoPodaci;
import org.foi.nwtis.nikfluks.helperi.OWMKlijent;
import org.foi.nwtis.nikfluks.helperi.Parkiraliste;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;

/**
 *
 * @author Nikola
 */
public class PreuzimanjeMeteoroloskihPodataka extends Thread {

    public static boolean aktivan = true;
    public static boolean radi = true;
    String urlBaza;
    String korImeBaza;
    String lozinkaBaza;
    String uprProgram;
    Connection con;
    int intervalDretve;
    String apikey;
    OWMKlijent owmk;

    public PreuzimanjeMeteoroloskihPodataka() {
    }

    @Override
    public void interrupt() {
        radi = false;
        System.out.println("Preuzimanje meteoroloskih podataka se gasi...");
        super.interrupt();
    }

    @Override
    public void run() {
        try {
            while (radi) {
                if (aktivan) {
                    System.out.println("dretva meteo podataka radi");
                    con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
                    List<Parkiraliste> svaParkiralista = dohvatiParkiralista();
                    if (svaParkiralista != null) {
                        for (Parkiraliste p : svaParkiralista) {
                            Lokacija lok = p.getGeolokacija();
                            //System.out.println("Obrađujem parkiralište: " + p.getNaziv());
                            MeteoPodaci mp = owmk.getRealTimeWeather(lok.getLatitude(), lok.getLongitude());
                            if (mp != null) {
                                upisiMeteoPodatke(mp, p);
                            } else {
                                System.err.println("Greska kod dohvacanja meteo podataka!");
                            }
                        }
                    }
                    con.close();
                }
                sleep(intervalDretve);
            }
        } catch (InterruptedException | SQLException ex) {
            System.err.println("Greška kod preuzimanja meteo pod: " + ex.getLocalizedMessage());
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
            uprProgram = bpk.getDriverDatabase();
            intervalDretve = Integer.parseInt(bpk.getIntervalDretveMeteo_()) * 1000;
            apikey = bpk.getApiKey();
            owmk = new OWMKlijent(apikey);
            Class.forName(uprProgram);

            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private List<Parkiraliste> dohvatiParkiralista() {
        List<Parkiraliste> svaParkiralista = new ArrayList<>();
        String upit = "SELECT * FROM PARKIRALISTA";

        try {
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                Lokacija lokacija = new Lokacija(rs.getString("latitude"), rs.getString("longitude"));
                svaParkiralista.add(new Parkiraliste(rs.getInt("id"), rs.getString("naziv"), rs.getString("adresa"), lokacija));
            }
            rs.close();
            stat.close();
            return svaParkiralista;
        } catch (SQLException ex) {
            System.err.println("Greska pri dohvacanju parkiralista: " + ex.getLocalizedMessage());
            return null;
        }
    }

    private synchronized boolean upisiMeteoPodatke(MeteoPodaci mp, Parkiraliste p) {
        String upit = "INSERT INTO meteopodaci "
                + "(parkiralista_id, latitude, longitude, vrijeme, vrijemeopis, temp, tempmin, tempmax, vlaga, tlak, vjetar, vjetarsmjer) "
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement stat = con.prepareStatement(upit);
            stat.setInt(1, p.getId());
            stat.setString(2, p.getGeolokacija().getLatitude());
            stat.setString(3, p.getGeolokacija().getLongitude());
            stat.setString(4, mp.getCloudsName());
            stat.setString(5, mp.getWeatherValue());
            stat.setFloat(6, mp.getTemperatureValue());
            stat.setFloat(7, mp.getTemperatureMin());
            stat.setFloat(8, mp.getTemperatureMax());
            stat.setFloat(9, mp.getHumidityValue());
            stat.setFloat(10, mp.getPressureValue());
            stat.setFloat(11, mp.getWindSpeedValue());
            stat.setFloat(12, mp.getWindDirectionValue());
            stat.execute();
            stat.close();
            //System.out.println("Upisani meteo podaci koji su zadnji put osvjezeni: " + mp.getLastUpdate());
            return true;
        } catch (SQLException ex) {
            System.err.println("Greska kod upisa meteo podataka! " + ex.getLocalizedMessage());
            return false;
        }
    }

}
