package fi.ylihallila.server.controllers;

import fi.ylihallila.server.util.Util;
import io.javalin.http.Context;

import java.util.*;

public class OrganizationController extends Controller {

    public void getAllOrganizations(Context ctx) {
        List<Map<String, String>> data = new ArrayList<>();

        for (var entry : Util.getKnownTenants().entrySet()) {
           var datum = new HashMap<String, String>();

           datum.put("id", entry.getKey());
           datum.put("name", entry.getValue());
           datum.put("logoUrl", "/logos/" + entry.getKey() + ".png");

           data.add(datum);
        }

        data.sort(mapComparator);

        ctx.status(200).json(data);
    }

    public Comparator<Map<String, String>> mapComparator = Comparator.comparing(m -> m.get("name"));
}
