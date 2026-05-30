package com.zavan.dedesite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zavan.dedesite.model.VisitorIdentity;
import com.zavan.dedesite.repository.VisitorIdentityRepository;
import com.zavan.dedesite.service.SiteStatusService;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SiteStatusServiceTests {

    @Test
    void humanVisitorKeepsNumberThroughCookie() {
        SiteStatusService service = service();
        MockHttpServletRequest firstRequest = humanRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        long firstNumber = service.getVisitorNumber(firstRequest, firstResponse);
        Cookie visitorCookie = firstResponse.getCookie("dedesite_visitor");

        MockHttpServletRequest nextRequest = humanRequest();
        nextRequest.setCookies(visitorCookie);
        long nextNumber = service.getVisitorNumber(nextRequest, new MockHttpServletResponse());

        assertThat(visitorCookie).isNotNull();
        assertThat(nextNumber).isEqualTo(firstNumber);
    }

    @Test
    void scanBotsDoNotReceiveOrConsumeVisitorNumbers() {
        SiteStatusService service = service();
        MockHttpServletRequest botRequest = new MockHttpServletRequest();
        botRequest.addHeader("User-Agent", "Mozilla/5.0 compatible; scannerbot/1.0");
        MockHttpServletResponse botResponse = new MockHttpServletResponse();

        assertThat(service.getVisitorNumber(botRequest, botResponse)).isZero();
        assertThat(botResponse.getCookie("dedesite_visitor")).isNull();
        assertThat(service.getVisitorNumber(humanRequest(), new MockHttpServletResponse())).isEqualTo(1);
    }

    @Test
    void visitorNumberSurvivesServiceRestart() {
        VisitorIdentityRepository repository = repository();
        SiteStatusService firstService = new SiteStatusService(repository);
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();

        long firstNumber = firstService.getVisitorNumber(humanRequest(), firstResponse);
        MockHttpServletRequest returningRequest = humanRequest();
        returningRequest.setCookies(firstResponse.getCookie("dedesite_visitor"));

        assertThat(new SiteStatusService(repository).getVisitorNumber(returningRequest, new MockHttpServletResponse()))
                .isEqualTo(firstNumber);
    }

    @Test
    void onlineVisitorsAreAnonymousAndMarkCurrentBrowser() {
        VisitorIdentityRepository repository = repository();
        SiteStatusService service = new SiteStatusService(repository);
        long visitorNumber = service.getVisitorNumber(humanRequest(), new MockHttpServletResponse());

        assertThat(service.getOnlineVisitors(visitorNumber))
                .containsExactly(new SiteStatusService.OnlineVisitor(visitorNumber, true));
    }

    private MockHttpServletRequest humanRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 Firefox/128.0");
        return request;
    }

    private SiteStatusService service() {
        return new SiteStatusService(repository());
    }

    private VisitorIdentityRepository repository() {
        VisitorIdentityRepository repository = mock(VisitorIdentityRepository.class);
        Map<String, VisitorIdentity> identities = new HashMap<>();
        AtomicLong sequence = new AtomicLong();
        when(repository.findByVisitorToken(any())).thenAnswer(invocation ->
                Optional.ofNullable(identities.get(invocation.getArgument(0))));
        when(repository.findAllByLastSeenAtAfterOrderByIdAsc(any())).thenAnswer(invocation -> identities.values()
                .stream()
                .filter(identity -> identity.getLastSeenAt().isAfter(invocation.getArgument(0)))
                .sorted(java.util.Comparator.comparing(VisitorIdentity::getId))
                .toList());
        when(repository.save(any())).thenAnswer(invocation -> {
            VisitorIdentity identity = invocation.getArgument(0);
            if (identity.getId() == null) {
                identity.setId(sequence.incrementAndGet());
            }
            identities.put(identity.getVisitorToken(), identity);
            return identity;
        });
        return repository;
    }
}
