package org.foi.nwtis.nikfluks.helperi;

import java.io.StringReader;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class OWMKlijent {

    String apiKey;
    OWMRESTHelper helper;
    Client client;

    public OWMKlijent(String apiKey) {
        this.apiKey = apiKey;
        helper = new OWMRESTHelper(apiKey);
        client = ClientBuilder.newClient();
    }

    public MeteoPodaci getRealTimeWeather(String latitude, String longitude) {
        WebTarget webResource = client.target(OWMRESTHelper.getOWM_BASE_URI())
                .path(OWMRESTHelper.getOWM_Current_Path());
        webResource = webResource.queryParam("lat", latitude);
        webResource = webResource.queryParam("lon", longitude);
        webResource = webResource.queryParam("lang", "hr");
        webResource = webResource.queryParam("units", "metric");
        webResource = webResource.queryParam("APIKEY", apiKey);

        String odgovor = webResource.request(MediaType.APPLICATION_JSON).get(String.class);
        try {
            JsonReader reader = Json.createReader(new StringReader(odgovor));
            JsonObject jo = reader.readObject();

            MeteoPodaci mp = new MeteoPodaci();
            mp.setSunRise(new Date(jo.getJsonObject("sys").getJsonNumber("sunrise").bigDecimalValue().longValue() * 1000));
            mp.setSunSet(new Date(jo.getJsonObject("sys").getJsonNumber("sunset").bigDecimalValue().longValue() * 1000));
            mp.setTemperatureValue(new Double(jo.getJsonObject("main").getJsonNumber("temp").doubleValue()).floatValue());//°C
            mp.setTemperatureMin(new Double(jo.getJsonObject("main").getJsonNumber("temp_min").doubleValue()).floatValue());//°C
            mp.setTemperatureMax(new Double(jo.getJsonObject("main").getJsonNumber("temp_max").doubleValue()).floatValue());//°C
            mp.setTemperatureUnit("°C");
            mp.setHumidityValue(new Double(jo.getJsonObject("main").getJsonNumber("humidity").doubleValue()).floatValue());//%
            mp.setHumidityUnit("%");
            mp.setPressureValue(new Double(jo.getJsonObject("main").getJsonNumber("pressure").doubleValue()).floatValue());//hPa
            mp.setPressureUnit("hPa");
            mp.setWindSpeedValue(new Double(jo.getJsonObject("wind").getJsonNumber("speed").doubleValue()).floatValue());//m/s
            if (jo.getJsonObject("wind").getJsonNumber("deg") != null) {
                mp.setWindDirectionValue(new Double(jo.getJsonObject("wind").getJsonNumber("deg").doubleValue()).floatValue());//°
            } else {
                mp.setWindDirectionValue(0.0f);
            }
            mp.setCloudsValue(jo.getJsonObject("clouds").getInt("all"));//%
            mp.setWeatherNumber(jo.getJsonArray("weather").getJsonObject(0).getInt("id"));
            mp.setCloudsName(jo.getJsonArray("weather").getJsonObject(0).getString("main"));//npr Rain, Snow...
            mp.setWeatherValue(jo.getJsonArray("weather").getJsonObject(0).getString("description"));//detaljniji opis gore navedenog
            mp.setWeatherIcon(jo.getJsonArray("weather").getJsonObject(0).getString("icon"));
            //vrijeme kad su zadnji put podaci osvjezeni na servisu (svakih 0,5h cca); timestamp (long)
            mp.setLastUpdate(new Date(jo.getJsonNumber("dt").bigDecimalValue().longValue() * 1000));
            return mp;
        } catch (Exception ex) {
            System.err.println("Greska kod dohvacanja meteo podataka sa web servisa: " + ex.getLocalizedMessage());
        }
        return null;
    }
}
