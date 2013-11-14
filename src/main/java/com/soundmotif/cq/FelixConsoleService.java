package com.soundmotif.cq;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class FelixConsoleService {

    public static Bundle getBundle(FelixConsoleResult r, String symbolicName) {
        for (Bundle b : r.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                return b;
            }
        }
        return null;
    }


    public static FelixConsoleResult populateFelixConsole(JSONObject p) {

        FelixConsoleResult r = new FelixConsoleResult();

        JSONArray stat = (JSONArray) p.get("s");

        r.setTotalBundle((Long) stat.get(0));
        r.setActiveBundle((Long) stat.get(1));
        r.setFragmentsBundle((Long) stat.get(2));
        r.setResolvedBundle((Long) stat.get(3));


        for (Object o : ((JSONArray) p.get("data"))) {
            JSONObject o1 = (JSONObject) o;

            Bundle b = new Bundle();
            b.setSymbolicName((String) o1.get("symbolicName"));
            b.setCategory((String) o1.get("category"));
            b.setFragment((Boolean) o1.get("fragment"));
            b.setId((Long) o1.get("id"));
            b.setName((String) o1.get("name"));
            b.setState((String) o1.get("state"));
            b.setVersion((String) o1.get("version"));

            r.addBundle(b);
        }

        return r;
    }


}
