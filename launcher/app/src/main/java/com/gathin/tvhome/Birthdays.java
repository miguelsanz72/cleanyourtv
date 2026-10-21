package com.gathin.tvhome;

import java.io.File;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Birthdays from config.xml or a theme's config.json ({@code birthdays}): {@code "YYYY-MM-DD=Name"}, or
 * {@code "MM-DD=Name"} when the year is not wanted. On the day the greeting wishes that person a happy birthday
 * (with their age when the year is known), every year, with no end date to keep up to date. A 29 February
 * birthday is kept on 28 February in other years. An entry that does not parse is left out on its own.
 */
final class Birthdays {

    static final Birthdays NONE = new Birthdays(Collections.emptyList());

    static final class Birthday {
        final MonthDay day;
        /** Year of birth, or 0 when the entry gave none. */
        final int year;
        final String name;

        Birthday(MonthDay day, int year, String name) {
            this.day = day;
            this.year = year;
            this.name = name;
        }

        boolean on(LocalDate today) {
            if (day.getMonthValue() == 2 && day.getDayOfMonth() == 29 && !today.isLeapYear()) {
                return today.getMonthValue() == 2 && today.getDayOfMonth() == 28;
            }
            return MonthDay.from(today).equals(day);
        }

        /** The age reached on {@code today}, or 0 when the year is unknown (or not yet a year old). */
        int age(LocalDate today) {
            return year > 0 && today.getYear() > year ? today.getYear() - year : 0;
        }
    }

    final List<Birthday> all;

    private Birthdays(List<Birthday> all) {
        this.all = Collections.unmodifiableList(all);
    }

    static Birthdays parse(List<String> entries) {
        if (entries == null || entries.isEmpty()) return NONE;
        List<Birthday> out = new ArrayList<>();
        for (String entry : entries) {
            if (entry == null) continue;
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String date = entry.substring(0, eq).trim();
            String name = Cards.cleanLabel(entry.substring(eq + 1), "");
            if (name.isEmpty()) continue;
            try {
                if (date.length() == 10) {
                    LocalDate d = LocalDate.parse(date);
                    out.add(new Birthday(MonthDay.from(d), d.getYear(), name));
                } else if (date.length() == 5) {
                    out.add(new Birthday(MonthDay.parse("--" + date), 0, name));
                }
            } catch (DateTimeParseException e) {
                // left out on its own; the other birthdays still count
            }
        }
        return out.isEmpty() ? NONE : new Birthdays(out);
    }

    /** Whose birthday it is on {@code today}, in config order; empty on any other day. */
    List<Birthday> on(LocalDate today) {
        List<Birthday> out = new ArrayList<>();
        for (Birthday b : all) {
            if (b.on(today)) out.add(b);
        }
        return out;
    }

    boolean any(LocalDate today) {
        for (Birthday b : all) {
            if (b.on(today)) return true;
        }
        return false;
    }

    /**
     * On someone's birthday, the pictures of a wallpaper collection whose file name holds their name
     * ({@code 02-leo-day.jpg}, or {@code 03-maya-leo.jpg} for both), ignoring case, accents and
     * punctuation. Any other day, or when no file name matches, the whole collection.
     */
    static List<File> wallpapersFor(List<Birthday> today, List<File> images) {
        if (today.isEmpty()) return images;
        List<File> out = new ArrayList<>();
        for (File f : images) {
            String file = fold(f.getName());
            for (Birthday b : today) {
                String name = fold(b.name);
                if (!name.isEmpty() && file.contains(name)) {
                    out.add(f);
                    break;
                }
            }
        }
        return out.isEmpty() ? images : out;
    }

    /** Lower case, accents dropped, letters and digits only: "Zoé" and "03-ZOE_día" compare as plain text. */
    static String fold(String s) {
        String plain = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
