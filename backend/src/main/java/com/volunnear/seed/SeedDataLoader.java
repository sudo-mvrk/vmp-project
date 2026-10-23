package com.volunnear.seed;

import com.volunnear.activity.Activity;
import com.volunnear.activity.ActivityRepository;
import com.volunnear.activity.ActivityWeights;
import com.volunnear.activity.Priority;
import com.volunnear.application.ActivityApplication;
import com.volunnear.application.ApplicationOrigin;
import com.volunnear.application.ApplicationRepository;
import com.volunnear.auth.AppUser;
import com.volunnear.auth.AppUserRepository;
import com.volunnear.auth.Role;
import com.volunnear.dictionary.Certification;
import com.volunnear.dictionary.CertificationRepository;
import com.volunnear.dictionary.Skill;
import com.volunnear.dictionary.SkillRepository;
import com.volunnear.organization.Organization;
import com.volunnear.organization.OrganizationMember;
import com.volunnear.organization.OrganizationMemberRepository;
import com.volunnear.organization.OrganizationRepository;
import com.volunnear.volunteer.Volunteer;
import com.volunnear.volunteer.VolunteerAvailability;
import com.volunnear.volunteer.VolunteerCertification;
import com.volunnear.volunteer.VolunteerMapper;
import com.volunnear.volunteer.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Demo and evaluation data (Slice 9, Q33 … Q37), loaded at startup under the profile {@code seed} only.
 * Writes the repositories directly, so it can also create the states the API never produces (an activity that
 * has started, a completed one). Times are relative to now, so the activities are always upcoming.
 * Skips when {@code seed_org1} exists, so a restart adds nothing.
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
@EnableConfigurationProperties(SeedProperties.class)
public class SeedDataLoader implements ApplicationRunner {
    static final String PASSWORD = "password1";
    // Q34: everything lies within this distance of the centre of Prague.
    private static final double CENTER_LAT = 50.08;
    private static final double CENTER_LON = 14.42;
    private static final double MAX_DISTANCE_KM = 25;

