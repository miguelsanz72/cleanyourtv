package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class BirthdaysTest {

    private static Birthdays of(String... entries) {
        return Birthdays.parse(Arrays.asList(entries));
    }

    private static String names(List<Birthdays.Birthday> list) {
        StringBuilder b = new StringBuilder();
        for (Birthdays.Birthday d : list) b.append(b.length() == 0 ? "" : ",").append(d.name);
        return b.toString();
    }

    private static List<File> files(String... names) {
        List<File> out = new ArrayList<>();
        for (String n : names) out.add(new File("/w", n));
        return out;
    }

    private static String fileNames(List<File> list) {
        StringBuilder b = new StringBuilder();
        for (File f : list) b.append(b.length() == 0 ? "" : ",").append(f.getName());
        return b.toString();
    }

    private static final List<File> SCENES = files("01-maya-day.jpg", "02-leo-day.jpg",
            "03-maya-leo-sunset.jpg", "04-maya-night.jpg", "05-LEO_night.jpg");

    @Test
    public void comesBackEveryYear() {
        Birthdays b = of("2020-03-03=Leo");
        assertTrue(b.any(LocalDate.of(2026, 3, 3)));
        assertTrue(b.any(LocalDate.of(2040, 3, 3)));
        assertFalse(b.any(LocalDate.of(2026, 3, 2)));
        assertFalse(b.any(LocalDate.of(2026, 3, 4)));
    }

    @Test
    public void ageFromTheYearOfBirth() {
        assertEquals(6, of("2020-03-03=Leo").all.get(0).age(LocalDate.of(2026, 3, 3)));
        Birthdays.Birthday maya = of("2018-05-12=Maya").all.get(0);
        assertEquals(8, maya.age(LocalDate.of(2026, 5, 12)));
        assertEquals(9, maya.age(LocalDate.of(2027, 5, 12)));
    }

    @Test
    public void noYearMeansNoAge() {
        Birthdays.Birthday b = of("05-12=Maya").all.get(0);
        assertTrue(b.on(LocalDate.of(2026, 5, 12)));
        assertEquals(0, b.age(LocalDate.of(2026, 5, 12)));
    }

    @Test
    public void onlyTodaysNamesInConfigOrder() {
        Birthdays b = of("2018-05-12=Maya", "2020-03-03=Leo", "03-03=Grandma");
        assertEquals("Leo,Grandma", names(b.on(LocalDate.of(2026, 3, 3))));
        assertEquals("Maya", names(b.on(LocalDate.of(2026, 5, 12))));
        assertEquals("", names(b.on(LocalDate.of(2026, 10, 21))));
    }

    @Test
    public void leapDayIsKeptOnTheTwentyEighth() {
        Birthdays b = of("2016-02-29=Leap");
        assertTrue(b.any(LocalDate.of(2027, 2, 28)));
        assertFalse(b.any(LocalDate.of(2028, 2, 28)));
        assertTrue(b.any(LocalDate.of(2028, 2, 29)));
    }

    @Test
    public void malformedEntriesAreLeftOutOnTheirOwn() {
        Birthdays b = of("2020-13-03=Wrong", "=NoDate", "03-03=", "3/3=Slash", " 2020-03-03 =  Leo  ");
        assertEquals(1, b.all.size());
        assertEquals("Leo", b.all.get(0).name);
        assertSame(Birthdays.NONE, of("nonsense"));
        assertSame(Birthdays.NONE, Birthdays.parse(Collections.emptyList()));
        assertSame(Birthdays.NONE, Birthdays.parse(null));
    }

    @Test
    public void birthdayKeepsOnlyThatPersonsPictures() {
        Birthdays b = of("2018-05-12=Maya", "2020-03-03=Leo");
        assertEquals("02-leo-day.jpg,03-maya-leo-sunset.jpg,05-LEO_night.jpg",
                fileNames(Birthdays.wallpapersFor(b.on(LocalDate.of(2026, 3, 3)), SCENES)));
        assertEquals("01-maya-day.jpg,03-maya-leo-sunset.jpg,04-maya-night.jpg",
                fileNames(Birthdays.wallpapersFor(b.on(LocalDate.of(2026, 5, 12)), SCENES)));
    }

    @Test
    public void anyOtherDayOrNoMatchKeepsTheWholeCollection() {
        Birthdays b = of("2018-05-12=Maya", "01-02=Zoé");
        assertSame(SCENES, Birthdays.wallpapersFor(b.on(LocalDate.of(2026, 10, 21)), SCENES));
        assertSame(SCENES, Birthdays.wallpapersFor(b.on(LocalDate.of(2027, 1, 2)), SCENES));
    }

    @Test
    public void namesMatchFileNamesWithoutCaseOrAccents() {
        Birthdays b = of("01-02=Zoé");
        assertEquals("zoe-beach.jpg", fileNames(Birthdays.wallpapersFor(b.on(LocalDate.of(2027, 1, 2)),
                files("zoe-beach.jpg", "sea.jpg"))));
        assertEquals("mariajose", Birthdays.fold("María José"));
    }

    @Test
    public void nameIsCleanedLikeALabel() {
        assertEquals("Ma ya", of("05-12=  Ma\u202e  ya ").all.get(0).name);
    }
}
