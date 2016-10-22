package at.favre.tools.apksigner.parser;

import java.util.*;

public class PackageMatcher {
    private List<String> packages;

    public PackageMatcher(List<String> packages) {
        this.packages = packages;
    }

    public static String[] parseFiltersArg(String arg) {
        if(arg != null && !arg.isEmpty()) {
            return arg.split(",");
        }
        throw new IllegalArgumentException("unexpected arg: "+arg);
    }

    public Set<String> findMatches(String... moreFilters) {
        Set<String> matchedPackages = new HashSet<>();

        List<String> filters = new ArrayList<>();
        if (moreFilters != null && moreFilters.length > 0) {
            filters.addAll(Arrays.asList(moreFilters));
        }

        for (String aPackage : packages) {
            for (String filter : filters) {
                if (match(filter, aPackage)) {
                    matchedPackages.add(aPackage);
                }
            }
        }
        return matchedPackages;
    }

    static boolean match(String filter, String aPackage) {
        if (filter == null || filter.isEmpty()) {
            return false;
        } else {
            String escapedFilterString = "^" + filter.replace(".", "\\Q.\\E").replace("*", ".*");
            return aPackage.matches(escapedFilterString);
        }
    }
}