    private static final List<String> ORGANIZATION_NAMES = List.of(
            "Prague Food Bank", "Helping Hands Prague", "Green City Volunteers", "Senior Friends", "Kids Club Praha");
    private static final List<String> FIRST_NAMES = List.of(
            "Anna", "Jakub", "Eva", "Tomas", "Lucie", "Martin", "Petra", "David", "Klara", "Ondrej", "Tereza", "Jan");
    private static final List<String> LAST_NAMES = List.of(
            "Novak", "Svoboda", "Dvorak", "Cerny", "Prochazka", "Kucera", "Vesely", "Horak", "Marek", "Pokorny");
    // Daily windows a volunteer may give; activities run between 09:00 and 19:00.
    private static final List<LocalTime[]> SLOTS = List.of(
            new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(14, 0)},
            new LocalTime[]{LocalTime.of(12, 0), LocalTime.of(20, 0)},
            new LocalTime[]{LocalTime.of(8, 0), LocalTime.of(20, 0)});

    private final SeedProperties properties;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final VolunteerRepository volunteerRepository;
    private final ActivityRepository activityRepository;
    private final ApplicationRepository applicationRepository;
    private final SkillRepository skillRepository;
    private final CertificationRepository certificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appUserRepository.existsByUsername("seed_org1")) {
            log.info("Seed data already present; nothing loaded");
            return;
        }
        Random random = new Random(properties.randomSeed());
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        Map<String, Skill> skills = skillRepository.findAll().stream()
                .collect(Collectors.toMap(Skill::getSkillName, Function.identity()));
        Map<String, Certification> certifications = certificationRepository.findAll().stream()
                .collect(Collectors.toMap(Certification::getName, Function.identity()));
        String encodedPassword = passwordEncoder.encode(PASSWORD);

        List<Organization> organizations = new ArrayList<>();
        for (int i = 1; i <= properties.organizations(); i++) {
            organizations.add(organization(i, encodedPassword, random));
        }
        List<Volunteer> volunteers = new ArrayList<>();
        for (int i = 1; i <= properties.volunteers(); i++) {
            volunteers.add(volunteer(i, encodedPassword, random, skills, certifications, now.toLocalDate()));
        }
        List<Activity> upcoming = new ArrayList<>();
        for (int i = 0; i < properties.activities(); i++) {
            upcoming.add(upcomingActivity(i, organizations.get(i % organizations.size()), random, skills, certifications,
                    now.toLocalDate()));
        }
        demoStates(organizations.get(0), volunteers, upcoming, skills, now);

        log.info("Seed data loaded: {} organizations, {} volunteers, {} upcoming activities. Log in as seed_org1 or "
                        + "seed_vol01 with password {}", organizations.size(), volunteers.size(), upcoming.size(), PASSWORD);
    }

    private Organization organization(int index, String encodedPassword, Random random) {
        AppUser user = account("seed_org" + index, Role.ROLE_ORGANIZATION, encodedPassword);
        Organization organization = new Organization();
        organization.setOrganizationName(ORGANIZATION_NAMES.get((index - 1) % ORGANIZATION_NAMES.size())
                + (index > ORGANIZATION_NAMES.size() ? " " + index : ""));
        organization.setDescription("Seed organization for the demo.");
        organization.setLocation(randomPoint(random));
        organization.setCity("Prague");
        organization.setCountryCode("CZ");
        organization = organizationRepository.save(organization);
        memberRepository.save(OrganizationMember.owner(organization.getId(), user.getId()));
        return organization;
    }

    private Volunteer volunteer(int index, String encodedPassword, Random random, Map<String, Skill> skills,
                                Map<String, Certification> certifications, LocalDate today) {
        AppUser user = account("seed_vol%02d".formatted(index), Role.ROLE_VOLUNTEER, encodedPassword);
        List<Skill> allSkills = sorted(skills.values(), Skill::getSkillName);
        Set<Skill> chosenSkills = new HashSet<>(pick(allSkills, 1 + random.nextInt(4), random));
        List<VolunteerCertification> held = new ArrayList<>();
        for (Certification certification : sorted(certifications.values(), Certification::getName)) {
            if (random.nextDouble() < 0.25) {
                // Half never expire, half are valid for a year.
                held.add(new VolunteerCertification(certification, random.nextBoolean() ? null : today.plusYears(1)));
            }
        }
        List<VolunteerAvailability> availability = new ArrayList<>();
        for (DayOfWeek day : pick(List.of(DayOfWeek.values()), 3 + random.nextInt(3), random)) {
            LocalTime[] slot = SLOTS.get(random.nextInt(SLOTS.size()));
            availability.add(new VolunteerAvailability(day, slot[0], slot[1]));
        }
        availability.sort(Comparator.comparing(VolunteerAvailability::getDayOfWeek));
        Volunteer volunteer = Volunteer.builder()
                .appUser(user)
                .firstName(FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size())))
                .lastName(LAST_NAMES.get(random.nextInt(LAST_NAMES.size())))
                .dateOfBirth(LocalDate.of(1970 + random.nextInt(36), 1 + random.nextInt(12), 1 + random.nextInt(28)))
                .bio("Seed volunteer for the demo.")
                .location(randomPoint(random))
                .locationName("Prague")
                .countryCode("CZ")
                .city("Prague")
                .radius(5 + random.nextInt(46))
                .maxActiveAssignments(1 + random.nextInt(3))
                .skills(chosenSkills)
                .certifications(held)
                .availability(availability)
                .build();
        return volunteerRepository.save(volunteer);
    }

    /** One of the templates, in the next 1 … 14 days, between 09:00 and 19:00. */
    private Activity upcomingActivity(int index, Organization organization, Random random, Map<String, Skill> skills,
                                      Map<String, Certification> certifications, LocalDate today) {
        ActivityTemplate template = ActivityTemplate.ALL.get(index % ActivityTemplate.ALL.size());
        LocalDate day = today.plusDays(1 + random.nextInt(14));
        LocalTime start = LocalTime.of(9 + random.nextInt(7), 0);
        Activity activity = activity(organization, template, skills, certifications, random,
                day.atTime(start), day.atTime(start.plusHours(2 + random.nextInt(3))));
        activity.setPriority(Priority.values()[random.nextInt(Priority.values().length)]);
        activity.setSpotsNeeded(1 + random.nextInt(5));
        // Q34: a couple of activities override the global weights (Q12), one favouring distance, one skills.
        if (index == 1) {
            activity.setWeights(new ActivityWeights(0.6, 0.2, 0.2));
        } else if (index == 4) {
            activity.setWeights(new ActivityWeights(0.2, 0.6, 0.2));
        }
        activity.publish();
        return activityRepository.save(activity);
    }

    /**
     * Q35: applications in several states, one activity that has started and one completed with a history.
     * Volunteers are chosen by index, never randomly, so capacity (at least 1 each) and spots always hold.
     * The first upcoming activity has no required certification (template 0), so availability decides who fits.
     */
    private void demoStates(Organization demoOrganization, List<Volunteer> volunteers, List<Activity> upcoming,
                            Map<String, Skill> skills, LocalDateTime now) {
        Activity first = upcoming.get(0);
        Activity second = upcoming.get(1);
        first.setSpotsNeeded(Math.max(first.getSpotsNeeded(), 2));
        second.setSpotsNeeded(Math.max(second.getSpotsNeeded(), 2));
        application(volunteers.get(0), first, now, true);
        application(volunteers.get(2), first, now, false);
        application(volunteers.get(1), second, now, true);
        application(volunteers.get(3), second, now, false);
        // The demo ranks volunteers for the first activity and shows seed_vol08's recommendations: whatever the
        // weekday, seed_vol08 … seed_vol12 are free that day, and a 50 km radius reaches anywhere in the 25 km disc.
        DayOfWeek demoDay = first.getStartsAt().getDayOfWeek();
        for (Volunteer volunteer : volunteers.subList(7, 12)) {
            volunteer.getAvailability().removeIf(slot -> slot.getDayOfWeek() == demoDay);
            volunteer.getAvailability().add(new VolunteerAvailability(demoDay, LocalTime.of(8, 0), LocalTime.of(20, 0)));
            volunteer.setRadius(50);
        }

        // Started yesterday and still OPEN: the organization completes it in the demo (Q8a).
        LocalDate yesterday = now.toLocalDate().minusDays(1);
        Activity started = pastActivity(demoOrganization, ActivityTemplate.ALL.get(0), skills, yesterday, false);
        application(volunteers.get(4), started, now, true);
        application(volunteers.get(5), started, now, true);

        // Completed a week ago: its accepted volunteers have a history (Q8e) and use no capacity (Q16).
        Activity completed = pastActivity(demoOrganization, ActivityTemplate.ALL.get(2), skills,
                now.toLocalDate().minusDays(7), true);
        for (int i : new int[]{0, 1, 6}) {
            application(volunteers.get(i), completed, now, true);
        }
    }

    // Skills only: the accepted volunteers were picked by index and need not hold the template's certifications.
    private Activity pastActivity(Organization organization, ActivityTemplate template, Map<String, Skill> skills,
                                  LocalDate day, boolean completed) {
        Activity activity = activity(organization, template, skills, Map.of(), null,
                day.atTime(9, 0), day.atTime(13, 0));
        activity.setSpotsNeeded(3);
        activity.publish();
        if (completed) {
            activity.complete();
        }
        return activityRepository.save(activity);
    }

    private Activity activity(Organization organization, ActivityTemplate template, Map<String, Skill> skills,
                              Map<String, Certification> certifications, Random randomOrNull,
                              LocalDateTime startsAt, LocalDateTime endsAt) {
        Activity activity = new Activity(organization);
        activity.setTitle(template.title());
        activity.setShortDescription(template.description());
        activity.setLocation(randomOrNull == null ? point(CENTER_LAT, CENTER_LON) : randomPoint(randomOrNull));
        activity.setCity("Prague");
        activity.setStartsAt(startsAt);
        activity.setEndsAt(endsAt);
        template.skills().stream().map(skills::get).filter(Objects::nonNull).forEach(activity.getRequiredSkills()::add);
        template.certifications().stream().map(certifications::get).filter(Objects::nonNull)
                .forEach(activity.getRequiredCertifications()::add);
        return activity;
    }

    private void application(Volunteer volunteer, Activity activity, LocalDateTime now, boolean accepted) {
        ActivityApplication application = new ActivityApplication(volunteer, activity, ApplicationOrigin.APPLICATION,
                min(now, activity.getStartsAt()).minusDays(2));
        if (accepted) {
            application.accept(min(now, activity.getStartsAt()).minusDays(1));
        }
        applicationRepository.save(application);
    }

    private AppUser account(String username, Role role, String encodedPassword) {
        return appUserRepository.save(AppUser.builder()
                .username(username)
                .email(username + "@example.com")
                .password(encodedPassword)
                .roles(EnumSet.of(role))
                .build());
    }

    /** Uniform over the disc of {@link #MAX_DISTANCE_KM} around the centre. */
    private static Point randomPoint(Random random) {
        double distanceKm = MAX_DISTANCE_KM * Math.sqrt(random.nextDouble());
        double angle = 2 * Math.PI * random.nextDouble();
        double lat = CENTER_LAT + distanceKm * Math.cos(angle) / 111.32;
        double lon = CENTER_LON + distanceKm * Math.sin(angle) / (111.32 * Math.cos(Math.toRadians(CENTER_LAT)));
        return point(lat, lon);
    }

    private static Point point(double lat, double lon) {
        return VolunteerMapper.GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
    }

    private static <T> List<T> pick(List<T> from, int count, Random random) {
        List<T> copy = new ArrayList<>(from);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(count, copy.size()));
    }

    // Sorted before random picks: the order of findAll is not guaranteed, and the same seed must give the same data.
    private static <T> List<T> sorted(Collection<T> items, Function<T, String> key) {
        return items.stream().sorted(Comparator.comparing(key)).toList();
    }

    private static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
