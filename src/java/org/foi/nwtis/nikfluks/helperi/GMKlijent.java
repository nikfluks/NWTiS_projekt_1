/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foi.nwtis.nikfluks.helperi;

import java.io.StringReader;
import java.net.URLEncoder;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author nwtis_1
 */
public class GMKlijent {

    GMRESTHelper helper;
    Client client;
    String apikey;

    public GMKlijent(String apikey) {
        client = ClientBuilder.newClient();
        this.apikey = apikey;
    }

    public Lokacija getGeoLocation(String adresa) {
        try {
            WebTarget webResource = client.target(GMRESTHelper.getGM_BASE_URI())
                    .path("maps/api/geocode/json");
            webResource = webResource.queryParam("address", URLEncoder.encode(adresa, "UTF-8"));
            webResource = webResource.queryParam("sensor", "false");
            webResource = webResource.queryParam("key", this.apikey);

            String odgovor = webResource.request(MediaType.APPLICATION_JSON).get(String.class);

            StringReader sr = new StringReader(odgovor);
            JsonReader reader = Json.createReader(sr);
            JsonObject jo = reader.readObject();

            JsonObject obj = jo.getJsonArray("results")
                    .getJsonObject(0)
                    .getJsonObject("geometry")
                    .getJsonObject("location");

            Lokacija loc = new Lokacija(obj.getJsonNumber("lat").toString(), obj.getJsonNumber("lng").toString());
            return loc;
        } catch (Exception ex) {
            System.err.println("Greska kod dohvacanja geolokacije iz adrese: " + adresa + " -> " + ex.getLocalizedMessage());
        }
        return null;
    }
}
