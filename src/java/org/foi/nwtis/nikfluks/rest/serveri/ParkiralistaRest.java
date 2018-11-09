package org.foi.nwtis.nikfluks.rest.serveri;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.foi.nwtis.nikfluks.helperi.GMKlijent;
import org.foi.nwtis.nikfluks.helperi.Korisnik;
import org.foi.nwtis.nikfluks.helperi.Lokacija;
import org.foi.nwtis.nikfluks.helperi.OWMKlijent;
import org.foi.nwtis.nikfluks.helperi.Parkiraliste;
import org.foi.nwtis.nikfluks.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.nikfluks.slusaci.SlusacAplikacije;
import org.foi.nwtis.nikfluks.wsParkiranje.StatusParkiralista;

/**
 * REST Web Service
 *
 * @author Nikola
 */
@Path("parkiralista")
public class ParkiralistaRest {

    String urlBaza;
    String korImeBaza;
    String lozinkaBaza;
    String uprProgram;
    String gmapikey;
    String apikey;
    OWMKlijent owmk;
    GMKlijent gmk;
    String greska;
    int idAutenticiranogKorisnika;
    String korisnickoImeParkiranje;
    String lozinkaParkiranje;
    StringBuilder upitZaAzurBaza;
    int idDodanogParkiralista;
    long pocetakMillis;

    @Context
    private UriInfo context;

    public ParkiralistaRest() {
        greska = "";
    }

