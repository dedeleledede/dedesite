package com.zavan.dedesite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zavan.dedesite.model.Comet;
import com.zavan.dedesite.model.Orbit;
import com.zavan.dedesite.model.Star;
import com.zavan.dedesite.model.StarSystem;
import com.zavan.dedesite.model.User;
import com.zavan.dedesite.config.LegacyPulsarMigration;
import com.zavan.dedesite.repository.CometRepository;
import com.zavan.dedesite.repository.OrbitRepository;
import com.zavan.dedesite.repository.StarRepository;
import com.zavan.dedesite.repository.StarSystemRepository;
import com.zavan.dedesite.repository.UserRepository;
import com.zavan.dedesite.service.CometService;
import com.zavan.dedesite.service.ChartCourseService;
import com.zavan.dedesite.service.ObservatorySearchService;
import com.zavan.dedesite.service.ObservatoryService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.observatory.encryption-key=test-observatory-key-for-local-builds")
@AutoConfigureMockMvc
class ObservatoryPrivacyTests {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired StarSystemRepository starSystemRepository;
    @Autowired StarRepository starRepository;
    @Autowired OrbitRepository orbitRepository;
    @Autowired CometRepository cometRepository;
    @Autowired ObservatorySearchService observatorySearchService;
    @Autowired ObservatoryService observatoryService;
    @Autowired CometService cometService;
    @Autowired ChartCourseService chartCourseService;
    @Autowired LegacyPulsarMigration legacyPulsarMigration;
    @Autowired JdbcTemplate jdbcTemplate;

    private User alice;
    private User bobby;

    @BeforeEach
    void setUp() {
        cometRepository.deleteAll();
        starRepository.deleteAll();
        orbitRepository.deleteAll();
        starSystemRepository.deleteAll();
        userRepository.deleteAll();
        alice = saveUser("alice");
        bobby = saveUser("bobby");
    }

