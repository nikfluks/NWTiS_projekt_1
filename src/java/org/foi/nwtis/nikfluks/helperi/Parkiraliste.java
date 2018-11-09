package org.foi.nwtis.nikfluks.helperi;

public class Parkiraliste {

    private int id;
    private String naziv;
    private String adresa;
    private Lokacija geolokacija;
    private int brojParkirnihMjesta;
    private int brojUlaznihMjesta;
    private int brojIzlaznihMjesta;
    private int korisnikId;

    public Parkiraliste() {
    }

    public Parkiraliste(int id, String naziv, String adresa, Lokacija geolokacija, int brojParkirnihMjesta, int brojUlaznihMjesta, int brojIzlaznihMjesta, int korisnikId) {
        this.id = id;
        this.naziv = naziv;
        this.adresa = adresa;
        this.geolokacija = geolokacija;
        this.brojParkirnihMjesta = brojParkirnihMjesta;
        this.brojUlaznihMjesta = brojUlaznihMjesta;
        this.brojIzlaznihMjesta = brojIzlaznihMjesta;
        this.korisnikId = korisnikId;
    }

    public Parkiraliste(int id, String naziv, String adresa, Lokacija geolokacija) {
        this.id = id;
        this.naziv = naziv;
        this.adresa = adresa;
        this.geolokacija = geolokacija;
    }

    public Lokacija getGeolokacija() {
        return geolokacija;
    }

    public void setGeolokacija(Lokacija geolokacija) {
        this.geolokacija = geolokacija;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public int getBrojParkirnihMjesta() {
        return brojParkirnihMjesta;
    }

    public void setBrojParkirnihMjesta(int brojParkirnihMjesta) {
        this.brojParkirnihMjesta = brojParkirnihMjesta;
    }

    public int getBrojUlaznihMjesta() {
        return brojUlaznihMjesta;
    }

    public void setBrojUlaznihMjesta(int brojUlaznihMjesta) {
        this.brojUlaznihMjesta = brojUlaznihMjesta;
    }

    public int getBrojIzlaznihMjesta() {
        return brojIzlaznihMjesta;
    }

    public void setBrojIzlaznihMjesta(int brojIzlaznihMjesta) {
        this.brojIzlaznihMjesta = brojIzlaznihMjesta;
    }

    public int getKorisnikId() {
        return korisnikId;
    }

    public void setKorisnikId(int korisnikId) {
        this.korisnikId = korisnikId;
    }

}