    private boolean dohvatiPodatkeIzKonfiguracije() {
        try {
            BP_Konfiguracija bpk = (BP_Konfiguracija) SlusacAplikacije.getServletContext().getAttribute("BP_Konfig");
            urlBaza = bpk.getServerDatabase() + bpk.getUserDatabaseMySQL();
            korImeBaza = bpk.getUserUsername();
            lozinkaBaza = bpk.getUserPassword();
            uprProgram = bpk.getDriverDatabase();
            gmapikey = bpk.getGmApiKey();
            gmk = new GMKlijent(gmapikey);
            korisnickoImeParkiranje = bpk.getKorisnickoImeParkiranje_();
            lozinkaParkiranje = bpk.getLozinkaParkiranje_();
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
            stat.setString(3, "REST");
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
     * Dohvaća podatke o svim parkiralištima iz baze.
     *
     * @param korisnickoIme
     * @param lozinka
     * @return odgovor u json formatu
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String preuzmiSvaParkiralista(@QueryParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        try {
            List<Parkiraliste> svaParkiralista = dajSvaParkiralista();
            if (svaParkiralista != null) {
                odg.append("{\"odgovor\": ");
                String json = new Gson().toJson(svaParkiralista);
                odg.append(json);
                odg.append(", \"status\": \"OK\"}");
                upisiUDnevnik(korisnickoIme, "preuzmiSvaParkiralista");
            } else {
                odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                upisiUDnevnik(korisnickoIme, "preuzmiSvaParkiralista - Greška");
            }
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "preuzmiSvaParkiralista - Greška");
        }
        return odg.toString();
    }

    /**
     * Dohvaća podatke izabranog parkirališta iz baze.
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id po kojem se dohvaća parkiralište
     * @return odgovor u json formatu
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public String preuzmiJednoParkiraliste(@PathParam("id") String id, @QueryParam("korisnickoIme") String korisnickoIme,
            @QueryParam("lozinka") String lozinka) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        try {
            Parkiraliste p = dajParkiraliste(Integer.parseInt(id));
            if (p != null) {
                String json = new Gson().toJson(p);
                odg.append("{\"odgovor\": [");
                odg.append(json);
                odg.append("], \"status\": \"OK\"}");
                upisiUDnevnik(korisnickoIme, "preuzmiJednoParkiraliste");
            } else {
                odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                upisiUDnevnik(korisnickoIme, "preuzmiJednoParkiraliste - Greška");
            }
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "preuzmiJednoParkiraliste - Greška");
        }
        return odg.toString();
    }

    /**
     * Dodaje parkiralište u bazu podataka i na web servis.
     *
     * @param korisnickoIme
     * @param lozinka
     * @param podaci naziv, adresa i broj parkirnih mjesta u json formatu
     * @return odgovor u json formatu
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public String dodajJednoParkiraliste(@QueryParam("korisnickoIme") String korisnickoIme,
            @QueryParam("lozinka") String lozinka, String podaci) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        if (!podaci.toLowerCase().contains("naziv") || !podaci.toLowerCase().contains("adresa")
                || !podaci.toLowerCase().contains("brojparkirnihmjesta")) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Nisu unešeni svi potrebni podaci!\"}");
            upisiUDnevnik(korisnickoIme, "dodajJednoParkiraliste - Greška");
            return odg.toString();
        }
        try {
            //ako ne uspije prebaciti u parkiraliste ide u catch, npr ako broj parkirnih mjesta nije broj
            Parkiraliste p = new Gson().fromJson(podaci, Parkiraliste.class);
            Lokacija lok = provjeriLokaciju(p.getAdresa());
            if (lok == null) {
                odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                upisiUDnevnik(korisnickoIme, "dodajJednoParkiraliste - Greška");
            } else {
                boolean uspjesnoDodanoUBazu = dodajParkiralisteUBazu(p, lok);
                if (uspjesnoDodanoUBazu) {
                    dodajNovoParkiralisteGrupi(korisnickoImeParkiranje, lozinkaParkiranje, idDodanogParkiralista, p.getNaziv(),
                            p.getAdresa(), p.getBrojParkirnihMjesta());
                    odg.append("{\"odgovor\": [], \"status\": \"OK\"}");
                    upisiUDnevnik(korisnickoIme, "dodajJednoParkiraliste");
                } else {
                    odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                    upisiUDnevnik(korisnickoIme, "dodajJednoParkiraliste - Greška");
                }
            }
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "dodajJednoParkiraliste - Greška");
        }
        return odg.toString();
    }

    /**
     * Ažurira podatke o parkiralištu u bazi podataka i na web servisu.
     *
     * @param id id parkirališta koje se ažurira
     * @param korisnickoIme
     * @param lozinka
     * @param podaci novi podaci koje će parkiralište poprimiti
     * @return odgovor u json formatu
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public String azurirajJednoParkiraliste(@PathParam("id") String id, @QueryParam("korisnickoIme") String korisnickoIme,
            @QueryParam("lozinka") String lozinka, String podaci) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        if (!podaci.toLowerCase().contains("naziv") || !podaci.toLowerCase().contains("adresa")
                || !podaci.toLowerCase().contains("brojparkirnihmjesta")) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Nisu unešeni svi potrebni podaci!\"}");
            upisiUDnevnik(korisnickoIme, "azurirajJednoParkiraliste - Greška");
            return odg.toString();
        }
        try {
            //park s novim podacima, ako broj parkirnih mjesta nije broj, ide u catch
            Parkiraliste p = new Gson().fromJson(podaci, Parkiraliste.class);
            p.setId(Integer.parseInt(id));
            Parkiraliste park = dajParkiraliste(p.getId());//park s trenutnim podacima iz baze, ne koristi se dalje osim za provjeru
            if (park != null) {
                Lokacija lok = provjeriLokaciju(p.getAdresa());
                if (lok == null) {
                    odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                    upisiUDnevnik(korisnickoIme, "azurirajJednoParkiraliste - Greška");
                } else {
                    boolean uspjesnoAzuriranoNaServisu = azurirajParkiralisteNaServisu(p);
                    if (uspjesnoAzuriranoNaServisu) {
                        //na bazi se azurira tek nakon sto je azurirano na servisu jer ako park ima vozila onda se ne smije brisati park
                        azurirajParkiralisteUBazi(p, lok);
                        odg.append("{\"odgovor\": [], \"status\": \"OK\"}");
                        upisiUDnevnik(korisnickoIme, "azurirajJednoParkiraliste");
                    } else {
                        odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                        upisiUDnevnik(korisnickoIme, "azurirajJednoParkiraliste - Greška");
                    }
                }
            } else {
                odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                upisiUDnevnik(korisnickoIme, "azurirajJednoParkiraliste - Greška");
            }
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "azurirajJednoParkiraliste - Greška");
        }
        return odg.toString();
    }

    /**
     * Briše odabrano parkiralište iz baze i web servisa ako još nema meteo podataka o njemu.
     *
     * @param id id parkirališta koje se želi obrisati
     * @param korisnickoIme
     * @param lozinka
     * @return odgovor u json formatu
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public String obrisiJednoParkiraliste(@PathParam("id") String id, @QueryParam("korisnickoIme") String korisnickoIme,
            @QueryParam("lozinka") String lozinka) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        int idInt = Integer.parseInt(id);
        try {
            Parkiraliste park = dajParkiraliste(idInt);
            if (park != null) {
                //ako je uspjesno obrisano sa servisa znaci da nema vozila
                boolean uspjesnoObrisanoSaServisa = obrisiParkiralisteSaServisa(idInt);
                if (uspjesnoObrisanoSaServisa) {
                    //ako je uspjesno obrisano iz baze znaci da nema meteo podataka za njega
                    boolean uspjesnoObrisanoIzBaze = obrisiParkiralisteIzBaze(idInt);
                    if (uspjesnoObrisanoIzBaze) {
                        odg.append("{\"odgovor\": [], \"status\": \"OK\"}");
                        upisiUDnevnik(korisnickoIme, "obrisiJednoParkiraliste");
                    } else {
                        //ako nije uspjesno izbrisano iz baze, dodaje se zapis nazad na servis
                        dodajNovoParkiralisteGrupi(korisnickoImeParkiranje, lozinkaParkiranje, idInt, park.getNaziv(),
                                park.getAdresa(), park.getBrojParkirnihMjesta());
                        odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                        upisiUDnevnik(korisnickoIme, "obrisiJednoParkiraliste - Greška");
                    }
                } else {
                    odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                    upisiUDnevnik(korisnickoIme, "obrisiJednoParkiraliste - Greška");
                }
            } else {
                odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
                upisiUDnevnik(korisnickoIme, "obrisiJednoParkiraliste - Greška");
            }
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "obrisiJednoParkiraliste - Greška");
        }
        return odg.toString();
    }

    /**
     * Dohvaća sva vozila za izabrano parkiraliste sa web servisa.
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista za kojeg se dohvaćaju vozila
     * @return odgovor u json formatu
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/vozila/")
    public String preuzmiSvaVozilaOdabranogParkiralista(@PathParam("id") String id,
            @QueryParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        try {
            List<org.foi.nwtis.nikfluks.wsParkiranje.Vozilo> svaVozilaZaParkiraliste
                    = dajSvaVozilaParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, Integer.parseInt(id));
            //TODO eventualno dohvatiti status dajStatusParkiralistaGrupe i ako je != od NEPOSTOJI onda vrati ok, inace err
            //Parkiraliste p = dajParkiraliste(Integer.parseInt(id));
            //if (p != null) {
            odg.append("{\"odgovor\": ");
            String json = new Gson().toJson(svaVozilaZaParkiraliste);
            odg.append(json);
            odg.append(", \"status\": \"OK\"}");
            upisiUDnevnik(korisnickoIme, "preuzmiSvaVozilaOdabranogParkiralista");
            /*} else {
                odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"").append(greska).append("\"}");
            }*/
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "preuzmiSvaVozilaOdabranogParkiralista - Greška");
        }
        return odg.toString();
    }

    /**
     * Dohvaća status parkiralista sa web servisa.
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista za kojeg se dohvaćaju vozila
     * @return odgovor u json formatu
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/vratiStatus/")
    public String dohvatiStatusParkiralista(@PathParam("id") String id,
            @QueryParam("korisnickoIme") String korisnickoIme, @QueryParam("lozinka") String lozinka) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        try {
            StatusParkiralista status = dajStatusParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, Integer.parseInt(id));
            odg.append("{\"odgovor\": \"").append(status.toString()).append("\"");
            odg.append(", \"status\": \"OK\"}");
            upisiUDnevnik(korisnickoIme, "dohvatiStatusParkiralista");
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "dohvatiStatusParkiralista - Greška");
        }
        return odg.toString();
    }

    /**
     * Mijenja status parkiralista na web servisu.
     *
     * @param korisnickoIme
     * @param lozinka
     * @param id id parkiralista za kojeg se dohvaćaju vozila
     * @param status status koji se želi postaviti
     * @return odgovor u json formatu
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}/promjeniStatus/")
    public String promjeniStatusParkiralista(@PathParam("id") String id, @QueryParam("korisnickoIme") String korisnickoIme,
            @QueryParam("lozinka") String lozinka, @QueryParam("status") String status) {
        pocetakMillis = System.currentTimeMillis();
        StringBuilder odg = new StringBuilder();
        if (!dohvatiPodatkeIzKonfiguracije() || !autenticitajKorisnika(korisnickoIme, lozinka)) {
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogrešno korisničko ime ili lozinka!\"}");
            return odg.toString();
        }
        try {
            //aktivan blokiran nepostoji pasivan
            int idInt = Integer.parseInt(id);
            StatusParkiralista trenutniStatus = dajStatusParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, idInt);
            System.out.println("moj status: " + status);
            System.out.println("trenutni status: " + trenutniStatus.toString());
            if (trenutniStatus.toString().equalsIgnoreCase("nepostoji")) {
                odg.append("{\"odgovor\": []");
                odg.append(", \"status\": \"ERR\", \"poruka\": \"Parkiralište ne postoji pa se ne može mijenjati status!\"}");
                upisiUDnevnik(korisnickoIme, "promjeniStatusParkiralista - Greška");
            } else if (trenutniStatus.toString().equalsIgnoreCase(status)) {
                odg.append("{\"odgovor\": []");
                odg.append(", \"status\": \"ERR\", \"poruka\": \"Parkiralište već ima status ")
                        .append(trenutniStatus.toString()).append("!\"}");
                upisiUDnevnik(korisnickoIme, "promjeniStatusParkiralista - Greška");
            } else {
                if (status.equals("blokiran")) {
                    blokirajParkiralisteGrupe(korisnickoImeParkiranje, lozinkaParkiranje, idInt);
                } else if (status.equals("aktivan")) {
                    aktivirajParkiralisteGrupe(korisnickoImeParkiranje, lozinkaParkiranje, idInt);
                }
                odg.append("{\"odgovor\": []");
                odg.append(", \"status\": \"OK\"}");
                upisiUDnevnik(korisnickoIme, "promjeniStatusParkiralista");
            }
        } catch (Exception e) {
            odg = new StringBuilder();
            odg.append("{\"odgovor\": [], \"status\": \"ERR\", \"poruka\": \"Pogreška kod parsiranja odgovora!\"}");
            upisiUDnevnik(korisnickoIme, "promjeniStatusParkiralista - Greška");
        }
        return odg.toString();
    }

    private Lokacija provjeriLokaciju(String adresa) {
        Lokacija lok = gmk.getGeoLocation(adresa);
        if (lok == null) {
            greska = "Greška pri dohvaćanju geolokacije!";
        }
        return lok;
    }

    private boolean obrisiParkiralisteIzBaze(int id) {
        String upit = "DELETE FROM PARKIRALISTA WHERE id = " + id;
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            stat.execute();
            stat.close();
            con.close();
            return true;
        } catch (SQLException ex) {
            greska = "Greška kod brisanja parkirališta u bazi. Ne može se obrisati jer postoje meteo podaci za njega!";
            return false;
        }
    }

    private boolean obrisiParkiralisteSaServisa(int idInt) {
        StatusParkiralista status = dajStatusParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, idInt);
        List<org.foi.nwtis.nikfluks.wsParkiranje.Vozilo> svaVozilaZaParkiraliste
                = dajSvaVozilaParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, idInt);
        if (status == StatusParkiralista.NEPOSTOJI) {
            greska = "Parkiralište s id " + idInt + " ne postoji na servisu!";
            return false;
        } else if (!(svaVozilaZaParkiraliste == null || svaVozilaZaParkiraliste.isEmpty())) {
            greska = "Parkiralište s id " + idInt + " ima pridružena vozila pa se ne može obrisati!";
            return false;
        } else {
            obrisiParkiralisteGrupe(korisnickoImeParkiranje, lozinkaParkiranje, idInt);
            return true;
        }
    }

    /**
     * Budući da ne postoji metoda ažuriraj na servisu, parkiralište se briše pa ponovo dodaje.
     *
     * @param p parkiralište koje se ažurira
     */
    private boolean azurirajParkiralisteNaServisu(Parkiraliste p) {
        StatusParkiralista status = dajStatusParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, p.getId());
        List<org.foi.nwtis.nikfluks.wsParkiranje.Vozilo> svaVozilaZaParkiraliste
                = dajSvaVozilaParkiralistaGrupe(korisnickoImeParkiranje, lozinkaParkiranje, p.getId());
        if (status == StatusParkiralista.NEPOSTOJI) {
            greska = "Parkiralište s id " + p.getId() + " ne postoji na servisu!";
            return false;
        } else if (!(svaVozilaZaParkiraliste == null || svaVozilaZaParkiraliste.isEmpty())) {
            greska = "Parkiralište s id " + p.getId() + " ima pridružena vozila pa se ne može ažurirati!";
            return false;
        } else {
            obrisiParkiralisteGrupe(korisnickoImeParkiranje, lozinkaParkiranje, p.getId());
            dodajNovoParkiralisteGrupi(korisnickoImeParkiranje, lozinkaParkiranje, p.getId(), p.getNaziv(),
                    p.getAdresa(), p.getBrojParkirnihMjesta());
            return true;
        }
    }

    private boolean azurirajParkiralisteUBazi(Parkiraliste p, Lokacija lok) {
        String upit = "UPDATE parkiralista SET naziv = '" + p.getNaziv() + "', adresa = '" + p.getAdresa()
                + "', brojParkirnihMjesta = " + p.getBrojParkirnihMjesta() + ", latitude = " + lok.getLatitude()
                + ", longitude = " + lok.getLongitude() + ", korisnik_id = " + idAutenticiranogKorisnika + " WHERE id = " + p.getId();
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            stat.executeUpdate();
            stat.close();
            con.close();
            return true;
        } catch (SQLException ex) {
            greska = "Greška kod ažuriranja parkirališta u bazi!";
            return false;
        }
    }

    /*private boolean kreirajUpitZaAzuriranjeNaBazi(Parkiraliste p, String podaci) {
        upitZaAzurBaza = new StringBuilder();
        upitZaAzurBaza.append("UPDATE PARKIRALISTA SET ");
        if (podaci.toLowerCase().contains("naziv")) {
            upitZaAzurBaza.append("naziv = '").append(p.getNaziv()).append("'");
        }
        if (podaci.toLowerCase().contains("adresa")) {
            if (upitZaAzurBaza.toString().endsWith("SET ")) {
                upitZaAzurBaza.append("adresa = '").append(p.getAdresa()).append("'");
            } else {
                upitZaAzurBaza.append(", adresa = '").append(p.getAdresa()).append("'");
            }
            Lokacija lok = gmk.getGeoLocation(p.getAdresa());
            if (lok == null) {
                greska = "Greška pri dohvaćanju geolokacije!";
                return false;
            } else {
                upitZaAzurBaza.append(", latitude = ").append(lok.getLatitude());
                upitZaAzurBaza.append(", longitude = ").append(lok.getLongitude());
            }
        }
        if (podaci.toLowerCase().contains("brojparkirnihmjesta")) {
            if (upitZaAzurBaza.toString().endsWith("SET ")) {
                upitZaAzurBaza.append("brojParkirnihMjesta = ").append(p.getBrojParkirnihMjesta());
            } else {
                upitZaAzurBaza.append(", brojParkirnihMjesta = ").append(p.getBrojParkirnihMjesta());
            }
        }
        if (upitZaAzurBaza.toString().endsWith("SET ")) {
            greska = "Nije unesen ni jedan podatak!";
            return false;
        } else {
            upitZaAzurBaza.append(" WHERE id = ").append(p.getId());
        }
        return true;
    }*/
    private boolean dodajParkiralisteUBazu(Parkiraliste p, Lokacija lok) {
        String upit = "INSERT INTO PARKIRALISTA (naziv, adresa, latitude, longitude, brojParkirnihMjesta, korisnik_id)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit, Statement.RETURN_GENERATED_KEYS);
            stat.setString(1, p.getNaziv());
            stat.setString(2, p.getAdresa());
            stat.setString(3, lok.getLatitude());
            stat.setString(4, lok.getLongitude());
            stat.setInt(5, p.getBrojParkirnihMjesta());
            stat.setInt(6, idAutenticiranogKorisnika);
            stat.execute();

            ResultSet rs = stat.getGeneratedKeys();
            if (rs.next()) {
                idDodanogParkiralista = rs.getInt(1);
            }
            rs.close();
            stat.close();
            con.close();
            return true;
        } catch (SQLException ex) {
            greska = "Greška kod upisa parkirališta u bazu!";
            return false;
        }
    }

    private Parkiraliste dajParkiraliste(int id) {
        String upit = "SELECT * FROM PARKIRALISTA WHERE id=" + id;
        Parkiraliste p = null;
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            if (rs.next()) {
                Lokacija lokacija = new Lokacija(rs.getString("latitude"), rs.getString("longitude"));
                p = new Parkiraliste(rs.getInt("id"), rs.getString("naziv"), rs.getString("adresa"), lokacija,
                        rs.getInt("brojParkirnihMjesta"), rs.getInt("brojUlaznihMjesta"), rs.getInt("brojIzlaznihMjesta"),
                        rs.getInt("korisnik_id"));
            }
            rs.close();
            stat.close();
            con.close();
            if (p == null) {
                greska = "Parkiralište s id " + id + " ne postoji u bazi!";
                return null;
            } else {
                return p;
            }
        } catch (SQLException ex) {
            greska = "Greška kod dohvaćanja parkirališta!";
            return null;
        }
    }

    private List<Parkiraliste> dajSvaParkiralista() {
        List<Parkiraliste> svaParkiralista = new ArrayList<>();
        String upit = "SELECT * FROM PARKIRALISTA";
        try {
            Connection con = DriverManager.getConnection(urlBaza, korImeBaza, lozinkaBaza);
            PreparedStatement stat = con.prepareStatement(upit);
            ResultSet rs = stat.executeQuery();
            while (rs.next()) {
                Lokacija lokacija = new Lokacija(rs.getString("latitude"), rs.getString("longitude"));
                svaParkiralista.add(new Parkiraliste(rs.getInt("id"), rs.getString("naziv"), rs.getString("adresa"), lokacija,
                        rs.getInt("brojParkirnihMjesta"), rs.getInt("brojUlaznihMjesta"), rs.getInt("brojIzlaznihMjesta"),
                        rs.getInt("korisnik_id")));
            }
            rs.close();
            stat.close();
            con.close();
            return svaParkiralista;
        } catch (SQLException ex) {
            greska = "Greška kod dohvaćanja svih parkirališta!";
            return null;
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

    private boolean autenticitajKorisnika(String korisnickoIme, String lozinka) {
        List<Korisnik> sviKorisnici = dohvatiKorisnike();
        for (Korisnik k : sviKorisnici) {
            if (k.getKorisnickoIme().equals(korisnickoIme)) {
                if (k.getLozinka().equals(lozinka)) {
                    idAutenticiranogKorisnika = k.getId();
                    return true;
                }
            }
        }
        return false;
    }

    private static Boolean dodajNovoParkiralisteGrupi(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka,
            int idParkiraliste, java.lang.String nazivParkiraliste, java.lang.String adresaParkiraliste, int kapacitetParkiraliste) {

        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.dodajNovoParkiralisteGrupi(korisnickoIme, korisnickaLozinka, idParkiraliste, nazivParkiraliste,
                adresaParkiraliste, kapacitetParkiraliste);
    }

    private static boolean obrisiParkiralisteGrupe(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka, int idParkiraliste) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.obrisiParkiralisteGrupe(korisnickoIme, korisnickaLozinka, idParkiraliste);
    }

    private static java.util.List<org.foi.nwtis.nikfluks.wsParkiranje.Vozilo> dajSvaVozilaParkiralistaGrupe(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka, int idParkiraliste) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.dajSvaVozilaParkiralistaGrupe(korisnickoIme, korisnickaLozinka, idParkiraliste);
    }

    private static StatusParkiralista dajStatusParkiralistaGrupe(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka, int idParkiraliste) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.dajStatusParkiralistaGrupe(korisnickoIme, korisnickaLozinka, idParkiraliste);
    }

    private static boolean aktivirajParkiralisteGrupe(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka, int idParkiraliste) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.aktivirajParkiralisteGrupe(korisnickoIme, korisnickaLozinka, idParkiraliste);
    }

    private static boolean blokirajParkiralisteGrupe(java.lang.String korisnickoIme, java.lang.String korisnickaLozinka, int idParkiraliste) {
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service service = new org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje_Service();
        org.foi.nwtis.nikfluks.wsParkiranje.Parkiranje port = service.getParkiranjePort();
        return port.blokirajParkiralisteGrupe(korisnickoIme, korisnickaLozinka, idParkiraliste);
    }

}