    @Test
    void observatoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/observatory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void userCannotAccessAnotherUsersStarSystem() throws Exception {
        StarSystem starSystem = saveStarSystem(bobby, "Bob Project");

        mockMvc.perform(get("/observatory/star-systems/" + starSystem.getPublicId()).with(user("alice").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAnotherUsersStar() throws Exception {
        Star star = saveStar(bobby, "Bob Task", null);

        mockMvc.perform(get("/observatory/stars/" + star.getPublicId() + "/edit").with(user("alice").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAnotherUsersOrbit() throws Exception {
        Orbit orbit = saveOrbit(bobby, "Bob Work");

        mockMvc.perform(get("/observatory/orbits/" + orbit.getPublicId() + "/edit").with(user("alice").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAccessAnotherUsersComet() throws Exception {
        Comet comet = saveComet(bobby, "Bob Exam");

        mockMvc.perform(get("/observatory/comets/" + comet.getPublicId() + "/edit").with(user("alice").roles("USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void encryptedFieldsAreNotStoredAsPlaintextAndDecryptOnLoad() {
        Orbit orbit = saveOrbit(alice, "Study Computational Physics");
        String raw = jdbcTemplate.queryForObject("select encrypted_title from orbits where id = ?", String.class, orbit.getId());

        assertThat(raw).startsWith("v1:");
        assertThat(raw).doesNotContain("Study Computational Physics");
        assertThat(orbitRepository.findByIdAndUser(orbit.getId(), alice).orElseThrow().getTitle())
                .isEqualTo("Study Computational Physics");
    }

    @Test
    void searchOnlyMatchesAuthenticatedUsersDecryptedData() {
        saveStar(alice, "physics worksheet", null);
        saveStar(bobby, "private violin plan", null);

        assertThat(observatorySearchService.search(alice, "physics")).hasSize(1);
        assertThat(observatorySearchService.search(alice, "violin")).isEmpty();
    }

    @Test
    void reminderQueriesUsePlaintextReminderMetadata() {
        Comet comet = saveComet(alice, "Hidden Reminder");
        comet.setRemindAt(LocalDateTime.now().minusMinutes(5));
        cometRepository.saveAndFlush(comet);

        assertThat(cometService.remindersDue(alice, LocalDateTime.now()))
                .extracting(Comet::getTitle)
                .contains("Hidden Reminder");
    }

    @Test
    void skyMapUsesPlaintextSchedulingMetadata() {
        Star star = saveStar(alice, "Hidden Scheduled Star", null);
        star.setScheduledStart(LocalDate.now().atTime(9, 0));
        star.setScheduledEnd(LocalDate.now().atTime(10, 0));
        starRepository.saveAndFlush(star);

        assertThat(observatoryService.skyMap(alice).scheduledWork().toMinutes()).isEqualTo(60);
    }

    @Test
    void skyMapRendersSelectedDayTimelineWithScheduledStars() throws Exception {
        Star star = saveStar(alice, "Visible Scheduled Star", null);
        star.setScheduledStart(LocalDate.now().atTime(9, 0));
        star.setScheduledEnd(LocalDate.now().atTime(10, 0));
        starRepository.saveAndFlush(star);

        mockMvc.perform(get("/observatory/sky-map")
                        .param("day", LocalDate.now().toString())
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Day Timeline")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Visible Scheduled Star")));
    }

    @Test
    void scheduledStarCreatedThroughFormAppearsInSkyMap() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(post("/observatory/stars")
                        .with(user("alice").roles("USER"))
                        .with(csrf())
                        .param("title", "Form Scheduled Star")
                        .param("status", "READY")
                        .param("priority", "MEDIUM")
                        .param("scheduledStart", today + "T13:00")
                        .param("scheduledEnd", today + "T14:00"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/observatory/sky-map")
                        .param("day", today.toString())
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Form Scheduled Star")));
    }

    @Test
    void observatoryUiNoLongerUsesConstellationAndRoutesUseStarSystems() throws Exception {
        mockMvc.perform(get("/observatory/star-systems").with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Constellation"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Star Systems")));
    }

    @Test
    void appsPageOffersObservatoryAccess() throws Exception {
        mockMvc.perform(get("/apps").with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Open Observatory")));
    }

    @Test
    void anonymousPresenceHeartbeatWorksWithoutCsrfToken() throws Exception {
        mockMvc.perform(post("/presence/ping")
                        .header("User-Agent", "Mozilla/5.0 Firefox/128.0"))
                .andExpect(status().isNoContent());
    }

    @Test
    void pulsarsAreStoredAsFlexibleOrbits() {
        Orbit pulsar = savePulsar(alice, "Physics review", null);

        assertThat(orbitRepository.findByPublicIdAndUser(pulsar.getPublicId(), alice).orElseThrow())
                .satisfies(stored -> {
                    assertThat(stored.getKind()).isEqualTo(Orbit.Kind.PULSAR);
                    assertThat(stored.getFlexibility()).isEqualTo(Orbit.Flexibility.FLEXIBLE);
                    assertThat(stored.getTargetMinutesPerWeek()).isEqualTo(60);
                });
    }

    @Test
    void chartCoursePreviewDoesNotSaveUntilSelectedSuggestionIsAccepted() {
        Star star = saveStar(alice, "Schedule worksheet", null);
        star.setStatus(Star.Status.READY);
        star.setEstimatedMinutes(60);
        starRepository.saveAndFlush(star);

        var suggestion = chartCourseService.preview(alice, LocalDate.now()).suggestions().stream()
                .filter(item -> item.sourceId().equals(star.getPublicId()))
                .findFirst()
                .orElseThrow();
        assertThat(starRepository.findByIdAndUser(star.getId(), alice).orElseThrow().getScheduledStart()).isNull();

        chartCourseService.accept(alice, LocalDate.now(), java.util.List.of(suggestion.key()));

        assertThat(starRepository.findByIdAndUser(star.getId(), alice).orElseThrow())
                .satisfies(scheduled -> {
                    assertThat(scheduled.getScheduledStart()).isEqualTo(suggestion.start());
                    assertThat(scheduled.getScheduledEnd()).isEqualTo(suggestion.end());
                    assertThat(scheduled.getStatus()).isEqualTo(Star.Status.SCHEDULED);
                });
    }

    @Test
    void acceptedLinkedPulsarSuggestionCreatesScheduledStar() {
        StarSystem starSystem = saveStarSystem(alice, "Physics");
        Orbit pulsar = savePulsar(alice, "Study physics", starSystem);

        var suggestion = chartCourseService.preview(alice, LocalDate.now()).suggestions().stream()
                .filter(item -> item.sourceId().equals(pulsar.getPublicId()))
                .findFirst()
                .orElseThrow();
        chartCourseService.accept(alice, LocalDate.now(), java.util.List.of(suggestion.key()));

        assertThat(starRepository.findByUserOrderByCreatedAtDesc(alice))
                .anySatisfy(star -> {
                    assertThat(star.getTitle()).isEqualTo("Pulsar: Study physics");
                    assertThat(star.getStarSystem().getId()).isEqualTo(starSystem.getId());
                    assertThat(star.getStatus()).isEqualTo(Star.Status.SCHEDULED);
                });
    }

    @Test
    void chartCourseOnlySuggestsAuthenticatedUsersDataAndRespectsDailyCap() {
        saveStar(alice, "Alice work", null).setEstimatedMinutes(240);
        Star bobbyStar = saveStar(bobby, "Bobby private work", null);
        bobbyStar.setEstimatedMinutes(60);
        starRepository.saveAndFlush(bobbyStar);

        var suggestions = chartCourseService.preview(alice, LocalDate.now()).suggestions();

        assertThat(suggestions).allMatch(suggestion -> !suggestion.title().contains("Bobby"));
        assertThat(suggestions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        suggestion -> suggestion.start().toLocalDate(),
                        java.util.stream.Collectors.summingInt(ChartCourseService.ScheduleSuggestion::durationMinutes)))
                .values())
                .allMatch(minutes -> minutes <= 180);
        assertThat(suggestions)
                .filteredOn(suggestion -> suggestion.energyType() == StarSystem.EnergyType.DEEP_FOCUS)
                .allMatch(suggestion -> suggestion.start().toLocalTime().isBefore(LocalTime.of(21, 0)));
    }

    @Test
    void chartCoursePageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/observatory/chart-course")
                        .param("generate", "true")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chart Course")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Generate Course")));
    }

    @Test
    void launchWindowsPageRendersForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/observatory/launch-windows")
                        .with(user("alice").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Launch Windows")));
    }

    @Test
    void legacyPulsarMigrationCopiesCiphertextIntoOrbitsWithoutDecrypting() throws Exception {
        Orbit encryptedSource = savePulsar(alice, "Legacy encrypted study", null);
        String encryptedTitle = jdbcTemplate.queryForObject("select encrypted_title from orbits where id = ?", String.class, encryptedSource.getId());
        String encryptedDescription = jdbcTemplate.queryForObject("select encrypted_description from orbits where id = ?", String.class, encryptedSource.getId());
        orbitRepository.delete(encryptedSource);
        orbitRepository.flush();
        java.util.UUID legacyPublicId = java.util.UUID.randomUUID();
        jdbcTemplate.execute("""
                create table pulsars (
                    public_id uuid not null,
                    user_id bigint not null,
                    encrypted_title varchar(255) not null,
                    encrypted_subject text,
                    target_minutes_per_week integer,
                    active boolean not null,
                    created_at timestamp,
                    updated_at timestamp,
                    encryption_key_version integer
                )
                """);
        try {
            jdbcTemplate.update("""
                    insert into pulsars (
                        public_id, user_id, encrypted_title, encrypted_subject,
                        target_minutes_per_week, active, created_at, updated_at, encryption_key_version
                    ) values (?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp, ?)
                    """, legacyPublicId, alice.getId(), encryptedTitle, encryptedDescription, 120, true, 1);

            legacyPulsarMigration.run(null);

            assertThat(jdbcTemplate.queryForObject(
                    "select encrypted_title from orbits where public_id = ?",
                    String.class,
                    legacyPublicId))
                    .isEqualTo(encryptedTitle)
                    .doesNotContain("Legacy encrypted study");
        } finally {
            jdbcTemplate.update("delete from orbits where public_id = ?", legacyPublicId);
            jdbcTemplate.execute("drop table pulsars");
        }
    }

    @Test
    void launchWindowsSubtractOwnedFixedOrbitScheduledStarAndTimedCometButNotPulsarsOrOtherUsersData() {
        LocalDate monday = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Orbit orbit = saveOrbit(alice, "Morning lock");
        orbit.setStartTime(LocalTime.of(8, 0));
        orbit.setEndTime(LocalTime.of(9, 0));
        orbitRepository.saveAndFlush(orbit);

        Star scheduled = saveStar(alice, "Scheduled block", null);
        scheduled.setScheduledStart(monday.atTime(10, 0));
        scheduled.setScheduledEnd(monday.atTime(11, 0));
        starRepository.saveAndFlush(scheduled);

        Comet comet = saveComet(alice, "Appointment");
        comet.setDate(monday);
        comet.setStartTime(LocalTime.of(12, 0));
        comet.setEndTime(LocalTime.of(13, 0));
        cometRepository.saveAndFlush(comet);

        Orbit bobbyOrbit = saveOrbit(bobby, "Bobby private block");
        bobbyOrbit.setStartTime(LocalTime.of(13, 0));
        bobbyOrbit.setEndTime(LocalTime.of(14, 0));
        orbitRepository.saveAndFlush(bobbyOrbit);
        savePulsar(alice, "Flexible study target", null);

        var mondayWindows = observatoryService.findLaunchWindows(alice, monday, monday).stream().toList();

        assertThat(mondayWindows).noneMatch(window -> overlaps(window.start(), window.end(), monday.atTime(8, 0), monday.atTime(9, 0)));
        assertThat(mondayWindows).noneMatch(window -> overlaps(window.start(), window.end(), monday.atTime(10, 0), monday.atTime(11, 0)));
        assertThat(mondayWindows).noneMatch(window -> overlaps(window.start(), window.end(), monday.atTime(12, 0), monday.atTime(13, 0)));
        assertThat(mondayWindows).anyMatch(window -> !window.start().isAfter(monday.atTime(13, 0))
                && !window.end().isBefore(monday.atTime(14, 0)));
    }

    @Test
    void unlinkedPulsarSuggestionCannotBeAcceptedSilently() {
        Orbit pulsar = savePulsar(alice, "Needs a system", null);
        var suggestion = chartCourseService.preview(alice, LocalDate.now()).suggestions().stream()
                .filter(item -> item.sourceId().equals(pulsar.getPublicId()))
                .findFirst()
                .orElseThrow();

        assertThat(suggestion.acceptable()).isFalse();
        assertThat(chartCourseService.accept(alice, LocalDate.now(), java.util.List.of(suggestion.key()))).isZero();
        assertThat(starRepository.findByUserOrderByCreatedAtDesc(alice)).isEmpty();
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole("USER");
        return userRepository.saveAndFlush(user);
    }

    private StarSystem saveStarSystem(User user, String name) {
        StarSystem starSystem = new StarSystem();
        starSystem.setUser(user);
        starSystem.setName(name);
        starSystem.setDescription(name + " description");
        return starSystemRepository.saveAndFlush(starSystem);
    }

    private Star saveStar(User user, String title, StarSystem starSystem) {
        Star star = new Star();
        star.setUser(user);
        star.setTitle(title);
        star.setDescription(title + " description");
        star.setStarSystem(starSystem);
        return starRepository.saveAndFlush(star);
    }

    private Orbit saveOrbit(User user, String title) {
        Orbit orbit = new Orbit();
        orbit.setUser(user);
        orbit.setTitle(title);
        orbit.setDescription(title + " description");
        orbit.setDayOfWeek(DayOfWeek.MONDAY);
        orbit.setStartTime(LocalTime.of(8, 0));
        orbit.setEndTime(LocalTime.of(9, 0));
        return orbitRepository.saveAndFlush(orbit);
    }

    private Orbit savePulsar(User user, String title, StarSystem starSystem) {
        Orbit pulsar = new Orbit();
        pulsar.setUser(user);
        pulsar.setTitle(title);
        pulsar.setDescription(title + " description");
        pulsar.setKind(Orbit.Kind.PULSAR);
        pulsar.setFlexibility(Orbit.Flexibility.FLEXIBLE);
        pulsar.setTargetMinutesPerWeek(60);
        pulsar.setMinimumSessionMinutes(30);
        pulsar.setMaximumSessionMinutes(60);
        pulsar.setEnergyType(StarSystem.EnergyType.STUDY);
        pulsar.setStarSystem(starSystem);
        pulsar.setAutoSchedule(true);
        return orbitRepository.saveAndFlush(pulsar);
    }

    private boolean overlaps(LocalDateTime firstStart, LocalDateTime firstEnd, LocalDateTime secondStart, LocalDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private Comet saveComet(User user, String title) {
        Comet comet = new Comet();
        comet.setUser(user);
        comet.setTitle(title);
        comet.setDescription(title + " description");
        comet.setDate(LocalDate.now());
        return cometRepository.saveAndFlush(comet);
    }
}
