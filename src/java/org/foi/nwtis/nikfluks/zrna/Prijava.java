package org.foi.nwtis.nikfluks.zrna;

import java.io.IOException;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Nikola
 */
@ManagedBean(name = "prijava")
@SessionScoped
public class Prijava implements Serializable {

    String korisnickoIme;
    String lozinka;

    public Prijava() {
    }

    public String prijava() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        try {
            request.login(korisnickoIme, lozinka);
        } catch (ServletException ex) {
            System.err.println("greska kod prijave: " + ex.getLocalizedMessage());
            return "error";
        }
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();
        try {
            request.authenticate(response);
        } catch (IOException | ServletException ex) {
            System.err.println("greska kod autentikacija prijave: " + ex.getLocalizedMessage());
            return "error";
        }
        return "j_security_check";
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
}
