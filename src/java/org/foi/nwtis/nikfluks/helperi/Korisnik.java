package org.foi.nwtis.nikfluks.helperi;

/**
 *
 * @author Nikola
 */
public class Korisnik {

    private int id;
    private String korisnickoIme;
    private String lozinka;
    private String ime;
    private String prezime;

    public Korisnik() {
    }

    public Korisnik(int id, String ime, String prezime) {
        this.id = id;
        this.ime = ime;
        this.prezime = prezime;
    }

    public Korisnik(String korisnickoIme, String prezime, String ime) {
        this.korisnickoIme = korisnickoIme;
        this.prezime = prezime;
        this.ime = ime;
    }

    public Korisnik(int id, String korisnickoIme, String lozinka, String ime, String prezime) {
        this.id = id;
        this.korisnickoIme = korisnickoIme;
        this.lozinka = lozinka;
        this.ime = ime;
        this.prezime = prezime;
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

    public String getLozinka() {
        return lozinka;
    }

    public void setLozinka(String lozinka) {
        this.lozinka = lozinka;
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

}
