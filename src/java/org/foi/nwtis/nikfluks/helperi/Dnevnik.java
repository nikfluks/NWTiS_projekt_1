package org.foi.nwtis.nikfluks.helperi;

import java.util.Date;

/**
 *
 * @author Nikola
 */
public class Dnevnik {
    private int id;
    private String korisnickoIme;
    private String sadrzaj;
    private String vrsta;
    private Date vrijemePrijema;
    private String url;
    private String ipAdresa;
    private int trajanjeZahtjeva;

    public Dnevnik() {
    }

    public Dnevnik(int id, String korisnickoIme, String sadrzaj, String vrsta, Date vrijemePrijema, String url, String ipAdresa, int trajanjeZahtjeva) {
        this.id = id;
        this.korisnickoIme = korisnickoIme;
        this.sadrzaj = sadrzaj;
        this.vrsta = vrsta;
        this.vrijemePrijema = vrijemePrijema;
        this.url = url;
        this.ipAdresa = ipAdresa;
        this.trajanjeZahtjeva = trajanjeZahtjeva;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKorisnickoIme() {
        return korisnickoIme;
    }

    public void setKorisnickoIme(String korisnickoIme) {
        this.korisnickoIme = korisnickoIme;
    }

    public String getSadrzaj() {
        return sadrzaj;
    }

    public void setSadrzaj(String sadrzaj) {
        this.sadrzaj = sadrzaj;
    }

    public String getVrsta() {
        return vrsta;
    }

    public void setVrsta(String vrsta) {
        this.vrsta = vrsta;
    }

    public Date getVrijemePrijema() {
        return vrijemePrijema;
    }

    public void setVrijemePrijema(Date vrijemePrijema) {
        this.vrijemePrijema = vrijemePrijema;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIpAdresa() {
        return ipAdresa;
    }

    public void setIpAdresa(String ipAdresa) {
        this.ipAdresa = ipAdresa;
    }

    public int getTrajanjeZahtjeva() {
        return trajanjeZahtjeva;
    }

    public void setTrajanjeZahtjeva(int trajanjeZahtjeva) {
        this.trajanjeZahtjeva = trajanjeZahtjeva;
    }


}
