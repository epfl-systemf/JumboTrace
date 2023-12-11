import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Main {

    public static void main(String[] args) {

        var names = List.of(
                "Adelaide",
                "Berenice",
                "Charles",
                "Dave",
                "Emilie",
                "Francoise",
                "Greg",
                "Hector",
                "Ilena",
                "Julien",
                "Karen",
                "Loic",
                "Maurice",
                "Norbert"
        );

        for (var greeting : generateFullGreetings(names)) {
            System.out.println(greeting);
        }

    }

    @SuppressWarnings("all")
    static List<String> generateFullGreetings(List<String> names) {
        var greetings = new LinkedList<String>();
        outer:
        for (var lastName : names) {
            for (int i = 0; i < names.size(); i++) {
                if (!isPotentialLastName(lastName)) {
                    continue outer;
                }
                var firstName = names.get(i);
                if (lastName.equals(firstName)) continue;
                if (firstName.equals("Maurice") && lastName.equals("Hector")) {
                    break outer;
                } else if (firstName.equals("Maurice")) {
                    break;
                }
                boolean frenchSounding = isFrenchSoundingName(firstName);
                var title = isFemaleSoundingName(firstName) ?
                        frenchSounding ? "Madame" : "Madam" :
                        frenchSounding ? "Monsieur" : "Sir";
                var greet = frenchSounding ? "Bonsoir" : "Good evening";
                greetings.add(greet + " " + title + " " + firstName + " " + lastName);
            }
        }
        return greetings;
    }

    // switch expr
    @SuppressWarnings("all")
    static boolean isFrenchSoundingName(String s) {
        return switch (dummyUpperCase(s)) {
            case "Adelaide", "Berenice", "Charles" -> true;
            case "Emilie", "Francoise" -> {
                yield true;
            }
            case "Julien", "Loic", "Maurice" -> true;
            case "Norbert" -> true;
            default -> false;
        };
    }

    // switch statement with colons
    @SuppressWarnings("all")
    static boolean isFemaleSoundingName(String s) {
        boolean res = false;
        switch (s) {
            case "Adelaide":
                return true;
            case "Berenice", "Emilie", "Francoise":
                res = true;
                break;
            case "Ilena": {
                res = true;
                break;
            }
            case "Karen":
                return true;
        }
        return res;
    }

    // switch statement with arrows
    @SuppressWarnings("all")
    static boolean isPotentialLastName(String s) {
        var res = false;
        switch (s) {
            case "Adelaide" -> {
                return true;
            }
            case "Charles", "Hector" -> {
                res = true;
                break;
            }
            case "Norbert" -> {
                res = true;
                return res;
            }
        }
        return res;
    }

    static String dummiestUpperCase(String name) {
        return switch (name) {
            case "adelaide":
                var r = "adelaide";
                r = r.replaceFirst("a", "A");
                yield r;
            case "berenice":
                yield "Berenice";
            case "charles":
                yield null;
            default:
                yield name.isEmpty() ? name : null;
        };
    }

    static String dummyUpperCase(String name){
        var r = dummiestUpperCase(name);
        return (r == null) ? name.replaceFirst(String.valueOf(name.charAt(0)), name.substring(0, 1).toUpperCase()) : r;
    }

}
