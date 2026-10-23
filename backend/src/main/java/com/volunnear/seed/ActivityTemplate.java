package com.volunnear.seed;

import java.util.List;

/** Titles and requirements of the seed activities (Q34); names match the dictionaries seeded by V3. */
record ActivityTemplate(String title, String description, List<String> skills, List<String> certifications) {
    static final List<ActivityTemplate> ALL = List.of(
            new ActivityTemplate("Sort food donations", "Sort and pack donated food for families in need.",
                    List.of("Food distribution", "Warehouse work"), List.of()),
            new ActivityTemplate("Deliver groceries to seniors", "Drive weekly grocery packages to elderly people.",
                    List.of("Driving", "Elderly care"), List.of("Driving licence B")),
            new ActivityTemplate("Homework club", "Help primary school children with their homework.",
                    List.of("Tutoring", "Childcare"), List.of("Child protection check")),
            new ActivityTemplate("Charity run first aid", "Stand by at the first aid point of a charity run.",
                    List.of("First aid", "Nursing"), List.of("First aid certificate")),
            new ActivityTemplate("Community festival setup", "Build stands and guide visitors at a district festival.",
                    List.of("Event setup", "Event hosting", "Translation"), List.of()),
            new ActivityTemplate("Website refresh for a shelter", "Update the website and social media of an animal shelter.",
                    List.of("Web development", "Social media"), List.of()),
            new ActivityTemplate("Senior centre afternoon", "Spend an afternoon talking and playing games with seniors.",
                    List.of("Elderly care", "Mental health support"), List.of()),
            new ActivityTemplate("Refugee help desk", "Translate and fill in forms at a help desk.",
                    List.of("Translation", "Data entry"), List.of()),
            new ActivityTemplate("Soup kitchen shift", "Cook and serve meals at a soup kitchen.",
                    List.of("Food distribution", "Event hosting"), List.of("Food hygiene certificate")),
            new ActivityTemplate("Warehouse inventory", "Count and record stock in a charity warehouse.",
                    List.of("Warehouse work", "Data entry", "Driving", "Event setup"), List.of()));
}
