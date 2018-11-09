package org.foi.nwtis.nikfluks.soap.serveri;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebService;
import org.foi.nwtis.nikfluks.helperi.Korisnik;
import org.foi.nwtis.nikfluks.helperi.Lokacija;
import org.foi.nwtis.nikfluks.helperi.MeteoPodaci;
import org.foi.nwtis.nikfluks.helperi.OWMKlijent;
import org.foi.nwtis.nikfluks.helperi.Parkiraliste;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;

/**
 * SOAP web service
 *
 * @author Nikola
 */
@WebService(serviceName = "MeteoSoap")
public class MeteoSoap {

    String urlBaza;
    String korImeBaza;
    String lozinkaBaza;
    String uprProgram;
    String apikey;
    OWMKlijent owmk;
    long pocetakMillis;

    private boolean dohvatiPodatkeIzKonfiguracije() {
        try {
            BP_Konfiguracija bpk = (BP_Konfiguracija) SlusacAplikacije.getServletContext().getAttribute("BP_Konfig");
            urlBaza = bpk.getServerDatabase() + bpk.getUserDatabaseMySQL();
            korImeBaza = bpk.getUserUsername();
            lozinkaBaza = bpk.getUserPassword();
            uprProgram = bpk.getDriverDatabase();
            apikey = bpk.getApiKey();
            owmk = new OWMKlijent(apikey);
            Class.forName(uprProgram);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private boolean upisiUDnevnik(String korisnickoIme, String sadrzaj) {
        long krajMillis = System.currentTimeMillis();
        long trajanjeMillis = krajMillis - pocetakMillis;

        String upit = "INSERT INTO dnevnik "
                + "(korisnickoIme, sadrzaj, vrsta, url, ipAdresa, trajanjeZahtjeva) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            stat.setString(1, korisnickoIme);
            stat.setString(2, sadrzaj);
            stat.setString(3, "SOAP");
            stat.setString(4, "http://localhost:8084/nikfluks_aplikacija_1/");
            stat.setString(5, "127.0.0.1");
            stat.setInt(6, (int) trajanjeMillis);
            stat.execute();
            stat.close();
            con.close();
            System.out.println("Upisano u dnevnik!" + "\n");
            return true;
        } catch (SQLException ex) {
            System.err.println("Greska kod upisa u dnevnik! " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista
     * @return zadnji meteo podaci iz baze za odabrano parkiraliste
     */
    @WebMethod(operationName = "dajZadnjeMeteoPodatke")
    public MeteoPodaci dajZadnjeMeteoPodatke(String korisnickoIme, String lozinka, int id) {
        pocetakMillis = System.currentTimeMillis();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            return null;
        }
        String upit = "SELECT * FROM meteopodaci WHERE parkiralista_id=" + id + " ORDER BY PREUZETO DESC LIMIT 1";

        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            MeteoPodaci mp = null;
            if (rs.next()) {
                mp = postaviMeteoPodatke(rs);
            }
            rs.close();
            stat.close();
            con.close();
            upisiUDnevnik(korisnickoIme, "dajZadnjeMeteoPodatke");
            return mp;
        } catch (SQLException ex) {
            System.err.println("Greška kod dohvaćanja zadnjih meteo podataka iz baze: " + ex.getLocalizedMessage());
            upisiUDnevnik(korisnickoIme, "dajZadnjeMeteoPodatke - Greška");
            return null;
        }
    }

    /**
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista
     * @param n broj koliko meteo podataka se zeli dobiti
     * @return zadnji n meteo podataka iz baze za odabrano parkiraliste
     */
    @WebMethod(operationName = "dajPosljednjihN")
    public List<MeteoPodaci> dajPosljednjihN(String korisnickoIme, String lozinka, int id, int n) {
        pocetakMillis = System.currentTimeMillis();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            return null;
        }
        List<MeteoPodaci> nMeteoPodataka = new ArrayList<>();
        String upit = "SELECT * FROM meteopodaci WHERE parkiralista_id=" + id + " ORDER BY PREUZETO DESC LIMIT " + n;

        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                MeteoPodaci mp = postaviMeteoPodatke(rs);
                nMeteoPodataka.add(mp);
            }
            rs.close();
            stat.close();
            con.close();
            upisiUDnevnik(korisnickoIme, "dajPosljednjihN");
            return nMeteoPodataka;
        } catch (SQLException ex) {
            System.err.println("Greška kod dohvaćanja zadnjih meteo podataka iz baze: " + ex.getLocalizedMessage());
            upisiUDnevnik(korisnickoIme, "dajPosljednjihN - Greška");
            return null;
        }
    }

    /**
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista
     * @param intervalOd od datuma
     * @param intervalDo do datuma
     * @return meteo podaci za odabrano parkiraliste u odabranom intervalu
     */
    @WebMethod(operationName = "dajSveMeteoPodatkeUIntervalu")
    public List<MeteoPodaci> dajSveMeteoPodatkeUIntervalu(String korisnickoIme, String lozinka, int id, long intervalOd, long intervalDo) {
        pocetakMillis = System.currentTimeMillis();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            return null;
        }
        Timestamp tsOd = new Timestamp(intervalOd);
        Timestamp tsDo = new Timestamp(intervalDo);
        List<MeteoPodaci> sviMeteoPodaci = dohvatiSveMeteoPodatkeUIntervalu(id, tsOd, tsDo);
        if (sviMeteoPodaci != null) {
            upisiUDnevnik(korisnickoIme, "dajSveMeteoPodatkeUIntervalu");
        } else {
            upisiUDnevnik(korisnickoIme, "dajSveMeteoPodatkeUIntervalu - Greška");
        }
        return sviMeteoPodaci;
    }

    /**
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista
     * @return
     */
    @WebMethod(operationName = "dajVazeceMeteoPodatke")
    public MeteoPodaci dajVazeceMeteoPodatke(String korisnickoIme, String lozinka, int id) {
        pocetakMillis = System.currentTimeMillis();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            return null;
        }
        Parkiraliste p = dohvatiUnesenoParkiraliste(id);
        if (p == null) {
            upisiUDnevnik(korisnickoIme, "dajVazeceMeteoPodatke - Greška");
            return null;
        } else {
            Lokacija lok = p.getGeolokacija();
            MeteoPodaci mp = owmk.getRealTimeWeather(lok.getLatitude(), lok.getLongitude());
            upisiUDnevnik(korisnickoIme, "dajVazeceMeteoPodatke");
            return mp;
        }
    }

    private Parkiraliste dohvatiUnesenoParkiraliste(int id) {
        String upit = "SELECT * FROM PARKIRALISTA WHERE id=" + id;
        Parkiraliste p = null;

        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();

            if (rs.next()) {
                Lokacija lokacija = new Lokacija(rs.getString("latitude"), rs.getString("longitude"));
                p = new Parkiraliste(rs.getInt("id"), rs.getString("naziv"), rs.getString("adresa"), lokacija);
            }
            rs.close();
            stat.close();
            con.close();
            return p;
        } catch (SQLException ex) {
            System.err.println("Greška kod dohvaćanja parkirališta po IDu!\n" + ex.getLocalizedMessage());
            return null;
        }
    }

    private List<MeteoPodaci> dohvatiSveMeteoPodatkeUIntervalu(int id, Timestamp tsOd, Timestamp tsDo) {
        String upit = "SELECT * FROM meteopodaci WHERE parkiralista_id=" + id + " AND (preuzeto BETWEEN '" + tsOd + "' AND '" + tsDo + "')";
        List<MeteoPodaci> sviMeteoPodaci = new ArrayList<>();

        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();

            while (rs.next()) {
                MeteoPodaci mp = postaviMeteoPodatke(rs);
                sviMeteoPodaci.add(mp);
            }
            rs.close();
            stat.close();
            con.close();
            return sviMeteoPodaci;
        } catch (SQLException ex) {
            System.err.println("Greška kod dohvaćanja svih meteo podataka u intervalu!\n" + ex.getLocalizedMessage());
            return null;
        }
    }

    private MeteoPodaci postaviMeteoPodatke(ResultSet rs) throws SQLException {
        MeteoPodaci mp = new MeteoPodaci();
        mp.setCloudsName(rs.getString("vrijeme"));
        mp.setWeatherValue(rs.getString("vrijemeopis"));
        mp.setTemperatureValue(rs.getFloat("temp"));
        mp.setTemperatureMin(rs.getFloat("tempmin"));
        mp.setTemperatureMax(rs.getFloat("tempmax"));
        mp.setHumidityValue(rs.getFloat("vlaga"));
        mp.setPressureValue(rs.getFloat("tlak"));
        mp.setWindSpeedValue(rs.getFloat("vjetar"));
        mp.setWindDirectionValue(rs.getFloat("vjetarsmjer"));
        mp.setLastUpdate(rs.getTimestamp("preuzeto"));
        return mp;
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

    private boolean autenticitajKorisnika(String korisnickoIme, String lozinka) {
        List<Korisnik> sviKorisnici = dohvatiKorisnike();
        for (Korisnik k : sviKorisnici) {
            if (k.getKorisnickoIme().equals(korisnickoIme)) {
                if (k.getLozinka().equals(lozinka)) {
                    return true;
                }
            }
        }
        return false;
    }
}
