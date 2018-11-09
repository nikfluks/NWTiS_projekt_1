package org.foi.nwtis.nikfluks.helperi;

/**
 *
 * @author Nikola
 */
public class KorisnikListaj {

    private int id;
    private String ki;
    private String prezime;
    private String ime;

    public KorisnikListaj() {
    }

    public KorisnikListaj(int id, String korisnickoIme, String prezime, String ime) {
        this.id = id;
        this.ki = korisnickoIme;
        this.prezime = prezime;
        this.ime = ime;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getKi() {
        return ki;
    }

    public void setKi(String ki) {
        this.ki = ki;
    }

}
