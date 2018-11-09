package org.foi.nwtis.nikfluks.zrna;

import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import org.foi.nwtis.nikfluks.helperi.Korisnik;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;

/**
 *
 * @author Nikola
 */
@ManagedBean(name = "pregledKorisnika")
@SessionScoped
public class PregledKorisnika implements Serializable {

    private List<Korisnik> listaSvihKorisnika = new ArrayList<>();
    String urlBaza;
    String korImeBaza;
    String lozinkaBaza;
    String uprProgram;
    static int limit;
    static int offset = 0;
    int ukupno;
    boolean prikaziPrethodnu = false;
    boolean prikaziSljedecu = true;

    public PregledKorisnika() {
        dohvatiPodatkeIzKonfiguracije();
        dohvatiBrojKorisnikaIzBaze();
        prikaziPrethodnu();
        prikaziSljedecu();
    }

    private boolean dohvatiPodatkeIzKonfiguracije() {
        try {
            BP_Konfiguracija bpk = (BP_Konfiguracija) SlusacAplikacije.getServletContext().getAttribute("BP_Konfig");
            urlBaza = bpk.getServerDatabase() + bpk.getUserDatabaseMySQL();
            korImeBaza = bpk.getUserUsername();
            lozinkaBaza = bpk.getUserPassword();
            uprProgram = bpk.getDriverDatabase();
            limit = Integer.parseInt(bpk.getStranicenjeKorisnika_());
            Class.forName(uprProgram);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private List<Korisnik> dohvatiKorisnikeIzBaze() {
        List<Korisnik> listaKorisnika = new ArrayList<>();
        String upit = "SELECT * FROM korisnici LIMIT " + limit + " OFFSET " + offset;
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                Korisnik korisnik = new Korisnik(rs.getInt("id"), rs.getString("korisnickoIme"), rs.getString("lozinka"),
                        rs.getString("ime"), rs.getString("prezime"));
                listaKorisnika.add(korisnik);
            }
            rs.close();
            stat.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println("Greška kod dohvaćanja korisnika!");
            return null;
        }
        return listaKorisnika;
    }

    private void dohvatiBrojKorisnikaIzBaze() {
        String upit = "SELECT COUNT(*) AS ukupno FROM korisnici";
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                ukupno = rs.getInt("ukupno");
            }
            rs.close();
            stat.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println("Greška kod dohvaćanja ukupnog broja korisnika!" + ex.getLocalizedMessage());
        }
    }

    private void prikaziPrethodnu() {
        if (offset <= 0) {
            offset = 0;
            prikaziPrethodnu = false;
        } else {
            prikaziPrethodnu = true;
        }
    }

    private void prikaziSljedecu() {
        if (offset + limit >= ukupno) {
            prikaziSljedecu = false;
        } else {
            prikaziSljedecu = true;
        }
    }

    public String prethodna() {
        offset -= limit;
        prikaziPrethodnu();
        prikaziSljedecu = true;
        return "";
    }

    public String sljedeca() {
        offset += limit;
        prikaziSljedecu();
        prikaziPrethodnu = true;
        return "";
    }

    public List<Korisnik> getListaSvihKorisnika() {
        listaSvihKorisnika = dohvatiKorisnikeIzBaze();
        return listaSvihKorisnika;
    }

    public void setListaSvihKorisnika(List<Korisnik> listaSvihKorisnika) {
        this.listaSvihKorisnika = listaSvihKorisnika;
    }

    public boolean isPrikaziPrethodnu() {
        return prikaziPrethodnu;
    }

    public void setPrikaziPrethodnu(boolean prikaziPrethodnu) {
        this.prikaziPrethodnu = prikaziPrethodnu;
    }

    public boolean isPrikaziSljedecu() {
        return prikaziSljedecu;
    }

    public void setPrikaziSljedecu(boolean prikaziSljedecu) {
        this.prikaziSljedecu = prikaziSljedecu;
    }

}
