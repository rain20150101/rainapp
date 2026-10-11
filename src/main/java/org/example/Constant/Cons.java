package org.example.Constant;

public class Cons {

    public static final String input = "./" + TaxYear.taxYear + "/input";
    public static final String output = "./" + TaxYear.taxYear + "/output";
    //timestamped per-run copies live here; output/ itself always holds the latest
    //run's files under their stable (non-timestamped) names.
    public static final String outputArchive = output + "/archive";
}
