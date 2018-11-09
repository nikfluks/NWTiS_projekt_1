package org.foi.nwtis.nikfluks.zrna;

import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.faces.bean.ManagedBean;
import org.foi.nwtis.nikfluks.helperi.Dnevnik;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;

/**
 *
 * @author Nikola
 */
@ManagedBean(name = "pregledDnevnika")
@SessionScoped
public class PregledDnevnika implements Serializable {

    private List<Dnevnik> listaZapisaIzDnevnika = new ArrayList<>();
    private String vrstaZapisa;
    private String odDatum;
    private String doDatum;
    String urlBaza;
    String korImeBaza;
    String lozinkaBaza;
    String uprProgram;
    static int limit;
    static int offset = 0;
    static int ukupno;
    private static boolean prikaziPrethodnu = false;
    private static boolean prikaziSljedecu = true;
    private Date odDat;
    private Date doDat;
    private Timestamp odDatTimestamp;
    private Timestamp doDatTimestamp;
    private static boolean prviPut = true;
    private static StringBuilder upitFiltrirani;

    public PregledDnevnika() {
        dohvatiPodatkeIzKonfiguracije();
        if (prviPut) {
            filtrirajPodatke();
            prviPut = false;
        }
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
            limit = Integer.parseInt(bpk.getStranicenjeDnevnika_());
            Class.forName(uprProgram);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private void provjeriDatum() throws ParseException {
        SimpleDateFormat sdfDat = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat sdfDatVrij = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdfDat.setLenient(false);
        sdfDatVrij.setLenient(false);

        if (odDatum != null && !odDatum.equals("")) {
            try {
                odDat = sdfDatVrij.parse(odDatum);
            } catch (ParseException e) {
                odDat = sdfDat.parse(odDatum);
            }
        } else {
            odDat = new Date(0);
        }
        if (doDatum != null && !doDatum.equals("")) {
            try {
                doDat = sdfDatVrij.parse(doDatum);
            } catch (ParseException e) {
                doDat = sdfDat.parse(doDatum);
            }
        } else {
            doDat = new Date();
        }
        odDatTimestamp = new Timestamp(odDat.getTime());
        doDatTimestamp = new Timestamp(doDat.getTime());
    }

    public String filtrirajPodatke() {
        try {
            provjeriDatum();
        } catch (ParseException ex) {
            System.out.println("Pogrešan datum!");
            return "";
        }
        upitFiltrirani = new StringBuilder();
        upitFiltrirani.append("SELECT * FROM DNEVNIK");
        upitFiltrirani.append(" WHERE vrijemePrijema BETWEEN ").append("'").append(odDatTimestamp).append("'");
        upitFiltrirani.append(" AND ").append("'").append(doDatTimestamp).append("'");
        if (vrstaZapisa != null && !vrstaZapisa.equals("")) {
            upitFiltrirani.append(" AND vrsta = '").append(vrstaZapisa).append("'");
        }
        dohvatiBrojZapisaIzDnevnika(upitFiltrirani.toString());
        offset = 0;
        upitFiltrirani.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
        dohvatiPodatkeIzDnevnika(upitFiltrirani.toString());
        System.out.println("upit: " + upitFiltrirani);
        prikaziPrethodnu();
        prikaziSljedecu();
        return "";
    }

    private void dohvatiPodatkeIzDnevnika(String upit) {
        listaZapisaIzDnevnika.clear();
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                Dnevnik dnevnik = new Dnevnik(rs.getInt("id"), rs.getString("korisnickoIme"), rs.getString("sadrzaj"),
                        rs.getString("vrsta"), rs.getTimestamp("vrijemePrijema"), rs.getString("url"),
                        rs.getString("ipAdresa"), rs.getInt("trajanjeZahtjeva"));
                listaZapisaIzDnevnika.add(dnevnik);
            }
            rs.close();
            stat.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println("Greška kod dohvaćanja zapisa iz dnevnika!" + ex.getLocalizedMessage());
        }
    }

    private void dohvatiBrojZapisaIzDnevnika(String upitCount) {
        try {
            String[] upitCountSplitani = upitCount.split(" ");
            String upit = "";
            for (int i = 0; i < upitCountSplitani.length; i++) {
                if (upitCountSplitani[i].equals("*")) {
                    upit += "COUNT(*) ";
                } else {
                    upit += upitCountSplitani[i] + " ";
                }
            }

            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                ukupno = rs.getInt(1);
            }
            System.out.println("ukupno: " + ukupno);
            System.out.println("upitCount: " + upit);
            rs.close();
            stat.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println("Greška kod dohvaćanja ukupnog broja zapisa iz dnevnika!" + ex.getLocalizedMessage());
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
        String[] upitFilterSplitani = upitFiltrirani.toString().split(" ");
        String upit = "";
        for (int i = 0; i < upitFilterSplitani.length; i++) {
            if (upitFilterSplitani[i].equals("OFFSET")) {
                upit += "OFFSET " + offset;
                break;
            } else {
                upit += upitFilterSplitani[i] + " ";
            }
        }
        System.out.println("upitprethodna: " + upit);
        dohvatiPodatkeIzDnevnika(upit);
        prikaziPrethodnu();
        prikaziSljedecu = true;
        return "";
    }

    public String sljedeca() {
        offset += limit;
        String[] upitFilterSplitani = upitFiltrirani.toString().split(" ");
        String upit = "";
        for (int i = 0; i < upitFilterSplitani.length; i++) {
            if (upitFilterSplitani[i].equals("OFFSET")) {
                upit += "OFFSET " + offset;
                break;
            } else {
                upit += upitFilterSplitani[i] + " ";
            }
        }
        System.out.println("upitsljedeca: " + upit);
        dohvatiPodatkeIzDnevnika(upit);
        prikaziSljedecu();
        prikaziPrethodnu = true;
        return "";
    }

    public List<Dnevnik> getListaZapisaIzDnevnika() {

        return listaZapisaIzDnevnika;
    }

    public void setListaZapisaIzDnevnika(List<Dnevnik> listaZapisaIzDnevnika) {
        this.listaZapisaIzDnevnika = listaZapisaIzDnevnika;
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

    public String getVrstaZapisa() {
        return vrstaZapisa;
    }

    public void setVrstaZapisa(String vrstaZapisa) {
        this.vrstaZapisa = vrstaZapisa;
    }

    public String getOdDatum() {
        return odDatum;
    }

    public void setOdDatum(String odDatum) {
        this.odDatum = odDatum;
    }

    public String getDoDatum() {
        return doDatum;
    }

    public void setDoDatum(String doDatum) {
        this.doDatum = doDatum;
    }

}
